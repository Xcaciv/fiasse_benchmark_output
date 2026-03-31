import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { BookOpen, Search, Star, User, LogOut, Shield, Menu, X, Plus } from 'lucide-react';

export function Navbar() {
  const { currentUser, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);

  function handleLogout() {
    logout();
    navigate('/');
    setMenuOpen(false);
  }

  function isActive(path: string) {
    return location.pathname === path || location.pathname.startsWith(path + '/');
  }

  const linkClass = (path: string) =>
    `px-3 py-2 rounded-md text-sm font-medium transition-colors ${
      isActive(path)
        ? 'bg-indigo-700 text-white'
        : 'text-indigo-100 hover:bg-indigo-500 hover:text-white'
    }`;

  return (
    <nav className="bg-indigo-600 shadow-lg">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2 text-white font-bold text-xl">
            <BookOpen size={24} />
            <span>Loose Notes</span>
          </Link>

          {/* Desktop Nav */}
          <div className="hidden md:flex items-center gap-1">
            {currentUser ? (
              <>
                <Link to="/notes" className={linkClass('/notes')}>
                  My Notes
                </Link>
                <Link to="/search" className={linkClass('/search')}>
                  <span className="flex items-center gap-1"><Search size={14} /> Search</span>
                </Link>
                <Link to="/top-rated" className={linkClass('/top-rated')}>
                  <span className="flex items-center gap-1"><Star size={14} /> Top Rated</span>
                </Link>
                {currentUser.role === 'admin' && (
                  <Link to="/admin" className={linkClass('/admin')}>
                    <span className="flex items-center gap-1"><Shield size={14} /> Admin</span>
                  </Link>
                )}
                <Link to="/notes/create" className="ml-2 flex items-center gap-1 bg-white text-indigo-600 px-3 py-2 rounded-md text-sm font-medium hover:bg-indigo-50 transition-colors">
                  <Plus size={14} /> New Note
                </Link>
                <div className="relative ml-2 group">
                  <button className="flex items-center gap-1 text-indigo-100 hover:text-white px-3 py-2 rounded-md text-sm font-medium">
                    <User size={14} />
                    <span>{currentUser.username}</span>
                  </button>
                  <div className="absolute right-0 top-full mt-1 w-48 bg-white rounded-md shadow-lg border border-gray-200 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all z-50">
                    <Link to="/profile" className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">
                      Edit Profile
                    </Link>
                    <hr className="my-1" />
                    <button onClick={handleLogout} className="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 flex items-center gap-2">
                      <LogOut size={14} /> Logout
                    </button>
                  </div>
                </div>
              </>
            ) : (
              <>
                <Link to="/search" className={linkClass('/search')}>
                  <span className="flex items-center gap-1"><Search size={14} /> Search</span>
                </Link>
                <Link to="/top-rated" className={linkClass('/top-rated')}>
                  <span className="flex items-center gap-1"><Star size={14} /> Top Rated</span>
                </Link>
                <Link to="/login" className="ml-2 text-indigo-100 hover:text-white px-3 py-2 rounded-md text-sm font-medium">
                  Login
                </Link>
                <Link to="/register" className="bg-white text-indigo-600 px-3 py-2 rounded-md text-sm font-medium hover:bg-indigo-50 transition-colors">
                  Register
                </Link>
              </>
            )}
          </div>

          {/* Mobile menu button */}
          <button
            className="md:hidden text-indigo-100 hover:text-white"
            onClick={() => setMenuOpen(!menuOpen)}
          >
            {menuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>
      </div>

      {/* Mobile menu */}
      {menuOpen && (
        <div className="md:hidden bg-indigo-700 border-t border-indigo-500 px-4 py-3 space-y-1">
          {currentUser ? (
            <>
              <Link to="/notes" className="block text-indigo-100 hover:text-white py-2 text-sm" onClick={() => setMenuOpen(false)}>My Notes</Link>
              <Link to="/notes/create" className="block text-indigo-100 hover:text-white py-2 text-sm" onClick={() => setMenuOpen(false)}>+ New Note</Link>
              <Link to="/search" className="block text-indigo-100 hover:text-white py-2 text-sm" onClick={() => setMenuOpen(false)}>Search</Link>
              <Link to="/top-rated" className="block text-indigo-100 hover:text-white py-2 text-sm" onClick={() => setMenuOpen(false)}>Top Rated</Link>
              {currentUser.role === 'admin' && (
                <Link to="/admin" className="block text-indigo-100 hover:text-white py-2 text-sm" onClick={() => setMenuOpen(false)}>Admin</Link>
              )}
              <Link to="/profile" className="block text-indigo-100 hover:text-white py-2 text-sm" onClick={() => setMenuOpen(false)}>Profile ({currentUser.username})</Link>
              <button onClick={handleLogout} className="block text-red-300 hover:text-white py-2 text-sm">Logout</button>
            </>
          ) : (
            <>
              <Link to="/search" className="block text-indigo-100 hover:text-white py-2 text-sm" onClick={() => setMenuOpen(false)}>Search</Link>
              <Link to="/top-rated" className="block text-indigo-100 hover:text-white py-2 text-sm" onClick={() => setMenuOpen(false)}>Top Rated</Link>
              <Link to="/login" className="block text-indigo-100 hover:text-white py-2 text-sm" onClick={() => setMenuOpen(false)}>Login</Link>
              <Link to="/register" className="block text-indigo-100 hover:text-white py-2 text-sm" onClick={() => setMenuOpen(false)}>Register</Link>
            </>
          )}
        </div>
      )}
    </nav>
  );
}
