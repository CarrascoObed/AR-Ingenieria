# Acceso remoto con Cloudflare Tunnel

Este proyecto se publica mediante el frontend de Nginx. Nginx reenvía `/api/` al backend dentro de Docker; PostgreSQL no se publica en el túnel.

## Requisitos

- Un dominio propio agregado a una cuenta de Cloudflare.
- Un túnel administrado desde el panel de Cloudflare con una ruta de aplicación pública.
- El token de ese túnel, guardado solo en `.env` del servidor (ese archivo está excluido de Git).

## Configuración en Cloudflare

1. En Cloudflare Zero Trust, crea un túnel remoto y copia su token.
2. Añade una ruta pública para el subdominio deseado, por ejemplo `app.tudominio.cl`.
3. Configura el servicio de origen como `http://frontend:80`.

## Arrancar el túnel en el servidor

Desde la carpeta del repositorio:

```bash
cp .env.example .env
nano .env
```

Reemplaza el valor de `CLOUDFLARE_TUNNEL_TOKEN` por el token real. No subas `.env` a GitHub.

Luego ejecuta:

```bash
sudo docker compose --env-file .env -f docker-compose.yml -f docker-compose.cloudflare.yml up -d --build
sudo docker compose --env-file .env -f docker-compose.yml -f docker-compose.cloudflare.yml ps
```

El contenedor del túnel usa `restart: unless-stopped`, por lo que Docker lo vuelve a iniciar cuando arranca el servidor.

## Aplicación Android

La URL del backend se puede definir al compilar el APK. Para que el teléfono use el dominio público, compila con el mismo origen del sitio (sin `/api` al final):

```bash
cd android
./gradlew assembleRelease -PAPI_BASE_URL=https://app.tudominio.cl
```

En Windows se usa `gradlew.bat` en lugar de `./gradlew`. Si no se entrega la propiedad, se conserva la URL local actual para pruebas en la red local.

## Prueba temporal sin dominio

Cloudflare Quick Tunnel puede servir para una prueba breve con una URL temporal. No es adecuada como dirección estable para los teléfonos; para uso habitual configura un dominio propio en Cloudflare.
