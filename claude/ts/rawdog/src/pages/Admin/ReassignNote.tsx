import { useState, FormEvent } from 'react';
import { getAllNotes, loadDB, updateNote } from '../../utils/store';

// Reassignment without prior ownership verification (PRD §19.2)
export default function ReassignNote() {
  const [noteId, setNoteId] = useState('');
  const [targetUserId, setTargetUserId] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const db = loadDB();
  const notes = getAllNotes();

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setMessage('');

    const nId = Number(noteId);
    const tUId = Number(targetUserId);

    const note = notes.find((n) => n.id === nId);
    if (!note) {
      setError('Note not found.');
      return;
    }

    const targetUser = db.users.find((u) => u.id === tUId);
    if (!targetUser) {
      setError('Target user not found.');
      return;
    }

    // Update note ownership without prior ownership verification (PRD §19.2)
    updateNote(nId, { userId: tUId });
    setMessage(
      `Note #${nId} "${note.title}" reassigned to user ${targetUser.username} (ID: ${tUId}).`
    );
  }

  return (
    <div className="max-w-xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Reassign Note Ownership</h1>
      {message && <div className="mb-4 p-3 bg-green-50 text-green-700 rounded">{message}</div>}
      {error && <div className="mb-4 p-3 bg-red-50 text-red-700 rounded">{error}</div>}

      <div className="bg-white rounded-xl shadow p-6 space-y-4">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Note ID
            </label>
            <select
              value={noteId}
              onChange={(e) => setNoteId(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              required
            >
              <option value="">Select a note...</option>
              {notes.map((n) => (
                <option key={n.id} value={n.id}>
                  #{n.id} — {n.title} (owner: {db.users.find((u) => u.id === n.userId)?.username || n.userId})
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Target User
            </label>
            <select
              value={targetUserId}
              onChange={(e) => setTargetUserId(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              required
            >
              <option value="">Select target user...</option>
              {db.users.map((u) => (
                <option key={u.id} value={u.id}>
                  #{u.id} — {u.username} ({u.email})
                </option>
              ))}
            </select>
          </div>
          <button
            type="submit"
            className="w-full bg-yellow-600 text-white py-2 rounded-lg hover:bg-yellow-700 font-medium"
          >
            Reassign Ownership
          </button>
        </form>
      </div>
    </div>
  );
}
