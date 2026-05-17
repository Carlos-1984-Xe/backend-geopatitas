# GeoPatitas — Estado de Integración Frontend ↔ Backend
> Documento interno de contexto · Actualizado 17 de mayo de 2026
> No se sube a ningún repositorio

---

## ✅ Lo que ya funciona (integrado y probado)

### Autenticación
- `POST /auth/register` → autologin inmediato via `GET /users/me`
- `POST /auth/login` → JWT almacenado en `localStorage`, nombre del usuario en el navbar
- Logout limpia token y estado
- Modo invitado funciona sin JWT (flujo separado)
- **Bug resuelto:** clave JWT se regeneraba en cada restart de Spring → fix: clave fija en `application.properties`

### Mapa — Búsqueda geográfica
- `GET /pets/nearby?lat&lng&radius&tipoReporte&especie&estado&color&tamano` completamente conectado
- Radio del slider (km) se multiplica × 1000 antes de enviar al backend (metros)
- Filtros de tipo, especie, estado, color y tamaño procesados en el servidor
- Filtro por nombre funciona en cliente
- Marcadores azul `!` (encontrado) y amarillo `?` (perdido) aparecen con datos reales

### Modal de detalle del reporte
- Foto real desde Supabase Storage con lightbox al hacer click
- Normalización de campos: `ENCONTRADO/PERDIDO → found/lost`, ISO 8601 → fecha legible en español
- Contacto muestra nombre, email y teléfono del reportante o del tercero registrado
- Prioridad: `contactoEmail/Telefono/Nombre` del tercero > datos del usuario registrado

### Creación de reportes
**Desde el sidebar del mapa:**
- Autenticado → `POST /pets` con JWT
- Invitado → `POST /pets/guest` con `contactoEmail` obligatorio
- Upload de foto via `POST /pets/upload-image` antes de crear el reporte
- **Bug resuelto:** overlay de Joyride bloqueaba pantalla al fallar el envío

**Desde `/reportar` (formulario completo):**
- Misma lógica de endpoints según autenticación
- Foto movida debajo de Ubicación para guiar el orden correcto de pasos
- Sección de contacto: usuarios logueados ven checkbox opcional "Registrar para alguien más"
- Campos de tercero: `contactoNombre`, `contactoEmail`, `contactoTelefono` enviados al backend

### Infraestructura
- `api.ts`: instancia axios con base URL `http://localhost:8080/api/v1`
- Interceptor agrega `Authorization: Bearer <token>` automáticamente
- Interceptor de respuesta redirige a `/login` en 401

---

## 🔴 Lo que falta — Pendiente

### 1. Dashboard de usuario (`/mis-reportes`)
La página existe pero usa datos mock. Falta conectar a:
- `GET /api/v1/pets` filtrado por el usuario autenticado (o `GET /api/v1/users/me/pets` si Carlos lo implementa)
- Mostrar casos **Activos** y **Resueltos** del usuario
- Botón para marcar un reporte como resuelto → `PUT /pets/{id}/estado` con `{ "estado": "RESUELTO" }`
- Opción de editar o eliminar un reporte propio

### 2. Matching por IA — HuggingFace + pgvector ⭐
El endpoint `GET /pets/match?q=...` ya existe en el backend pero el frontend no lo usa aún.
- Al crear un pet, el backend llama automáticamente a HuggingFace (`sentence-transformers/all-MiniLM-L6-v2`) para generar un embedding de 384 dimensiones
- `pgvector` permite búsqueda por similitud semántica en Supabase
- **Lo que falta en el frontend:**
  - Buscador en el mapa por descripción libre (ej: "perro labrador dorado con collar rojo")
  - Sección "Posibles coincidencias" en el modal o dashboard que llame a `/pets/match?q=<descripcion>`
  - Mostrar score de similitud entre mascotas perdidas y encontradas

### 3. Formularios incompletos para filtros de búsqueda
Los dos formularios de reporte (sidebar del mapa y `/reportar`) **no piden la misma información** y algunos campos que sí existen en el backend no se capturan:

| Campo | Sidebar mapa | Formulario /reportar | Backend |
|---|---|---|---|
| Especie | ✅ | ✅ | ✅ |
| Color | ✅ | ✅ | ✅ |
| Tamaño | ❌ falta | ❌ falta | ✅ `tamano` |
| Raza | ❌ falta | ✅ (opcional) | ✅ `raza` |
| Sexo | ❌ falta | ❌ falta | ✅ `sexo` (nullable) |

Sin `tamano` y `raza` en ambos formularios, los filtros del mapa no pueden filtrar por esos campos aunque el backend los soporta.

**Acción requerida:** Agregar campos `tamano` (Pequeño/Mediano/Grande) y `raza` al formulario del sidebar. Homologar ambos formularios.

### 4. Foto en formulario del sidebar del mapa
El sidebar permite subir foto pero:
- Solo usuarios autenticados pueden usar `POST /pets/upload-image`
- Si falla el upload, el reporte se crea sin foto silenciosamente
- No hay feedback visual del progreso de carga

### 5. Columna `fotos` en backend (pendiente verificar)
Carlos aplicó el fix de `@Type(ListArrayType.class)` para el array `text[]` de PostgreSQL.
Falta verificar que los reportes nuevos guarden la URL de foto correctamente en Supabase.

### 6. Endpoints faltantes del backend
```
GET  /api/v1/users/me/pets     → Reportes del usuario autenticado (para dashboard)
GET  /api/v1/pets/{id}         → Detalle de un reporte (para modal desde URL directa)
PUT  /api/v1/pets/{id}/estado  → Marcar como resuelto (para dashboard)
```

---

## 🏗 Arquitectura de integración actual

```
Frontend (React + Vite)          Backend (Spring Boot 3.2.5)
http://localhost:5173      →     http://localhost:8080/api/v1

AuthContext ─────────────────→  POST /auth/login
                                 POST /auth/register
                                 GET  /users/me

Map.tsx (handleSearch) ──────→  GET  /pets/nearby?lat&lng&radius&filtros...

Map.tsx (handleReportSubmit) →  POST /pets/upload-image  (foto)
                                 POST /pets               (autenticado)
                                 POST /pets/guest         (invitado)

CreateReport.tsx ────────────→  POST /pets/upload-image  (foto)
                                 POST /pets               (autenticado)
                                 POST /pets/guest         (invitado)

[PENDIENTE] match ───────────→  GET  /pets/match?q=...   (IA pgvector)
[PENDIENTE] dashboard ───────→  GET  /users/me/pets
                                 PUT  /pets/{id}/estado
```

---

## 📋 Próximos pasos sugeridos (orden de prioridad)

1. **Homologar formularios** — agregar `tamano`, `raza` y `sexo` a ambos formularios para que los filtros del mapa sean útiles
2. **Dashboard `/mis-reportes`** — conectar con los reportes reales del usuario, marcar como resuelto
3. **Matching por IA** — integrar `GET /pets/match?q=` en el mapa con buscador semántico y sección "Posibles coincidencias"
4. **Verificar fotos** — confirmar que el fix de `ListArrayType` guarda URLs correctamente en Supabase
5. **Endpoint `GET /pets/{id}`** — para poder abrir el detalle de un reporte desde URL directa

---

## 🔧 Variables de entorno del backend requeridas

```env
DB_PASSWORD=...           # PostgreSQL en Supabase
HUGGINGFACE_API_KEY=...   # sentence-transformers/all-MiniLM-L6-v2
SUPABASE_API_KEY=...      # Service role key para Storage
JWT_SECRET=...            # Clave fija (fix aplicado esta sesión)
```

> ⚠️ `application-local.yml` con credenciales debe estar en `.gitignore`

---

*GeoPatitas · Proyecto académico DuocUC 2026*
