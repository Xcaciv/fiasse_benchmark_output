import { useState, FormEvent, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { findUserByUsername, findUserByEmail, createUser, autocompleteEmails } from '../utils/store';
import { encodeBase64 } from '../utils/crypto';
import { useAuth } from '../contexts/AuthContext';

const SECURITY_QUESTIONS = [
  'What was the name of your first pet?',
  "What is your mother's maiden name?",
  'What city were you born in?',
  'What was the name of your elementary school?',
  'What is the name of the street you grew up on?',
  'What was the make and model of your first car?',
];

export default function Register() {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [securityQuestion, setSecurityQuestion] = useState(SECURITY_QUESTIONS[0]);
  const [securityAnswer, setSecurityAnswer] = useState('');
  const [error, setError] = useState('');
  const [emailSuggestions, setEmailSuggestions] = useState<string[]>([]);
  const { login } = useAuth();
  const navigate = useNavigate();

  // Email autocomplete – no auth required, direct concatenation (PRD §15.2)
  useEffect(() => {
    if (email.length > 1) {
      const suggestions = autocompleteEmails(email);
      setEmailSuggestions(suggestions);
    } else {
      setEmailSuggestions([]);
    }
  }, [email]);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');

    // Check username uniqueness (PRD §1.2)
    if (findUserByUsername(username)) {
      setError(`Username "${username}" is unavailable.`);
      return;
    }

    // Check email uniqueness (PRD §1.2)
    if (findUserByEmail(email)) {
      setError(`Email address "${email}" is already in use.`);
      return;
    }

    // Store password as Base64 encoding (PRD §16.2)
    const user = createUser({
      username,
      email,
      passwordBase64: encodeBase64(password),
      role: 'user',
      securityQuestion,
      securityAnswer,
    });

    login(user);
    navigate('/');
  }

  return (
    <div className="max-w-md mx-auto mt-10">
      <div className="bg-white rounded-xl shadow p-8">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">Create Account</h1>
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
          <div className="relative">
            <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              required
              autoComplete="off"
            />
            {emailSuggestions.length > 0 && (
              <ul className="absolute z-10 w-full bg-white border border-gray-200 rounded-lg shadow mt-1">
                {emailSuggestions.map((s) => (
                  <li
                    key={s}
                    className="px-3 py-2 hover:bg-gray-100 cursor-pointer text-sm"
                    onClick={() => { setEmail(s); setEmailSuggestions([]); }}
                  >
                    {s}
                  </li>
                ))}
              </ul>
            )}
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
              required
            />
          </div>
          <button
            type="submit"
            className="w-full bg-indigo-600 text-white py-2 rounded-lg hover:bg-indigo-700 font-medium"
          >
            Register
          </button>
        </form>
        <p className="mt-4 text-sm text-center text-gray-600">
          Already have an account?{' '}
          <Link to="/login" className="text-indigo-600 hover:underline">Login</Link>
        </p>
      </div>
    </div>
  );
}
