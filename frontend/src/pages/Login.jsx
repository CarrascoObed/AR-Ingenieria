import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Loader2, LogIn } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { mensajeError } from '../api/client';

const CLAVE_RECORDAR = 'aring_recordar';

function cargarRecordadas() {
  try {
    return JSON.parse(localStorage.getItem(CLAVE_RECORDAR)) || null;
  } catch {
    return null;
  }
}

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [recordadas] = useState(cargarRecordadas);

  const [username, setUsername] = useState(recordadas?.username || '');
  const [password, setPassword] = useState(recordadas?.password || '');
  const [recordar, setRecordar] = useState(!!recordadas);
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(false);
  const [verPassword, setVerPassword] = useState(false);

  useEffect(() => {
    if (!recordar) {
      localStorage.removeItem(CLAVE_RECORDAR);
    }
  }, [recordar]);

  const completo = username.trim() !== '' && password !== '';

  const onSubmit = async (e) => {
    e.preventDefault();
    if (!completo) return;
    setCargando(true);
    setError('');
    try {
      if (recordar) {
        localStorage.setItem(
          CLAVE_RECORDAR,
          JSON.stringify({ username: username.trim(), password })
        );
      } else {
        localStorage.removeItem(CLAVE_RECORDAR);
      }
      await login(username.trim(), password);
      navigate('/', { replace: true });
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  };

  return (
    <div className="login-fondo">
      <div className="login-tarjeta">
        <div className="login-logo">
          <img src="/favicon.svg" alt="Logo mantenimiento eléctrico" width={64} height={64} />
          <h1>Panel de Administración</h1>
          <p>Gestión de mantenimiento eléctrico</p>
        </div>

        <form onSubmit={onSubmit}>
          <div className="campo">
            <label htmlFor="username">Usuario</label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Ingrese su usuario"
              autoComplete="username"
            />
          </div>
          <div className="campo">
            <label htmlFor="password">Contraseña</label>
            <div className="campo-contraseña">
              <input
                id="password"
                type={verPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Ingrese su contraseña"
                autoComplete="current-password"
              />
              <button
                type="button"
                className="ojo-btn"
                onClick={() => setVerPassword((v) => !v)}
                aria-label={verPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                title={verPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
              >
                {verPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>

          <label className="checkbox-recordar">
            <input
              type="checkbox"
              checked={recordar}
              onChange={(e) => setRecordar(e.target.checked)}
            />
            <span>Recordar credenciales</span>
          </label>

          {error && <div className="error-banner">{error}</div>}

          <button
            type="submit"
            className="btn btn-primario"
            style={{ width: '100%', marginTop: 8 }}
            disabled={!completo || cargando}
          >
            {cargando ? <Loader2 className="rotando" size={18} /> : <LogIn size={18} />}
            Iniciar sesión
          </button>
        </form>
      </div>

      <footer className="login-footer">
        © 2026 · Creado por Eliezer Carrasco para AR-Ingeniería · Versión 1.0 · Todos los
        derechos reservados.
      </footer>
    </div>
  );
}
