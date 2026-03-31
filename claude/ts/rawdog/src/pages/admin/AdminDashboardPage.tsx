import { Link } from 'react-router-dom';
import { getUsers, getNotes, getAuditLogs, getRatings } from '../../utils/storage';
import { Card } from '../../components/ui/Card';
import { formatDateTime } from '../../utils/helpers';
import {
  LineChart, Line, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from 'recharts';
import { Users, FileText, Star, Activity, Shield } from 'lucide-react';

function getMonthLabel(dateStr: string): string {
  const d = new Date(dateStr);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

function buildTimeSeriesData(items: { createdAt: string }[], label: string) {
  const counts: Record<string, number> = {};
  for (const item of items) {
    const month = getMonthLabel(item.createdAt);
    counts[month] = (counts[month] || 0) + 1;
  }
  return Object.entries(counts)
    .sort(([a], [b]) => a.localeCompare(b))
    .slice(-6)
    .map(([month, count]) => ({ month, [label]: count }));
}

const COLORS = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

export function AdminDashboardPage() {
  const users = getUsers();
  const notes = getNotes();
  const ratings = getRatings();
  const auditLogs = getAuditLogs().slice(0, 20);

  const totalUsers = users.length;
  const totalNotes = notes.length;
  const totalRatings = ratings.length;
  const publicNotes = notes.filter(n => n.visibility === 'public').length;
  const privateNotes = notes.filter(n => n.visibility === 'private').length;

  const userGrowth = buildTimeSeriesData(users, 'Users');
  const noteGrowth = buildTimeSeriesData(notes, 'Notes');

  const visibilityData = [
    { name: 'Public', value: publicNotes },
    { name: 'Private', value: privateNotes },
  ];

  const roleData = [
    { name: 'Users', value: users.filter(u => u.role === 'user').length },
    { name: 'Admins', value: users.filter(u => u.role === 'admin').length },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Shield size={28} className="text-indigo-600" />
        <h1 className="text-2xl font-bold text-gray-900">Admin Dashboard</h1>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          { label: 'Total Users', value: totalUsers, icon: Users, color: 'text-indigo-600 bg-indigo-50' },
          { label: 'Total Notes', value: totalNotes, icon: FileText, color: 'text-green-600 bg-green-50' },
          { label: 'Total Ratings', value: totalRatings, icon: Star, color: 'text-yellow-600 bg-yellow-50' },
          { label: 'Public Notes', value: publicNotes, icon: Activity, color: 'text-blue-600 bg-blue-50' },
        ].map(({ label, value, icon: Icon, color }) => (
          <Card key={label}>
            <div className="flex items-center gap-3">
              <div className={`rounded-lg p-2 ${color}`}>
                <Icon size={20} />
              </div>
              <div>
                <p className="text-2xl font-bold text-gray-900">{value}</p>
                <p className="text-xs text-gray-500">{label}</p>
              </div>
            </div>
          </Card>
        ))}
      </div>

      {/* Quick links */}
      <div className="flex gap-3 flex-wrap">
        <Link
          to="/admin/users"
          className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 transition-colors text-sm font-medium"
        >
          <Users size={14} /> Manage Users
        </Link>
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* User growth */}
        <Card>
          <h2 className="font-semibold text-gray-900 mb-4">User Growth (Last 6 Months)</h2>
          {userGrowth.length > 0 ? (
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={userGrowth}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="month" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
                <Tooltip />
                <Bar dataKey="Users" fill="#6366f1" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          ) : <p className="text-sm text-gray-400 py-8 text-center">No data</p>}
        </Card>

        {/* Note growth */}
        <Card>
          <h2 className="font-semibold text-gray-900 mb-4">Note Creation (Last 6 Months)</h2>
          {noteGrowth.length > 0 ? (
            <ResponsiveContainer width="100%" height={220}>
              <LineChart data={noteGrowth}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="month" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
                <Tooltip />
                <Line type="monotone" dataKey="Notes" stroke="#10b981" strokeWidth={2} dot={{ fill: '#10b981' }} />
              </LineChart>
            </ResponsiveContainer>
          ) : <p className="text-sm text-gray-400 py-8 text-center">No data</p>}
        </Card>

        {/* Note visibility pie */}
        <Card>
          <h2 className="font-semibold text-gray-900 mb-4">Notes by Visibility</h2>
          <ResponsiveContainer width="100%" height={220}>
            <PieChart>
              <Pie data={visibilityData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label={({ name, value }) => `${name}: ${value}`}>
                {visibilityData.map((_, i) => <Cell key={i} fill={COLORS[i]} />)}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </Card>

        {/* Role distribution */}
        <Card>
          <h2 className="font-semibold text-gray-900 mb-4">Users by Role</h2>
          <ResponsiveContainer width="100%" height={220}>
            <PieChart>
              <Pie data={roleData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label={({ name, value }) => `${name}: ${value}`}>
                {roleData.map((_, i) => <Cell key={i} fill={COLORS[i + 2]} />)}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </Card>
      </div>

      {/* Recent activity */}
      <Card>
        <h2 className="font-semibold text-gray-900 mb-4 flex items-center gap-2">
          <Activity size={16} /> Recent Activity
        </h2>
        {auditLogs.length === 0 ? (
          <p className="text-sm text-gray-400">No activity recorded yet.</p>
        ) : (
          <div className="space-y-2">
            {auditLogs.map(log => {
              const user = getUsers().find(u => u.id === log.userId);
              return (
                <div key={log.id} className="flex items-start gap-3 py-2 border-b last:border-0">
                  <span className="flex-shrink-0 text-xs font-mono bg-gray-100 text-gray-600 px-2 py-0.5 rounded">{log.action}</span>
                  <span className="text-sm text-gray-600 flex-1">{log.details}</span>
                  <div className="text-right flex-shrink-0">
                    <div className="text-xs text-gray-400">{formatDateTime(log.timestamp)}</div>
                    {user && <div className="text-xs text-gray-500">{user.username}</div>}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </Card>
    </div>
  );
}
