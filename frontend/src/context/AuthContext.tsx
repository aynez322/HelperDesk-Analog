import {
  createContext,
  useContext,
  useState,
  useCallback,
  useEffect,
  type ReactNode,
} from 'react';
import type { User } from '../types';
import { api } from '../api/client';

interface AuthState {
  token: string | null;
  user: User | null;
  role: string | null;
  loading: boolean;
}

interface AuthContextType extends AuthState {
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, fullName: string) => Promise<void>;
  logout: () => void;
  setToken: (token: string, role: string) => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({
    token: localStorage.getItem('token'),
    user: null,
    role: localStorage.getItem('role'),
    loading: !!localStorage.getItem('token'),
  });

  const fetchMe = useCallback(async () => {
    try {
      const me = await api.me();
      const role = me.authorities[0]?.authority?.replace('ROLE_', '') || 'CLIENT';
      setState((s) => ({
        ...s,
        user: {
          id: 0,
          email: me.username,
          fullName: '',
          role,
          active: true,
          createdAt: '',
        },
        role,
        loading: false,
      }));
      localStorage.setItem('role', role);
    } catch {
      localStorage.removeItem('token');
      localStorage.removeItem('role');
      setState({ token: null, user: null, role: null, loading: false });
    }
  }, []);

  useEffect(() => {
    if (state.token && !state.user) {
      fetchMe();
    } else if (!state.token) {
      setState((s) => ({ ...s, loading: false }));
    }
  }, [state.token, state.user, fetchMe]);

  const setToken = useCallback((token: string, role: string) => {
    localStorage.setItem('token', token);
    localStorage.setItem('role', role);
    setState((s) => ({ ...s, token, role, loading: true }));
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const res = await api.login({ email, password });
    setToken(res.token, res.role);
    await fetchMe();
  }, [setToken, fetchMe]);

  const register = useCallback(async (email: string, password: string, fullName: string) => {
    const res = await api.register({ email, password, fullName });
    setToken(res.token, res.role);
    await fetchMe();
  }, [setToken, fetchMe]);

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    setState({ token: null, user: null, role: null, loading: false });
  }, []);

  return (
    <AuthContext.Provider value={{ ...state, login, register, logout, setToken }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
