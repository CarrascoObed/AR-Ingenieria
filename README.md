# AR-Ingeniería — Aplicación Web de Administración

Aplicación web para el administrador de AR-Ingeniería. Permite gestionar técnicos, tareas, revisar el dashboard con gráficos de estado y descargar informes en Excel y PDF.

## Stack

- **Frontend:** React 18 (Vite) servido por Nginx
- **Backend (microservicio):** Spring Boot 3 (Java 21) con seguridad JWT
- **Base de datos:** PostgreSQL 16
- **Orquestación:** Docker Compose

## Requisitos

- Docker (con Docker Compose)

## Cómo levantar

```bash
docker compose up --build
```

La aplicación queda disponible en:

- **Aplicación web:** http://localhost:3000
- **API backend:** http://localhost:8080
- **PostgreSQL:** localhost:5432

### Credenciales de acceso (administrador)

| Campo       | Valor     |
|-------------|-----------|
| Usuario     | `12345678` |
| Contraseña  | `45678`    |

## Datos de demostración

Al primer arranque se siembran datos de ejemplo: el administrador (Luis Rojas), 2 técnicos, 3 tareas (una por estado) y notificaciones de tareas finalizadas. Para arrancar sin datos demo, cambiar `SEED_DEMO_DATA` a `false` en `docker-compose.yml`.

## Funcionalidades

- **Login:** autenticación con JWT y footer de derechos de autor.
- **Navbar persistente:** avatar con iniciales del administrador, campana de notificaciones (solo tareas finalizadas), botón de actualizar y cerrar sesión.
- **Dashboard:** gráfico de barras con el estado de las tareas (En proceso / Finalizadas / No iniciadas) y listado de técnicos.
- **Técnicos:** crear técnico (datos del técnico + contacto de emergencia), editar contraseña/contacto y eliminar (bloqueado si tiene tareas).
- **Tareas:** crear tareas de tipo Mantenimiento (ID `M-0000001`, ...) con técnico de cabecera; editar/eliminar solo si la tarea no está iniciada; control de demo para iniciar/finalizar tareas.
- **Documentos:** descargar historial en Excel y informes (Excel + PDF) de tareas finalizadas, con filtros por ID de tarea, nombre del técnico y recinto.

## Desarrollo local (sin Docker para el frontend)

Levantar solo `db` y `backend` con Docker y correr el frontend con Vite:

```bash
docker compose up --build db backend
cd frontend
npm install
npm run dev
```

El frontend en http://localhost:5173 usa un proxy hacia el backend en `http://localhost:8080`.

## Estructura

```
AR-Ing/
├── docker-compose.yml
├── backend/   # Spring Boot (microservicio)
└── frontend/  # React (Vite + Nginx)
```

---

Creado por **Eliezer Carrasco** para **AR-Ingeniería** · © 2026 · Versión 1.0 · Todos los derechos reservados.

## Contraseñas

| Campo                  | Valor                                                        |
|------------------------|--------------------------------------------------------------|
| Usuario servidor       | `ar-ingenieria`                                              |
| Contraseña servidor    | `1q2w3e4r5t6y`                                               |
| URL aplicación web     | https://ar-ingenieria.taila89688.ts.net                      |

### Comandos del servidor

**Modo gráfico**

Iniciar el modo gráfico de inmediato (sin reiniciar):
```bash
sudo systemctl start graphical.target
```

Configurar para que arranque SIEMPRE en modo gráfico por defecto:
```bash
sudo systemctl set-default graphical.target
```

**Modo Consola (CLI)**

Cambiar al modo consola de inmediato (sin reiniciar):
```bash
sudo systemctl isolate multi-user.target
```

Configurar para que arranque SIEMPRE en modo consola por defecto:
```bash
sudo systemctl set-default multi-user.target
```

**Comando para descargar todos los cambios de GitHub:**
```bash
cd ~/Desktop/AR-Ingenieria && git pull origin main && sudo docker compose up -d --build backend frontend
```
