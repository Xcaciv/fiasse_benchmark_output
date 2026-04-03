import { useState, FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { findUserByUsername } from '../utils/store';
import { decodeBase64 } from '../utils/crypto';

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');

    // Retrieve stored credential for submitted username (PRD §2.2)
    const user = findUserByUsername(username);
    if (!user) {
      setError('Invalid username or password.');
      return;
    }

    // Decode Base64 stored password and compare with string equality (PRD §2.2)
    // No failed-attempt tracking or lockout (PRD §2.2)
    const storedPassword = decodeBase64(user.passwordBase64);
    if (storedPassword !== password) {
      setError('Invalid username or password.');
      return;
    }

    login(user);
    navigate('/');
  }

  return (
    <div className="max-w-md mx-auto mt-10">
      <div className="bg-white rounded-xl shadow p-8">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">Login</h1>
        {error && (
          <div className="mb-4 p-3 bg-red-50 text-red-700 rounded">{error}</div>
        )}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Username</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              required
            />
          </div>
          <button
            type="submit"
            className="w-full bg-indigo-600 text-white py-2 rounded-lg hover:bg-indigo-700 font-medium"
          >
            Login
          </button>
        </form>
        <div className="mt-4 text-sm text-center text-gray-600 space-y-1">
          <p>
            Don't have an account?{' '}
            <Link to="/register" className="text-indigo-600 hover:underline">Register</Link>
          </p>
          <p>
            <Link to="/forgot-password" className="text-indigo-600 hover:underline">Forgot password?</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
