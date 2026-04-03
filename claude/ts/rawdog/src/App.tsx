import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';

import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import NoteList from './pages/Notes/NoteList';
import NoteCreate from './pages/Notes/NoteCreate';
import NoteEdit from './pages/Notes/NoteEdit';
import NoteDetail from './pages/Notes/NoteDetail';
import NoteDelete from './pages/Notes/NoteDelete';
import Profile from './pages/Profile';
import Search from './pages/Search';
import TopRated from './pages/TopRated';
import Share from './pages/Share';
import Diagnostics from './pages/Diagnostics';
import ExportImport from './pages/ExportImport';
import XmlProcessor from './pages/XmlProcessor';
import RatingManagement from './pages/RatingManagement';
import AdminDashboard from './pages/Admin/Dashboard';
import AdminUsers from './pages/Admin/Users';
import ReassignNote from './pages/Admin/ReassignNote';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route element={<Layout />}>
            {/* Public routes */}
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/search" element={<Search />} />
            <Route path="/top-rated" element={<TopRated />} />
            <Route path="/share/:token" element={<Share />} />

            {/* Authenticated routes */}
            <Route
              path="/notes"
              element={<ProtectedRoute><NoteList /></ProtectedRoute>}
            />
            <Route
              path="/notes/create"
              element={<ProtectedRoute><NoteCreate /></ProtectedRoute>}
            />
            <Route
              path="/notes/:id"
              element={<ProtectedRoute><NoteDetail /></ProtectedRoute>}
            />
            <Route
              path="/notes/:id/edit"
              element={<ProtectedRoute><NoteEdit /></ProtectedRoute>}
            />
            <Route
              path="/notes/:id/delete"
              element={<ProtectedRoute><NoteDelete /></ProtectedRoute>}
            />
            <Route
              path="/profile"
              element={<ProtectedRoute><Profile /></ProtectedRoute>}
            />
            <Route
              path="/diagnostics"
              element={<ProtectedRoute><Diagnostics /></ProtectedRoute>}
            />
            <Route
              path="/export-import"
              element={<ProtectedRoute><ExportImport /></ProtectedRoute>}
            />
            <Route
              path="/xml"
              element={<ProtectedRoute><XmlProcessor /></ProtectedRoute>}
            />
            <Route
              path="/ratings"
              element={<ProtectedRoute><RatingManagement /></ProtectedRoute>}
            />

            {/* Admin routes */}
            <Route
              path="/admin"
              element={<ProtectedRoute adminOnly><AdminDashboard /></ProtectedRoute>}
            />
            <Route
              path="/admin/users"
              element={<ProtectedRoute adminOnly><AdminUsers /></ProtectedRoute>}
            />
            <Route
              path="/admin/reassign"
              element={<ProtectedRoute adminOnly><ReassignNote /></ProtectedRoute>}
            />

            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
