# CONTEXT.md — Contexto completo del proyecto AR-Ingeniería

> Documento revisado el 2026-10-31 contrastando la implementación actual de `AR-Ingenieria/`; incluye cambios locales posteriores al commit `a95d69a` (solo pestaña Tareas para técnico, sin generar APK).
> Fuente del análisis: `C:\Mis proyectos\AR-Ingenieria\AR-Ingenieria`
> Propósito: dar todo el contexto del sistema (funcional, técnico y de negocio) a cualquier desarrollador, agente de IA o nuevo integrante del equipo.

---

## 1. Visión general del negocio

AR-Ingeniería es una empresa de **mantenimiento eléctrico**. El sistema cubre el ciclo completo del trabajo:

1. Un **administrador** (web/panel) crea **tareas de mantenimiento** asociadas a una dirección y un recinto, y asigna un técnico de cabecera con su cédula chilena (RUT).
2. Los **técnicos** utilizan una **app Android (móvil)** para recibir e iniciar la tarea con conexión, invitar hasta 5 ayudantes, y completar inspecciones monofásicas o trifásicas. Los formularios, fotos y tareas descargadas se conservan en el teléfono, de modo que el trabajo iniciado puede continuar sin Internet; informes y finalización se sincronizan al recuperar conexión.
3. El **panel web** permite revisar el dashboard con los estados, gestionar técnicos/tareas, recibir notificaciones, y descargar informes en **Excel (XLSX) y Word (DOCX)**.
4. Un rol **SUPER** (superusuario) gestiona administradores, habilita/deshabilita pantallas del menú, administra tareas y técnicos y descarga informes individuales y paquetes ZIP por período.

**Énfasis clave:** los valores numéricos de las mediciones del tablero NO se exigen ni se muestran como input en la UI móvil — lo relevante es la **evidencia fotográfica** de cada medición y del checklist.

---

## 2. Composición del repo

```
AR-Ingenieria/
├── docker-compose.yml     # Orquestación: db (PostgreSQL 16) + backend + frontend
├── docker-compose.cloudflare.yml # Servicio opcional cloudflared
├── .env.example           # Plantilla sin secretos para el token del túnel
├── README.md              # Documentación de uso (admin web)
├── .gitignore
├── backend/               # Spring Boot 3.3.5 / Java 21 → microservicio REST
├── frontend/              # React 18 + Vite → SPA del panel de administración (servida en Nginx)
├── android/               # App Android Kotlin + Jetpack Compose  "ARIngMovil" (admin + técnico)
├── deployment/            # Guía de Cloudflare Tunnel y script SQL de limpieza
└── performance/           # Escenarios k6 y resultados de pruebas de carga
```

### Dónde vive qué

| Capa | Ruta | Tecnología |
|---|---|---|
| API | `backend/src/main/java/com/aring/...` | Spring Boot (Web, Data JPA, Security, Validation), Lombok |
| BD | PostgreSQL 16 (Docker) | Hibernate `ddl-auto=update` |
| Panel web | `frontend/src/**` | React, axios, react-router v6, recharts, lucide-react |
| App móvil | `android/app/src/main/java/com/aring/movil/...` | Kotlin 2.0.21, Compose (BOM 2024.12.01), CameraX 1.4.1, OkHttp, EXIF 1.3.7, WorkManager para cola offline, SharedPreferences; navegación manual |

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

El `vite.config.js` hace **proxy de `/api` → http://localhost:8080** (`host: 0.0.0.0`, puerto 5173). El `frontend/nginx.conf` de producción enruta `location /api/` a `http://backend:8080/api/` y sirve la SPA con `try_files ... /index.html`.

### Acceso remoto observado en el servidor local (2026-10-31)

- El servidor Linux `ar-ingenieria` tiene Tailscale instalado, habilitado y conectado; en las capturas su IP Tailscale era `100.115.34.113`.
- Tailscale Funnel quedó activado para el puerto local **3000** (contenedor frontend/Nginx): `https://ar-ingenieria.taila89688.ts.net/`. Nginx sirve la web y pasa `/api/` al backend Docker. El acceso público funciona desde redes externas sin instalar Tailscale en cada móvil, mientras servidor, conexión a Internet y Funnel permanezcan activos.
- Funnel no publica directamente PostgreSQL ni el puerto 8080. La aplicación móvil usa el mismo origen HTTPS y la ruta `/api` proxificada por Nginx.
- Verificación local en el servidor: `sudo systemctl status tailscaled`, `sudo tailscale status` y `sudo tailscale funnel status`. El estado mostrado en las capturas no garantiza que Funnel siga activo hoy; comprobarlo si falla el acceso.
- El móvil Android necesita instalar el APK actualizado de la app; el acceso externo a la web se realiza con el navegador normal.

### La app Android

- `ApiService.BASE_URL` usa `BuildConfig.API_BASE_URL`, configurable mediante Gradle `-PAPI_BASE_URL=...`; valor por defecto de producción: `https://ar-ingenieria.taila89688.ts.net` (HTTPS Funnel → Nginx → `/api` backend). Para desarrollo/emulador se puede sobreescribir, por ejemplo `-PAPI_BASE_URL=http://10.0.2.2:8080`.
- Manifest con permisos `INTERNET` y `CAMERA`; `usesCleartextTraffic="true"` sigue habilitado para pruebas HTTP locales, aunque el valor predeterminado usa HTTPS.
- `minSdk 26 / targetSdk 35 / Java 17` · versionCode 1 · versionName 1.0 · AGP 8.7.3 · Gradle wrapper 8.12.

---

## 4. Usuarios y datos iniciales

`DataSeeder` se ejecuta en cada arranque. Si no existe el superusuario con usuario `29094728`, crea uno con nombre `Super Usuario` y la contraseña inicial codificada en el seeder (BCrypt). Si la cuenta ya existe, el seed **no restablece su contraseña**. El seeder también crea las pantallas `DASHBOARD`, `TECNICOS`, `TAREAS`, `DOCUMENTOS`, `CLIENTES` (habilitadas) y la secuencia `MANTENIMIENTO` en 0.

No existe actualmente una bandera `SEED_DEMO_DATA` ni un seed de administradores, técnicos, tareas o notificaciones; esas cuentas y registros se crean desde el sistema. Las credenciales iniciales del superusuario son solo para bootstrap/desarrollo: hay que cambiarlas antes de exponer una instalación.

El login (`POST /api/auth/login`) busca en este orden: Admin por `username` → SuperUsuario por `username` → Técnico por RUT. Las contraseñas se verifican con BCrypt. La base puede tener cuentas adicionales creadas después del seed.

---

## 5. Roles y reglas de acceso

- **AUTENTICACIÓN:** JWT (JJWT 0.12.6, HMAC), expiración **24 h** (`app.jwt.expiration-ms=86400000`), secret por defecto en `application.yml` (override vía `JWT_SECRET`). Header `Authorization: Bearer <token>`.
  - Claims del token: `rol` (`ROLE_ADMIN` / `ROLE_SUPER` / `ROLE_TECNICO`), `tecnicoId` (solo técnicos), `nombre`, `apellido`, subject = username o RUT.
  - Única ruta pública: `POST /api/auth/login`. Todo lo demás exige token. `@PreAuthorize("hasRole('SUPER')")` protege `/api/super/**`.
  - `SecurityFilterChain` stateless, CSRF off; la API exige autenticación salvo el login y `/api/super/**` además exige rol SUPER. CORS permite `localhost:*`, `127.0.0.1:*`, las IPs LAN codificadas (`192.168.1.1`, `.12`, `.13`, `.22`) y `https://ar-ingenieria.taila89688.ts.net` para el panel web. CORS aplica a navegador; el cliente Android nativo usa HTTPS/OkHttp.
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
| GET | `/tecnicos` | Lista ordenada por nombre/apellido |
| POST | `/tecnicos` | Crear — 409 si RUT duplicado ("Ya existe un técnico con el RUT ingresado.") |
| PUT | `/tecnicos/{id}` | Actualiza contacto de emergencia + contraseña opcional |
| DELETE | `/tecnicos/{id}` | Bloqueado con **409** si el técnico tiene tareas en curso (cabecera o invitado) con estado ≠ FINALIZADA |

### Clientes (`/api/clientes`)
| Método | Ruta | Nota |
|---|---|---|
| GET/POST | `/clientes` | Listar / crear clientes o recintos |
| PUT/DELETE | `/clientes/{id}` | Actualizar / eliminar cliente |

### Tareas (`/api/tareas`)
| Método | Ruta | Nota |
|---|---|---|
| GET | `/tareas` | Todas |
| GET | `/tareas/mias?tecnicoId=` | Tareas del técnico (cabecera o invitado), orden inverso, distinct |
| POST | `/tareas` | Solo `MANTENIMIENTO` (400 en otro tipo) — código gen `M-0001` con **lock pesimista** sobre `SecuenciaTarea` |
| POST | `/tareas/{id}/iniciar` | Body `{ayudantesIds:[Long]}` — crea notificaciones: `TAREA_INICIADA` al cabecera e `INVITACION` a los ayudantes |
| PUT | `/tareas/{id}` | Cambia técnico de cabecera (solo si `NO_INICIADA`) |
| PUT | `/tareas/{id}/estado` | `{estado}` — Transiciones válidas: `NO_INICIADA→EN_PROCESO`, `EN_PROCESO→FINALIZADA`; finalizar una tarea ya `FINALIZADA` ahora devuelve la tarea sin duplicar notificación (idempotencia para reintentos); 400 fallback "Transición de estado no permitida." |
| DELETE | `/tareas/{id}` | Solo si `NO_INICIADA` |

### Tableros (`/api/tareas/{tareaId}/tableros`)
| Método | Ruta | Nota |
|---|---|---|
| POST | `/tareas/{tareaId}/tableros` | Payload con medición, checklist, imágenes (base64), observaciones y `clientOperationId` opcional. La clave de operación tiene índice único; repetir una clave para la misma tarea devuelve el informe existente y evita duplicar el tablero/contador. Reusar la clave en otra tarea produce 409. |
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
- **Cliente** (`cliente`): registros de clientes/recintos gestionados desde el panel.
- **Tecnico** (`tecnico`): `nombre`, `apellido`, `rut` (UNIQUE — es el usuario de login), `password`, contacto de emergencia `contactoNombre/Apellido/Telefono`, `fechaCreacion (Instant)`.
- **Tarea** (`tarea`): `codigo` (UNIQUE, formato `M-0001`), `tipo` (TipoTarea), `direccion`, `nombreRecinto`, `descripcion (text)`, `estado` (EstadoTarea), `tecnicoCabecera → Tecnico (ManyToOne EAGER, nullable)`, `fechaCreacion/fechaInicio/fechaFin (Instant)`, `tablerosRealizados (int, default 0)`, y **N→N** con Tecnicos vía tabla `tarea_tecnico_invitado` (`tecnicosInvitados`).
- **SecuenciaTarea** (`secuencia_tarea`): `tipo (TYPO_TAREA, UNIQUE)`, `ultimoNumero` — contador para el código (`@Lock(PESSIMISTIC_WRITE)`).
- **Notificacion** (`notificacion`): sin FKs, solo ids. `tipo (TipoNotificacion)`, `mensaje`, `fecha`, `leida`, `tareaId`, `tecnicoDestinoId` (null = notificación global para admins).
- **PantallaConfig** (`pantalla_config`): `nombre` UNIQUE (`DASHBOARD|TECNICOS|TAREAS|DOCUMENTOS|CLIENTES`), `habilitada`.

### Informes de tableros (núcleo de la evidencia)
- **InformeTablero** (`informe_tablero`): `tarea → Tarea (FK)`, `tipoTablero (TipoTablero)`, `tamanio` (nullable, tamaño del tablero trifásico: Pequeño/Mediano/Grande/Especial), `descripcion`, `ubicacion`, `nivel`, `reapriete`, `aseo`, `tecnico → Tecnico (nullable = "Responsable Mantenimiento" del informe)`, `fechaCreacion`, `clientOperationId` (nullable, único, clave de idempotencia móvil). Con **4 colecciones `cascade ALL / orphanRemoval`** y FK `informe_id`.
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
- `TableroService.crear`: convierte `TableroRequest` → `InformeTablero` + colecciones; asigna el técnico responsable con prioridad al `tecnicoId **del JWT**` (token re-parseado en el controller, no usa `SecurityContextHolder` porque el principal es el RUT y el `tecnicoId` vive solo en claims); suma `tablerosRealizados`. Si llega un `clientOperationId` ya registrado para la misma tarea devuelve el informe previo sin volver a incrementar el contador.

### Documentos (`DocumentoService` — genera XLSX/DOCX con Apache POI)
⚠️ **No se usa OpenPDF** (dependencia declarada en el `pom.xml` pero sin uso); el "informe 'PDF'" del README es en realidad un **Documento Word (.docx)**.

- Excel "Historial de tareas": hoja "Historial" con grilla oculta, título B2:H2 (fondo DARK_BLUE, cuadri azul #D9E2F3), zebra #F2F7FB, freeze pane, fechas `dd/MM/yyyy HH:mm` Santiago. Sin datos: celda combinada "No hay tareas registradas con los filtros seleccionados.".
- Excel "Rendicion de servicios": bloque por tarea, columnas *Piso, Ubicación, Descripcion, Tipo de tablero, Tamaño tablero, Observaciones, Nivel de criticidad, Valor reparación* (2 últimas vacías, con formato moneda `$#,##0.00`); `tamanioTablero` en monofásico → **"No Aplica"**.
- Word: incluye el generador del informe de mantenimiento preventivo a partir de recursos de plantilla en `backend/src/main/resources/informe/`. La portada usa datos variables del edificio/ubicación/mes/año; las secciones incluyen instrumentación, pautas por tablero con técnico/ayudante/detalles y collage fotográfico, levantamiento/valorización, galería global de termografías, resumen y agradecimientos. Se omiten textos explicativos fechados que no son datos de la tarea. La composición concreta depende de las evidencias almacenadas en cada tarea.
- Filtros compartidos: `TareaSpecifications.conFiltros(...)` (ojo: **el filtro de fecha filtra por `fechaCreacion`, no por `fechaInicio`**).
- Errores: 404 "La tarea {codigo} no existe o aún no está finalizada." / "...no tiene tableros inspeccionados..." / "No hay tareas finalizadas con tableros registrados para los filtros seleccionados.".

### Tests (JUnit 5 + Mockito, 46 casos anotados `@Test`)
- `AdminServiceTest`, `ClienteServiceTest`, `TareaServiceTest`, `TecnicoServiceTest`, `DocumentoServiceTest` (este último valida el **ZIP real**); no hay tests de `TableroService`, `NotificacionService`, `PantallaConfigService`, ni seguridad/JWT.
- **Nota de build:** el Dockerfile del backend compila con `-DskipTests` (los tests no corren en el build Docker).

### Configuración y despliegue
`application.yml`: `spring.jackson.default-property-inclusion: non_null` · `open-in-view: false` · `ddl-auto: update` · servidor 8080. PostgreSQL y los servicios se levantan con Compose. `docker-compose.cloudflare.yml`/`deployment/CLOUDFLARE_TUNNEL.md` documentan el túnel opcional Cloudflare. El servidor observado por capturas usa Tailscale Funnel, no Cloudflare: Funnel publica el frontend/Nginx en el puerto 3000, y Nginx proxifica `/api/` al contenedor backend. URL observada: `https://ar-ingenieria.taila89688.ts.net`.

La app Android usa esa URL HTTPS como predeterminada; la propiedad Gradle `API_BASE_URL` (`BuildConfig`) permite sobreescribirla. En el servidor Linux (si el checkout está en `~/Desktop/AR-Ingenieria`), actualizar y reconstruir el backend con:

```bash
cd ~/Desktop/AR-Ingenieria
git pull origin main
sudo docker compose up -d --build backend
sudo docker compose ps
sudo docker compose logs --tail=100 backend
```

El commit `3f663a2` (`Support offline task completion and sync`) fue subido a `origin/main`. No se ha verificado desde este repositorio si esos cambios ya fueron desplegados en el servidor. El Dockerfile del backend compila Maven (`-DskipTests`) y al iniciar Hibernate `ddl-auto=update` agrega la columna/índice de idempotencia. El frontend no requiere reconstrucción para este cambio de sincronización.

Docker backend: imagen base `maven:3.9-eclipse-temurin-21` build + `eclipse-temurin:21-jre` runtime; `.dockerignore` excluye `target/`. Imagen de salida `aring-backend-1.0.0.jar`.

**Tamaño de cuerpos en Nginx (2026-10-31):** los informes móviles llevan fotos Base64 embebidas y superan el límite default de Nginx (1 MB) → el servidor responde HTTP **413 Request Entity Too Large** y el outbox nunca confirmaba. `frontend/nginx.conf` define `client_max_body_size 128m` + `proxy_send/read_timeout 180s` para `/api/`. Al desplegar: `sudo docker compose up -d --build frontend` (no requiere reconstruir backend).

**Contador de tableros en móvil:** `Repositorio.conEstadoLocal` suma al `tablerosRealizados` del servidor los informes aún pendientes en el outbox (`OfflineSync.informes(id).size`), de modo que el contador no aparece en cero tras guardar tableros sin conexión.

**Historial del técnico (2026-10-31):** la pestaña Tareas del técnico agrega la sección "Tareas finalizadas" (`Repositorio.finalizadasDeTecnico`, solo lectura): las tareas ya completadas dejan de desaparecer de la lista y se abren en modo detalle sin botón de inicio. Antes, toda tarea FINALIZADA quedaba oculta para el técnico.

**Selector de clientes al crear tarea (web):** `Tareas.jsx` normaliza la búsqueda (`normalizar()` quita tildes/ñ) para sugerencias tolerantes a acentos, muestra todos los clientes registrados al enfocar el campo con texto vacío y avisa cuando no hay coincidencia ("La tarea se creará sin cliente asociado"); sin cliente seleccionado el `clienteId` va null.

**Selector de clientes al crear tarea (móvil admin, paridad con web):** la app Android del administrador ahora descarga `/api/clientes` (`Repositorio.clientes`, cargado en `cargarAdmin`) y en el modal "Crear tarea" despliega sugerencias al escribir la dirección, insensibles a tildes/ñ (`Repositorio.sugerenciasClientes`); seleccionar un cliente rellena recinto y envía `clienteId` a `POST /tareas`, de modo que la tarea queda vinculada al cliente igual que en el panel web.

---

## 9. Frontend (panel web React)

### Arquitectura y App
- Providers, de afuera hacia dentro: `BrowserRouter → AuthProvider → RefreshProvider → App (Layout) → PantallasProvider → AppRoutes`.
- Sesión: `localStorage['aring_admin'] = {token, nombre, apellido, iniciales, username, rol}` (`AuthContext`). `api/client.js` instancia axios con `baseURL='/api'` que inyecta `Authorization: Bearer <token>` y en 401 limpia la sesión y redirige a `/login`. `mensajeErrorBlob(error)` para respuestas de descarga fallidas con body Blob.
- Contextos: `RefreshContext` (botón refresco global "key+1" en TopBar; Dashboard/Tecnicos/Tareas escuchan `key`) y `PantallasContext` (feature-toggle de pantallas, `fail-open` — si falla la carga asume todo habilitado).
- **Login:** checkbox "Recordar credenciales" guarda usuario y contraseña en texto plano en `localStorage['aring_recordar']`.

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
- **Tareas:** tarjetas con `codigo` (ej. M-0001) + ChipTipo + ChipEstado; acciones según estado: No iniciada → Editar/Eliminar; En proceso → finalizar vía `PUT /tareas/{id}/estado` con `estado:'FINALIZADA'`; Finalizada → solo chip. Modal "Limpiar finalizadas" solo oculta del estado local (ids en `aring_finalizadas_ocultas` en localStorage; no borra de BD). Modal "Crear tarea": solo MANTENIMIENTO está disponible; Reparación/Cotización están deshabilitadas. Campos: dirección, nombre del recinto, descripción y técnico de cabecera. "Editar tarea" cambia solo el técnico de cabecera.
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
      única pestaña: Tareas
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
- **Persistencia local:** `TableroDraftStore` (SharedPreferences `"aring_tableros"`, clave `draft_{tecnicoId}_{tareaId}_{tipo}` desde el 2026-10-31; antes era `draft_{tareaId}_{tipo}`) guarda cada cambio del formulario. El técnico en la clave evita que el ayudante retome —en el mismo dispositivo— el borrador que dejó el técnico de cabecera: cada cuenta ve solo su propio tablero en curso y abre siempre uno nuevo de los demás. `OfflineTaskCache` conserva en SharedPreferences la última lista de tareas descargada para reabrirla sin red. Las fotos quedan en `filesDir/photos/`.
- **Aviso "La sincronización está pendiente" (2026-10-31):** aparece **una sola vez por pantalla** — como banner rojo superior en la lista de tareas del técnico — y ya sin la línea roja dentro de cada tarjeta de tarea. Desaparece automáticamente al reconectar: `MainScreenTecnico` observa la red (`NetworkCallback`) y el worker `aring-offline-sync` (`LiveData`), refrescando la UI cuando el servidor confirma el envío. Requiere APK con estos cambios instalado en el equipo.
- **Outbox/sincronización:** `OfflineSync` guarda cada informe JSON completo (incluidas fotos Base64) en archivos atómicos bajo `filesDir/offline-outbox/`, y un marcador `finish-{tareaId}.json` para finalizar después. WorkManager (`NetworkType.CONNECTED`) sube informes antes de pedir al backend marcar la tarea finalizada; usa reintentos y la clave `clientOperationId` UUID para que un envío repetido no duplique tableros. La cola se retoma al abrir la app y cada informe/tarea queda persistido hasta recibir confirmación del servidor.
- **Reactividad de conexión (2026-10-31):** `MainScreenTecnico` registra un `ConnectivityManager.NetworkCallback` (al volver la conexión re-programa el outbox y refresca `cargarTecnico`) y observa con `LiveData` el estado del trabajo único `aring-offline-sync` (`WorkManager`): al terminar con éxito la UI se actualiza sola — el aviso rojo de sincronización desaparece sin tocar nada y la tarea local pasa de "En proceso" a "Finalizada".
- **Iniciado offline (`start-{tareaId}.json`):** `OfflineSync.guardarInicio` encola el inicio con invitados cuando falla el POST por red. `IngresarTareaScreen` detecta el fallo de conexión (`ApiException.codigo == null`), marca la tarea local como "En proceso" pendiente de sincronizar y permite continuar con los tableros sin conexión. El worker procesa los archivos en orden **start → report → finish**, de modo que las notificaciones `INVITACION` se generan al reconectar. Con esto, el backend `/tareas/{id}/iniciar` es ahora idempotente: si la tarea ya está EN_PROCESO responde 200 con la tarea sin duplicar notificaciones (requiere re-deploy del backend).
- **Refresco periódico (Fase 3):** `RefrescoTecnicoWorker` (único periódico `aring-refresco-tecnico`, 15 min, `CONNECTED`, programado en `MainActivity`) re-lee tareas, técnicos y notificaciones del técnico en segundo plano, para que la invitación aparezca sin abrir/refrescar manualmente la app (tiempo real exacto requeriría FCM/push, no implementado).
- **Autenticación de la cola:** guarda RUT y contraseña por técnico en preferencias privadas `aring_outbox_auth` mientras tenga envíos pendientes, para que la sincronización siga funcionando tras cerrar sesión. Al terminar su cola, borra esas credenciales. Al actualizar desde una versión previa intenta asociar el outbox existente con la sesión de técnico guardada. La pantalla permite cerrar sesión con cola pendiente; los informes siguen en espera.
- **Envío del informe:** `EnviarInformeScreen` construye el JSON con `TableroRepository` (fotos Base64, JPEG q85 máx. 1280px vía `decodeSampledBitmap`) y lo pone en la cola local. La pantalla confirma que quedó guardado en el dispositivo; WorkManager realiza el POST cuando hay red. La cuenta local de tableros se incrementa para permitir finalizar aunque el worker ya haya subido el informe y retirado el archivo de la cola.
- **Finalizar tarea** (cabecera, en `TableroSeleccion`): valida que haya al menos un tablero conocido (servidor o registrado localmente), guarda el marcador de finalización y muestra `TableroConfirmacion` indicando pendiente. Hasta que el backend confirme, la tarea local se muestra como finalizada con sincronización pendiente; no se debe interpretar ese estado local como confirmado por el servidor.
- **Errores de sincronización:** `OfflineSync` conserva el último mensaje de error del servidor. El técnico puede reintentar desde la actualización de la barra superior o al abrir la app. La UI de técnico no descarga informes; solo el panel web usa las rutas de documentos. El listado del administrador cambia cuando la petición de finalización llega al backend.

### Cámara y anotación de evidencia
- `PhotoBus` (singleton con `MutableStateFlow`) para comunicación pantalla↔cámara: `PhotoRequest(phased, category PREVIOUS|MEASUREMENT|CHECKLIST|POST, referenceId?, slotIndex, label)`.
- `PhotoStorage` guarda en `filesDir/photos/foto_{ts}_{uuid}.jpg`; `CameraScreen` ocupa la pantalla, usa **CameraX 1.4.1** con `CAPTURE_MODE_MINIMIZE_LATENCY`, flash y bloqueo durante la captura. Al tomar la foto se guarda y devuelve automáticamente al formulario, sin paso de confirmación "Usar foto". Runtime de permisos CAMERA con `RequestPermission`.
- El anotador abre en diálogo de pantalla completa; mantiene la proporción de la foto (sin deformarla ni recortarla), permite dibujar, deshacer/borrar, cancelar y confirmar. Guarda la anotación como archivo nuevo y preserva el original; sus controles consideran las barras del sistema.
- Corrección de **orientación EXIF** en la subida (dependencia `androidx.exifinterface`).

### Pantallas del técnico
- `TareasTecnicoScreen`: tareas propias, chips "Invitado" y "Pendiente de sincronizar" para cierres locales aún no confirmados; clic → Detalle.
- `DetalleTareaTecnico`: tarjeta datos + **"Ir a la dirección"** (intento `geo:0,0?q=dirección` con funcionalidad de maps) + "Ingresar/Retomar tarea".
- `IngresarTareaScreen` (cabecera): si ya en proceso muestra fecha inicio + "Retomarás la tarea desde el último punto guardado."; si no, selector de hasta **5 invitados** ("Invitaciones: N/5") → `Iniciar tarea`.
- El rol técnico ya no tiene pestañas Inicio ni Documentos, ni función móvil para descargar documentos. La descarga de informes continúa disponible en el panel web del administrador; el endpoint del backend sigue perteneciendo a esa funcionalidad.
- La pantalla Tareas muestra los cierres locales todavía pendientes de sincronizar. `OfflineSync` guarda el último error; el técnico puede pedir un reintento con el botón de actualización de la barra superior o al abrir la aplicación. Al quitar la pantalla Documentos, el error ya no se muestra en un banner dedicado.

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

1. **Secretos en claro**: JWT secret por defecto y contraseña inicial del superusuario están en el código; contraseñas/credenciales/tokens en `SharedPreferences` Android sin `EncryptedSharedPreferences`; "Recordar credenciales" guardado en texto plano en `localStorage`. No guardar el token JWT de autenticación dentro de resultados k6 que se compartan.
2. **Cleartext HTTP permitido en móvil**: `usesCleartextTraffic="true"` se conserva para desarrollo local, aunque el `API_BASE_URL` predeterminado ya usa HTTPS Funnel. No compilar producción con una URL HTTP pública.
3. **Payloads grandes**: fotos Base64 embebidas en JSON (write timeout 90 s en OkHttp) — considerar multipart o redimensionado menor si el dataset crece.
4. **Sin Retrofit/Kotlinx-serialization**: JSON construido manualmente con `org.json` — no se usa un modelo de fallo tipado (mejorar con serialización de tipo seguro si continúa el desarrollo).
5. Cobertura de tests limitada (sin tests de `TableroService`, `NotificacionService`, pantallas, WorkManager/offline o security/JWT; build Docker usa `-DskipTests`). El APK Android se compiló correctamente en el entorno de desarrollo; los cambios del backend no se compilaron localmente porque no había Maven instalado (el Dockerfile sí contiene Maven para construirlo en el servidor).
6. La actualización del menú del superusuario usa actualización optimista; conviene verificar que la UI revierta el interruptor si falla `PUT /super/pantallas`.
7. El panel web permite finalizar una tarea en proceso directamente; el flujo móvil del técnico valida y envía los tableros antes de finalizar.
8. El filtro por código en endpoints de informes permanece por compatibilidad; la pantalla de Documentos ahora prioriza búsqueda de edificio y selección de una tarea finalizada.

---

## 13. Prueba de carga y rendimiento (2026-10-30)

Se agregó un escenario k6 de solo lectura en `performance/k6/super-admin-readonly.js` y dos resultados de ejecución en `performance/results/`. `500vu-20261030-213019.json` registra 500 VUs máximos, 52.322 solicitudes HTTP (157,7 solicitudes/s), 0 respuestas HTTP fallidas, 52.321 checks correctos, latencia promedio 4,15 ms, p95 6,56 ms, p99 11,82 ms y máximo 157,60 ms. El directorio también incluye `500vu-20261030-212930.json`.

Es una prueba local dentro del entorno de desarrollo, sobre rutas de lectura y la página inicial. No representa 500 sesiones reales con navegador/móvil y no incluye carga de fotos, generación masiva de documentos, escrituras ni red móvil/infraestructura de producción. No extrapolar estas cifras directamente a producción.

---

## Anexo — checklist inicial de desarrollo

- [ ] ¿Reproducir localmente con Docker (`docker compose up --build`)?
- [ ] Login web con el superusuario inicial (solo si el usuario no existía previamente y no se cambió su contraseña; revisar `DataSeeder`).
- [ ] Crear un administrador y un técnico desde la aplicación antes de probar los flujos correspondientes.
- [ ] Crear tarea (MANTENIMIENTO) → iniciar con ayudantes → registrar tablero monofásico → finalizar → notificar.
- [ ] Generar informes web (Historial Excel / Rendición Excel / Informe Word).

---

*Creado/documentado por Eliezer Carrasco para **AR-Ingeniería** · © 2026 · Versión 1.0*
