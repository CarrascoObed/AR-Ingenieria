import { useEffect, useState } from 'react';
import { Pencil, Plus, Trash2, UserPlus } from 'lucide-react';
import { api, mensajeError } from '../api/client';
import {
  Avatar,
  Cargando,
  Confirmar,
  ErrorBanner,
  Modal,
  OkBanner,
  SeccionFormulario,
  Vacio,
} from '../ui/Componentes';

const VACIO = {
  nombre: '',
  apellido: '',
  rut: '',
  password: '',
  contactoNombre: '',
  contactoApellido: '',
  contactoTelefono: '',
};

export default function Tecnicos() {
  const [tecnicos, setTecnicos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');

  const [crearAbierto, setCrearAbierto] = useState(false);
  const [form, setForm] = useState(VACIO);
  const [creando, setCreando] = useState(false);

  const [editando, setEditando] = useState(null);
  const [formEdit, setFormEdit] = useState(null);
  const [guardando, setGuardando] = useState(false);

  const [eliminando, setEliminando] = useState(null);
  const [confirmarAbierto, setConfirmarAbierto] = useState(false);

  const cargar = async () => {
    setCargando(true);
    setError('');
    try {
      const res = await api.get('/tecnicos');
      setTecnicos(res.data);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => {
    cargar();
  }, []);

  const formCompleto = Object.values(form).every((v) => v.trim() !== '');

  const abrirCrear = () => {
    setForm(VACIO);
    setError('');
    setCrearAbierto(true);
  };

  const crearTecnico = async (e) => {
    e.preventDefault();
    if (!formCompleto) return;
    setCreando(true);
    setError('');
    try {
      await api.post('/tecnicos', form);
      setCrearAbierto(false);
      setAviso('Técnico creado correctamente.');
      cargar();
      setTimeout(() => setAviso(''), 4000);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCreando(false);
    }
  };

  const abrirEditar = (t) => {
    setEditando(t);
    setFormEdit({
      password: '',
      contactoNombre: t.contactoNombre || '',
      contactoApellido: t.contactoApellido || '',
      contactoTelefono: t.contactoTelefono || '',
    });
    setError('');
  };

  const editarCompleto = formEdit
    ? formEdit.password.trim() !== '' &&
      formEdit.contactoNombre.trim() !== '' &&
      formEdit.contactoApellido.trim() !== '' &&
      formEdit.contactoTelefono.trim() !== ''
    : false;

  const guardarEdicion = async (e) => {
    e.preventDefault();
    if (!editarCompleto) return;
    setGuardando(true);
    setError('');
    try {
      await api.put(`/tecnicos/${editando.id}`, formEdit);
      setEditando(null);
      setAviso('Técnico actualizado correctamente.');
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
      await api.delete(`/tecnicos/${eliminando.id}`);
      setConfirmarAbierto(false);
      setEliminando(null);
      setAviso('Técnico eliminado correctamente.');
      cargar();
      setTimeout(() => setAviso(''), 4000);
    } catch (err) {
      setError(mensajeError(err));
      setConfirmarAbierto(false);
    }
  };

  const actualizar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));

  return (
    <div>
      <h1 className="titulo-pagina">Técnicos</h1>
      <p className="subtitulo-pagina">
        Administra los técnicos registrados en el sistema.
      </p>

      <ErrorBanner mensaje={error} />
      <OkBanner mensaje={aviso} />

      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'center' }}>
        <button className="btn btn-primario" onClick={abrirCrear}>
          <Plus size={18} />
          Crear técnico
        </button>
      </div>

      {cargando ? (
        <Cargando mensaje="Cargando técnicos…" />
      ) : tecnicos.length === 0 ? (
        <Vacio mensaje="Aún no hay técnicos creados." />
      ) : (
        <div className="lista-franjas">
          {tecnicos.map((t) => (
            <div key={t.id} className="tarjeta-tecnico franja-entidad">
              <div className="tarjeta-tecnico-cabecera">
                <Avatar nombre={t.nombre} apellido={t.apellido} size={40} fontSize={16} />
                <div className="datos">
                  <h3>
                    {t.nombre} {t.apellido}
                  </h3>
                  <p>RUT {t.rut}</p>
                </div>
              </div>
              {t.contactoTelefono && <span className="franja-secundaria">Contacto: {t.contactoTelefono}</span>}
              <div className="tarjeta-tecnico-acciones">
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
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal crear técnico */}
      <Modal
        titulo="Crear técnico"
        abierto={crearAbierto}
        onCerrar={() => setCrearAbierto(false)}
      >
        <form onSubmit={crearTecnico}>
          <SeccionFormulario titulo="Datos del técnico">
            <div className="fila">
              <div className="campo">
                <label>Nombre del técnico</label>
                <input value={form.nombre} onChange={actualizar('nombre')} placeholder="Ej: Juan" />
              </div>
              <div className="campo">
                <label>Apellido del técnico</label>
                <input value={form.apellido} onChange={actualizar('apellido')} placeholder="Ej: Pérez" />
              </div>
            </div>
            <div className="fila">
              <div className="campo">
                <label>RUT del técnico</label>
                <input value={form.rut} onChange={actualizar('rut')} placeholder="Ej: 12345678-9" />
              </div>
              <div className="campo">
                <label>Contraseña del técnico</label>
                <input
                  type="password"
                  value={form.password}
                  onChange={actualizar('password')}
                  placeholder="Asignar contraseña"
                />
              </div>
            </div>
          </SeccionFormulario>

          <SeccionFormulario titulo="Datos de contacto de emergencia">
            <div className="fila">
              <div className="campo">
                <label>Nombre del contacto</label>
                <input
                  value={form.contactoNombre}
                  onChange={actualizar('contactoNombre')}
                  placeholder="Ej: Carlos"
                />
              </div>
              <div className="campo">
                <label>Apellido del contacto</label>
                <input
                  value={form.contactoApellido}
                  onChange={actualizar('contactoApellido')}
                  placeholder="Ej: Pérez"
                />
              </div>
            </div>
            <div className="campo">
              <label>Número telefónico</label>
              <input
                value={form.contactoTelefono}
                onChange={actualizar('contactoTelefono')}
                placeholder="Ej: +56 9 1234 5678"
              />
            </div>
          </SeccionFormulario>

          <div className="modal-pie">
            <button
              type="button"
              className="btn btn-secundario"
              onClick={() => setCrearAbierto(false)}
            >
              Cancelar
            </button>
            <button type="submit" className="btn btn-primario" disabled={!formCompleto || creando}>
              <UserPlus size={18} />
              Crear técnico
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal editar técnico */}
      <Modal
        titulo="Editar técnico"
        abierto={!!editando}
        onCerrar={() => setEditando(null)}
      >
        {formEdit && (
          <form onSubmit={guardarEdicion}>
            <p style={{ color: 'var(--texto-gris)', marginTop: 0 }}>
              Editar {editando?.nombre} {editando?.apellido} (RUT {editando?.rut})
            </p>
            <SeccionFormulario titulo="Datos de contacto de emergencia">
              <div className="fila">
                <div className="campo">
                  <label>Nombre del contacto</label>
                  <input
                    value={formEdit.contactoNombre}
                    onChange={(e) =>
                      setFormEdit((f) => ({ ...f, contactoNombre: e.target.value }))
                    }
                  />
                </div>
                <div className="campo">
                  <label>Apellido del contacto</label>
                  <input
                    value={formEdit.contactoApellido}
                    onChange={(e) =>
                      setFormEdit((f) => ({ ...f, contactoApellido: e.target.value }))
                    }
                  />
                </div>
              </div>
              <div className="campo">
                <label>Número telefónico</label>
                <input
                  value={formEdit.contactoTelefono}
                  onChange={(e) =>
                    setFormEdit((f) => ({ ...f, contactoTelefono: e.target.value }))
                  }
                />
              </div>
            </SeccionFormulario>

            <SeccionFormulario titulo="Contraseña del técnico">
              <div className="campo">
                <label>Nueva contraseña</label>
                <input
                  type="password"
                  value={formEdit.password}
                  onChange={(e) => setFormEdit((f) => ({ ...f, password: e.target.value }))}
                  placeholder="Escriba la nueva contraseña"
                />
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
        titulo="Eliminar técnico"
        mensaje={`¿Seguro que deseas eliminar a ${eliminando?.nombre} ${eliminando?.apellido}? Esta acción no se puede deshacer.`}
        confirmarTexto="Eliminar"
        onConfirmar={confirmarEliminar}
        onCancelar={() => setConfirmarAbierto(false)}
      />
    </div>
  );
}
