import { useState } from 'react';
import { loadDB, updateUser } from '../../utils/store';
import { decodeBase64 } from '../../utils/crypto';
import { User } from '../../types';

export default function AdminUsers() {
  const [db, setDb] = useState(loadDB());
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editRole, setEditRole] = useState<'user' | 'admin'>('user');
  const [message, setMessage] = useState('');

  function refresh() {
    setDb(loadDB());
  }

  function handleRoleChange(user: User) {
    setEditingId(user.id);
    setEditRole(user.role);
  }

  function saveRole(userId: number) {
    updateUser(userId, { role: editRole });
    setEditingId(null);
    setMessage('User role updated.');
    refresh();
  }

  return (
    <div className="max-w-5xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">User Management</h1>
      {message && (
        <div className="mb-4 p-3 bg-green-50 text-green-700 rounded">{message}</div>
      )}
      <div className="bg-white rounded-xl shadow overflow-hidden">
        <table className="w-full text-sm text-left">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="py-3 px-4 font-medium text-gray-600">ID</th>
              <th className="py-3 px-4 font-medium text-gray-600">Username</th>
              <th className="py-3 px-4 font-medium text-gray-600">Email</th>
              <th className="py-3 px-4 font-medium text-gray-600">Password (Base64)</th>
              <th className="py-3 px-4 font-medium text-gray-600">Decoded</th>
              <th className="py-3 px-4 font-medium text-gray-600">Role</th>
              <th className="py-3 px-4 font-medium text-gray-600">Actions</th>
            </tr>
          </thead>
          <tbody>
            {db.users.map((user) => (
              <tr key={user.id} className="border-b border-gray-100 hover:bg-gray-50">
                <td className="py-3 px-4">{user.id}</td>
                <td className="py-3 px-4 font-medium">{user.username}</td>
                <td className="py-3 px-4">{user.email}</td>
                <td className="py-3 px-4 font-mono text-xs">{user.passwordBase64}</td>
                <td className="py-3 px-4 font-mono text-xs text-red-600">
                  {decodeBase64(user.passwordBase64)}
                </td>
                <td className="py-3 px-4">
                  {editingId === user.id ? (
                    <select
                      value={editRole}
                      onChange={(e) => setEditRole(e.target.value as 'user' | 'admin')}
                      className="border border-gray-300 rounded px-2 py-1 text-xs"
                    >
                      <option value="user">user</option>
                      <option value="admin">admin</option>
                    </select>
                  ) : (
                    <span
                      className={`px-2 py-0.5 rounded-full text-xs ${
                        user.role === 'admin'
                          ? 'bg-yellow-100 text-yellow-700'
                          : 'bg-gray-100 text-gray-600'
                      }`}
                    >
                      {user.role}
                    </span>
                  )}
                </td>
                <td className="py-3 px-4">
                  {editingId === user.id ? (
                    <div className="flex gap-2">
                      <button
                        onClick={() => saveRole(user.id)}
                        className="text-xs text-green-600 hover:underline"
                      >
                        Save
                      </button>
                      <button
                        onClick={() => setEditingId(null)}
                        className="text-xs text-gray-500 hover:underline"
                      >
                        Cancel
                      </button>
                    </div>
                  ) : (
                    <button
                      onClick={() => handleRoleChange(user)}
                      className="text-xs text-indigo-600 hover:underline"
                    >
                      Edit Role
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
