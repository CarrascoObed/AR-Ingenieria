import { useState } from 'react';
import { Download, FileSpreadsheet, FileText, History } from 'lucide-react';
import { api, mensajeErrorBlob } from '../api/client';

function Filtros({ filtro, setFiltro }) {
  const actualizar = (campo) => (e) => setFiltro((f) => ({ ...f, [campo]: e.target.value }));
  const actualizarId = (e) => setFiltro((f) => ({ ...f, idTarea: e.target.value.toUpperCase() }));
  return (
    <div className="fila">
      <div className="campo">
        <label>ID de tarea</label>
        <input
          value={filtro.idTarea}
          onChange={actualizarId}
          placeholder="Ej: M-0001"
        />
      </div>
      <div className="campo">
        <label>Nombre técnico</label>
        <input
          value={filtro.nombreTecnico}
          onChange={actualizar('nombreTecnico')}
          placeholder="Ej: Juan"
        />
      </div>
      <div className="campo">
        <label>Nombre del recinto</label>
        <input
          value={filtro.nombreRecinto}
          onChange={actualizar('nombreRecinto')}
          placeholder="Ej: Edificio Central"
        />
      </div>
    </div>
  );
}

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

export default function Documentos() {
  const [filtroHistorial, setFiltroHistorial] = useState({
    fechaDesde: '',
    fechaHasta: '',
  });
  const [filtroFinalizadas, setFiltroFinalizadas] = useState({
    idTarea: '',
    nombreTecnico: '',
    nombreRecinto: '',
  });
  const [error, setError] = useState('');
  const [descargando, setDescargando] = useState('');

  const descargar = async (ruta, nombreArchivo, filtros) => {
    setError('');
    if (filtros.idTarea && !filtros.idTarea.trim()) {
      setError('Debe indicar el ID de la tarea a descargar.');
      return;
    }
    setDescargando(ruta);
    try {
      const params = {};
      if (filtros.idTarea?.trim()) params.idTarea = filtros.idTarea.trim();
      if (filtros.nombreTecnico?.trim()) params.nombreTecnico = filtros.nombreTecnico.trim();
      if (filtros.nombreRecinto?.trim()) params.nombreRecinto = filtros.nombreRecinto.trim();
      if (filtros.fechaDesde?.trim()) params.fechaDesde = filtros.fechaDesde.trim();
      if (filtros.fechaHasta?.trim()) params.fechaHasta = filtros.fechaHasta.trim();

      const res = await api.get(ruta, { params, responseType: 'blob' });
      const blobUrl = URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = blobUrl;
      link.download = nombreArchivo;
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
          Descarga la rendición de servicios de las tareas finalizadas en formato Excel o el informe
          de cada tablero inspeccionado en formato Word. Puedes filtrar por ID de tarea, nombre del
          técnico o nombre del recinto.
        </p>
        <Filtros filtro={filtroFinalizadas} setFiltro={setFiltroFinalizadas} />
        <p className="descripcion">Para descargar, primero indica el ID de la tarea.</p>
        <div className="fila-descarga">
          <button
            className="btn btn-secundario"
            onClick={() =>
              descargar(
                '/documentos/finalizadas.xlsx',
                `Rendición de servicios-${filtroFinalizadas.idTarea.trim()}.xlsx`,
                filtroFinalizadas
              )
            }
            disabled={
              !filtroFinalizadas.idTarea.trim() || descargando === '/documentos/finalizadas.xlsx'
            }
          >
            <FileSpreadsheet size={18} />
            {descargando === '/documentos/finalizadas.xlsx'
              ? 'Descargando…'
              : 'Descargar Rendición de servicios'}
          </button>
          <button
            className="btn btn-secundario"
            onClick={() =>
              descargar(
                '/documentos/finalizadas.docx',
                `informe-tareas-finalizadas-${filtroFinalizadas.idTarea.trim()}.docx`,
                filtroFinalizadas
              )
            }
            disabled={
              !filtroFinalizadas.idTarea.trim() || descargando === '/documentos/finalizadas.docx'
            }
          >
            <Download size={18} />
            {descargando === '/documentos/finalizadas.docx' ? 'Descargando…' : 'Descargar Informe'}
          </button>
        </div>
      </section>
    </div>
  );
}
