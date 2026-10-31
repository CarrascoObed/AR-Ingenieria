import { useNavigate } from 'react-router-dom';
import { LayoutDashboard, ShieldCheck } from 'lucide-react';

export default function SuperInicio() {
  const navigate = useNavigate();

  return (
    <>
      <h1 className="titulo-pagina">Panel del Super Usuario</h1>
      <p className="subtitulo-pagina">
        Selecciona el modo en el que deseas trabajar.
      </p>

      <div className="super-opciones">
        <button className="super-opcion" onClick={() => navigate('/admin')}>
          <span className="super-opcion-icono">
            <LayoutDashboard size={40} />
          </span>
          <span className="super-opcion-titulo">Modo administrador</span>
          <span className="super-opcion-desc">
            Gestiona el panel como un administrador: dashboard, tareas, técnicos y documentos.
          </span>
        </button>

        <button className="super-opcion" onClick={() => navigate('/super')}>
          <span className="super-opcion-icono">
            <ShieldCheck size={40} />
          </span>
          <span className="super-opcion-titulo">Modo super usuario</span>
          <span className="super-opcion-desc">
            Administra administradores, pantallas, descargas, y herramientas avanzadas del sistema.
          </span>
        </button>
      </div>
    </>
  );
}
