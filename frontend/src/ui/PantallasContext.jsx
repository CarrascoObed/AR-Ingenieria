import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';

const PantallasContext = createContext(null);

export function PantallasProvider({ children }) {
  const { admin } = useAuth();
  const [pantallas, setPantallas] = useState([]);

  const esPanel = admin?.rol === 'ROLE_ADMIN' || admin?.rol === 'ROLE_SUPER';

  useEffect(() => {
    if (!esPanel) return;
    let activo = true;
    api
      .get('/pantallas')
      .then((res) => {
        if (activo) setPantallas(res.data || []);
      })
      .catch(() => {
        /* si falla se asume que todo está habilitado */
      });
    return () => {
      activo = false;
    };
  }, [esPanel]);

  const habilitada = (nombre) => {
    const config = pantallas.find((p) => p.nombre === nombre);
    return config ? config.habilitada : true;
  };

  const value = useMemo(() => ({ pantallas, habilitada }), [pantallas]);
  return <PantallasContext.Provider value={value}>{children}</PantallasContext.Provider>;
}

export function usePantallas() {
  const ctx = useContext(PantallasContext);
  if (!ctx) throw new Error('usePantallas debe usarse dentro de PantallasProvider');
  return ctx;
}
