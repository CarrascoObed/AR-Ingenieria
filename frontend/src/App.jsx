import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import { PantallasProvider, usePantallas } from './ui/PantallasContext';
import TopBar from './ui/TopBar';
import Sidebar from './ui/Sidebar';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Clientes from './pages/Clientes';
import Tecnicos from './pages/Tecnicos';
import Tareas from './pages/Tareas';
import Documentos from './pages/Documentos';
import SuperInicio from './pages/SuperInicio';
import SuperPanel from './pages/SuperPanel';

function Layout({ base = '/' }) {
  return (
    <>
      <TopBar />
      <div className="layout">
        <Sidebar base={base} />
        <main className="contenido">
          <Outlet />
        </main>
      </div>
    </>
  );
}

function RequireAdmin({ children }) {
  const { admin } = useAuth();
  return admin?.rol === 'ROLE_ADMIN' ? children : <Navigate to="/login" replace />;
}

function RequireSuper({ children }) {
  const { admin } = useAuth();
  return admin?.rol === 'ROLE_SUPER' ? children : <Navigate to="/login" replace />;
}

function RequirePanelAdmin({ children }) {
  const { admin } = useAuth();
  return admin?.rol === 'ROLE_ADMIN' || admin?.rol === 'ROLE_SUPER' ? (
    children
  ) : (
    <Navigate to="/login" replace />
  );
}

function PantallaHabilitada({ nombre, etiqueta, children }) {
  const { habilitada } = usePantallas();
  if (habilitada(nombre)) return children;
  return (
    <div className="tarjeta-padding">
      <p>
        La pantalla <strong>{etiqueta}</strong> está deshabilitada por el super usuario.
      </p>
    </div>
  );
}

function Home() {
  const { admin } = useAuth();
  if (admin?.rol === 'ROLE_SUPER') return <SuperInicio />;
  return <Dashboard />;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route element={<RequirePanelAdmin><Layout /></RequirePanelAdmin>}>
        <Route path="/" element={<Home />} />
        <Route
          path="/clientes"
          element={
            <RequireAdmin>
              <PantallaHabilitada nombre="CLIENTES" etiqueta="Clientes">
                <Clientes />
              </PantallaHabilitada>
            </RequireAdmin>
          }
        />
        <Route
          path="/tecnicos"
          element={
            <RequireAdmin>
              <PantallaHabilitada nombre="TECNICOS" etiqueta="Técnicos">
                <Tecnicos />
              </PantallaHabilitada>
            </RequireAdmin>
          }
        />
        <Route
          path="/tareas"
          element={
            <RequireAdmin>
              <PantallaHabilitada nombre="TAREAS" etiqueta="Tareas">
                <Tareas />
              </PantallaHabilitada>
            </RequireAdmin>
          }
        />
        <Route
          path="/documentos"
          element={
            <RequireAdmin>
              <PantallaHabilitada nombre="DOCUMENTOS" etiqueta="Documentos">
                <Documentos />
              </PantallaHabilitada>
            </RequireAdmin>
          }
        />
      </Route>

      <Route element={<RequireSuper><Layout base="/admin" /></RequireSuper>}>
        <Route path="/admin" element={<PantallaHabilitada nombre="DASHBOARD" etiqueta="Dashboard"><Dashboard /></PantallaHabilitada>} />
        <Route
          path="/admin/clientes"
          element={
            <PantallaHabilitada nombre="CLIENTES" etiqueta="Clientes">
              <Clientes />
            </PantallaHabilitada>
          }
        />
        <Route
          path="/admin/tecnicos"
          element={
            <PantallaHabilitada nombre="TECNICOS" etiqueta="Técnicos">
              <Tecnicos />
            </PantallaHabilitada>
          }
        />
        <Route
          path="/admin/tareas"
          element={
            <PantallaHabilitada nombre="TAREAS" etiqueta="Tareas">
              <Tareas />
            </PantallaHabilitada>
          }
        />
        <Route
          path="/admin/documentos"
          element={
            <PantallaHabilitada nombre="DOCUMENTOS" etiqueta="Documentos">
              <Documentos />
            </PantallaHabilitada>
          }
        />
      </Route>

      <Route path="/super" element={<RequireSuper><SuperPanel /></RequireSuper>} />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <PantallasProvider>
      <AppRoutes />
    </PantallasProvider>
  );
}
