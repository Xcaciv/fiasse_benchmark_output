import { useState } from 'react';
import { Link } from 'react-router-dom';
import { getUsers, getNotes } from '../../utils/storage';
import { Card } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { formatDate } from '../../utils/helpers';
import { Users, Search, ChevronLeft, FileText } from 'lucide-react';

export function AdminUsersPage() {
  const [query, setQuery] = useState('');

  const allUsers = getUsers().sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  const notes = getNotes();

  const filtered = query.trim()
    ? allUsers.filter(u =>
        u.username.toLowerCase().includes(query.toLowerCase()) ||
        u.email.toLowerCase().includes(query.toLowerCase())
      )
    : allUsers;

  function getNoteCount(userId: string) {
    return notes.filter(n => n.userId === userId).length;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Link to="/admin" className="text-gray-400 hover:text-gray-600">
          <ChevronLeft size={20} />
        </Link>
        <Users size={24} className="text-indigo-600" />
        <h1 className="text-2xl font-bold text-gray-900">User Management</h1>
      </div>

      <div className="flex items-center gap-4">
        <div className="flex-1 max-w-md">
          <div className="relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              value={query}
              onChange={e => setQuery(e.target.value)}
              placeholder="Search by username or email..."
              className="w-full pl-9 pr-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
        </div>
        <span className="text-sm text-gray-500">{filtered.length} of {allUsers.length} users</span>
      </div>

      <Card padding={false}>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="text-left px-4 py-3 font-medium text-gray-600">Username</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">Email</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">Role</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">Notes</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">Registered</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filtered.map(user => (
                <tr key={user.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-900">{user.username}</td>
                  <td className="px-4 py-3 text-gray-600">{user.email}</td>
                  <td className="px-4 py-3">
                    <Badge color={user.role === 'admin' ? 'indigo' : 'gray'}>
                      {user.role}
                    </Badge>
                  </td>
                  <td className="px-4 py-3">
                    <span className="flex items-center gap-1 text-gray-600">
                      <FileText size={12} />
                      {getNoteCount(user.id)}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-500">{formatDate(user.createdAt)}</td>
                  <td className="px-4 py-3">
                    <Link
                      to={`/admin/reassign?userId=${user.id}`}
                      className="text-xs text-indigo-600 hover:underline"
                    >
                      Reassign Notes
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {filtered.length === 0 && (
            <div className="text-center py-8 text-gray-400 text-sm">
              No users found matching "{query}"
            </div>
          )}
        </div>
      </Card>
    </div>
  );
}
