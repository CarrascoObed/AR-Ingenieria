BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM super_usuario) THEN
        RAISE EXCEPTION 'No hay superusuario; se cancela la limpieza para evitar dejar el sistema sin acceso.';
    END IF;
END
$$;

-- Elimina cuentas administrativas, tecnicos, clientes, tareas y registros relacionados.
-- CASCADE incluye evidencias, informes, checklist, mediciones y relaciones de tareas.
-- super_usuario, pantalla_config y secuencia_tarea se conservan.
TRUNCATE TABLE admin, cliente, tarea, tecnico, notificacion
    RESTART IDENTITY CASCADE;

COMMIT;
