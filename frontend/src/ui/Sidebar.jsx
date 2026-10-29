import { NavLink } from 'react-router-dom';
import { Building2, ClipboardList, FileText, LayoutDashboard, Users } from 'lucide-react';
import { usePantallas } from './PantallasContext';

const ITEMS = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, end: true, pantalla: 'DASHBOARD' },
  { to: '/clientes', label: 'Clientes', icon: Building2, end: false, pantalla: 'CLIENTES' },
  { to: '/tecnicos', label: 'Técnicos', icon: Users, end: false, pantalla: 'TECNICOS' },
  { to: '/tareas', label: 'Tareas', icon: ClipboardList, end: false, pantalla: 'TAREAS' },
  { to: '/documentos', label: 'Documentos', icon: FileText, end: false, pantalla: 'DOCUMENTOS' },
];

export default function Sidebar({ base = '/' }) {
  const { habilitada } = usePantallas();
  const visibles = ITEMS.filter((item) => habilitada(item.pantalla));
  return (
    <aside className="sidebar">
      <nav className="menu-lateral">
        {visibles.map(({ to, label, icon: Icon, end }) => {
          const destino = base === '/' ? to : base + (to === '/' ? '' : to);
          return (
            <NavLink
              key={destino}
              to={destino}
              end={end}
              className={({ isActive }) => (isActive ? 'activo' : undefined)}
            >
              <Icon size={20} />
              <span>{label}</span>
            </NavLink>
          );
        })}
      </nav>
    </aside>
  );
}
