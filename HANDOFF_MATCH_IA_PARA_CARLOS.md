# GeoPatitas — Handoff Backend: Sistema de Match por IA + PostGIS
> Para: Carlos (Backend)
> De: Simón (Frontend)
> Fecha: 18 de mayo de 2026

---

## Contexto

El endpoint `GET /api/v1/pets/match?q=` ya existe y funciona a nivel técnico (pgvector + HuggingFace `sentence-transformers/all-MiniLM-L6-v2`), pero tiene problemas de calidad que hacen que los resultados sean incorrectos o sin sentido para el usuario.

El frontend ya consume este endpoint desde la sección **"Match de reportes"** en `/mis-reportes`.

---

## Problema 1 — No se devuelve el porcentaje de similitud

### Lo que pasa hoy
El endpoint devuelve una lista de mascotas ordenadas por similitud, pero **sin incluir el score** en la respuesta. El frontend no puede mostrar "85% de coincidencia" porque no recibe ese dato.

### Lo que se muestra en el frontend (subóptimo)
```
#1 en relevancia
#2 en relevancia
```

### Lo que debería mostrarse
```
92% coincidencia   ← badge verde
74% coincidencia   ← badge amarillo
41% coincidencia   ← badge gris
```

### Fix requerido
Incluir el score de similitud en cada objeto de la respuesta. Puede llamarse `score` (0.0–1.0) o `porcentajeSimilitud` (0–100). El frontend maneja ambos:

```json
[
  {
    "id": "uuid-abc",
    "tipoReporte": "PERDIDO",
    "nombre": "Max",
    "especie": "Perro",
    "raza": "Labrador",
    "color": "Dorado",
    "tamano": "Grande",
    "descripcion": "Perro dorado muy dócil...",
    "fotos": ["https://..."],
    "latitud": -33.04,
    "longitud": -71.52,
    "estado": "ACTIVO",
    "fechaReporte": "2026-05-17T20:00:00",
    "porcentajeSimilitud": 87.3,   // ← REQUERIDO (score combinado final, ver sección PostGIS)
    "distanciaKm": 2.3             // ← REQUERIDO (ver sección PostGIS)
  }
]
```

---

## Problema 2 — No se filtra por tipo de reporte opuesto

### Lo que pasa hoy
El endpoint busca en **toda la base de datos** sin distinguir PERDIDO/ENCONTRADO. Resultado observado: al buscar coincidencias para un reporte ENCONTRADO (perro marrón), devuelve como #1 a **Stolas**, un gato PERDIDO con descripción textualmente similar.

### Fix requerido
Aceptar parámetro `tipoOpuesto` y filtrar en la query:

```
GET /api/v1/pets/match?q=perro marrón dócil&tipoOpuesto=PERDIDO&lat=-33.04&lng=-71.52
```

El frontend enviará siempre el tipo opuesto:
- Reporte ENCONTRADO → `tipoOpuesto=PERDIDO`
- Reporte PERDIDO    → `tipoOpuesto=ENCONTRADO`

---

## Problema 3 — Sin umbral mínimo de similitud

Filtrar resultados con score combinado menor a **0.30** (30%). Sin esto se devuelve basura.

---

## Problema 4 — Incluye reportes RESUELTO

Agregar `WHERE estado = 'ACTIVO'` en todas las queries de match.

---

## ⭐ Nueva feature: PostGIS — Score combinado semántico + geográfico

### Motivación

El modelo de embeddings trabaja solo con texto y no tiene noción de espacio físico. Un perro encontrado a 500 metros de donde se perdió otro perro debería rankearse mucho más alto que uno encontrado a 40 km, aunque ambas descripciones sean igualmente similares semánticamente.

Supabase ya tiene PostGIS habilitado por defecto — no requiere setup adicional.

### Fórmula del score combinado

```
score_final = α × score_semántico + (1 - α) × score_proximidad
```

Donde:
- `α = 0.6` (peso semántico, ajustable)
- `score_semántico` = similitud coseno pgvector (0.0–1.0)
- `score_proximidad = MAX(0,  1 - distancia_km / radio_max_km)`
  - Radio máximo sugerido: **15 km** (ajustable con parámetro)
  - A 0 km → proximidad = 1.0 (máxima)
  - A 15 km → proximidad = 0.0
  - A >15 km → proximidad = 0.0 (no aporta nada, pero no penaliza)

### Ejemplo de scores

| Caso | Similitud semántica | Distancia | Score proximidad | Score final |
|---|---|---|---|---|
| Mismo barrio, descripción muy similar | 0.92 | 0.3 km | 0.98 | **0.94** |
| Barrio cercano, descripción similar | 0.85 | 3 km | 0.80 | **0.83** |
| Lejos pero descripción casi igual | 0.91 | 14 km | 0.07 | **0.57** |
| Lejos y descripción poco similar | 0.55 | 25 km | 0.00 | **0.33** |

Stolas el gato (descripción textualmente parecida pero probablemente lejos) bajaría del puesto #1 al fondo de la lista.

### Query SQL con pgvector + PostGIS

```sql
SELECT
  p.*,
  -- Score semántico (cosine similarity)
  (1 - (p.embedding <=> :queryEmbedding)) AS semantic_score,

  -- Distancia en km
  ST_Distance(
    ST_SetSRID(ST_MakePoint(p.longitud, p.latitud), 4326)::geography,
    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
  ) / 1000.0 AS distancia_km,

  -- Score de proximidad (0–1, decae linealmente hasta maxRadiusKm)
  GREATEST(0,
    1.0 - ST_Distance(
      ST_SetSRID(ST_MakePoint(p.longitud, p.latitud), 4326)::geography,
      ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
    ) / 1000.0 / :maxRadiusKm
  ) AS proximity_score,

  -- Score combinado final (lo que se expone como porcentajeSimilitud × 100)
  (
    0.6 * (1 - (p.embedding <=> :queryEmbedding)) +
    0.4 * GREATEST(0,
      1.0 - ST_Distance(
        ST_SetSRID(ST_MakePoint(p.longitud, p.latitud), 4326)::geography,
        ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
      ) / 1000.0 / :maxRadiusKm
    )
  ) AS combined_score

FROM pets p
WHERE
  p.tipo_reporte = :tipoOpuesto
  AND p.estado = 'ACTIVO'
  AND p.id <> :petId  -- excluir el propio reporte
  AND ST_DWithin(     -- pre-filtro geográfico eficiente (usa índice GIST)
    ST_SetSRID(ST_MakePoint(p.longitud, p.latitud), 4326)::geography,
    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
    :maxRadiusMeters  -- maxRadiusKm * 1000
  )
  AND (1 - (p.embedding <=> :queryEmbedding)) >= 0.20  -- mínimo semántico

ORDER BY combined_score DESC
LIMIT :limit
```

> **Nota:** `ST_DWithin` con geografía usa el índice GIST automáticamente — es muy eficiente aunque la tabla sea grande.

### Índice recomendado (si no existe)

```sql
CREATE INDEX IF NOT EXISTS pets_location_gist
  ON pets
  USING GIST (ST_SetSRID(ST_MakePoint(longitud, latitud), 4326));
```

---

## Signature del endpoint final esperada

```
GET /api/v1/pets/match
  ?q={descripcion_libre}              (requerido)
  &tipoOpuesto={PERDIDO|ENCONTRADO}   (requerido)
  &lat={latitud}                      (requerido para score geográfico)
  &lng={longitud}                     (requerido para score geográfico)
  &maxRadius=15                       (opcional, km, default: 15)
  &minScore=0.30                      (opcional, default: 0.30)
  &limit=10                           (opcional, default: 10)
```

---

## Campos requeridos en la respuesta (DTO)

```json
{
  // Campos existentes del Pet:
  "id": "uuid",
  "tipoReporte": "PERDIDO",
  "nombre": "Max",
  "especie": "Perro",
  "raza": "Labrador",
  "color": "Dorado",
  "tamano": "Grande",
  "descripcion": "...",
  "fotos": ["https://..."],
  "estado": "ACTIVO",
  "fechaReporte": "2026-05-17T20:00:00",
  "latitud": -33.04,
  "longitud": -71.52,

  // Campos NUEVOS requeridos por el frontend:
  "porcentajeSimilitud": 87.3,   // combined_score × 100, redondeado a 1 decimal
  "distanciaKm": 2.3             // distancia real en km, redondeado a 1 decimal
}
```

---

## Lo que mostrará el frontend con estos datos

### En "Match de reportes" (MyReports)
Cada tarjeta de coincidencia mostrará:
- Badge **"87% coincidencia"** (color según rango: verde ≥80%, amarillo ≥60%, gris <60%)
- Texto **"📍 2.3 km de tu reporte"** debajo de la descripción

### En notificaciones (campana del Navbar)
El mensaje de notificación pasará de:
> *"Tu mascota Max tiene una posible coincidencia del 85% con Perrito sin collar."*

A:
> *"Tu mascota Max tiene una coincidencia del 85% con Perrito sin collar, a 2.3 km de tu reporte."*

Para esto, el sistema de notificaciones también necesita calcular y almacenar `distanciaKm` al momento de crear la alerta (cuando se crea el reporte y se detecta el match automático).

---

## Resumen de todos los cambios requeridos

| # | Problema / Feature | Fix | Prioridad |
|---|---|---|---|
| 1 | Sin `porcentajeSimilitud` en respuesta | Incluir score combinado en DTO | 🔴 Alta |
| 2 | Sin `distanciaKm` en respuesta | Incluir distancia calculada con PostGIS | 🔴 Alta |
| 3 | Sin filtro por tipo opuesto | Parámetro `tipoOpuesto` obligatorio | 🔴 Alta |
| 4 | Sin filtro por estado | `WHERE estado = 'ACTIVO'` | 🟡 Media |
| 5 | Sin umbral mínimo | `minScore` con default 0.30 | 🟡 Media |
| 6 | Score solo semántico | Score combinado pgvector + PostGIS | 🟠 Alta (mejora calidad) |
| 7 | Notificaciones sin distancia | Guardar `distanciaKm` al crear alerta | 🟡 Media |

---

## Cómo lo consume el frontend (referencia actualizada)

```typescript
// MyReports.tsx — openMatches()
const { data } = await api.get('/pets/match', {
  params: {
    q:           pet.descripcion,
    tipoOpuesto: pet.tipoReporte === 'PERDIDO' ? 'ENCONTRADO' : 'PERDIDO',
    lat:         pet.latitud,
    lng:         pet.longitud,
  }
});
```

---

## Contexto de lo que ya funciona bien (no tocar)

- `POST /api/v1/pets` → genera embedding automáticamente al crear un reporte ✅
- `GET /api/v1/notifications` → devuelve matches ≥80% con `porcentajeSimilitud` ✅
- `GET /api/v1/notifications/unread-count` ✅
- `PUT /api/v1/notifications/{id}/read` ✅

---

*GeoPatitas · Proyecto académico DuocUC 2026*
