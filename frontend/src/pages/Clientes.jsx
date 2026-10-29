import { useEffect, useRef, useState } from 'react';
import { Building2, ImagePlus, Pencil, Trash2, X } from 'lucide-react';
import { api, mensajeError } from '../api/client';
import { useRefresh } from '../ui/RefreshContext';
import {
  Cargando,
  Confirmar,
  ErrorBanner,
  Modal,
  OkBanner,
  SeccionFormulario,
  Vacio,
} from '../ui/Componentes';

const TAMAÑO_MAX = 5 * 1024 * 1024; // 5 MB para el archivo original
const LADO_MAX = 1280; // mismo criterio de compresión que la app móvil

const FORM_VACIO = {
  direccion: '',
  nombreRecinto: '',
};

/**
 * Convierte el archivo a JPEG base64 (máx. 1280 px) para guardarlo
 * en la base de datos como lo hace ya el backend con las mediciones.
 */
function archivoABase64(archivo) {
  return new Promise((resolve, reject) => {
    if (!archivo.type.startsWith('image/')) {
      reject(new Error('El archivo seleccionado no es una imagen.'));
      return;
    }
    if (archivo.size > TAMAÑO_MAX) {
      reject(new Error('La imagen supera el tamaño máximo permitido (5 MB).'));
      return;
    }
    const lector = new FileReader();
    lector.onerror = () => reject(new Error('No se pudo leer la imagen.'));
    lector.onload = () => {
      const imagen = new Image();
      imagen.onerror = () => reject(new Error('El archivo seleccionado no es una imagen válida.'));
      imagen.onload = () => {
        const escala = Math.min(1, LADO_MAX / Math.max(imagen.width, imagen.height));
        const lienzo = document.createElement('canvas');
        lienzo.width = Math.round(imagen.width * escala);
        lienzo.height = Math.round(imagen.height * escala);
        const contexto = lienzo.getContext('2d');
        contexto.drawImage(imagen, 0, 0, lienzo.width, lienzo.height);
        resolve(lienzo.toDataURL('image/jpeg', 0.85).split(',')[1]);
      };
      imagen.src = lector.result;
    };
    lector.readAsDataURL(archivo);
  });
}

export default function Clientes() {
  const { key } = useRefresh();

  const [clientes, setClientes] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');

  const [form, setForm] = useState(FORM_VACIO);
  const [imagen, setImagen] = useState(null); // { base64, tipo, nombre }
  const [creando, setCreando] = useState(false);

  const [editando, setEditando] = useState(null);
  const [formEdit, setFormEdit] = useState(null);
  const [imagenEdit, setImagenEdit] = useState(null); // nueva imagen opcional
  const [guardando, setGuardando] = useState(false);

  const [eliminando, setEliminando] = useState(null);
  const [confirmarAbierto, setConfirmarAbierto] = useState(false);

  const inputArchivo = useRef(null);
  const inputArchivoEdit = useRef(null);

  const cargar = async () => {
    setCargando(true);
    setError('');
    try {
      const res = await api.get('/clientes');
      setClientes(res.data);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => {
    cargar();
  }, [key]);

  const actualizar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));

  const limpiarFormulario = () => {
    setForm(FORM_VACIO);
    setImagen(null);
    if (inputArchivo.current) inputArchivo.current.value = '';
  };

  const elegirImagen = async (e) => {
    const archivo = e.target.files?.[0];
    if (!archivo) return;
    setError('');
    try {
      const base64 = await archivoABase64(archivo);
      setImagen({ base64, nombre: archivo.name });
    } catch (err) {
      setError(err.message);
      e.target.value = '';
    }
  };

  const formCompleto =
    form.direccion.trim() !== '' && form.nombreRecinto.trim() !== '';

  const crearCliente = async (e) => {
    e.preventDefault();
    if (!formCompleto) return;
    setCreando(true);
    setError('');
    try {
      await api.post('/clientes', {
        direccion: form.direccion,
        nombreRecinto: form.nombreRecinto,
        imagenBase64: imagen?.base64 || null,
      });
      limpiarFormulario();
      setAviso('Cliente creado correctamente.');
      cargar();
      setTimeout(() => setAviso(''), 4000);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCreando(false);
    }
  };

  const abrirEditar = (c) => {
    setEditando(c);
    setFormEdit({
      direccion: c.direccion,
      nombreRecinto: c.nombreRecinto,
    });
    setImagenEdit(null);
    if (inputArchivoEdit.current) inputArchivoEdit.current.value = '';
    setError('');
  };

  const elegirImagenEdicion = async (e) => {
    const archivo = e.target.files?.[0];
    if (!archivo) return;
    setError('');
    try {
      const base64 = await archivoABase64(archivo);
      setImagenEdit({ base64, nombre: archivo.name });
    } catch (err) {
      setError(err.message);
      e.target.value = '';
    }
  };

  const editarCompleto = formEdit
    ? formEdit.direccion.trim() !== '' && formEdit.nombreRecinto.trim() !== ''
    : false;

  const guardarEdicion = async (e) => {
    e.preventDefault();
    if (!editarCompleto) return;
    setGuardando(true);
    setError('');
    try {
      await api.put(`/clientes/${editando.id}`, {
        direccion: formEdit.direccion,
        nombreRecinto: formEdit.nombreRecinto,
        imagenBase64: imagenEdit?.base64 || null,
      });
      setEditando(null);
      setImagenEdit(null);
      setAviso('Cliente actualizado correctamente.');
      cargar();
      setTimeout(() => setAviso(''), 4000);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  };

  const confirmarEliminar = async () => {
    setError('');
    try {
      await api.delete(`/clientes/${eliminando.id}`);
      setConfirmarAbierto(false);
      setEliminando(null);
      setAviso('Cliente eliminado correctamente.');
      cargar();
      setTimeout(() => setAviso(''), 4000);
    } catch (err) {
      setError(mensajeError(err));
      setConfirmarAbierto(false);
    }
  };

  const uriImagen = (c) =>
    c.imagenBase64 ? `data:image/${c.imagenTipo || 'jpeg'};base64,${c.imagenBase64}` : null;

  return (
    <div>
      <h1 className="titulo-pagina">Clientes</h1>
      <p className="subtitulo-pagina">
        Registra los recintos de tus clientes y úsalos al crear tareas.
      </p>

      <ErrorBanner mensaje={error} />
      <OkBanner mensaje={aviso} />

      {/* Formulario de creación */}
      <form className="tarjeta tarjeta-padding" onSubmit={crearCliente} style={{ marginBottom: 24 }}>
        <h2 className="seccion-titulo" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Building2 size={18} style={{ color: 'var(--violeta)' }} />
          Crear cliente
        </h2>
        <SeccionFormulario titulo="Datos del cliente">
          <div className="fila">
            <div className="campo">
              <label>Dirección</label>
              <input
                value={form.direccion}
                onChange={actualizar('direccion')}
                placeholder="Ej: Av. Vicuña Mackenna 3001. Ñuñoa."
              />
            </div>
            <div className="campo">
              <label>Nombre del recinto</label>
              <input
                value={form.nombreRecinto}
                onChange={actualizar('nombreRecinto')}
                placeholder="Ej: Edificio Central"
              />
            </div>
          </div>
          <div className="campo">
            <label>Imagen del recinto (opcional)</label>
            <input
              ref={inputArchivo}
              type="file"
              accept="image/*"
              onChange={elegirImagen}
            />
            {imagen && (
              <div className="preview-imagen">
                <img src={`data:image/jpeg;base64,${imagen.base64}`} alt={`Imagen de ${form.nombreRecinto || 'el recinto'}`} />
                <button
                  type="button"
                  className="preview-quitar"
                  onClick={() => {
                    setImagen(null);
                    if (inputArchivo.current) inputArchivo.current.value = '';
                  }}
                  aria-label="Quitar imagen"
                >
                  <X size={14} />
                </button>
              </div>
            )}
          </div>
        </SeccionFormulario>
        <div className="modal-pie" style={{ marginTop: 20 }}>
          <button type="button" className="btn btn-secundario" onClick={limpiarFormulario}>
            Cancelar
          </button>
          <button type="submit" className="btn btn-primario" disabled={!formCompleto || creando}>
            <ImagePlus size={18} />
            Crear
          </button>
        </div>
      </form>

      {/* Lista de clientes */}
      {cargando ? (
        <Cargando mensaje="Cargando clientes…" />
      ) : clientes.length === 0 ? (
        <Vacio mensaje="Aún no hay clientes registrados." />
      ) : (
        <div className="grid-tecnicos">
          {clientes.map((c) => {
            const uri = uriImagen(c);
            return (
              <div key={c.id} className="tarjeta-tecnico">
                <div className="tarjeta-tecnico-cabecera">
                  {uri ? (
                    <img className="imagen-cliente" src={uri} alt={`Imagen de ${c.nombreRecinto}`} />
                  ) : (
                    <span
                      className="imagen-cliente imagen-cliente-vacia"
                      aria-hidden="true"
                    >
                      <Building2 size={22} />
                    </span>
                  )}
                  <div className="datos">
                    <h3>{c.nombreRecinto}</h3>
                    <p>{c.direccion}</p>
                  </div>
                </div>
                <div className="tarjeta-tecnico-acciones">
                  <button className="btn btn-chico btn-secundario" onClick={() => abrirEditar(c)}>
                    <Pencil size={15} />
                    Editar
                  </button>
                  <button
                    className="btn btn-chico btn-riesgo"
                    onClick={() => {
                      setEliminando(c);
                      setConfirmarAbierto(true);
                    }}
                  >
                    <Trash2 size={15} />
                    Eliminar
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Modal editar cliente */}
      <Modal
        titulo="Editar cliente"
        abierto={!!editando}
        onCerrar={() => setEditando(null)}
      >
        {formEdit && (
          <form onSubmit={guardarEdicion}>
            <p style={{ color: 'var(--texto-gris)', marginTop: 0 }}>
              Editando <strong>{editando?.nombreRecinto}</strong>
            </p>
            <SeccionFormulario titulo="Datos del cliente">
              <div className="fila">
                <div className="campo">
                  <label>Dirección</label>
                  <input
                    value={formEdit.direccion}
                    onChange={(e) => setFormEdit((f) => ({ ...f, direccion: e.target.value }))}
                    placeholder="Ej: Av. Vicuña Mackenna 3001. Ñuñoa."
                  />
                </div>
                <div className="campo">
                  <label>Nombre del recinto</label>
                  <input
                    value={formEdit.nombreRecinto}
                    onChange={(e) => setFormEdit((f) => ({ ...f, nombreRecinto: e.target.value }))}
                    placeholder="Ej: Edificio Central"
                  />
                </div>
              </div>
              <div className="campo">
                <label>Imagen del recinto</label>
                {uriImagen(editando) && !imagenEdit && (
                  <div className="preview-imagen">
                    <img src={uriImagen(editando)} alt={`Imagen de ${editando.nombreRecinto}`} />
                  </div>
                )}
                {imagenEdit && (
                  <div className="preview-imagen">
                    <img src={`data:image/jpeg;base64,${imagenEdit.base64}`} alt="Nueva imagen del recinto" />
                    <button
                      type="button"
                      className="preview-quitar"
                      onClick={() => {
                        setImagenEdit(null);
                        if (inputArchivoEdit.current) inputArchivoEdit.current.value = '';
                      }}
                      aria-label="Quitar imagen nueva"
                    >
                      <X size={14} />
                    </button>
                  </div>
                )}
                <input
                  ref={inputArchivoEdit}
                  type="file"
                  accept="image/*"
                  onChange={elegirImagenEdicion}
                />
                <span style={{ fontSize: 12, color: 'var(--texto-gris)' }}>
                  {imagenEdit
                    ? 'Se reemplazará la imagen al guardar.'
                    : 'Se conservará la imagen actual si no seleccionas una nueva.'}
                </span>
              </div>
            </SeccionFormulario>

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
                disabled={!editarCompleto || guardando}
              >
                Guardar cambios
              </button>
            </div>
          </form>
        )}
      </Modal>

      <Confirmar
        abierto={confirmarAbierto}
        titulo="Eliminar cliente"
        mensaje={`¿Seguro que deseas eliminar la dirección ${eliminando?.direccion} (${eliminando?.nombreRecinto})? Esta acción no se puede deshacer.`}
        confirmarTexto="Eliminar"
        onConfirmar={confirmarEliminar}
        onCancelar={() => setConfirmarAbierto(false)}
      />
    </div>
  );
}
