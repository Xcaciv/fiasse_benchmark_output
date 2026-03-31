import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { Layout } from './components/layout/Layout';
import { ProtectedRoute } from './components/layout/ProtectedRoute';

import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';
import { ForgotPasswordPage } from './pages/auth/ForgotPasswordPage';
import { ResetPasswordPage } from './pages/auth/ResetPasswordPage';
import { NotesListPage } from './pages/notes/NotesListPage';
import { NoteCreatePage } from './pages/notes/NoteCreatePage';
import { NoteDetailPage } from './pages/notes/NoteDetailPage';
import { NoteEditPage } from './pages/notes/NoteEditPage';
import { SearchPage } from './pages/notes/SearchPage';
import { TopRatedPage } from './pages/notes/TopRatedPage';
import { ShareViewPage } from './pages/share/ShareViewPage';
import { ProfilePage } from './pages/profile/ProfilePage';
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage';
import { AdminUsersPage } from './pages/admin/AdminUsersPage';
import { AdminReassignPage } from './pages/admin/AdminReassignPage';
import { initializeSeedData } from './utils/storage';

// Initialize seed data once
initializeSeedData();

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Layout>
          <Routes>
            {/* Public routes */}
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/reset-password/:token" element={<ResetPasswordPage />} />
            <Route path="/search" element={<SearchPage />} />
            <Route path="/top-rated" element={<TopRatedPage />} />
            <Route path="/share/:token" element={<ShareViewPage />} />

            {/* Protected routes */}
            <Route path="/notes" element={<ProtectedRoute><NotesListPage /></ProtectedRoute>} />
            <Route path="/notes/create" element={<ProtectedRoute><NoteCreatePage /></ProtectedRoute>} />
            <Route path="/notes/:id" element={<NoteDetailPage />} />
            <Route path="/notes/:id/edit" element={<ProtectedRoute><NoteEditPage /></ProtectedRoute>} />
            <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />

            {/* Admin routes */}
            <Route path="/admin" element={<ProtectedRoute adminOnly><AdminDashboardPage /></ProtectedRoute>} />
            <Route path="/admin/users" element={<ProtectedRoute adminOnly><AdminUsersPage /></ProtectedRoute>} />
            <Route path="/admin/reassign" element={<ProtectedRoute adminOnly><AdminReassignPage /></ProtectedRoute>} />

            {/* Fallback */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Layout>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
