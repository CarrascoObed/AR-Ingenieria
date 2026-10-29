import axios from 'axios';

export const api = axios.create({ baseURL: '/api' });

api.interceptors.request.use((config) => {
  try {
    const admin = JSON.parse(localStorage.getItem('aring_admin') || 'null');
    if (admin?.token) {
      config.headers.Authorization = `Bearer ${admin.token}`;
    }
  } catch {
    /* sin token */
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('aring_admin');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export function mensajeError(error) {
  return error?.response?.data?.mensaje || error?.message || 'Ocurrió un error inesperado.';
}

export async function mensajeErrorBlob(error) {
  const data = error?.response?.data;
  if (data instanceof Blob) {
    try {
      const texto = await data.text();
      const json = JSON.parse(texto);
      if (json?.mensaje) {
        return json.mensaje;
      }
    } catch {
      /* el cuerpo no es JSON válido */
    }
  }
  return mensajeError(error);
}

export function iniciales(nombre, apellido) {
  return ((nombre || '')[0] || '') + ((apellido || '')[0] || '');
}
