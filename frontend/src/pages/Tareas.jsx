import { useEffect, useState } from 'react';
import { Check, Pencil, Plus, Trash2, Wrench } from 'lucide-react';
import { api, mensajeError } from '../api/client';
import {
  Cargando,
  ChipEstado,
  Confirmar,
  ErrorBanner,
  Modal,
  OkBanner,
  Vacio,
} from '../ui/Componentes';

const FORM_VACIO = {
  tipo: 'MANTENIMIENTO',
  direccion: '',
  nombreRecinto: '',
  descripcion: '',
  tecnicoCabeceraId: '',
};

const CLAVE_OCULTAS = 'aring_finalizadas_ocultas';

function leerOcultas() {
  try {
    const ids = JSON.parse(localStorage.getItem(CLAVE_OCULTAS));
    return Array.isArray(ids) ? new Set(ids) : new Set();
  } catch {
    return new Set();
  }
}

function guardarOcultas(ids) {
  localStorage.setItem(CLAVE_OCULTAS, JSON.stringify(Array.from(ids)));
}

/** Minúsculas sin tildes ni 'ñ': hace la búsqueda insensible a acentos. */
function normalizar(texto) {
  return (texto || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');
}

export default function Tareas() {
  const [tareas, setTareas] = useState([]);
  const [tecnicos, setTecnicos] = useState([]);
  const [clientes, setClientes] = useState([]);
  const [clienteId, setClienteId] = useState('');
  const [sugerenciasAbiertas, setSugerenciasAbiertas] = useState(false);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');

  const [crearAbierto, setCrearAbierto] = useState(false);
  const [form, setForm] = useState(FORM_VACIO);
  const [creando, setCreando] = useState(false);

  const [editando, setEditando] = useState(null);
  const [formEdit, setFormEdit] = useState(null);
  const [guardando, setGuardando] = useState(false);

  const [eliminando, setEliminando] = useState(null);
  const [confirmarAbierto, setConfirmarAbierto] = useState(false);
  const [confirmarLimpiar, setConfirmarLimpiar] = useState(false);

  const cargar = async () => {
    setCargando(true);
    setError('');
    try {
      const [resTareas, resTecnicos] = await Promise.all([
        api.get('/tareas'),
        api.get('/tecnicos'),
      ]);
      const ocultas = leerOcultas();
      setTareas(resTareas.data.filter((t) => !ocultas.has(t.id)));
      setTecnicos(resTecnicos.data);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
    // Los clientes alimentan las sugerencias del formulario de creación;
    // si no se pueden cargar, la creación manual sigue funcionando igual.
    api
      .get('/clientes')
      .then((res) => setClientes(res.data))
      .catch(() => {});
  };

  useEffect(() => {
    cargar();
  }, []);

  const formCompleto =
    form.tipo === 'MANTENIMIENTO' &&
    form.direccion.trim() !== '' &&
    form.nombreRecinto.trim() !== '' &&
    form.descripcion.trim() !== '' &&
    form.tecnicoCabeceraId !== '';

  const abrirCrear = () => {
    setForm(FORM_VACIO);
    setClienteId('');
    setSugerenciasAbiertas(false);
    setError('');
    setCrearAbierto(true);
  };

  const crearTarea = async (e) => {
    e.preventDefault();
    if (!formCompleto) return;
    setCreando(true);
    setError('');
    try {
      const res = await api.post('/tareas', {
        ...form,
        tecnicoCabeceraId: Number(form.tecnicoCabeceraId),
        clienteId: clienteId ? Number(clienteId) : null,
      });
      setCrearAbierto(false);
      setAviso(`Tarea ${res.data.codigo} creada correctamente.`);
      cargar();
      setTimeout(() => setAviso(''), 5000);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCreando(false);
    }
  };

  const abrirEditar = (t) => {
    setEditando(t);
    setFormEdit({ tecnicoCabeceraId: String(t.tecnicoCabecera.id) });
    setError('');
  };

  const guardarEdicion = async (e) => {
    e.preventDefault();
    if (!formEdit?.tecnicoCabeceraId) return;
    setGuardando(true);
    setError('');
    try {
      await api.put(`/tareas/${editando.id}`, {
        tecnicoCabeceraId: Number(formEdit.tecnicoCabeceraId),
      });
      setEditando(null);
      setAviso(`Técnico de la tarea ${editando.codigo} actualizado.`);
      cargar();
      setTimeout(() => setAviso(''), 5000);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  };

  const confirmarEliminar = async () => {
    setError('');
    try {
      await api.delete(`/tareas/${eliminando.id}`);
      setConfirmarAbierto(false);
      setEliminando(null);
      setAviso(`Tarea ${eliminando.codigo} eliminada.`);
      cargar();
      setTimeout(() => setAviso(''), 5000);
    } catch (err) {
      setError(mensajeError(err));
      setConfirmarAbierto(false);
    }
  };

  const cambiarEstado = async (tarea, nuevoEstado) => {
    setError('');
    try {
      await api.put(`/tareas/${tarea.id}/estado`, { estado: nuevoEstado });
      setAviso(`La tarea ${tarea.codigo} fue finalizada. Se envió una notificación.`);
      cargar();
      setTimeout(() => setAviso(''), 5000);
    } catch (err) {
      setError(mensajeError(err));
    }
  };

  const actualizar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));

  const cambiarDireccion = (e) => {
    setForm((f) => ({ ...f, direccion: e.target.value }));
    setClienteId('');
    setSugerenciasAbiertas(true);
  };

  const elegirCliente = (c) => {
    setForm((f) => ({ ...f, direccion: c.direccion, nombreRecinto: c.nombreRecinto }));
    setClienteId(String(c.id));
    setSugerenciasAbiertas(false);
  };

  const textoDireccion = form.direccion.trim();
  const textoBusqueda = normalizar(textoDireccion);
  // Busca por dirección, sin distinguir tildes ni "ñ".
  // Con el campo vacío muestra TODOS los clientes registrados (selector completo).
  const sugerencias =
    textoBusqueda === ''
      ? clientes
      : clientes.filter((c) => normalizar(c.direccion).includes(textoBusqueda));
  const mostrarSugerencias =
    crearAbierto && sugerenciasAbiertas && clientes.length > 0 && sugerencias.length > 0;
  const sinCoincidencia =
    crearAbierto && sugerenciasAbiertas && textoBusqueda !== '' && clientes.length > 0 && sugerencias.length === 0;

  const limpiarFinalizadas = () => {
    const ocultas = leerOcultas();
    tareas.forEach((t) => {
      if (t.estado === 'Finalizada') ocultas.add(t.id);
    });
    guardarOcultas(ocultas);
    setTareas((prev) => prev.filter((t) => t.estado !== 'Finalizada'));
    setConfirmarLimpiar(false);
    setAviso('Las tareas finalizadas fueron ocultadas de la lista.');
    setTimeout(() => setAviso(''), 5000);
  };

  return (
    <div>
      <h1 className="titulo-pagina">Tareas</h1>
      <p className="subtitulo-pagina">
        Crea y administra las tareas asignadas a los técnicos.
      </p>

      <ErrorBanner mensaje={error} />
      <OkBanner mensaje={aviso} />

      <div
        style={{
          marginBottom: 24,
          display: 'flex',
          justifyContent: 'center',
          gap: 12,
          flexWrap: 'wrap',
        }}
      >
        <button className="btn btn-primario" onClick={abrirCrear}>
          <Plus size={18} />
          Crear tarea
        </button>
        {tareas.some((t) => t.estado === 'Finalizada') && (
          <button className="btn btn-secundario" onClick={() => setConfirmarLimpiar(true)}>
            <Trash2 size={18} />
            Limpiar finalizadas
          </button>
        )}
      </div>

      {cargando ? (
        <Cargando mensaje="Cargando tareas…" />
      ) : tareas.length === 0 ? (
        <Vacio mensaje="Aún no hay tareas creadas." />
      ) : (
        <div className="lista-tareas">
          {tareas.map((t) => (
            <div key={t.id} className="tarjeta-tarea franja-tarea">
              <span className="codigo franja-tarea-id">{t.codigo}</span>
              <span className="franja-tarea-cliente">{t.nombreRecinto}</span>
              <span className="franja-tarea-direccion">{t.direccion}</span>
              <span className="franja-tarea-estado"><ChipEstado estado={t.estado} /></span>

              {(t.estado === 'No iniciada' || t.estado === 'En proceso') && (
                <div className="tarjeta-tarea-acciones franja-acciones">
                  {t.estado === 'No iniciada' && (
                  <>
                    <button className="btn btn-chico btn-secundario" onClick={() => abrirEditar(t)}>
                      <Pencil size={15} />
                      Editar
                    </button>
                    <button
                      className="btn btn-chico btn-riesgo"
                      onClick={() => {
                        setEliminando(t);
                        setConfirmarAbierto(true);
                      }}
                    >
                      <Trash2 size={15} />
                      Eliminar
                    </button>
                  </>
                  )}
                  {t.estado === 'En proceso' && (
                  <button
                    className="btn btn-chico btn-primario"
                    onClick={() => cambiarEstado(t, 'FINALIZADA')}
                  >
                    <Check size={15} />
                    Finalizar (demo)
                  </button>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Modal crear tarea */}
      <Modal titulo="Crear tarea" abierto={crearAbierto} onCerrar={() => setCrearAbierto(false)}>
        <form onSubmit={crearTarea}>
          <div className="campo">
            <label>Tipo de tarea</label>
            <select value={form.tipo} onChange={actualizar('tipo')}>
              <option value="MANTENIMIENTO">Mantenimiento</option>
              <option value="REPARACION" disabled>
                Reparación (próximamente)
              </option>
              <option value="COTIZACION" disabled>
                Cotización (próximamente)
              </option>
            </select>
          </div>

          <div className="campo">
            <label>Dirección</label>
            <div className="campo-sugerencias">
              <input
                value={form.direccion}
                onChange={cambiarDireccion}
                onFocus={() => setSugerenciasAbiertas(true)}
                onBlur={() => setSugerenciasAbiertas(false)}
                placeholder="Ej: Av. Vicuña Mackena 3001. Ñuñoa."
                autoComplete="off"
              />
              {mostrarSugerencias && (
                <div className="sugerencias">
                  {sugerencias.map((c) => (
                    <button
                      key={c.id}
                      type="button"
                      className="sugerencia-item"
                      onMouseDown={(e) => {
                        e.preventDefault();
                        elegirCliente(c);
                      }}
                    >
                      <strong>{c.direccion}</strong>
                      <span>{c.nombreRecinto}</span>
                    </button>
                  ))}
                </div>
              )}
              {sinCoincidencia && (
                <small style={{ fontSize: 12, color: 'var(--rojo-error)', display: 'block', marginTop: 4 }}>
                  Ningún cliente coincide con la dirección escrita. La tarea se creará sin cliente asociado; revise las tildes o registre el cliente en la pantalla Clientes.
                </small>
              )}
            </div>
            <small style={{ fontSize: 12, color: 'var(--texto-gris)', display: 'block', marginTop: 4 }}>
              {clienteId && clientes.length > 0 ? (
                <span style={{ color: 'var(--verde-ok)' }}>
                  Cliente seleccionado:{' '}
                  {(() => {
                    const c = clientes.find((x) => String(x.id) === clienteId);
                    return c ? `${c.direccion} · ${c.nombreRecinto}` : '';
                  })()}
                  . Puedes seguir editando el nombre del recinto.
                </span>
              ) : clientes.length > 0 ? (
                'Toca el campo de dirección para ver todos los clientes registrados, o escribe para buscar (sin distinguir tildes).'
              ) : (
                'No hay clientes registrados todavía. Puede escribir la dirección a mano.'
              )}
            </small>
          </div>

          <div className="campo">
            <label>Nombre del recinto</label>
            <input
              value={form.nombreRecinto}
              onChange={actualizar('nombreRecinto')}
              placeholder="Ej: Edificio Central"
            />
          </div>

          <div className="campo">
            <label>Descripción de la tarea</label>
            <textarea
              value={form.descripcion}
              onChange={actualizar('descripcion')}
              placeholder="Describa el detalle de la tarea a realizar"
            />
          </div>

          <div className="campo">
            <label>Técnico de cabecera</label>
            <select value={form.tecnicoCabeceraId} onChange={actualizar('tecnicoCabeceraId')}>
              <option value="">Seleccione un técnico</option>
              {tecnicos.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.nombre} {t.apellido}
                </option>
              ))}
            </select>
            {tecnicos.length === 0 && (
              <span style={{ fontSize: 12, color: 'var(--rojo-error)' }}>
                No hay técnicos registrados. Cree uno primero.
              </span>
            )}
          </div>

          <div className="modal-pie">
            <button
              type="button"
              className="btn btn-secundario"
              onClick={() => setCrearAbierto(false)}
            >
              Cancelar
            </button>
            <button type="submit" className="btn btn-primario" disabled={!formCompleto || creando}>
              <Wrench size={18} />
              Crear tarea
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal editar tarea */}
      <Modal titulo="Editar tarea" abierto={!!editando} onCerrar={() => setEditando(null)}>
        {formEdit && (
          <form onSubmit={guardarEdicion}>
            <p style={{ color: 'var(--texto-gris)', marginTop: 0 }}>
              Solo se puede modificar el técnico de cabecera de la tarea{' '}
              <strong>{editando?.codigo}</strong>.
            </p>
            <div className="campo">
              <label>Técnico de cabecera</label>
              <select
                value={formEdit.tecnicoCabeceraId}
                onChange={(e) =>
                  setFormEdit((f) => ({ ...f, tecnicoCabeceraId: e.target.value }))
                }
              >
                <option value="">Seleccione un técnico</option>
                {tecnicos.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.nombre} {t.apellido}
                  </option>
                ))}
              </select>
            </div>
            <div className="modal-pie">
              <button
                type="button"
                className="btn btn-secundario"
                onClick={() => setEditando(null)}
              >
                Cancelar
              </button>
              <button
                type="submit"
                className="btn btn-primario"
                disabled={!formEdit.tecnicoCabeceraId || guardando}
              >
                Guardar cambios
              </button>
            </div>
          </form>
        )}
      </Modal>

      <Confirmar
        abierto={confirmarAbierto}
        titulo="Eliminar tarea"
        mensaje={`¿Seguro que deseas eliminar la tarea ${eliminando?.codigo}? Esta acción no se puede deshacer.`}
        confirmarTexto="Eliminar"
        onConfirmar={confirmarEliminar}
        onCancelar={() => setConfirmarAbierto(false)}
      />

      <Confirmar
        abierto={confirmarLimpiar}
        titulo="Limpiar tareas finalizadas"
        mensaje="¿Está seguro de limpiar la lista de tareas finalizadas? Solo se ocultarán de la lista; no se eliminará información de la base de datos."
        confirmarTexto="Limpiar"
        onConfirmar={limpiarFinalizadas}
        onCancelar={() => setConfirmarLimpiar(false)}
      />
    </div>
  );
}
