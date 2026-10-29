import { createContext, useCallback, useContext, useMemo, useState } from 'react';

const RefreshContext = createContext(null);

export function RefreshProvider({ children }) {
  const [key, setKey] = useState(0);

  const trigger = useCallback(() => setKey((k) => k + 1), []);

  const value = useMemo(() => ({ key, trigger }), [key, trigger]);
  return <RefreshContext.Provider value={value}>{children}</RefreshContext.Provider>;
}

export function useRefresh() {
  const ctx = useContext(RefreshContext);
  if (!ctx) throw new Error('useRefresh debe usarse dentro de RefreshProvider');
  return ctx;
}
