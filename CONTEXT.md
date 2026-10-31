# CONTEXT.md — Contexto completo del proyecto AR-Ingeniería

> Documento revisado el 2026-10-30 contrastando la implementación actual de `AR-Ingenieria/` y los artefactos recientes de validación.
> Fuente del análisis: `C:\Mis proyectos\AR-Ingenieria\AR-Ingenieria`
> Propósito: dar todo el contexto del sistema (funcional, técnico y de negocio) a cualquier desarrollador, agente de IA o nuevo integrante del equipo.

---

## 1. Visión general del negocio

AR-Ingeniería es una empresa de **mantenimiento eléctrico**. El sistema cubre el ciclo completo del trabajo:

1. Un **administrador** (web/panel) crea **tareas de mantenimiento** asociadas a una dirección y un recinto, y asigna un técnico de cabecera con su cédula chilena (RUT).
2. Los **técnicos** utilizan una **app Android (móvil)** para iniciar la tarea, invitar hasta 5 ayudantes, tomar fotos con la cámara, medir variables eléctricas del tablero (multímetro) y llenar un checklist de inspección por cada **tablero monofásico o trifásico**, luego pueden finalizar la tarea.
3. El **panel web** permite revisar el dashboard con los estados, gestionar técnicos/tareas, recibir notificaciones, y descargar informes en **Excel (XLSX) y Word (DOCX)**.
4. Un rol **SUPER** (superusuario) gestiona administradores, habilita/deshabilita pantallas del menú, administra tareas y técnicos y descarga informes individuales y paquetes ZIP por período.

**Énfasis clave:** los valores numéricos de las mediciones del tablero NO se exigen ni se muestran como input en la UI móvil — lo relevante es la **evidencia fotográfica** de cada medición y del checklist.

---

## 2. Composición del repo

```
AR-Ingenieria/
├── docker-compose.yml     # Orquestación: db (PostgreSQL 16) + backend + frontend
├── README.md              # Documentación de uso (admin web)
├── .gitignore
├── backend/               # Spring Boot 3.3.5 / Java 21 → microservicio REST
├── frontend/              # React 18 + Vite → SPA del panel de administración (servida en Nginx)
├── android/               # App Android Kotlin + Jetpack Compose  "ARIngMovil" (admin + técnico)
└── performance/           # Escenarios k6 y resultados de pruebas de carga
```

### Dónde vive qué

| Capa | Ruta | Tecnología |
|---|---|---|
| API | `backend/src/main/java/com/aring/...` | Spring Boot (Web, Data JPA, Security, Validation), Lombok |
| BD | PostgreSQL 16 (Docker) | Hibernate `ddl-auto=update` |
| Panel web | `frontend/src/**` | React, axios, react-router v6, recharts, lucide-react |
| App móvil | `android/app/src/main/java/com/aring/movil/...` | Kotlin 2.0.21, Compose (BOM 2024.12.01), CameraX 1.4.1, OkHttp crudo, EXIF 1.3.7, SharedPreferences, SIN Navigation-Compose (navegación manual con estado) |

---

## 3. Cómo levantar el sistema

### Todo con Docker (modo completo)

```bash
docker compose up --build
```

| Servicio | Contenedor | Puerto |
|---|---|---|
| PostgreSQL | aring-db | 5432 (DB: `aring`, usuario `aring`, pass `aring123`, volumen `aring-pgdata`) |
| Backend API | aring-backend | 8080 |
| Panel web (Nginx) | aring-frontend | **3000 → 80** |

Healthcheck de la BD habilitado; el backend solo arranca cuando la BD responde.

### Modo desarrollo (front en caliente)

```bash
docker compose up --build db backend
cd frontend && npm install && npm run dev   # http://localhost:5173
```

El `vite.config.js` hace **proxy de `/api` → http://localhost:8080** (`host: 0.0.0.0`, puerto 5173). El `nginx.conf` de producción enrutará `location /api/` a `http://backend:8080/api/` y sirve la SPA con `try_files ... /index.html`.

### La app Android

- Conecta a `BASE_URL = http://192.168.1.12:8080` (IP LAN codificada en `data/ApiService.kt`; para emulador se usa `http://10.0.2.2:8080`).
- Manifest con `usesCleartextTraffic="true"` (API HTTP sin TLS), permiso `CAMERA`.
- `minSdk 26 / targetSdk 35 / Java 17` · versionCode 1 · versionName 1.0 · AGP 8.7.3 · Gradle wrapper 8.12.

---

## 4. Credenciales y usuarios (seed por `DataSeeder`)

Se ejecuta en cada arranque; `SEED_DEMO_DATA=true` en `docker-compose.yml` controla los datos demo:

| Rol | Usuario (login) | Contraseña | Nombre | Acceso |
|---|---|---|---|---|
| Admin | `12345678` | `45678` | Luis Rojas | Panel web (rutas `/`, `/tecnicos`, `/tareas`, `/documentos`) |
| SuperUsuario | `29094728` | `18330707` | Super Usuario | `/` (home de selección), `/admin/*`, `/super` |
| Técnico demo | RUT `12345678-9` | `tecnico1` | Juan Pérez | App Android |
| Técnico demo | RUT `98765432-1` | `tecnico2` | María González | App Android |

**Datos demo:** 2 técnicos + 3 tareas ("Edificio Central", "Bodega Norte", "Local Comercial Sur") con los 3 estados, y una notificación de tarea finalizada. Pantallas semillas: `DASHBOARD`, `CLIENTES`, `TECNICOS`, `TAREAS`, `DOCUMENTOS` (habilitadas). Secuencia `MANTENIMIENTO` inicializada en 0.

⚠️ El login polimórfico (`POST /api/auth/login`) busca en ese orden: Admin por `username` → SuperUsuario por `username` → Técnico por `RUT`. Contraseña con BCrypt. Las credenciales de la tabla son solo datos demo del `DataSeeder`; no son credenciales de producción y pueden no estar disponibles si la cuenta ya existe en la base.

---

## 5. Roles y reglas de acceso

- **AUTENTICACIÓN:** JWT (JJWT 0.12.6, HMAC), expiración **24 h** (`app.jwt.expiration-ms=86400000`), secret por defecto hardcodeado en `application.yml` (override vía `JWT_SECRET`). Header `Authorization: Bearer <token>`.
  - Claims del token: `rol` (`ROLE_ADMIN` / `ROLE_SUPER` / `ROLE_TECNICO`), `tecnicoId` (solo técnicos), `nombre`, `apellido`, subject = username o RUT.
  - Única ruta pública: `POST /api/auth/login`. Todo lo demás exige token. `@PreAuthorize("hasRole('SUPER')")` protege `/api/super/**`.
  - `SecurityFilterChain` stateless, CSRF off, CORS para orígenes `localhost:*`, `127.0.0.1:*`, `192.168.1.12:*`, `192.168.1.13:*`.
- **FRONTEND web:** el panel es **exclusivo de administradores y superusuarios**; si un técnico intenta loguear web recibe "Acceso restringido: esta aplicación es solo para administradores." Guardas: `RequirePanelAdmin` (admin o super), `RequireAdmin`, `RequireSuper`, `PantallaHabilitada(nombre)`.
- **APP Android:** autologin por rol: si Prefs tiene sesiones de admin o de técnico arranca en `MainScreen` o `MainScreenTecnico` (exclusión mutua de roles).
  - Flujo del técnico: pilas de pantallas preds `PantallaTec` con listSaver (sobrevive rotación y muerte de proceso).
  - El técnico **invitado (ayudante)** NO puede finalizar tareas y salta `IngresarTareaScreen`; solo el **cabecera** inicia con invitados y finaliza.

---

## 6. API REST — Endpoints completos

Errores = `{ "mensaje": "..." }` (`ApiExceptionHandler`, catch-all en 500).

### Auth (`/api/auth`)
| Método | Ruta | Nota |
|---|---|---|
| POST | `/auth/login` | Body `{username, password}` (para técnicos username=RUT) → `{token, nombre, apellido, iniciales, username, rol, tecnicoId?}`; 401 con mensaje genérico "Usuario o contraseña incorrectos." |

### Técnicos (`/api/tecnicos`)
| Método | Ruta | Nota |
|---|---|---|
| GET | `/clientes` | Lista de clientes/recintos |
| POST | `/clientes` | Crear cliente |
| PUT | `/clientes/{id}` | Actualizar cliente |
| DELETE | `/clientes/{id}` | Eliminar cliente |
| GET | `/tecnicos` | Lista ordenada por nombre/apellido |
| POST | `/tecnicos` | Crear — 409 si RUT duplicado ("Ya existe un técnico con el RUT ingresado.") |
| PUT | `/tecnicos/{id}` | Actualiza contacto de emergencia + contraseña opcional |
| DELETE | `/tecnicos/{id}` | Bloqueado con **409** si el técnico tiene tareas en curso (cabecera o invitado) con estado ≠ FINALIZADA |

### Tareas (`/api/tareas`)
| Método | Ruta | Nota |
|---|---|---|
| GET | `/tareas` | Todas |
| GET | `/tareas/mias?tecnicoId=` | Tareas del técnico (cabecera o invitado), orden inverso, distinct |
| POST | `/tareas` | Solo `MANTENIMIENTO` (400 en otro tipo) — código gen `M-0001` con **lock pesimista** sobre `SecuenciaTarea` |
| POST | `/tareas/{id}/iniciar` | Body `{ayudantesIds:[Long]}` — crea notificaciones: `TAREA_INICIADA` al cabecera e `INVITACION` a los ayudantes |
| PUT | `/tareas/{id}` | Cambia técnico de cabecera (solo si `NO_INICIADA`) |
| PUT | `/tareas/{id}/estado` | `{estado}` — Transiciones válidas: `NO_INICIADA→EN_PROCESO`, `EN_PROCESO→FINALIZADA`; esta última crea notificación global `TAREA_FINALIZADA`; 400 fallback "Transición de estado no permitida." |
| DELETE | `/tareas/{id}` | Solo si `NO_INICIADA` |

### Tableros (`/api/tareas/{tareaId}/tableros`)
| Método | Ruta | Nota |
|---|---|---|
| POST | `/tareas/{tareaId}/tableros` | Payload con medición, checklist, imágenes (base64), observaciones → guarda `InformeTablero`, suma 1 a `tablerosRealizados` |
| GET | `/tareas/{tareaId}/tableros` | Lista los tableros de la tarea |

### Dashboard, documentos, notificaciones, pantallas y Super

| Método | Ruta | Nota |
|---|---|---|
| GET | `/dashboard/stats` | `{noIniciadas, enProceso, finalizadas}` |
| GET | `/dashboard/tecnicos` | Lista de técnicos |
| GET | `/documentos/edificios/sugerencias?q=` | Sugerencias de edificios para la búsqueda incremental del informe |
| GET | `/documentos/edificios/tareas?nombre=` | Tareas finalizadas de un edificio, con nombre, dirección, fecha e ID/código |
| GET | `/documentos/historial.xlsx` | Rango opcional `fechaDesde, fechaHasta` (zona **America/Santiago**, hasta fin del día) — Excel de historial |
| GET | `/documentos/finalizadas.xlsx` | "Rendicion de servicios" — solo tareas FINALIZADAS con tableros por observación |
| GET | `/documentos/finalizadas.docx` | Informe Word por tablero |
| GET | `/documentos/informe-tarea.docx?tareaId=` | Word de una tarea puntual; 404 si existe tarea pero no hay tableros |
| GET | `/notificaciones?tecnicoId=` | Admin: sin `tecnicoId` devuelve las globales `TAREA_FINALIZADA`; técnico: las de su id; 404 en marcar leída si el id no existe |
| PUT | `/notificaciones/{id}/leida`, `/notificaciones/leer-todas?tecnicoId=` | Marcado masivo como leídas (update where leida=false) |
| DELETE | `/notificaciones?tecnicoId=` | Admin: borra solo `TAREA_FINALIZADA`; técnico: todas las suyas |
| GET | `/pantallas` | Config de visibilidad de pantallas |
| DELETE | `/super/tareas`, `/super/tareas/{id}` | Borrado absoluto + reinicio de secuencias (numeración vuelve a `M-0001`), limpieza en cascada de informes/notificaciones/invitaciones |
| DELETE | `/super/tecnicos/{id}` | Borrar técnico aunque tenga tareas: queda `tecnicoCabecera=null`, y se limpian rapeles, invitaciones, informes y notificaciones |
| GET/POST | `/super/administradores`, PUT `/super/administradores/{id}/password`, DELETE `/super/administradores/{id}` | CRUD de admins (username único como RUT) |
| GET/PUT | `/super/pantallas` | leer / guardar completo; `PantallaConfigService` valida nombre no vacío y 404 "La pantalla configurada no existe." |
| GET | `/super/informe.docx` | Informe Word global con filtros de tarea/técnico/recinto/fechas (endpoint conservado) |
| GET | `/super/documentos-finalizadas.zip?anio=&mes=` | ZIP con Excel y Word de todas las tareas finalizadas del año y, opcionalmente, mes indicado; nombres de entrada `Edificio-Mes-Año-ID` |

---

## 7. Modelo de datos

Todas las entidades son `Long id` con `GenerationType.IDENTITY`. Lombok `@Getter/@Setter`.

### Entidades principales
- **Admin** (`admin`): `nombre`, `apellido`, `username` (UNIQUE), `password` (BCrypt).
- **SuperUsuario** (`super_usuario`): idéntico a Admin.
- **Tecnico** (`tecnico`): `nombre`, `apellido`, `rut` (UNIQUE — es el usuario de login), `password`, contacto de emergencia `contactoNombre/Apellido/Telefono`, `fechaCreacion (Instant)`.
- **Tarea** (`tarea`): `codigo` (UNIQUE, formato `M-0001`), `tipo` (TipoTarea), `direccion`, `nombreRecinto`, `descripcion (text)`, `estado` (EstadoTarea), `tecnicoCabecera → Tecnico (ManyToOne EAGER, nullable)`, `fechaCreacion/fechaInicio/fechaFin (Instant)`, `tablerosRealizados (int, default 0)`, y **N→N** con Tecnicos vía tabla `tarea_tecnico_invitado` (`tecnicosInvitados`).
- **SecuenciaTarea** (`secuencia_tarea`): `tipo (TYPO_TAREA, UNIQUE)`, `ultimoNumero` — contador para el código (`@Lock(PESSIMISTIC_WRITE)`).
- **Notificacion** (`notificacion`): sin FKs, solo ids. `tipo (TipoNotificacion)`, `mensaje`, `fecha`, `leida`, `tareaId`, `tecnicoDestinoId` (null = notificación global para admins).
- **PantallaConfig** (`pantalla_config`): `nombre` UNIQUE (`DASHBOARD|TECNICOS|TAREAS|DOCUMENTOS`), `habilitada`.

### Informes de tableros (núcleo de la evidencia)
- **InformeTablero** (`informe_tablero`): `tarea → Tarea (FK)`, `tipoTablero (TipoTablero)`, `tamanio` (nullable, tamaño del tablero trifásico: Pequeño/Mediano/Grande/Especial), `descripcion`, `ubicacion`, `nivel`, `reapriete`, `aseo`, `tecnico → Tecnico (nullable = "Responsable Mantenimiento" del informe)`, `fechaCreacion`. Con **4 colecciones `cascade ALL / orphanRemoval`** y FK `informe_id`.
- **MedicionTablero** (`medicion_tablero`): `variableId`, `label`, `unit`, `valor`, `fotoBase64 (text)`.
- **ChecklistTablero** (`checklist_tablero`): `itemId`, `label`, `status` (p.ej. "OK"), `fotoBase64`.
- **ImagenTablero** (`imagen_tablero`): `categoria` ("PREVIA"/"POSTERIOR"), `indice`, `base64 (text)`.
- **ObservacionTablero** (`observacion_tablero`): `texto (text)`, `orden` (incremental desde 0).

### Enums
| Enum | Valores |
|---|---|
| EstadoTarea | `NO_INICIADA` (No iniciada / ROJO `#D64545`), `EN_PROCESO` (En proceso / AMARILLO `#F59E0B`), `FINALIZADA` (Finalizada / VERDE `#22A06B`) |
| TipoTarea | `MANTENIMIENTO` (prefijo **"M"**), `REPARACION` ("R", "Reparación", futuro), `COTIZACION` ("C", "Cotización", futuro) |
| TipoNotificacion | `TAREA_INICIADA`, `INVITACION`, `TAREA_FINALIZADA` |
| TipoTablero | `MONOFASICO` ("Monofásico"), `TRIFASICO` ("Trifásico") |

---

## 8. Backend — detalles de implementación

### Lógica de negocio principal (`TareaService`, `TecnicoService`, `NotaService`)
- `TareaService` genera el código: lock pesimista, formato `M-%04d` (`M-0001, M-0002 …`, llega a 5 dígitos desde 10000). `500` si la secuencia no existe.
- La **tarea utilitaria de iniciar**: encadena notificaciones (invitación a ayudantes, tarea iniciada al cabecera). `cambiarEstado` a FINALIZADA notifica global "Tarea {codigo} finalizada por {nombre} {apellido}." con `tecnicoDestinoId=null`.
- Cambios de estado, edición de cabecera y borrado **solo con estado `NO_INICIADA`** (si no, 400).
- `TecnicoService.eliminar`: validaciónstrict de 2 checks (cabecera ≠ FINALIZADA → 409 con mensaje como "No se puede eliminar: el técnico está invitado a tareas en curso." / cabecera especial "el técnico tiene tareas en curso asignadas."), luego limpieza (usuario, invitaciones via SQL nativo, tableros del técnico, delete).
- `AdminService`: creación/actualizar/eliminar; username con trim; BCrypt; 409 "Ya existe un administrador con el usuario ingresado.".
- `TableroService.crear`: convierte `TableroRequest` → `InformeTablero` + colecciones; asigna el técnico responsable con prioridad al `tecnicoId **del JWT**` (token re-parseado en el controller, no usa `SecurityContextHolder` porque el principal es el RUT y el `tecnicoId` vive solo en claims); suma `tablerosRealizados`.

### Documentos (`DocumentoService` — genera XLSX/DOCX con Apache POI)
⚠️ **No se usa OpenPDF** (dependencia declarada en el `pom.xml` pero sin uso); el "informe 'PDF'" del README es en realidad un **Documento Word (.docx)**.

- Excel "Historial de tareas": hoja "Historial" con grilla oculta, título B2:H2 (fondo DARK_BLUE, cuadri azul #D9E2F3), zebra #F2F7FB, freeze pane, fechas `dd/MM/yyyy HH:mm` Santiago. Sin datos: celda combinada "No hay tareas registradas con los filtros seleccionados.".
- Excel "Rendicion de servicios": bloque por tarea, columnas *Piso, Ubicación, Descripcion, Tipo de tablero, Tamaño tablero, Observaciones, Nivel de criticidad, Valor reparación* (2 últimas vacías, con formato moneda `$#,##0.00`); `tamanioTablero` en monofásico → **"No Aplica"**.
- Word: incluye el generador del informe de mantenimiento preventivo a partir de recursos de plantilla en `backend/src/main/resources/informe/`. La portada usa datos variables del edificio/ubicación/mes/año; las secciones incluyen instrumentación, pautas por tablero con técnico/ayudante/detalles y collage fotográfico, levantamiento/valorización, galería global de termografías, resumen y agradecimientos. Se omiten textos explicativos fechados que no son datos de la tarea. La composición concreta depende de las evidencias almacenadas en cada tarea.
- Filtros compartidos: `TareaSpecifications.conFiltros(...)` (ojo: **el filtro de fecha filtra por `fechaCreacion`, no por `fechaInicio`**).
- Errores: 404 "La tarea {codigo} no existe o aún no está finalizada." / "...no tiene tableros inspeccionados..." / "No hay tareas finalizadas con tableros registrados para los filtros seleccionados.".

### Tests (JUnit 5 + Mockito, 31 tests aprox.)
- `AdminServiceTest`, `TareaServiceTest`, `TecnicoServiceTest`, `DocumentoServiceTest` (este último valida el **ZIP real**); **NO hay tests de `TableroService`, `NotificacionService`, `PantallaConfigService`, ni seguridad/JWT**.
- **Nota de build:** el Dockerfile del backend compila con `-DskipTests` (los tests no corren en el build Docker).

### Configuración (`application.yml`)
`spring.jackson.default-property-inclusion: non_null` (JSON no serializa nulls) · `open-in-view: false` · `ddl-auto: update` · servidor 8080. Docker imagen base `maven:3.9-eclipse-temurin-21` build + `eclipse-temurin:21-jre` runtime; `.dockerignore` excluye `target/`. Imagen de salida `aring-backend-1.0.0.jar`.

---

## 9. Frontend (panel web React)

### Arquitectura y App
- Providers, de afuera hacia dentro: `BrowserRouter → AuthProvider → RefreshProvider → App (Layout) → PantallasProvider → AppRoutes`.
- Sesión: `localStorage['aring_admin'] = {token, nombre, apellido, iniciales, username, rol}` (`AuthContext`). `api/client.js` instancia axios con `baseURL='/api'` que inyecta `Authorization: Bearer <token>` y en 401 limpia la sesión y redirige a `/login`. `mensajeErrorBlob(error)` para respuestas de descarga fallidas con body Blob.
- Contextos: `RefreshContext` (botón refresco global "key+1" en TopBar; Dashboard/Tecnicos/Tareas escuchan `key`) y `PantallasContext` (feature-toggle de pantallas, `fail-open` — si falla la carga asume todo habilitado).
- **Login:** checkbox "Recordar credenciales" guarda usuario y contraseña en texto plano en `localStorage['aring_recordar']` (real de app interna; no saneado).

### Rutas
| Ruta | Protección | Vista |
|---|---|---|
| `/login` | pública | Login (gradiente violeta + card) |
| `/` | PanelAdmin | `rol===ROLE_SUPER ? SuperInicio : Dashboard` |
| `/clientes`, `/tecnicos`, `/tareas`, `/documentos` | PanelAdmin + PantallaHabilitada | Páginas del modo administrador |
| `/admin`, `/admin/clientes`, `/admin/tecnicos`, `/admin/tareas`, `/admin/documentos` | **RequireSuper** | Modo administrador del superusuario; sus enlaces navegan bajo `/admin` |
| `/super` | RequireSuper | `SuperPanel` (su propio `SuperLayout`) |
| `*` | — | `<Navigate to="/">` |

### Páginas
- **Dashboard:** gráfico de barras recharts con estados (En proceso=amarillo, Finalizadas=verde, No iniciadas=rojo) + chips de contadores + listado de técnicos (avatar + RUT).
- **Técnicos:** modal Crear técnico con **7 campos obligatorios** divididos en 2 secciones — "Datos del técnico" (*Nombre del técnico, Apellido del técnico, RUT del técnico, Contraseña del técnico*) y "Datos de contacto de emergencia" (*Nombre del contacto, Apellido del contacto, Número telefónico*). Modal Editar: solo 4 campos `PUT /tecnicos/{id}` `{password, contactoNombre, contactoApellido, contactoTelefono}` (es decir, sin cambios de nombre/RUT).
- **Tareas:** tarjetas con `codigo` (ej. M-0001) + ChipTipo + ChipEstado; acciones según estado: No iniciada → Editar/Eliminar; En proceso → **"Finalizar (demo)"** (`PUT /tareas/{id}/estado` con `estado:'FINALIZADA'`); Finalizada → solo chip. Modal "Limpiar finalizadas" solo oculta del estado local (ids en `aring_finalizadas_ocultas` en localStorage; NO borra de BD). Modal "Crear tarea": *Tipo de tarea* (solo MANTENIMIENTO; Reparación/Cotización "próximamente" deshabilitados), *Dirección, Nombre del recinto, Descripción de la tarea, Técnico de cabecera* → `tecnicoCabeceraId: Number`. Modal "Editar tarea": solo el técnico de cabecera.
- **Documentos:** historial Excel con rango de fechas y sección de informes finalizados. La búsqueda de informes se hace por nombre de edificio con sugerencias incrementales; al elegirlo se listan sus tareas finalizadas con nombre/dirección/fecha/ID y se descarga Excel o Word de la tarea seleccionada. El nombre del archivo sigue `Edificio-Mes-Año-ID` (zona `America/Santiago`).
- **SuperInicio:** botones para elegir "Modo administrador" (`/admin`) o "Modo super usuario" (`/super`).
- **SuperPanel:** apartados de informes, tareas, técnicos, administradores y pantallas. Informes usan la misma búsqueda por edificio y descarga individual Excel/Word. En Tareas se pueden eliminar registros individualmente o todos (reinicia secuencia M-0001) y descargar en ZIP los documentos de tareas finalizadas por año o por año/mes. SuperInicio ofrece acceso a modo administrador y modo superusuario; el encabezado global identifica el rol y la pantalla inicial no duplica el navbar.

### Estilo / marca
`theme.css` con variables: `--violeta #6C3CE9`, `--violeta-oscuro #4B2495`, `--violeta-medio #8B5CF6`, `--violeta-claro #EEE8FF`, fondo `#F2F2F8`, texto `#222/#6B6B78`, borde `#E3E1EA`; gradiente `linear-gradient(135deg, #6C3CE9 0%, #4B2495 100%)` en topbar/sidebar activo/botones primario; tipografía `'Segoe UI', Roboto, Helvetica, Arial`; radio 16px; sin fuentes web (identidad color+rayo). Favicon: cuadrado con gradiente violeta y **rayo eléctrico blanco**. index.html title "Panel de Administración", `<html lang="es">`.

Dockerfile frontend multi-stage: build `node:22-alpine` (copia package.json, `npm install`, `npm run build`) + `nginx:alpine` serve.

---

## 10. App Android (ARIngMovil)

### Estructura de navegación (manual, sin Navigation-Compose)
```text
App.kt
├── LoginScreen (solo si no hay sesión almacenada)
│    └── POST /api/auth/login → AdminSesion | TecnicoSesion
├── ADMIN → MainScreen:  BarraNavegación con ids: Dashboard / Técnicos / Tareas
└── TÉCNICO → MainScreenTecnico:
      tabs: Inicio | Tareas | Documentos
      pila: Lista → Detalle(tarea)
              ├→ IngresarTareaScreen (cabecera: elegir hasta 5 invitados) → tablero (POST /tareas/{id}/iniciar)
              └→ tablero de flujo → TableroSeleccion (tipo) → TableroFlujo(Registro→EvidenciasPrevias→Variables→
                        Checklist→EvidenciasPosteriores→Observaciones) → EnviarInformeScreen → TableroConfirmacion
```
- El técnico invitado no ve `IngresarTareaScreen` ni puede finalizar; el cabecera ya finalizado navega al historial "Tareas asignadas" en la hoja de notificaciones.
- Prefs: ficha `SharedPreferences "aring_movil"` con **sistema de secretos en texto plano** (tecnico_password, cred_user, cred_pass, tokens, ocultas_finalizadas).

### Registro de tablero (el módulo estrella)
- Constantes de tipo `Trifasico` vs `Monofasico` con configuración distinta:
  - **Monofásico:** 4 variables (Corriente I/Amp, Voltaje F-N, Voltaje N-T, Termografía/°C) + **9 checks** (anclaje, rotulado, barras dist/tierra, conexión, conducción/temperatura, acrílicos, conexiones, termografía) + 2 fotos previas y 2 posteriores.
  - **Trifásico:** **11 variables** (corrientes I(1..3), voltajes F1-F2/F1-F3/F2-F3/F1-N/F2-N/F3-N/N-T, Temperatura) + **21 checks** + campo `tamanioTablero` (Pequeño/Mediano/Grande/Especial).
  - `Reapriete` y `Aseo` = solo selector `Sí / No / N/A` (por fase de Variables).
  - **Validación (6 fases):** F1 datos del registro (trifásico también requiere tamaño), F2 fotos previas (2 obligatorias), F3 **foto por cada variable** (`"Debe tomar la fotografía de {label}"` — el input de valor se oculta (`showValueInput = false`) pero campos Numéricos no se exigidos) + Reapriete + Aseo, F4 checklist con estado por item y **C/R obliga foto** ("Evidencia fotográfica requerida para..."), F5 2 fotos posteriores, F6 observaciones libres.
- **Persistencia local:** `TableroDraftStore` (SharedPreferences `"aring_tableros"`, clave `draft_{tareaId}_{tipo}`), estado completo serializado a JSON (así se sobrevive la muerte del proceso). Cada mutación del ViewModel persiste **inmediatamente**.
- **Envío del informe:** `EnviarInformeScreen` re-loguea el técnico (`loginTecnico(rut, password)`) y `POST /api/tareas/{id}/tableros` con build de JSON por `TableroRepository` (todo embebido en Base64 — JPEG q85 máx 1280px vía `decodeSampledBitmap` sobre archivo original, con `OkHttpClient` timeouts **write 90s**). Estados `Inactivo | Enviando | Exito | Error` con pantalla de reintento y bloqueo de Back durante envío.
- **Finalizar tarea** (cabecera, en `TableroSeleccion`): envia drafts pendientes si alguno; si ya hay tableros en el servidor iguales a la tarea (por el conteo `tablerosRealizados`) procede; si no hay tableros → error "Esta tarea no tiene tableros inspeccionados..." → `PUT /tareas/{id}/estado FINALIZADA` → `TableroConfirmacion`.

### Cámara y anotación de evidencia
- `PhotoBus` (singleton con `MutableStateFlow`) para comunicación pantalla↔cámara: `PhotoRequest(phased, category PREVIOUS|MEASUREMENT|CHECKLIST|POST, referenceId?, slotIndex, label)`.
- `PhotoStorage` guarda en `filesDir/photos/foto_{ts}_{uuid}.jpg`; `CameraScreen` ocupa la pantalla, usa **CameraX 1.4.1** con `CAPTURE_MODE_MINIMIZE_LATENCY`, flash y bloqueo durante la captura. Al tomar la foto se guarda y devuelve automáticamente al formulario, sin paso de confirmación "Usar foto". Runtime de permisos CAMERA con `RequestPermission`.
- El anotador abre en diálogo de pantalla completa; mantiene la proporción de la foto (sin deformarla ni recortarla), permite dibujar, deshacer/borrar, cancelar y confirmar. Guarda la anotación como archivo nuevo y preserva el original; sus controles consideran las barras del sistema.
- Corrección de **orientación EXIF** en la subida (dependencia `androidx.exifinterface`).

### Pantallas del técnico
- `TareasTecnicoScreen`: tareas propias (sin finalizadas), chips "Invitado", clic → Detalle.
- `DetalleTareaTecnico`: tarjeta datos + **"Ir a la dirección"** (intento `geo:0,0?q=dirección` con funcionalidad de maps) + "Ingresar/Retomar tarea".
- `IngresarTareaScreen` (cabecera): si ya en proceso muestra fecha inicio + "Retomarás la tarea desde el último punto guardado."; si no, selector de hasta **5 invitados** ("Invitaciones: N/5") → `Iniciar tarea`.
- `DocumentosTecnicoScreen`: lista tareas `Finalizadas` (ocultables, localStorage ocultas) + **"Descargar informe"** con `ActivityResultContracts.CreateDocument("docx")` → `GET /api/documentos/informe-tarea.docx?tareaId=` y `Write` via `ContentResolver`.

### UI kit propio en Compose (`ui/components/Componentes.kt`)
`LogoRayo`, `IconoOjo`, `Avatar`, `ChipEstado` (rojo/amarillo/verde), `ChipTipo`, `ChipLegenda`, `Tarjeta`, `BannerError/BannerOk`, `Cargando`, `Vacio`, `BtnPrimario/BtnSecundario/BtnRiesgo`, `ModalDialog`, `ConfirmarDialog`, `SeccionFormulario`, `CampoTexto` (BasicTextField custom con ojo/halo), `CampoSelect` (DropdownMenu medido), `HorizontalSeparador`. Tema `ARIngMovilTheme` light-only con colorscheme primario violeta.

### Colores y marca
Idénticos al web (`Violeta #6C3CE9`, `VioletaOscuro #4B2495`, gradiente, status bar violeta `#4B2495`, fondo `#F2F2F8`). appName en `strings.xml`: **"AR-Ingeniería"**. Todo el texto hardcodeado en español (Chile), sin i18n.

---

## 11. Estado de las notificaciones (resumen de UX)

| Actor | Ve | Crea (backend) |
|---|---|---|
| Panel web y administrador Android | `TAREA_FINALIZADA` (badge de pendientes) | al finalizar una tarea |
| Técnico en Android | `TAREA_INICIADA` e `INVITACION` propias | al iniciar tarea con invitados |

El estado `leida` se persiste en backend. Al desplegar la lista de notificaciones, las aplicaciones marcan como leídas las pendientes; el badge vuelve a aparecer cuando llega una nueva notificación sin leer. Aplica al administrador web, administrador móvil y técnico móvil.

---

## 12. Riesgos / deudas técnicas detectadas

1. **Secretos en claro**: JWT secret por defecto + credenciales demo hardcodeadas en el seed según `SEED_DEMO_DATA`; contraseñas/credenciales/tokens en `SharedPreferences` Android sin `EncryptedSharedPreferences`; "Recordar credenciales" guardado en texto plano en `localStorage` (web y móvil).
2. **HTTP sin TLS en móvil**: `usesCleartextTraffic="true"` + `BASE_URL` fija (`http://192.168.1.12:8080`) — hay que parametrizar/quebrar por entorno para producción.
3. **Payloads grandes**: fotos Base64 embebidas en JSON (write timeout 90 s en OkHttp) — considerar multipart o redimensionado menor si el dataset crece.
4. **Sin Retrofit/Kotlinx-serialization**: JSON construido manualmente con `org.json` — no se usa un modelo de fallo tipado (mejorar con serialización de tipo seguro si continúa el desarrollo).
5. Cobertura de tests limitada (sin tests de `TableroService`, `NotificacionService`, pantallas, o security/JWT; build Docker usa `-DskipTests` para ahorrar build time).
6. La actualización del menú del superusuario usa actualización optimista; conviene verificar que la UI revierta el interruptor si falla `PUT /super/pantallas`.
7. El botón "Finalizar (demo)" de los paneles administrativos cambia el estado de la tarea directamente; el flujo móvil del técnico sí valida y envía los tableros antes de finalizar.
8. El filtro por código en endpoints de informes permanece por compatibilidad; la pantalla de Documentos ahora prioriza búsqueda de edificio y selección de una tarea finalizada.

---

## 13. Prueba de carga y rendimiento (2026-10-30)

Se agregó un escenario k6 de solo lectura en `performance/k6/super-admin-readonly.js` y el resultado de la ejecución de 500 usuarios virtuales en `performance/results/500vu-20261030-213019.json`. La prueba escaló hasta 500 VUs, sostuvo esa carga durante 2 minutos y duró aproximadamente 5 min 30 s. Ejecutó 52.322 solicitudes HTTP (157,7 solicitudes/s), con 0 respuestas HTTP fallidas y 52.321 checks correctos. La latencia HTTP promedio fue 4,15 ms, p95 6,56 ms, p99 11,82 ms y máximo 157,60 ms.

Es una prueba local dentro del entorno de desarrollo, sobre rutas de lectura y la página inicial. No representa 500 sesiones reales con navegador/móvil y no incluye carga de fotos, generación masiva de documentos, escrituras ni red móvil/infraestructura de producción. No extrapolar estas cifras directamente a producción.

---

## Anexo — checklist inicial de desarrollo

- [ ] ¿Reproducir localmente con Docker (`docker compose up --build`)?
- [ ] Login web con admin `12345678` / `45678`.
- [ ] Login móvil con técnico `12345678-9` / `tecnico1` (IP de red ajustada en `data/ApiService.kt`).
- [ ] Crear tarea (MANTENIMIENTO) → iniciar con ayudantes → registrar tablero monofásico → finalizar → notificar.
- [ ] Generar informes web (Historial Excel / Rendición Excel / Informe Word).

---

*Creado/documentado por Eliezer Carrasco para **AR-Ingeniería** · © 2026 · Versión 1.0*
