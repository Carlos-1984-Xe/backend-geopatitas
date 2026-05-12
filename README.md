# 🐾 GeoPatitas - Backend API

Plataforma de centralización y matching de mascotas perdidas y encontradas mediante **Inteligencia Artificial** y búsqueda vectorial.

## 🚀 Tecnologías
- **Java 17** + **Spring Boot 3.x**
- **PostgreSQL** (vía Supabase) + **pgvector**
- **HuggingFace API** (Inference API para embeddings de texto)
- **Spring Security**

## 🛠️ Configuración Previa

1. **Base de Datos (Supabase):**
   - Habilita la extensión de vectores ejecutando: `CREATE EXTENSION IF NOT EXISTS vector;`
2. **IA (HuggingFace):**
   - Genera un Access Token en tu cuenta de HuggingFace.

## 🔑 Variables de Entorno Requeridas

Debes configurar las siguientes variables en tu entorno local para que la aplicación arranque:

| Variable | Descripción |
| :--- | :--- |
| `SUPABASE_HOST` | Host de tu base de datos en Supabase |
| `SUPABASE_USER` | Usuario de la DB (normalmente postgres) |
| `SUPABASE_PASSWORD` | Tu contraseña de Supabase |
| `HUGGINGFACE_API_KEY` | Tu token de HuggingFace API |

## 📦 Instalación y Ejecución

```bash
# Clonar el proyecto
git clone <url-de-tu-repo>

# Compilar y ejecutar
mvn spring-boot:run
```
