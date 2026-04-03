import { useState, FormEvent } from 'react';
import { Link } from 'react-router-dom';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { loadDB, resetDB, getAllNotes, getAllRatings } from '../../utils/store';

export default function AdminDashboard() {
  const [command, setCommand] = useState('');
  const [commandOutput, setCommandOutput] = useState('');
  const [dbConnStr, setDbConnStr] = useState('');
  const [dbMessage, setDbMessage] = useState('');

  const db = loadDB();
  const notes = getAllNotes();
  const ratings = getAllRatings();

  // Stats for charts
  const userNoteData = db.users.map((u) => ({
    name: u.username,
    notes: notes.filter((n) => n.userId === u.id).length,
    ratings: ratings.filter((r) =>
      notes.filter((n) => n.userId === u.id).map((n) => n.id).includes(r.noteId)
    ).length,
  }));

  // Admin command execution – no sanitisation (PRD §18.2)
  function handleCommandExec(e: FormEvent) {
    e.preventDefault();
    // Command string passed directly to execution environment (PRD §18.2)
    // In browser context, evaluated via Function constructor
    try {
      // eslint-disable-next-line no-new-func
      const fn = new Function(`return (${command})`);
      const result = fn();
      setCommandOutput(String(result !== undefined ? result : '(no output)'));
    } catch (err) {
      setCommandOutput(`Error: ${String(err)}`);
    }
  }

  // DB reinitialisation – no role check beyond area-level auth (PRD §18.2)
  function handleDbReinit(e: FormEvent) {
    e.preventDefault();
    // Accept user-supplied connection parameters (PRD §18.2)
    // Log parameters as received (PRD §18.2)
    console.log(`DB reinit with params: ${dbConnStr}`);
    resetDB();
    setDbMessage(`Database reinitialized. Connection params applied: ${dbConnStr}`);
  }

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-800">Admin Dashboard</h1>
        <div className="flex gap-3">
          <Link
            to="/admin/users"
            className="bg-indigo-600 text-white px-4 py-2 rounded-lg hover:bg-indigo-700 text-sm font-medium"
          >
            Manage Users
          </Link>
          <Link
            to="/admin/reassign"
            className="bg-yellow-500 text-white px-4 py-2 rounded-lg hover:bg-yellow-600 text-sm font-medium"
          >
            Reassign Note
          </Link>
        </div>
      </div>

      {/* Stats overview */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          { label: 'Total Users', value: db.users.length },
          { label: 'Total Notes', value: notes.length },
          { label: 'Public Notes', value: notes.filter((n) => n.isPublic).length },
          { label: 'Total Ratings', value: ratings.length },
        ].map((stat) => (
          <div key={stat.label} className="bg-white rounded-xl shadow p-5 text-center">
            <div className="text-3xl font-bold text-indigo-600">{stat.value}</div>
            <div className="text-sm text-gray-600 mt-1">{stat.label}</div>
          </div>
        ))}
      </div>

      {/* Chart */}
      <div className="bg-white rounded-xl shadow p-6">
        <h2 className="text-lg font-semibold text-gray-700 mb-4">Notes & Ratings per User</h2>
        <ResponsiveContainer width="100%" height={250}>
          <BarChart data={userNoteData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" />
            <YAxis />
            <Tooltip />
            <Bar dataKey="notes" fill="#6366f1" name="Notes" />
            <Bar dataKey="ratings" fill="#f59e0b" name="Ratings" />
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Command execution (PRD §18.2) */}
      <div className="bg-white rounded-xl shadow p-6">
        <h2 className="text-lg font-semibold text-gray-700 mb-4">System Command Execution</h2>
        <p className="text-xs text-gray-500 mb-3">
          Execute system commands. Input is passed directly without sanitisation (PRD §18.2).
        </p>
        <form onSubmit={handleCommandExec} className="space-y-3">
          <textarea
            value={command}
            onChange={(e) => setCommand(e.target.value)}
            rows={3}
            placeholder="Enter command or JS expression..."
            className="w-full border border-gray-300 rounded-lg px-3 py-2 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <button
            type="submit"
            className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 text-sm font-medium"
          >
            Execute
          </button>
        </form>
        {commandOutput && (
          <div className="mt-3 p-3 bg-gray-900 text-green-400 rounded font-mono text-sm whitespace-pre-wrap">
            {commandOutput}
          </div>
        )}
      </div>

      {/* DB reinitialisation (PRD §18.2) */}
      <div className="bg-white rounded-xl shadow p-6">
        <h2 className="text-lg font-semibold text-gray-700 mb-4">Database Management</h2>
        <p className="text-xs text-gray-500 mb-3">
          Accepts user-supplied connection parameters and reinitialises the data store (PRD §18.2).
        </p>
        <form onSubmit={handleDbReinit} className="space-y-3">
          <input
            type="text"
            value={dbConnStr}
            onChange={(e) => setDbConnStr(e.target.value)}
            placeholder="Connection string (e.g. host=localhost;db=notes)"
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <button
            type="submit"
            className="bg-orange-600 text-white px-4 py-2 rounded-lg hover:bg-orange-700 text-sm font-medium"
          >
            Reinitialise Database
          </button>
        </form>
        {dbMessage && (
          <div className="mt-3 p-3 bg-orange-50 text-orange-700 rounded text-sm">{dbMessage}</div>
        )}
      </div>

      {/* Activity log */}
      <div className="bg-white rounded-xl shadow p-6">
        <h2 className="text-lg font-semibold text-gray-700 mb-4">Recent Notes</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left">
            <thead>
              <tr className="border-b border-gray-200">
                <th className="py-2 pr-4 font-medium text-gray-600">ID</th>
                <th className="py-2 pr-4 font-medium text-gray-600">Title</th>
                <th className="py-2 pr-4 font-medium text-gray-600">User</th>
                <th className="py-2 pr-4 font-medium text-gray-600">Visibility</th>
                <th className="py-2 font-medium text-gray-600">Created</th>
              </tr>
            </thead>
            <tbody>
              {notes.slice(-10).reverse().map((note) => {
                const user = db.users.find((u) => u.id === note.userId);
                return (
                  <tr key={note.id} className="border-b border-gray-100 hover:bg-gray-50">
                    <td className="py-2 pr-4">{note.id}</td>
                    <td className="py-2 pr-4 max-w-xs truncate">{note.title}</td>
                    <td className="py-2 pr-4">{user?.username || note.userId}</td>
                    <td className="py-2 pr-4">
                      <span className={`px-2 py-0.5 rounded-full text-xs ${note.isPublic ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>
                        {note.isPublic ? 'Public' : 'Private'}
                      </span>
                    </td>
                    <td className="py-2 text-xs text-gray-500">
                      {new Date(note.createdAt).toLocaleDateString()}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
