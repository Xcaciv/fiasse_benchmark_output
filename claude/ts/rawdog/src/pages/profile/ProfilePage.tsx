import { useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { getUserByUsername, getUserByEmail } from '../../utils/storage';
import { hashPassword, verifyPassword, isValidEmail, isValidPassword } from '../../utils/auth';
import { Card } from '../../components/ui/Card';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { Alert } from '../../components/ui/Alert';
import { User } from 'lucide-react';

export function ProfilePage() {
  const { currentUser, updateCurrentUser } = useAuth();

  const [profileForm, setProfileForm] = useState({
    username: currentUser?.username || '',
    email: currentUser?.email || '',
  });
  const [profileSuccess, setProfileSuccess] = useState('');
  const [profileError, setProfileError] = useState('');
  const [profileLoading, setProfileLoading] = useState(false);

  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [passwordSuccess, setPasswordSuccess] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [passwordLoading, setPasswordLoading] = useState(false);

  if (!currentUser) return null;

  function handleProfileSubmit(e: React.FormEvent) {
    e.preventDefault();
    setProfileError('');
    if (!profileForm.username.trim() || profileForm.username.length < 3) {
      setProfileError('Username must be at least 3 characters.');
      return;
    }
    if (!/^[a-zA-Z0-9_]+$/.test(profileForm.username)) {
      setProfileError('Username can only contain letters, numbers, and underscores.');
      return;
    }
    if (!isValidEmail(profileForm.email)) {
      setProfileError('Please enter a valid email address.');
      return;
    }
    const existingUsername = getUserByUsername(profileForm.username);
    if (existingUsername && existingUsername.id !== currentUser.id) {
      setProfileError('Username is already taken.');
      return;
    }
    const existingEmail = getUserByEmail(profileForm.email);
    if (existingEmail && existingEmail.id !== currentUser.id) {
      setProfileError('Email is already registered.');
      return;
    }
    setProfileLoading(true);
    setTimeout(() => {
      updateCurrentUser({ ...currentUser, username: profileForm.username.trim(), email: profileForm.email.trim() });
      setProfileLoading(false);
      setProfileSuccess('Profile updated successfully!');
      setTimeout(() => setProfileSuccess(''), 3000);
    }, 300);
  }

  function handlePasswordSubmit(e: React.FormEvent) {
    e.preventDefault();
    setPasswordError('');
    if (!passwordForm.currentPassword) {
      setPasswordError('Please enter your current password.');
      return;
    }
    if (!verifyPassword(passwordForm.currentPassword, currentUser.passwordHash)) {
      setPasswordError('Current password is incorrect.');
      return;
    }
    if (!isValidPassword(passwordForm.newPassword)) {
      setPasswordError('New password must be at least 8 characters with uppercase, lowercase, and a number.');
      return;
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordError('New passwords do not match.');
      return;
    }
    setPasswordLoading(true);
    setTimeout(() => {
      updateCurrentUser({ ...currentUser, passwordHash: hashPassword(passwordForm.newPassword) });
      setPasswordLoading(false);
      setPasswordSuccess('Password changed successfully!');
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
      setTimeout(() => setPasswordSuccess(''), 3000);
    }, 300);
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="flex items-center gap-3">
        <div className="bg-indigo-100 rounded-full p-2">
          <User size={24} className="text-indigo-600" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Profile Settings</h1>
          <p className="text-sm text-gray-500">Member since {new Date(currentUser.createdAt).toLocaleDateString()}</p>
        </div>
      </div>

      {/* Profile Info */}
      <Card>
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Profile Information</h2>
        <form onSubmit={handleProfileSubmit} className="space-y-4">
          {profileError && <Alert type="error" message={profileError} onClose={() => setProfileError('')} />}
          {profileSuccess && <Alert type="success" message={profileSuccess} onClose={() => setProfileSuccess('')} />}

          <Input
            label="Username"
            value={profileForm.username}
            onChange={e => setProfileForm(f => ({ ...f, username: e.target.value }))}
            hint="3+ characters, letters, numbers and underscores only"
          />
          <Input
            label="Email Address"
            type="email"
            value={profileForm.email}
            onChange={e => setProfileForm(f => ({ ...f, email: e.target.value }))}
          />

          <div className="flex gap-3 justify-end">
            <Button type="submit" loading={profileLoading}>Save Changes</Button>
          </div>
        </form>
      </Card>

      {/* Change Password */}
      <Card>
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Change Password</h2>
        <form onSubmit={handlePasswordSubmit} className="space-y-4">
          {passwordError && <Alert type="error" message={passwordError} onClose={() => setPasswordError('')} />}
          {passwordSuccess && <Alert type="success" message={passwordSuccess} onClose={() => setPasswordSuccess('')} />}

          <Input
            label="Current Password"
            type="password"
            value={passwordForm.currentPassword}
            onChange={e => setPasswordForm(f => ({ ...f, currentPassword: e.target.value }))}
            autoComplete="current-password"
          />
          <Input
            label="New Password"
            type="password"
            value={passwordForm.newPassword}
            onChange={e => setPasswordForm(f => ({ ...f, newPassword: e.target.value }))}
            hint="Min 8 characters with uppercase, lowercase, and a number"
            autoComplete="new-password"
          />
          <Input
            label="Confirm New Password"
            type="password"
            value={passwordForm.confirmPassword}
            onChange={e => setPasswordForm(f => ({ ...f, confirmPassword: e.target.value }))}
            autoComplete="new-password"
          />

          <div className="flex gap-3 justify-end">
            <Button type="submit" loading={passwordLoading}>Change Password</Button>
          </div>
        </form>
      </Card>

      {/* Account Info */}
      <Card>
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Account Information</h2>
        <dl className="space-y-3 text-sm">
          <div className="flex gap-4">
            <dt className="text-gray-500 w-24">Role</dt>
            <dd className="text-gray-900 font-medium capitalize">{currentUser.role}</dd>
          </div>
          <div className="flex gap-4">
            <dt className="text-gray-500 w-24">User ID</dt>
            <dd className="text-gray-500 font-mono text-xs">{currentUser.id}</dd>
          </div>
        </dl>
      </Card>
    </div>
  );
}
