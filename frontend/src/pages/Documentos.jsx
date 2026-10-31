import { useEffect, useState } from 'react';
import { Download, FileSpreadsheet, FileText, History } from 'lucide-react';
import { api, mensajeErrorBlob } from '../api/client';

function FiltrosFechas({ filtro, setFiltro }) {
  const actualizar = (campo) => (e) => setFiltro((f) => ({ ...f, [campo]: e.target.value }));
  return (
    <div className="fila">
      <div className="campo">
        <label>Fecha desde</label>
        <input
          type="date"
          value={filtro.fechaDesde}
          onChange={actualizar('fechaDesde')}
        />
      </div>
      <div className="campo">
        <label>Fecha hasta</label>
        <input
          type="date"
          value={filtro.fechaHasta}
          onChange={actualizar('fechaHasta')}
        />
      </div>
    </div>
  );
}

function nombreDesdeRespuesta(contentDisposition, nombreAlternativo) {
  if (!contentDisposition) return nombreAlternativo;

  const utf8 = contentDisposition.match(/filename\*\s*=\s*UTF-8''([^;]+)/i);
  if (utf8) {
    try {
      return decodeURIComponent(utf8[1].trim().replace(/^"|"$/g, ''));
    } catch {
      // Si el nombre codificado no se puede leer, intenta el filename simple.
    }
  }

  const simple = contentDisposition.match(/filename\s*=\s*"?([^";]+)"?/i);
  return simple?.[1]?.trim() || nombreAlternativo;
}

export default function Documentos() {
  const [filtroHistorial, setFiltroHistorial] = useState({
    fechaDesde: '',
    fechaHasta: '',
  });
  const [busquedaEdificio, setBusquedaEdificio] = useState('');
  const [edificioSeleccionado, setEdificioSeleccionado] = useState('');
  const [sugerencias, setSugerencias] = useState([]);
  const [cargandoSugerencias, setCargandoSugerencias] = useState(false);
  const [tareasEdificio, setTareasEdificio] = useState([]);
  const [tareaSeleccionada, setTareaSeleccionada] = useState(null);
  const [cargandoTareas, setCargandoTareas] = useState(false);
  const [error, setError] = useState('');
  const [descargando, setDescargando] = useState('');

  useEffect(() => {
    const consulta = busquedaEdificio.trim();
    if (consulta.length < 2 || edificioSeleccionado === consulta) {
      setSugerencias([]);
      setCargandoSugerencias(false);
      return undefined;
    }

    let cancelada = false;
    const temporizador = window.setTimeout(async () => {
      setCargandoSugerencias(true);
      try {
        const res = await api.get('/documentos/edificios/sugerencias', { params: { q: consulta } });
        if (!cancelada) setSugerencias(res.data);
      } catch (err) {
        if (!cancelada) setError(err?.response?.data?.mensaje || 'No se pudieron buscar edificios.');
      } finally {
        if (!cancelada) setCargandoSugerencias(false);
      }
    }, 250);

    return () => {
      cancelada = true;
      window.clearTimeout(temporizador);
    };
  }, [busquedaEdificio, edificioSeleccionado]);

  const seleccionarEdificio = async (nombre) => {
    const seleccionado = nombre.trim();
    if (!seleccionado) return;
    setBusquedaEdificio(seleccionado);
    setEdificioSeleccionado(seleccionado);
    setSugerencias([]);
    setTareasEdificio([]);
    setTareaSeleccionada(null);
    setError('');
    setCargandoTareas(true);
    try {
      const res = await api.get('/documentos/edificios/tareas', {
        params: { nombre: seleccionado },
      });
      setTareasEdificio(res.data);
    } catch (err) {
      setError(err?.response?.data?.mensaje || 'No se pudieron cargar los mantenimientos.');
    } finally {
      setCargandoTareas(false);
    }
  };

  const descargar = async (ruta, nombreArchivo, params = {}) => {
    setError('');
    setDescargando(ruta);
    try {
      const res = await api.get(ruta, { params, responseType: 'blob' });
      const blobUrl = URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = blobUrl;
      link.download = nombreDesdeRespuesta(res.headers['content-disposition'], nombreArchivo);
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(blobUrl);
    } catch (err) {
      setError(await mensajeErrorBlob(err));
    } finally {
      setDescargando('');
    }
  };

  const nombreHistorial = () => {
    const partes = [];
    if (filtroHistorial.fechaDesde.trim()) {
      partes.push(`desde-${filtroHistorial.fechaDesde.trim()}`);
    }
    if (filtroHistorial.fechaHasta.trim()) {
      partes.push(`hasta-${filtroHistorial.fechaHasta.trim()}`);
    }
    return `historial-tareas${partes.length ? `-${partes.join('-')}` : ''}.xlsx`;
  };

  const fechaTarea = (fecha) => {
    if (!fecha) return 'Fecha no disponible';
    return new Intl.DateTimeFormat('es-CL', {
      dateStyle: 'long',
      timeZone: 'America/Santiago',
    }).format(new Date(fecha));
  };

  const nombreAlternativo = (tarea, extension) => {
    const fecha = tarea.fecha ? new Date(tarea.fecha) : null;
    const mesAnio = fecha
      ? new Intl.DateTimeFormat('es-CL', {
          month: 'long',
          year: 'numeric',
          timeZone: 'America/Santiago',
        }).format(fecha)
      : '';
    return `${tarea.nombreRecinto}${mesAnio ? `-${mesAnio}` : ''}-${tarea.codigo}${extension}`;
  };

  return (
    <div>
      <h1 className="titulo-pagina">Documentos</h1>
      <p className="subtitulo-pagina">Busca y descarga informes del sistema.</p>

      {error && <div className="error-banner">{error}</div>}

      <section className="tarjeta-documento">
        <h3>
          <History size={18} style={{ verticalAlign: -3 }} color="var(--violeta)" /> Historial de
          tareas
        </h3>
        <p className="descripcion">
          Descarga el historial completo de todas las tareas en formato Excel, sin importar su
          estado. Puedes filtrar por rango de fechas (desde y hasta); si no indicas fechas, se
          descargan todas.
        </p>
        <FiltrosFechas filtro={filtroHistorial} setFiltro={setFiltroHistorial} />
        <div className="fila-descarga">
          <button
            className="btn btn-primario"
            onClick={() =>
              descargar('/documentos/historial.xlsx', nombreHistorial(), filtroHistorial)
            }
            disabled={descargando === '/documentos/historial.xlsx'}
          >
            <FileSpreadsheet size={18} />
            {descargando === '/documentos/historial.xlsx' ? 'Descargando…' : 'Descargar Excel'}
          </button>
        </div>
      </section>

      <section className="tarjeta-documento">
        <h3>
          <FileText size={18} style={{ verticalAlign: -3 }} color="var(--violeta)" /> Informes de
          tareas finalizadas
        </h3>
        <p className="descripcion">
          Busca un edificio, selecciona el mantenimiento que necesitas y descarga su informe en
          Excel o Word.
        </p>
        <div className="fila">
          <div className="campo-sugerencias campo-busqueda-edificio">
            <div className="campo">
              <label htmlFor="buscar-edificio">Nombre del edificio</label>
              <input
                id="buscar-edificio"
                value={busquedaEdificio}
                onChange={(e) => {
                  setBusquedaEdificio(e.target.value);
                  setEdificioSeleccionado('');
                  setTareasEdificio([]);
                  setTareaSeleccionada(null);
                  setError('');
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    seleccionarEdificio(busquedaEdificio);
                  }
                  if (e.key === 'Escape') setSugerencias([]);
                }}
                placeholder="Ej: Edificio Vicuña Urban"
                autoComplete="off"
                aria-autocomplete="list"
                aria-controls="sugerencias-edificios"
              />
            </div>
            {!edificioSeleccionado && busquedaEdificio.trim().length >= 2 && (
              <div className="sugerencias" id="sugerencias-edificios" role="listbox">
                {cargandoSugerencias ? (
                  <div className="sugerencias-vacias">Buscando edificios…</div>
                ) : sugerencias.length ? (
                  sugerencias.map((nombre) => (
                    <button
                      className="sugerencia-item"
                      key={nombre}
                      type="button"
                      role="option"
                      aria-selected="false"
                      onClick={() => seleccionarEdificio(nombre)}
                    >
                      <strong>{nombre}</strong>
                      <span>Ver mantenimientos finalizados</span>
                    </button>
                  ))
                ) : (
                  <div className="sugerencias-vacias">
                    No hay sugerencias. Presiona Enter para buscar este nombre.
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {edificioSeleccionado && (
          <div className="resultado-edificio">
            <div className="resultado-edificio-cabecera">
              <strong>{edificioSeleccionado}</strong>
              <button
                className="btn btn-secundario btn-chico"
                type="button"
                onClick={() => {
                  setEdificioSeleccionado('');
                  setBusquedaEdificio('');
                  setTareasEdificio([]);
                  setTareaSeleccionada(null);
                }}
              >
                Cambiar edificio
              </button>
            </div>
            {cargandoTareas ? (
              <p className="descripcion">Cargando mantenimientos…</p>
            ) : tareasEdificio.length ? (
              <>
                <p className="descripcion">Selecciona un mantenimiento para descargar sus archivos.</p>
                <div className="lista-mantenimientos" role="radiogroup" aria-label="Mantenimientos del edificio">
                  {tareasEdificio.map((tarea) => (
                    <label
                      className={`mantenimiento-opcion${tareaSeleccionada?.id === tarea.id ? ' seleccionado' : ''}`}
                      key={tarea.id}
                    >
                      <input
                        type="radio"
                        name="mantenimiento-seleccionado"
                        value={tarea.id}
                        checked={tareaSeleccionada?.id === tarea.id}
                        onChange={() => setTareaSeleccionada(tarea)}
                      />
                      <span className="mantenimiento-detalle">
                        <strong>{tarea.nombreRecinto}</strong>
                        <span>{tarea.direccion}</span>
                        <span>{fechaTarea(tarea.fecha)} · ID {tarea.codigo}</span>
                      </span>
                    </label>
                  ))}
                </div>
              </>
            ) : (
              <p className="descripcion">No hay mantenimientos finalizados con informes para este edificio.</p>
            )}
          </div>
        )}

        <div className="fila-descarga">
          <button
            className="btn btn-secundario"
            onClick={() => tareaSeleccionada && descargar(
              '/documentos/finalizadas.xlsx',
              nombreAlternativo(tareaSeleccionada, '.xlsx'),
              { idTarea: tareaSeleccionada.codigo }
            )}
            disabled={!tareaSeleccionada || descargando === '/documentos/finalizadas.xlsx'}
          >
            <FileSpreadsheet size={18} />
            {descargando === '/documentos/finalizadas.xlsx'
              ? 'Descargando…'
              : 'Descargar Rendición de servicios'}
          </button>
          <button
            className="btn btn-secundario"
            onClick={() => tareaSeleccionada && descargar(
              '/documentos/finalizadas.docx',
              nombreAlternativo(tareaSeleccionada, '.docx'),
              { idTarea: tareaSeleccionada.codigo }
            )}
            disabled={!tareaSeleccionada || descargando === '/documentos/finalizadas.docx'}
          >
            <Download size={18} />
            {descargando === '/documentos/finalizadas.docx' ? 'Descargando…' : 'Descargar Informe'}
          </button>
        </div>
      </section>
    </div>
  );
}
