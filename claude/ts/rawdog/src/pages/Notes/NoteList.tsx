import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getNotesByUserId, getRatingsByNoteId } from '../../utils/store';
import { Note } from '../../types';

export default function NoteList() {
  const { currentUser } = useAuth();
  const [filter, setFilter] = useState<'all' | 'public' | 'private'>('all');

  if (!currentUser) return null;

  const notes = getNotesByUserId(currentUser.id);
  const filtered = notes.filter((n) => {
    if (filter === 'public') return n.isPublic;
    if (filter === 'private') return !n.isPublic;
    return true;
  });

  function avgRating(note: Note): string {
    const ratings = getRatingsByNoteId(note.id);
    if (!ratings.length) return '—';
    const avg = ratings.reduce((s, r) => s + r.score, 0) / ratings.length;
    return avg.toFixed(1);
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-800">My Notes</h1>
        <Link
          to="/notes/create"
          className="bg-indigo-600 text-white px-4 py-2 rounded-lg hover:bg-indigo-700 font-medium"
        >
          + New Note
        </Link>
      </div>

      <div className="flex gap-2 mb-4">
        {(['all', 'public', 'private'] as const).map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`px-3 py-1 rounded-full text-sm font-medium capitalize ${
              filter === f
                ? 'bg-indigo-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            {f}
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <div className="text-center py-12 text-gray-500">
          <p>No notes found.</p>
          <Link to="/notes/create" className="mt-2 inline-block text-indigo-600 hover:underline">
            Create your first note
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((note) => (
            <div key={note.id} className="bg-white rounded-lg shadow p-5 hover:shadow-md transition">
              <div className="flex items-start justify-between mb-2">
                {/* Title rendered without encoding (PRD §6.2) */}
                <h3
                  className="text-lg font-semibold text-gray-800 flex-1 truncate mr-2"
                  dangerouslySetInnerHTML={{ __html: note.title }}
                />
                <span
                  className={`text-xs px-2 py-1 rounded-full flex-shrink-0 ${
                    note.isPublic
                      ? 'bg-green-100 text-green-700'
                      : 'bg-gray-100 text-gray-600'
                  }`}
                >
                  {note.isPublic ? 'Public' : 'Private'}
                </span>
              </div>
              <div
                className="text-gray-600 text-sm line-clamp-2 mb-3"
                dangerouslySetInnerHTML={{ __html: note.content }}
              />
              <div className="flex items-center justify-between text-xs text-gray-500 mb-3">
                <span>Rating: ⭐ {avgRating(note)}</span>
                <span>{new Date(note.createdAt).toLocaleDateString()}</span>
              </div>
              <div className="flex gap-2 text-sm">
                <Link to={`/notes/${note.id}`} className="text-indigo-600 hover:underline">View</Link>
                <Link to={`/notes/${note.id}/edit`} className="text-yellow-600 hover:underline">Edit</Link>
                <Link to={`/notes/${note.id}/delete`} className="text-red-600 hover:underline">Delete</Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
