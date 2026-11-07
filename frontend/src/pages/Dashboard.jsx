import { useEffect, useState } from 'react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { BarChart3, Users } from 'lucide-react';
import { api, mensajeError } from '../api/client';
import { Avatar, Cargando, ErrorBanner, Vacio } from '../ui/Componentes';

const COLORES = {
  'En proceso': '#F59E0B',
  'Finalizadas': '#22A06B',
  'No iniciadas': '#D64545',
};

export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [tecnicos, setTecnicos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const cargar = async () => {
      setCargando(true);
      setError('');
      try {
        const [resStats, resTecnicos] = await Promise.all([
          api.get('/dashboard/stats'),
          api.get('/dashboard/tecnicos'),
        ]);
        setStats(resStats.data);
        setTecnicos(resTecnicos.data);
      } catch (err) {
        setError(mensajeError(err));
      } finally {
        setCargando(false);
      }
    };
    cargar();
  }, []);

  if (cargando) return <Cargando mensaje="Cargando dashboard…" />;

  const datosGrafico = stats
    ? [
        { name: 'En proceso', valor: stats.enProceso },
        { name: 'Finalizadas', valor: stats.finalizadas },
        { name: 'No iniciadas', valor: stats.noIniciadas },
      ]
    : [];

  return (
    <div>
      <h1 className="titulo-pagina">Dashboard</h1>
      <p className="subtitulo-pagina">Resumen general del sistema de tareas.</p>

      <ErrorBanner mensaje={error} />

      <div className="dashboard-grid">
        <section className="tarjeta tarjeta-padding">
          <h2 className="seccion-titulo">
            <BarChart3 size={20} color="var(--violeta)" />
            Estado de las tareas
          </h2>
          <div style={{ width: '100%', height: 300 }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={datosGrafico} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E3E1EA" />
                <XAxis
                  dataKey="name"
                  tick={{ fill: '#6B6B78', fontSize: 13 }}
                  axisLine={{ stroke: '#E3E1EA' }}
                  tickLine={false}
                />
                <YAxis
                  allowDecimals={false}
                  tick={{ fill: '#6B6B78', fontSize: 13 }}
                  axisLine={false}
                  tickLine={false}
                />
                <Tooltip
                  cursor={{ fill: '#F7F7FA' }}
                  formatter={(value) => [`${value} tarea(s)`, 'Cantidad']}
                />
                <Bar dataKey="valor" radius={[10, 10, 0, 0]}>
                  {datosGrafico.map((d) => (
                    <Cell key={d.name} fill={COLORES[d.name]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
          <div style={{ display: 'flex', justifyContent: 'center', gap: 16, marginTop: 14, flexWrap: 'wrap' }}>
            {datosGrafico.map((d) => (
              <span key={d.name} className="chip chip-gris">
                <span
                  style={{
                    width: 10,
                    height: 10,
                    borderRadius: '50%',
                    background: COLORES[d.name],
                    display: 'inline-block',
                  }}
                />
                {d.name}: <strong>&nbsp;{d.valor}</strong>
              </span>
            ))}
          </div>
        </section>

        <section className="tarjeta tarjeta-padding">
          <h2 className="seccion-titulo">
            <Users size={20} color="var(--violeta)" />
            Técnicos del sistema
          </h2>
          {tecnicos.length === 0 ? (
            <Vacio mensaje="Aún no hay técnicos registrados." />
          ) : (
            <div className="lista-tecnicos-dash">
              {tecnicos.map((t) => (
                <div key={t.id} className="item-tecnico-dash">
                  <Avatar nombre={t.nombre} apellido={t.apellido} size={38} fontSize={15} />
                  <div>
                    <div className="nombre">
                      {t.nombre} {t.apellido}
                    </div>
                    <div className="rut">RUT {t.rut}</div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
