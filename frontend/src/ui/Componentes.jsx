import { Loader2, X } from 'lucide-react';

export function Avatar({ nombre, apellido, size = 40, fontSize }) {
  const iniciales = ((nombre || '')[0] || '') + ((apellido || '')[0] || '');
  return (
    <div
      className="avatar"
      style={{ width: size, height: size, fontSize: fontSize || size * 0.38 }}
    >
      {iniciales.toUpperCase()}
    </div>
  );
}

export function Cargando({ mensaje = 'Cargando…' }) {
  return (
    <div className="cargando">
      <Loader2 className="rotando" size={20} />
      {mensaje}
    </div>
  );
}

export function Vacio({ mensaje }) {
  return <div className="vacio">{mensaje}</div>;
}

export function ErrorBanner({ mensaje }) {
  if (!mensaje) return null;
  return <div className="error-banner">{mensaje}</div>;
}

export function OkBanner({ mensaje }) {
  if (!mensaje) return null;
  return <div className="ok-banner">{mensaje}</div>;
}

export function Modal({ titulo, abierto, onCerrar, children, ancho }) {
  if (!abierto) return null;
  return (
    <div className="modal-overlay" onClick={onCerrar}>
      <div
        className="modal"
        style={ancho ? { maxWidth: ancho } : undefined}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-cabecera">
          <h3>{titulo}</h3>
          <button className="icono-btn" onClick={onCerrar} aria-label="Cerrar">
            <X size={20} />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function SeccionFormulario({ titulo, children }) {
  return (
    <div className="seccion-formulario">
      <h4>{titulo}</h4>
      {children}
    </div>
  );
}

export function Confirmar({ abierto, titulo, mensaje, confirmarTexto = 'Eliminar', cargando, centrado, onConfirmar, onCancelar }) {
  if (!abierto) return null;
  return (
    <div
      className="modal-overlay"
      style={centrado ? { alignItems: 'center' } : undefined}
      onClick={onCancelar}
    >
      <div
        className="modal"
        style={{ maxWidth: 420 }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-cabecera">
          <h3>{titulo}</h3>
          <button className="icono-btn" onClick={onCancelar} aria-label="Cerrar">
            <X size={20} />
          </button>
        </div>
        <p style={{ color: 'var(--texto-gris)', margin: '0 0 20px', textAlign: 'center' }}>{mensaje}</p>
        <div className="modal-pie">
          <button className="btn btn-secundario" onClick={onCancelar} disabled={cargando}>
            Cancelar
          </button>
          <button className="btn btn-riesgo" onClick={onConfirmar} disabled={cargando}>
            {cargando ? <Loader2 className="rotando" size={16} /> : null}
            {confirmarTexto}
          </button>
        </div>
      </div>
    </div>
  );
}

export function ChipEstado({ estado }) {
  const map = {
    'No iniciada': { clase: 'chip-rojo', punto: 'var(--rojo-error)' },
    'En proceso': { clase: 'chip-amarillo', punto: 'var(--amarillo)' },
    'Finalizada': { clase: 'chip-verde', punto: 'var(--verde-ok)' },
  };
  const cfg = map[estado] || { clase: 'chip-gris', punto: 'var(--texto-gris)' };
  return (
    <span className={`chip ${cfg.clase}`}>
      <span
        style={{
          width: 8,
          height: 8,
          borderRadius: '50%',
          background: cfg.punto,
          display: 'inline-block',
        }}
      />
      {estado}
    </span>
  );
}
