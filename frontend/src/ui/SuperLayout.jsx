import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ArrowLeftRight, LogOut } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { Avatar, Confirmar } from './Componentes';

export default function SuperLayout({ children }) {
  const { admin, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [confirmarSalida, setConfirmarSalida] = useState(false);

  const cerrarSesion = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="super-inicio">
      <header className="topbar">
        <div className="topbar-izq">
          <div className="topbar-logo">
            <img src="/favicon.svg" alt="Logo" />
            <span className="nombre">Super Usuario</span>
          </div>
        </div>
        <div className="topbar-der">
          {location.pathname.startsWith('/super') && (
            <button
              className="btn btn-secundario btn-chico"
              title="Volver al inicio del super usuario"
              onClick={() => navigate('/')}
            >
              <ArrowLeftRight size={16} />
              Salir del modo super usuario
            </button>
          )}

          <div className="topbar-usuario">
            <Avatar nombre={admin?.nombre} apellido={admin?.apellido} size={34} fontSize={14} />
            <span className="nombre">
              {admin?.nombre} {admin?.apellido}
            </span>
          </div>
          <button
            className="topbar-icono"
            title="Cerrar sesión"
            aria-label="Cerrar sesión"
            onClick={() => setConfirmarSalida(true)}
          >
            <LogOut size={20} />
          </button>
        </div>
      </header>

      <main className="super-contenido">{children}</main>

      <Confirmar
        abierto={confirmarSalida}
        titulo="Cerrar sesión"
        mensaje="¿Está seguro que desea cerrar la sesión?"
        confirmarTexto="Sí"
        centrado
        onConfirmar={cerrarSesion}
        onCancelar={() => setConfirmarSalida(false)}
      />
    </div>
  );
}
