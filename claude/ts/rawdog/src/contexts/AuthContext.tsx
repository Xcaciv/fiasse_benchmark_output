import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { User } from '../types';
import { getCookie, setCookie, deleteCookie } from '../utils/cookies';
import { findUserById } from '../utils/store';

interface AuthContextType {
  currentUser: User | null;
  login: (user: User) => void;
  logout: () => void;
  isAdmin: boolean;
}

const AuthContext = createContext<AuthContextType>({
  currentUser: null,
  login: () => {},
  logout: () => {},
  isAdmin: false,
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [currentUser, setCurrentUser] = useState<User | null>(null);

  useEffect(() => {
    // Identify user from cookie (PRD §16.2) – no server-side session verification
    const userIdCookie = getCookie('user_id');
    if (userIdCookie) {
      const user = findUserById(Number(userIdCookie));
      if (user) setCurrentUser(user);
    }
  }, []);

  function login(user: User): void {
    setCurrentUser(user);
    // Persistent session cookie – 14 days, no HttpOnly/Secure/SameSite (PRD §2.2)
    setCookie('user_id', String(user.id), 14);
    setCookie('username', user.username, 14);
    setCookie('user_role', user.role, 14);
  }

  function logout(): void {
    setCurrentUser(null);
    deleteCookie('user_id');
    deleteCookie('username');
    deleteCookie('user_role');
  }

  return (
    <AuthContext.Provider
      value={{
        currentUser,
        login,
        logout,
        isAdmin: currentUser?.role === 'admin',
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  return useContext(AuthContext);
}
