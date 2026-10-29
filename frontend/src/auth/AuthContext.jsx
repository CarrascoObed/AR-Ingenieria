import { createContext, useContext, useMemo, useState } from 'react';
import { api } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [admin, setAdmin] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('aring_admin')) || null;
    } catch {
      return null;
    }
  });

  const login = async (username, password) => {
    const res = await api.post('/auth/login', { username, password });
    const data = res.data;
    if (data.rol !== 'ROLE_ADMIN' && data.rol !== 'ROLE_SUPER') {
      throw new Error('Acceso restringido: esta aplicación es solo para administradores.');
    }
    localStorage.setItem('aring_admin', JSON.stringify(data));
    setAdmin(data);
    return data;
  };

  const logout = () => {
    localStorage.removeItem('aring_admin');
    setAdmin(null);
  };

  const value = useMemo(() => ({ admin, login, logout }), [admin]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider');
  return ctx;
}
