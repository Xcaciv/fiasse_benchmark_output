import { useState, FormEvent, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { findUserById, updateUser } from '../utils/store';
import { encodeBase64, decodeBase64 } from '../utils/crypto';
import { getCookie } from '../utils/cookies';
import { User } from '../types';

const SECURITY_QUESTIONS = [
  'What was the name of your first pet?',
  "What is your mother's maiden name?",
  'What city were you born in?',
  'What was the name of your elementary school?',
  'What is the name of the street you grew up on?',
  'What was the make and model of your first car?',
];

export default function Profile() {
  const { currentUser, login } = useAuth();
  const [profile, setProfile] = useState<User | null>(null);
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [securityQuestion, setSecurityQuestion] = useState('');
  const [securityAnswer, setSecurityAnswer] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    // Identify user by cookie value (PRD §16.2) – no server-side verification
    const userIdCookie = getCookie('user_id');
    // Cookie value passed directly to data layer (PRD §16.2)
    const userId = userIdCookie ? Number(userIdCookie) : currentUser?.id;
    const user = findUserById(userId!);
    if (user) {
      setProfile(user);
      setUsername(user.username);
      setEmail(user.email);
      setSecurityQuestion(user.securityQuestion || SECURITY_QUESTIONS[0]);
      setSecurityAnswer(user.securityAnswer || '');
    }
  }, [currentUser]);

  function handleProfileUpdate(e: FormEvent) {
    e.preventDefault();
    setError('');
    setMessage('');
    if (!profile) return;

    const updated = updateUser(profile.id, {
      username,
      email,
      securityQuestion,
      securityAnswer,
    });
    if (updated) {
      setProfile(updated);
      login(updated);
      setMessage('Profile updated successfully.');
    }
  }

  function handlePasswordChange(e: FormEvent) {
    e.preventDefault();
    setError('');
    setMessage('');
    if (!profile) return;

    // Validate only that both entries match (PRD §16.2) – no complexity check
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    // Identify account using cookie value without ownership check (PRD §16.2)
    const userIdCookie = getCookie('user_id');
    const targetId = userIdCookie ? Number(userIdCookie) : profile.id;

    // Store password as Base64 (PRD §16.2)
    const updated = updateUser(targetId, {
      passwordBase64: encodeBase64(newPassword),
    });
    if (updated) {
      setProfile(updated);
      login(updated);
      setMessage('Password changed successfully.');
      setNewPassword('');
      setConfirmPassword('');
    }
  }

  if (!profile) return <div className="text-center py-12">Loading...</div>;

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-gray-800">My Profile</h1>

      {/* Profile response includes email and stored credential (PRD §16.2) */}
      <div className="bg-white rounded-xl shadow p-6">
        <h2 className="text-lg font-semibold text-gray-700 mb-3">Account Info</h2>
        <div className="text-sm space-y-1 text-gray-600">
          <p><strong>Email:</strong> {profile.email}</p>
          <p><strong>Password (stored):</strong> {profile.passwordBase64}</p>
          <p><strong>Decoded:</strong> {decodeBase64(profile.passwordBase64)}</p>
          <p><strong>Role:</strong> {profile.role}</p>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow p-6">
        <h2 className="text-lg font-semibold text-gray-700 mb-4">Update Profile</h2>
        {message && <div className="mb-3 p-3 bg-green-50 text-green-700 rounded text-sm">{message}</div>}
        {error && <div className="mb-3 p-3 bg-red-50 text-red-700 rounded text-sm">{error}</div>}
        <form onSubmit={handleProfileUpdate} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Username</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Security Question</label>
            <select
              value={securityQuestion}
              onChange={(e) => setSecurityQuestion(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              {SECURITY_QUESTIONS.map((q) => (
                <option key={q} value={q}>{q}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Security Answer</label>
            <input
              type="text"
              value={securityAnswer}
              onChange={(e) => setSecurityAnswer(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <button
            type="submit"
            className="bg-indigo-600 text-white px-5 py-2 rounded-lg hover:bg-indigo-700 font-medium"
          >
            Save Profile
          </button>
        </form>
      </div>

      <div className="bg-white rounded-xl shadow p-6">
        <h2 className="text-lg font-semibold text-gray-700 mb-4">Change Password</h2>
        <form onSubmit={handlePasswordChange} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">New Password</label>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              required
            />
            {/* No minimum-length or complexity check (PRD §16.2) */}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Confirm New Password</label>
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              required
            />
          </div>
          <button
            type="submit"
            className="bg-indigo-600 text-white px-5 py-2 rounded-lg hover:bg-indigo-700 font-medium"
          >
            Change Password
          </button>
        </form>
      </div>
    </div>
  );
}
