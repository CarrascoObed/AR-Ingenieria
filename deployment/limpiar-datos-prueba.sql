BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM super_usuario WHERE username = '29094728') THEN
        RAISE EXCEPTION 'No se encontró el superusuario principal (29094728); se cancela la limpieza.';
    END IF;
END
$$;

DELETE FROM super_usuario WHERE username <> '29094728';

-- Elimina cuentas administrativas, tecnicos, clientes, tareas y registros relacionados.
-- CASCADE incluye evidencias, informes, checklist, mediciones y relaciones de tareas.
-- Se conserva el superusuario principal, pantalla_config y secuencia_tarea.
TRUNCATE TABLE admin, cliente, tarea, tecnico, notificacion
    RESTART IDENTITY CASCADE;

COMMIT;
