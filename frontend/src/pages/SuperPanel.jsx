import { useEffect, useState } from 'react';
import {
  ClipboardList,
  Download,
  FileSpreadsheet,
  FileText,
  KeyRound,
  Loader2,
  Monitor,
  ShieldCheck,
  Trash2,
  UserCog,
  UserPlus,
  Users,
} from 'lucide-react';
import { api, mensajeError, mensajeErrorBlob } from '../api/client';
import { ChipEstado, Confirmar, Modal, SeccionFormulario } from '../ui/Componentes';
import SuperLayout from '../ui/SuperLayout';

const PANTALLAS = [
  { nombre: 'DASHBOARD', etiqueta: 'Dashboard' },
  { nombre: 'CLIENTES', etiqueta: 'Clientes' },
  { nombre: 'TECNICOS', etiqueta: 'Técnicos' },
  { nombre: 'TAREAS', etiqueta: 'Tareas' },
  { nombre: 'DOCUMENTOS', etiqueta: 'Documentos' },
];

function Interruptor({ activo, disabled, onChange }) {
  return (
    <button
      type="button"
      className={`interruptor ${activo ? 'activo' : ''}`}
      role="switch"
      aria-checked={activo}
      disabled={disabled}
      onClick={onChange}
    >
      <span className="interruptor-boton" />
    </button>
  );
}

function SeccionInformes() {
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
      } catch (e) {
        if (!cancelada) setError(mensajeError(e));
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
    } catch (e) {
      setError(mensajeError(e));
    } finally {
      setCargandoTareas(false);
    }
  };

  const descargar = async (ruta, nombreAlternativo) => {
    if (!tareaSeleccionada) return;
    setError('');
    setDescargando(ruta);
    try {
      const res = await api.get(ruta, {
        params: { idTarea: tareaSeleccionada.codigo },
        responseType: 'blob',
      });
      const blobUrl = URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = blobUrl;
      const disposition = res.headers['content-disposition'] || '';
      const utf8 = disposition.match(/filename\*\s*=\s*UTF-8''([^;]+)/i);
      const simple = disposition.match(/filename\s*=\s*"?([^";]+)"?/i);
      let nombre = simple?.[1]?.trim() || nombreAlternativo;
      if (utf8) {
        try {
          nombre = decodeURIComponent(utf8[1].trim().replace(/^"|"$/g, ''));
        } catch {
          // Usa el nombre alternativo si el encabezado UTF-8 no es válido.
        }
      }
      link.download = nombre;
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

  const fechaTarea = (fecha) => fecha
    ? new Intl.DateTimeFormat('es-CL', { dateStyle: 'long', timeZone: 'America/Santiago' }).format(new Date(fecha))
    : 'Fecha no disponible';

  const nombreAlternativo = (extension) => `${tareaSeleccionada?.codigo || 'informe'}${extension}`;

  return (
    <section className="tarjeta-documento">
      <h3>
        <FileText size={18} style={{ verticalAlign: -3 }} color="var(--violeta)" /> Informes
      </h3>
      <p className="descripcion">
        Busca el edificio, selecciona el mantenimiento finalizado y descarga su rendición en Excel o
        el informe en Word.
      </p>

      {error && <div className="error-banner">{error}</div>}

      <div className="fila">
        <div className="campo-sugerencias campo-busqueda-edificio">
          <div className="campo">
            <label htmlFor="buscar-edificio-super">Nombre del edificio</label>
            <input
              id="buscar-edificio-super"
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
              aria-controls="sugerencias-edificios-super"
            />
          </div>
          {!edificioSeleccionado && busquedaEdificio.trim().length >= 2 && (
            <div className="sugerencias" id="sugerencias-edificios-super" role="listbox">
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
                      name="mantenimiento-super-seleccionado"
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
          onClick={() => descargar('/documentos/finalizadas.xlsx', nombreAlternativo('.xlsx'))}
          disabled={!tareaSeleccionada || descargando === '/documentos/finalizadas.xlsx'}
        >
          <FileSpreadsheet size={18} />
          {descargando === '/documentos/finalizadas.xlsx' ? 'Descargando…' : 'Descargar Rendición de servicios'}
        </button>
        <button
          className="btn btn-secundario"
          onClick={() => descargar('/documentos/finalizadas.docx', nombreAlternativo('.docx'))}
          disabled={!tareaSeleccionada || descargando === '/documentos/finalizadas.docx'}
        >
          <Download size={18} />
          {descargando === '/documentos/finalizadas.docx' ? 'Descargando…' : 'Descargar Informe'}
        </button>
      </div>
    </section>
  );
}

function SeccionTareas() {
  const [tareas, setTareas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [borrarTodas, setBorrarTodas] = useState(false);
  const [tareaAEliminar, setTareaAEliminar] = useState(null);
  const [trabajando, setTrabajando] = useState(false);
  const [anioDescarga, setAnioDescarga] = useState(String(new Date().getFullYear()));
  const [mesDescarga, setMesDescarga] = useState('');
  const [descargandoDocumentos, setDescargandoDocumentos] = useState(false);

  const fechaPeriodo = (tarea) => tarea.fechaFin || tarea.fechaInicio || tarea.fechaCreacion;
  const anioDeFecha = (fecha) => Number(new Intl.DateTimeFormat('en', {
    year: 'numeric',
    timeZone: 'America/Santiago',
  }).format(new Date(fecha)));
  const aniosDisponibles = [...new Set(
    tareas
      .filter((tarea) => tarea.estado === 'Finalizada' && fechaPeriodo(tarea))
      .map((tarea) => anioDeFecha(fechaPeriodo(tarea)))
  )].sort((a, b) => b - a);

  const cargar = async () => {
    setCargando(true);
    try {
      const res = await api.get('/tareas');
      setTareas(res.data);
      const anios = [...new Set(
        res.data
          .filter((tarea) => tarea.estado === 'Finalizada' && (tarea.fechaFin || tarea.fechaInicio || tarea.fechaCreacion))
          .map((tarea) => anioDeFecha(tarea.fechaFin || tarea.fechaInicio || tarea.fechaCreacion))
      )].sort((a, b) => b - a);
      if (anios.length) {
        setAnioDescarga((actual) => anios.includes(Number(actual)) ? actual : String(anios[0]));
      }
    } catch (e) {
      setError(mensajeError(e));
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => {
    cargar();
  }, []);

  const descargarDocumentosFinalizados = async () => {
    setError('');
    setOk('');
    setDescargandoDocumentos(true);
    try {
      const params = { anio: Number(anioDescarga) };
      if (mesDescarga) params.mes = Number(mesDescarga);
      const res = await api.get('/super/documentos-finalizadas.zip', {
        params,
        responseType: 'blob',
      });
      const blobUrl = URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = blobUrl;
      link.download = `documentos-tareas-finalizadas-${anioDescarga}${mesDescarga ? `-${mesDescarga.padStart(2, '0')}` : ''}.zip`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(blobUrl);
      setOk('Se descargó el paquete de documentos de tareas finalizadas.');
    } catch (e) {
      setError(await mensajeErrorBlob(e));
    } finally {
      setDescargandoDocumentos(false);
    }
  };

  const eliminarTodas = async () => {
    setTrabajando(true);
    setError('');
    setOk('');
    try {
      await api.delete('/super/tareas');
      setOk('Se eliminaron todas las tareas y se reinició la numeración (próxima tarea M-0001).');
      setBorrarTodas(false);
      await cargar();
    } catch (e) {
      setError(mensajeError(e));
    } finally {
      setTrabajando(false);
    }
  };

  const eliminarTarea = async () => {
    setTrabajando(true);
    setError('');
    setOk('');
    try {
      await api.delete(`/super/tareas/${tareaAEliminar.id}`);
      setOk(`Se eliminó la tarea ${tareaAEliminar.codigo} sin restricciones.`);
      setTareaAEliminar(null);
      await cargar();
    } catch (e) {
      setError(mensajeError(e));
    } finally {
      setTrabajando(false);
    }
  };

  return (
    <section className="tarjeta-documento">
      <h3>
        <ClipboardList size={18} style={{ verticalAlign: -3 }} color="var(--violeta)" /> Tareas
      </h3>
      <p className="descripcion">
        Elimina cualquier tarea sin importar su estado, o borra todas las tareas de la base de
        datos. Al borrarlas todas, la numeración se reinicia en M-0001.
      </p>

      {error && <div className="error-banner">{error}</div>}
      {ok && <div className="ok-banner">{ok}</div>}

      <div className="seccion-formulario">
        <h4>Descargar documentos de tareas finalizadas</h4>
        <p className="descripcion">
          Descarga un ZIP con la rendición Excel y el informe Word de cada tarea finalizada con
          tableros registrados en el período seleccionado.
        </p>
        <div className="fila">
          <div className="campo">
            <label htmlFor="anio-documentos-finalizados">Año</label>
            <select
              id="anio-documentos-finalizados"
              value={anioDescarga}
              onChange={(e) => setAnioDescarga(e.target.value)}
            >
              {aniosDisponibles.length ? aniosDisponibles.map((anio) => (
                <option key={anio} value={anio}>{anio}</option>
              )) : <option value={anioDescarga}>{anioDescarga}</option>}
            </select>
          </div>
          <div className="campo">
            <label htmlFor="mes-documentos-finalizados">Mes</label>
            <select
              id="mes-documentos-finalizados"
              value={mesDescarga}
              onChange={(e) => setMesDescarga(e.target.value)}
            >
              <option value="">Todo el año</option>
              {Array.from({ length: 12 }, (_, indice) => {
                const mes = indice + 1;
                const nombre = new Intl.DateTimeFormat('es-CL', { month: 'long' })
                  .format(new Date(2026, indice, 1));
                return <option key={mes} value={mes}>{nombre.charAt(0).toUpperCase() + nombre.slice(1)}</option>;
              })}
            </select>
          </div>
        </div>
        <div className="fila-descarga">
          <button
            className="btn btn-secundario"
            onClick={descargarDocumentosFinalizados}
            disabled={descargandoDocumentos || !anioDescarga}
          >
            <Download size={18} />
            {descargandoDocumentos ? 'Preparando documentos…' : 'Descargar documentos (ZIP)'}
          </button>
        </div>
      </div>

      <div className="fila-descarga">
        <button className="btn btn-riesgo" onClick={() => setBorrarTodas(true)}>
          <Trash2 size={18} />
          Borrar todas las tareas
        </button>
      </div>

      <div className="lista-tareas" style={{ marginTop: 16 }}>
        {cargando && (
          <div className="cargando">
            <Loader2 className="rotando" size={20} />
            Cargando tareas…
          </div>
        )}
        {!cargando && tareas.length === 0 && (
          <div className="vacio">No hay tareas registradas.</div>
        )}
        {!cargando &&
          tareas.map((tarea) => (
            <div className="tarjeta-tarea" key={tarea.id}>
              <div className="tarjeta-tarea-cabecera">
                <strong>{tarea.codigo}</strong>
                <ChipEstado estado={tarea.estado} />
              </div>
              <div className="tarjeta-tarea-acciones">
                <button className="btn btn-chico btn-riesgo" onClick={() => setTareaAEliminar(tarea)}>
                  <Trash2 size={14} />
                  Eliminar
                </button>
              </div>
            </div>
          ))}
      </div>

      <Confirmar
        abierto={borrarTodas}
        titulo="Borrar todas las tareas"
        mensaje="Se eliminarán TODAS las tareas, sus tableros, informes y notificaciones. Esta acción no se puede deshacer. ¿Desea continuar?"
        confirmarTexto="Borrar todo"
        cargando={trabajando}
        onConfirmar={eliminarTodas}
        onCancelar={() => setBorrarTodas(false)}
      />
      <Confirmar
        abierto={!!tareaAEliminar}
        titulo="Eliminar tarea"
        mensaje={`Se eliminará la tarea ${tareaAEliminar?.codigo || ''} en cualquier estado, junto a sus tableros e informes. ¿Desea continuar?`}
        confirmarTexto="Eliminar"
        cargando={trabajando}
        onConfirmar={eliminarTarea}
        onCancelar={() => setTareaAEliminar(null)}
      />
    </section>
  );
}

function SeccionTecnicos() {
  const [tecnicos, setTecnicos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [tecnicoAEliminar, setTecnicoAEliminar] = useState(null);
  const [trabajando, setTrabajando] = useState(false);

  const cargar = async () => {
    setCargando(true);
    try {
      const res = await api.get('/tecnicos');
      setTecnicos(res.data);
    } catch (e) {
      setError(mensajeError(e));
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => {
    cargar();
  }, []);

  const eliminarTecnico = async () => {
    setTrabajando(true);
    setError('');
    setOk('');
    try {
      await api.delete(`/super/tecnicos/${tecnicoAEliminar.id}`);
      setOk(
        `Se eliminó el técnico ${tecnicoAEliminar.nombre} ${tecnicoAEliminar.apellido}. Sus tareas quedaron en el sistema sin su asignación.`
      );
      setTecnicoAEliminar(null);
      await cargar();
    } catch (e) {
      setError(mensajeError(e));
    } finally {
      setTrabajando(false);
    }
  };

  return (
    <section className="tarjeta-documento">
      <h3>
        <Users size={18} style={{ verticalAlign: -3 }} color="var(--violeta)" /> Técnicos
      </h3>
      <p className="descripcion">
        Elimina técnicos sin restricciones. Sus tareas en curso quedan en el sistema pero se les
        quita la asignación e invitaciones.
      </p>

      {error && <div className="error-banner">{error}</div>}
      {ok && <div className="ok-banner">{ok}</div>}

      <div className="lista-tareas" style={{ marginTop: 8 }}>
        {cargando && (
          <div className="cargando">
            <Loader2 className="rotando" size={20} />
            Cargando técnicos…
          </div>
        )}
        {!cargando && tecnicos.length === 0 && (
          <div className="vacio">No hay técnicos registrados.</div>
        )}
        {!cargando &&
          tecnicos.map((tecnico) => (
            <div className="tarjeta-tarea" key={tecnico.id}>
              <div className="tarjeta-tarea-cabecera">
                <strong>
                  {tecnico.nombre} {tecnico.apellido}
                </strong>
                <span className="chip chip-azul">{tecnico.rut}</span>
              </div>
              <div className="tarjeta-tarea-acciones">
                <button
                  className="btn btn-chico btn-riesgo"
                  onClick={() => setTecnicoAEliminar(tecnico)}
                >
                  <Trash2 size={14} />
                  Eliminar
                </button>
              </div>
            </div>
          ))}
      </div>

      <Confirmar
        abierto={!!tecnicoAEliminar}
        titulo="Eliminar técnico"
        mensaje={`Se eliminará el técnico ${tecnicoAEliminar?.nombre || ''} ${
          tecnicoAEliminar?.apellido || ''
        } sin importar las tareas que tenga asignadas. ¿Desea continuar?`}
        confirmarTexto="Eliminar"
        cargando={trabajando}
        onConfirmar={eliminarTecnico}
        onCancelar={() => setTecnicoAEliminar(null)}
      />
    </section>
  );
}

function SeccionAdministradores() {
  const [administradores, setAdministradores] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [crearAbierto, setCrearAbierto] = useState(false);
  const [passwordDe, setPasswordDe] = useState(null);
  const [adminAEliminar, setAdminAEliminar] = useState(null);
  const [trabajando, setTrabajando] = useState(false);
  const [form, setForm] = useState({ nombre: '', apellido: '', username: '', password: '' });
  const [nuevaPassword, setNuevaPassword] = useState('');

  const cargar = async () => {
    setCargando(true);
    try {
      const res = await api.get('/super/administradores');
      setAdministradores(res.data);
    } catch (e) {
      setError(mensajeError(e));
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => {
    cargar();
  }, []);

  const crear = async (e) => {
    e.preventDefault();
    setTrabajando(true);
    setError('');
    setOk('');
    try {
      await api.post('/super/administradores', form);
      setOk(`Se creó el administrador ${form.nombre} ${form.apellido}.`);
      setCrearAbierto(false);
      setForm({ nombre: '', apellido: '', username: '', password: '' });
      await cargar();
    } catch (e) {
      setError(mensajeError(e));
    } finally {
      setTrabajando(false);
    }
  };

  const cambiarPassword = async (e) => {
    e.preventDefault();
    setTrabajando(true);
    setError('');
    setOk('');
    try {
      await api.put(`/super/administradores/${passwordDe.id}/password`, { password: nuevaPassword });
      setOk(`Se cambió la contraseña de ${passwordDe.nombre} ${passwordDe.apellido}.`);
      setPasswordDe(null);
      setNuevaPassword('');
    } catch (e) {
      setError(mensajeError(e));
    } finally {
      setTrabajando(false);
    }
  };

  const eliminarAdmin = async () => {
    setTrabajando(true);
    setError('');
    setOk('');
    try {
      await api.delete(`/super/administradores/${adminAEliminar.id}`);
      setOk(
        `Se eliminó el administrador ${adminAEliminar.nombre} ${adminAEliminar.apellido} (${adminAEliminar.username}).`
      );
      setAdminAEliminar(null);
      await cargar();
    } catch (e) {
      setError(mensajeError(e));
    } finally {
      setTrabajando(false);
    }
  };

  const cambiar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));

  return (
    <section className="tarjeta-documento">
      <h3>
        <UserCog size={18} style={{ verticalAlign: -3 }} color="var(--violeta)" /> Administradores
      </h3>
      <p className="descripcion">
        Crea, elimina y cambia la contraseña de los administradores del panel web.
      </p>

      {error && <div className="error-banner">{error}</div>}
      {ok && <div className="ok-banner">{ok}</div>}

      <div className="fila-descarga">
        <button className="btn btn-primario" onClick={() => setCrearAbierto(true)}>
          <UserPlus size={18} />
          Crear administrador
        </button>
      </div>

      <div className="lista-tareas" style={{ marginTop: 8 }}>
        {cargando && (
          <div className="cargando">
            <Loader2 className="rotando" size={20} />
            Cargando administradores…
          </div>
        )}
        {!cargando &&
          administradores.map((admin) => (
            <div className="tarjeta-tarea" key={admin.id}>
              <div className="tarjeta-tarea-cabecera">
                <strong>
                  {admin.nombre} {admin.apellido}
                </strong>
                <span className="chip chip-azul">{admin.username}</span>
              </div>
              <div className="tarjeta-tarea-acciones">
                <button
                  className="btn btn-chico btn-secundario"
                  onClick={() => {
                    setPasswordDe(admin);
                    setNuevaPassword('');
                  }}
                >
                  <KeyRound size={14} />
                  Cambiar contraseña
                </button>
                <button
                  className="btn btn-chico btn-riesgo"
                  onClick={() => setAdminAEliminar(admin)}
                >
                  <Trash2 size={14} />
                  Eliminar
                </button>
              </div>
            </div>
          ))}
      </div>

      <Modal
        titulo="Crear administrador"
        abierto={crearAbierto}
        onCerrar={() => setCrearAbierto(false)}
      >
        <form onSubmit={crear}>
          <SeccionFormulario titulo="Datos del administrador">
            <div className="fila">
              <div className="campo">
                <label>Nombre</label>
                <input value={form.nombre} onChange={cambiar('nombre')} placeholder="Nombre" required />
              </div>
              <div className="campo">
                <label>Apellido</label>
                <input
                  value={form.apellido}
                  onChange={cambiar('apellido')}
                  placeholder="Apellido"
                  required
                />
              </div>
            </div>
            <div className="fila">
              <div className="campo">
                <label>Usuario (RUT)</label>
                <input
                  value={form.username}
                  onChange={cambiar('username')}
                  placeholder="Ej: 11111111"
                  required
                />
              </div>
              <div className="campo">
                <label>Contraseña</label>
                <input
                  type="password"
                  value={form.password}
                  onChange={cambiar('password')}
                  placeholder="Contraseña"
                  required
                />
              </div>
            </div>
          </SeccionFormulario>
          <div className="modal-pie">
            <button type="button" className="btn btn-secundario" onClick={() => setCrearAbierto(false)}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primario" disabled={trabajando}>
              {trabajando ? <Loader2 className="rotando" size={16} /> : null}
              Crear
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        titulo="Cambiar contraseña"
        abierto={!!passwordDe}
        onCerrar={() => setPasswordDe(null)}
      >
        <form onSubmit={cambiarPassword}>
          <SeccionFormulario titulo={`Administrador: ${passwordDe?.nombre || ''} ${passwordDe?.apellido || ''}`}>
            <div className="campo">
              <label>Nueva contraseña</label>
              <input
                type="password"
                value={nuevaPassword}
                onChange={(e) => setNuevaPassword(e.target.value)}
                placeholder="Nueva contraseña"
                required
              />
            </div>
          </SeccionFormulario>
          <div className="modal-pie">
            <button type="button" className="btn btn-secundario" onClick={() => setPasswordDe(null)}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primario" disabled={trabajando}>
              {trabajando ? <Loader2 className="rotando" size={16} /> : null}
              Guardar
            </button>
          </div>
        </form>
      </Modal>

      <Confirmar
        abierto={!!adminAEliminar}
        titulo="Eliminar administrador"
        mensaje={`Se eliminará el administrador ${adminAEliminar?.nombre || ''} ${
          adminAEliminar?.apellido || ''
        } (${adminAEliminar?.username || ''}). Perderá el acceso al panel. ¿Desea continuar?`}
        confirmarTexto="Eliminar"
        cargando={trabajando}
        onConfirmar={eliminarAdmin}
        onCancelar={() => setAdminAEliminar(null)}
      />
    </section>
  );
}

function SeccionPantallas() {
  const [pantallas, setPantallas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');

  useEffect(() => {
    api
      .get('/super/pantallas')
      .then((res) => setPantallas(res.data || []))
      .catch((e) => setError(mensajeError(e)))
      .finally(() => setCargando(false));
  }, []);

  const alternar = async (nombre) => {
    const actualizado = pantallas.map((p) =>
      p.nombre === nombre ? { ...p, habilitada: !p.habilitada } : p
    );
    setPantallas(actualizado);
    setGuardando(true);
    setError('');
    setOk('');
    try {
      const res = await api.put('/super/pantallas', actualizado);
      setPantallas(res.data || actualizado);
      setOk('Configuración de pantallas actualizada.');
    } catch (e) {
      setError(mensajeError(e));
    } finally {
      setGuardando(false);
    }
  };

  return (
    <section className="tarjeta-documento">
      <h3>
        <Monitor size={18} style={{ verticalAlign: -3 }} color="var(--violeta)" /> Pantallas del
        administrador
      </h3>
      <p className="descripcion">
        Habilita o deshabilita las pantallas que el administrador puede ver en su menú. El super
        usuario siempre accede a todas.
      </p>

      {error && <div className="error-banner">{error}</div>}
      {ok && <div className="ok-banner">{ok}</div>}

      {cargando && (
        <div className="cargando">
          <Loader2 className="rotando" size={20} />
          Cargando configuración…
        </div>
      )}

      {!cargando && (
        <div className="lista-pantallas">
          {PANTALLAS.map(({ nombre, etiqueta }) => {
            const config = pantallas.find((p) => p.nombre === nombre);
            const activo = config ? config.habilitada : true;
            return (
              <div className="pantalla-item" key={nombre}>
                <div>
                  <strong>{etiqueta}</strong>
                  <span className={activo ? 'pantalla-estado ok' : 'pantalla-estado'}>
                    {activo ? 'Habilitada' : 'Deshabilitada'}
                  </span>
                </div>
                <Interruptor activo={activo} disabled={guardando} onChange={() => alternar(nombre)} />
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}

export default function SuperPanel() {
  return (
    <SuperLayout>
      <h1 className="titulo-pagina">
        <ShieldCheck size={26} style={{ verticalAlign: -4 }} /> Modo super usuario
      </h1>
      <p className="subtitulo-pagina">
        Herramientas avanzadas de administración del sistema.
      </p>

      <SeccionInformes />
      <SeccionTareas />
      <SeccionTecnicos />
      <SeccionAdministradores />
      <SeccionPantallas />
    </SuperLayout>
  );
}
