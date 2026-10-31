import http from 'k6/http';
import { check, sleep } from 'k6';

const API_BASE_URL = __ENV.API_BASE_URL || 'http://aring-backend:8080/api';
const WEB_BASE_URL = __ENV.WEB_BASE_URL || 'http://aring-frontend';

export const options = {
  scenarios: {
    super_admin_readonly: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 100 },
        { duration: '1m', target: 250 },
        { duration: '1m', target: 500 },
        { duration: '2m', target: 500 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

const ENDPOINTS = [
  '/dashboard/stats',
  '/dashboard/tecnicos',
  '/tareas',
  '/tecnicos',
  '/notificaciones',
  '/pantallas',
  '/super/pantallas',
];

export function setup() {
  if (!__ENV.K6_USERNAME || !__ENV.K6_PASSWORD) {
    throw new Error('Define K6_USERNAME y K6_PASSWORD para autenticar la prueba.');
  }

  const response = http.post(
    `${API_BASE_URL}/auth/login`,
    JSON.stringify({ username: __ENV.K6_USERNAME, password: __ENV.K6_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' }, tags: { endpoint: 'login_setup' } },
  );

  if (response.status !== 200) {
    throw new Error(`No se pudo autenticar la prueba: HTTP ${response.status}. No se inició la carga.`);
  }

  const token = response.json('token');
  if (!token) {
    throw new Error('El login no devolvió un token; no se inició la carga.');
  }
  return { token };
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}` };

  if (__ITER === 0) {
    const home = http.get(WEB_BASE_URL, { tags: { layer: 'frontend', endpoint: 'home' } });
    check(home, { 'frontend responde HTTP 200': (r) => r.status === 200 });
  }

  const endpoint = ENDPOINTS[(__VU + __ITER) % ENDPOINTS.length];
  const response = http.get(`${API_BASE_URL}${endpoint}`, {
    headers,
    tags: { layer: 'api', endpoint },
  });
  check(response, { [`${endpoint} responde HTTP 200`]: (r) => r.status === 200 });

  sleep(2);
}
