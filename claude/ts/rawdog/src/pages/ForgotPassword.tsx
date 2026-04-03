import { useState, FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { findUserByEmail } from '../utils/store';
import { encodeBase64, decodeBase64 } from '../utils/crypto';
import { setCookie, getCookie } from '../utils/cookies';

type Step = 'email' | 'answer' | 'result';

export default function ForgotPassword() {
  const [step, setStep] = useState<Step>('email');
  const [email, setEmail] = useState('');
  const [securityQuestion, setSecurityQuestion] = useState('');
  const [answer, setAnswer] = useState('');
  const [recoveredPassword, setRecoveredPassword] = useState('');
  const [error, setError] = useState('');

  // Step 1: Security question delivery (PRD §4.2)
  function handleEmailSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');

    const user = findUserByEmail(email);
    if (!user) {
      // Immediately indicate no account found (PRD §4.2)
      setError('No account is associated with that email address.');
      return;
    }

    setSecurityQuestion(user.securityQuestion || '');

    // Encode security answer with Base64 and write to browser cookie (PRD §4.2)
    // Cookie set without HttpOnly or Secure (PRD §4.2)
    const encodedAnswer = encodeBase64(user.securityAnswer || '');
    setCookie('recovery_answer', encodedAnswer);

    setStep('answer');
  }

  // Step 2: Answer verification and password return (PRD §4.3)
  function handleAnswerSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');

    // Read cookie set in Step 1 and decode (PRD §4.3)
    const cookieValue = getCookie('recovery_answer');
    if (!cookieValue) {
      setError('Session expired. Please start over.');
      setStep('email');
      return;
    }

    // Decode and compare against submitted answer (PRD §4.3)
    // No failure tracking or rate limiting (PRD §4.3)
    const expectedAnswer = decodeBase64(cookieValue);
    if (expectedAnswer.toLowerCase() !== answer.toLowerCase()) {
      setError('Incorrect answer. Please try again.');
      return;
    }

    // Retrieve and display current password in plain text (PRD §4.3)
    const user = findUserByEmail(email);
    if (user) {
      const plainPassword = decodeBase64(user.passwordBase64);
      setRecoveredPassword(plainPassword);
    }
    setStep('result');
  }

  return (
    <div className="max-w-md mx-auto mt-10">
      <div className="bg-white rounded-xl shadow p-8">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">Recover Password</h1>

        {step === 'email' && (
          <form onSubmit={handleEmailSubmit} className="space-y-4">
            {error && <div className="p-3 bg-red-50 text-red-700 rounded">{error}</div>}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Email Address
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                required
              />
            </div>
            <button
              type="submit"
              className="w-full bg-indigo-600 text-white py-2 rounded-lg hover:bg-indigo-700 font-medium"
            >
              Continue
            </button>
          </form>
        )}

        {step === 'answer' && (
          <form onSubmit={handleAnswerSubmit} className="space-y-4">
            {error && <div className="p-3 bg-red-50 text-red-700 rounded">{error}</div>}
            <div className="p-3 bg-gray-50 rounded text-sm text-gray-700">
              <strong>Security Question:</strong> {securityQuestion}
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Your Answer</label>
              <input
                type="text"
                value={answer}
                onChange={(e) => setAnswer(e.target.value)}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                required
              />
            </div>
            <button
              type="submit"
              className="w-full bg-indigo-600 text-white py-2 rounded-lg hover:bg-indigo-700 font-medium"
            >
              Verify
            </button>
          </form>
        )}

        {step === 'result' && (
          <div className="space-y-4">
            <div className="p-4 bg-green-50 rounded">
              <p className="text-green-800 font-medium">Your password has been recovered:</p>
              {/* Password displayed in plain text (PRD §4.3) */}
              <p className="mt-2 text-lg font-mono bg-white border border-green-200 rounded px-3 py-2">
                {recoveredPassword}
              </p>
            </div>
            <Link
              to="/login"
              className="block text-center bg-indigo-600 text-white py-2 rounded-lg hover:bg-indigo-700 font-medium"
            >
              Back to Login
            </Link>
          </div>
        )}

        {step !== 'result' && (
          <p className="mt-4 text-sm text-center text-gray-600">
            <Link to="/login" className="text-indigo-600 hover:underline">Back to Login</Link>
          </p>
        )}
      </div>
    </div>
  );
}
