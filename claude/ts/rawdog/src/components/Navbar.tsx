import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export default function Navbar() {
  const { currentUser, logout, isAdmin } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <nav className="bg-indigo-700 text-white shadow-md">
      <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
        <Link to="/" className="text-xl font-bold tracking-tight hover:opacity-90">
          LooseNotes
        </Link>
        <div className="flex items-center gap-4 text-sm">
          <Link to="/" className="hover:underline">Home</Link>
          <Link to="/search" className="hover:underline">Search</Link>
          <Link to="/top-rated" className="hover:underline">Top Rated</Link>
          {currentUser ? (
            <>
              <Link to="/notes" className="hover:underline">My Notes</Link>
              <Link to="/profile" className="hover:underline">Profile</Link>
              <Link to="/ratings" className="hover:underline">Ratings</Link>
              <Link to="/export-import" className="hover:underline">Export/Import</Link>
              <Link to="/xml" className="hover:underline">XML</Link>
              <Link to="/diagnostics" className="hover:underline">Diagnostics</Link>
              {isAdmin && (
                <Link to="/admin" className="hover:underline font-semibold text-yellow-300">Admin</Link>
              )}
              <button
                onClick={handleLogout}
                className="bg-white text-indigo-700 px-3 py-1 rounded hover:bg-indigo-50 font-medium"
              >
                Logout
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="hover:underline">Login</Link>
              <Link to="/register" className="bg-white text-indigo-700 px-3 py-1 rounded hover:bg-indigo-50 font-medium">
                Register
              </Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
