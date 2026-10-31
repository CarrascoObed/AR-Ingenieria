import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Bell, LogOut, RefreshCw, Trash2, ArrowLeftRight } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { useRefresh } from './RefreshContext';
import { api, mensajeError } from '../api/client';
import { Avatar, Confirmar } from './Componentes';

export default function TopBar() {
  const { admin, logout } = useAuth();
  const { key, trigger } = useRefresh();
  const navigate = useNavigate();
  const location = useLocation();

  const [notificaciones, setNotificaciones] = useState([]);
  const [notifAbierto, setNotifAbierto] = useState(false);
  const [refrescando, setRefrescando] = useState(false);
  const [confirmarSalida, setConfirmarSalida] = useState(false);
  const notifRef = useRef(null);

  const noLeidas = notificaciones.filter((n) => !n.leida).length;

  const cargarNotificaciones = async () => {
    try {
      const res = await api.get('/notificaciones');
      setNotificaciones(res.data);
    } catch {
      /* silencioso */
    }
  };

  useEffect(() => {
    cargarNotificaciones();
  }, [key]);

  useEffect(() => {
    const cerrar = (e) => {
      if (notifRef.current && !notifRef.current.contains(e.target)) {
        setNotifAbierto(false);
      }
    };
    document.addEventListener('mousedown', cerrar);
    return () => document.removeEventListener('mousedown', cerrar);
  }, []);

  const onActualizar = async () => {
    setRefrescando(true);
    trigger();
    await cargarNotificaciones();
    setTimeout(() => setRefrescando(false), 600);
  };

  const marcarLeida = async (id) => {
    try {
      await api.put(`/notificaciones/${id}/leida`);
      setNotificaciones((prev) => prev.map((n) => (n.id === id ? { ...n, leida: true } : n)));
    } catch (e) {
      console.error(mensajeError(e));
    }
  };

  const marcarTodas = async () => {
    try {
      setNotificaciones((prev) => prev.map((n) => ({ ...n, leida: true })));
      await api.put('/notificaciones/leer-todas');
    } catch (e) {
      console.error(mensajeError(e));
    }
  };

  const alternarNotificaciones = async () => {
    const abrir = !notifAbierto;
    setNotifAbierto(abrir);
    if (abrir && noLeidas > 0) {
      await marcarTodas();
    }
  };

  const limpiarNotificaciones = async () => {
    try {
      await api.delete('/notificaciones');
      setNotificaciones([]);
      setNotifAbierto(false);
    } catch (e) {
      console.error(mensajeError(e));
    }
  };

  const cerrarSesion = () => {
    logout();
    navigate('/login');
  };

  const formatFecha = (iso) => {
    if (!iso) return '';
    try {
      return new Date(iso).toLocaleString('es-CL', {
        day: '2-digit',
        month: 'short',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return '';
    }
  };

  return (
    <>
      <header className="topbar">
      <div className="topbar-izq">
        <div className="topbar-logo">
          <img src="/favicon.svg" alt="Logo" />
          <span className="nombre">
            {admin?.rol === 'ROLE_SUPER' ? 'Super Usuario' : 'Panel de Administración'}
          </span>
        </div>
      </div>

      <div className="topbar-der">
        {admin?.rol === 'ROLE_SUPER' && location.pathname.startsWith('/admin') && (
          <button
            className="btn btn-secundario btn-chico"
            title="Volver al inicio del super usuario"
            onClick={() => navigate('/')}
          >
            <ArrowLeftRight size={16} />
            Salir del modo administrador
          </button>
        )}

        <button
          className="topbar-icono"
          title="Actualizar información"
          onClick={onActualizar}
          aria-label="Actualizar"
        >
          <RefreshCw size={20} className={refrescando ? 'rotando' : ''} />
        </button>

        <div className="notif-wrap" ref={notifRef}>
          <button
            className="topbar-icono"
            title="Notificaciones"
            aria-label="Notificaciones"
            onClick={alternarNotificaciones}
          >
            <Bell size={20} />
            {noLeidas > 0 && <span className="notif-badge">{noLeidas}</span>}
          </button>

          {notifAbierto && (
            <div className="notif-dropdown">
              <div className="notif-dropdown-cabecera">
                <span>Notificaciones</span>
                {notificaciones.length > 0 && (
                  <div className="notif-acciones">
                    {noLeidas > 0 && (
                      <button className="btn btn-chico btn-secundario" onClick={marcarTodas}>
                        Leer todas
                      </button>
                    )}
                    <button className="btn btn-chico btn-riesgo" onClick={limpiarNotificaciones}>
                      <Trash2 size={14} />
                      Limpiar
                    </button>
                  </div>
                )}
              </div>
              <div className="notif-lista">
                {notificaciones.length === 0 && (
                  <div className="notif-vacio">No tienes notificaciones.</div>
                )}
                {notificaciones.map((n) => (
                  <div
                    key={n.id}
                    className={`notif-item ${n.leida ? '' : 'no-leida'}`}
                    onClick={() => !n.leida && marcarLeida(n.id)}
                  >
                    <div>
                      <div className="mensaje">{n.mensaje}</div>
                      <div className="fecha">{formatFecha(n.fecha)}</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

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

    <Confirmar
        abierto={confirmarSalida}
        titulo="Cerrar sesión"
        mensaje="¿Está seguro que desea cerrar la sesión?"
        confirmarTexto="Sí"
        centrado
        onConfirmar={cerrarSesion}
        onCancelar={() => setConfirmarSalida(false)}
      />
    </>
  );
}
