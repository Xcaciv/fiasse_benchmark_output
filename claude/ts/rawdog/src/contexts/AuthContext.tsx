import React, { createContext, useContext, useState, useEffect } from 'react';
import type { User } from '../types';
import {
  getUserByUsername,
  getUserByEmail,
  getUserById,
  createUser,
  updateUser,
  addAuditLog,
} from '../utils/storage';
import { hashPassword, verifyPassword, isValidEmail, isValidPassword } from '../utils/auth';

interface AuthContextType {
  currentUser: User | null;
  login: (usernameOrEmail: string, password: string) => { success: boolean; error?: string };
  logout: () => void;
  register: (username: string, email: string, password: string) => { success: boolean; error?: string };
  updateCurrentUser: (user: User) => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

const SESSION_KEY = 'ln_session_user_id';

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [currentUser, setCurrentUser] = useState<User | null>(() => {
    const savedId = localStorage.getItem(SESSION_KEY);
    if (savedId) {
      return getUserById(savedId) ?? null;
    }
    return null;
  });

  useEffect(() => {
    // Keep currentUser in sync if it was updated elsewhere
    if (currentUser) {
      const fresh = getUserById(currentUser.id);
      if (fresh && JSON.stringify(fresh) !== JSON.stringify(currentUser)) {
        setCurrentUser(fresh);
      }
    }
  }, []);

  function login(usernameOrEmail: string, password: string): { success: boolean; error?: string } {
    const user = getUserByUsername(usernameOrEmail) || getUserByEmail(usernameOrEmail);
    if (!user) {
      addAuditLog('system', 'LOGIN_FAILED', `Failed login attempt for: ${usernameOrEmail}`);
      return { success: false, error: 'Invalid username or password.' };
    }
    if (!verifyPassword(password, user.passwordHash)) {
      addAuditLog(user.id, 'LOGIN_FAILED', `Failed login attempt for user: ${user.username}`);
      return { success: false, error: 'Invalid username or password.' };
    }
    localStorage.setItem(SESSION_KEY, user.id);
    setCurrentUser(user);
    addAuditLog(user.id, 'LOGIN', `User ${user.username} logged in`);
    return { success: true };
  }

  function logout() {
    if (currentUser) {
      addAuditLog(currentUser.id, 'LOGOUT', `User ${currentUser.username} logged out`);
    }
    localStorage.removeItem(SESSION_KEY);
    setCurrentUser(null);
  }

  function register(username: string, email: string, password: string): { success: boolean; error?: string } {
    if (!username.trim() || username.length < 3) {
      return { success: false, error: 'Username must be at least 3 characters.' };
    }
    if (!/^[a-zA-Z0-9_]+$/.test(username)) {
      return { success: false, error: 'Username can only contain letters, numbers, and underscores.' };
    }
    if (!isValidEmail(email)) {
      return { success: false, error: 'Please enter a valid email address.' };
    }
    if (!isValidPassword(password)) {
      return { success: false, error: 'Password must be at least 8 characters with uppercase, lowercase, and a number.' };
    }
    if (getUserByUsername(username)) {
      return { success: false, error: 'Username is already taken.' };
    }
    if (getUserByEmail(email)) {
      return { success: false, error: 'Email is already registered.' };
    }
    const user = createUser({
      username,
      email,
      passwordHash: hashPassword(password),
      role: 'user',
    });
    localStorage.setItem(SESSION_KEY, user.id);
    setCurrentUser(user);
    addAuditLog(user.id, 'USER_REGISTERED', `New user registered: ${user.username}`);
    return { success: true };
  }

  function updateCurrentUser(user: User) {
    updateUser(user);
    setCurrentUser(user);
  }

  return (
    <AuthContext.Provider value={{ currentUser, login, logout, register, updateCurrentUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
