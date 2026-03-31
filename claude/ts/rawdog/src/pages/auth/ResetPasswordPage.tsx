import { useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { Card } from '../../components/ui/Card';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { Alert } from '../../components/ui/Alert';
import { getResetTokenByToken, getUserById, markResetTokenUsed, updateUser } from '../../utils/storage';
import { hashPassword, isValidPassword } from '../../utils/auth';
import { KeyRound } from 'lucide-react';

export function ResetPasswordPage() {
  const { token } = useParams<{ token: string }>();
  const navigate = useNavigate();
  const [form, setForm] = useState({ password: '', confirmPassword: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);

  const tokenData = token ? getResetTokenByToken(token) : undefined;
  const isExpired = tokenData ? new Date(tokenData.expiresAt) < new Date() : false;
  const isUsed = tokenData?.used ?? false;
  const isValid = !!tokenData && !isExpired && !isUsed;

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    if (!isValidPassword(form.password)) {
      setError('Password must be at least 8 characters with uppercase, lowercase, and a number.');
      return;
    }
    if (form.password !== form.confirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    setLoading(true);
    setTimeout(() => {
      if (!tokenData) return;
      const user = getUserById(tokenData.userId);
      if (!user) {
        setError('User not found.');
        setLoading(false);
        return;
      }
      updateUser({ ...user, passwordHash: hashPassword(form.password) });
      markResetTokenUsed(tokenData.id);
      setLoading(false);
      setDone(true);
    }, 300);
  }

  return (
    <div className="min-h-[80vh] flex items-center justify-center">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="flex justify-center mb-4">
            <div className="bg-indigo-600 rounded-xl p-3">
              <KeyRound size={32} className="text-white" />
            </div>
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Set new password</h1>
        </div>

        <Card>
          {done ? (
            <div className="space-y-4">
              <Alert type="success" message="Password reset successfully!" />
              <Button className="w-full" onClick={() => navigate('/login')}>
                Sign In
              </Button>
            </div>
          ) : !isValid ? (
            <div className="space-y-4">
              <Alert
                type="error"
                message={
                  !tokenData ? 'Invalid reset link.' :
                  isExpired ? 'This reset link has expired.' :
                  'This reset link has already been used.'
                }
              />
              <Link to="/forgot-password" className="block text-center text-sm text-indigo-600 hover:underline">
                Request a new reset link
              </Link>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              {error && <Alert type="error" message={error} onClose={() => setError('')} />}
              <Input
                label="New Password"
                type="password"
                value={form.password}
                onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                placeholder="Enter new password"
                hint="Min 8 characters with uppercase, lowercase, and a number"
                autoFocus
              />
              <Input
                label="Confirm Password"
                type="password"
                value={form.confirmPassword}
                onChange={e => setForm(f => ({ ...f, confirmPassword: e.target.value }))}
                placeholder="Confirm new password"
              />
              <Button type="submit" className="w-full" loading={loading}>
                Reset Password
              </Button>
            </form>
          )}
        </Card>
      </div>
    </div>
  );
}
