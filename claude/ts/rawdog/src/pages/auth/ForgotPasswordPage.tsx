import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Card } from '../../components/ui/Card';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { Alert } from '../../components/ui/Alert';
import { getUserByEmail, createResetToken } from '../../utils/storage';
import { isValidEmail } from '../../utils/auth';
import { Mail } from 'lucide-react';

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [sent, setSent] = useState(false);
  const [token, setToken] = useState('');
  const [loading, setLoading] = useState(false);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    if (!isValidEmail(email)) {
      setError('Please enter a valid email address.');
      return;
    }
    setLoading(true);
    setTimeout(() => {
      const user = getUserByEmail(email);
      setLoading(false);
      if (user) {
        const resetToken = createResetToken(user.id);
        setToken(resetToken.token);
      }
      // Always show success to prevent email enumeration
      setSent(true);
    }, 500);
  }

  return (
    <div className="min-h-[80vh] flex items-center justify-center">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="flex justify-center mb-4">
            <div className="bg-indigo-600 rounded-xl p-3">
              <Mail size={32} className="text-white" />
            </div>
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Reset your password</h1>
          <p className="text-gray-600 mt-1">Enter your email to receive a reset link.</p>
        </div>

        <Card>
          {sent ? (
            <div className="space-y-4">
              <Alert
                type="success"
                message="If an account exists with that email, a reset link has been sent."
              />
              {token && (
                <div className="bg-blue-50 border border-blue-200 rounded p-3 text-sm">
                  <p className="text-blue-800 font-medium mb-1">Demo mode — your reset token:</p>
                  <Link
                    to={`/reset-password/${token}`}
                    className="text-blue-600 break-all hover:underline"
                  >
                    Click here to reset your password
                  </Link>
                </div>
              )}
              <Link to="/login" className="block text-center text-sm text-indigo-600 hover:underline">
                Back to sign in
              </Link>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              {error && <Alert type="error" message={error} onClose={() => setError('')} />}
              <Input
                label="Email Address"
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                placeholder="Enter your email"
                autoFocus
              />
              <Button type="submit" className="w-full" loading={loading}>
                Send Reset Link
              </Button>
              <p className="text-center text-sm text-gray-600">
                <Link to="/login" className="text-indigo-600 hover:underline">Back to sign in</Link>
              </p>
            </form>
          )}
        </Card>
      </div>
    </div>
  );
}
