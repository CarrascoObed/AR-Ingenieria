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
  contactoTelefono: '+569',
};

const soloDigitos = (valor, maximo = 8) => String(valor || '').replace(/\D/g, '').slice(0, maximo);

const rutBase = (valor) => {
  const rut = String(valor || '').trim();
  const antesDelDigitoVerificador = rut.includes('-') ? rut.split('-')[0] : rut;
  return soloDigitos(antesDelDigitoVerificador, 8);
};

const telefonoLocal = (valor) => {
  const digitos = String(valor || '').replace(/\D/g, '');
  if (digitos.startsWith('569')) return digitos.slice(3, 11);
  if (digitos.startsWith('56')) {
    const nacional = digitos.slice(2);
    return (nacional.startsWith('9') ? nacional.slice(1) : nacional).slice(0, 8);
  }
  if (digitos.startsWith('9') && digitos.length === 9) return digitos.slice(1);
  return digitos.slice(0, 8);
};

function CampoTelefono({ value, onChange }) {
  return (
    <div className="campo">
      <label>Número telefónico</label>
      <div className="telefono-prefijado">
        <span className="telefono-prefijo">+569</span>
        <input
          type="tel"
          inputMode="numeric"
          autoComplete="tel-national"
          maxLength={8}
          value={telefonoLocal(value)}
          onChange={(e) => onChange(`+569${soloDigitos(e.target.value, 8)}`)}
          placeholder="12345678"
        />
      </div>
      <small>Ingrese los 8 dígitos del número móvil.</small>
    </div>
  );
}

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

  const formCompleto = Object.values(form).every((v) => String(v).trim() !== '');
  const telefonoCrearValido = telefonoLocal(form.contactoTelefono).length === 8;
  const rutCrearValido = /^\d{1,8}$/.test(form.rut);
  const crearValido = formCompleto && telefonoCrearValido && rutCrearValido;

  const abrirCrear = () => {
    setForm(VACIO);
    setError('');
    setCrearAbierto(true);
  };

  const crearTecnico = async (e) => {
    e.preventDefault();
    if (!crearValido) return;
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
      rut: rutBase(t.rut),
      password: '',
      contactoNombre: t.contactoNombre || '',
      contactoApellido: t.contactoApellido || '',
      contactoTelefono: `+569${telefonoLocal(t.contactoTelefono)}`,
    });
    setError('');
  };

  const editarCompleto = formEdit
    ? /^\d{1,8}$/.test(formEdit.rut) &&
      telefonoLocal(formEdit.contactoTelefono).length === 8 &&
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
                <label>RUT del técnico (sin guion ni dígito verificador)</label>
                <input
                  inputMode="numeric"
                  maxLength={8}
                  value={form.rut}
                  onChange={(e) => setForm((f) => ({ ...f, rut: soloDigitos(e.target.value, 8) }))}
                  placeholder="Ej: 12345678"
                />
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
            <CampoTelefono
              value={form.contactoTelefono}
              onChange={(contactoTelefono) => setForm((f) => ({ ...f, contactoTelefono }))}
            />
          </SeccionFormulario>

          <div className="modal-pie">
            <button
              type="button"
              className="btn btn-secundario"
              onClick={() => setCrearAbierto(false)}
            >
              Cancelar
            </button>
            <button type="submit" className="btn btn-primario" disabled={!crearValido || creando}>
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
            <SeccionFormulario titulo="Datos del técnico">
              <div className="campo">
                <label>RUT del técnico (sin guion ni dígito verificador)</label>
                <input
                  inputMode="numeric"
                  maxLength={8}
                  value={formEdit.rut}
                  onChange={(e) => setFormEdit((f) => ({ ...f, rut: soloDigitos(e.target.value, 8) }))}
                  placeholder="Ej: 12345678"
                />
              </div>
            </SeccionFormulario>
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
              <CampoTelefono
                value={formEdit.contactoTelefono}
                onChange={(contactoTelefono) => setFormEdit((f) => ({ ...f, contactoTelefono }))}
              />
            </SeccionFormulario>

            <SeccionFormulario titulo="Contraseña del técnico (opcional)">
              <div className="campo">
                <label>Nueva contraseña</label>
                <input
                  type="password"
                  value={formEdit.password}
                  onChange={(e) => setFormEdit((f) => ({ ...f, password: e.target.value }))}
                  placeholder="Dejar en blanco para conservar la actual"
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
