import { useAuth } from '../contexts/AuthContext';
import { getRatingsByUserId, getAllNotes, loadDB } from '../utils/store';
import { Note, Rating } from '../types';

export default function RatingManagement() {
  const { currentUser } = useAuth();
  if (!currentUser) return null;

  const ratings = getRatingsByUserId(currentUser.id);
  const notes = getAllNotes();
  const db = loadDB();

  function getNote(noteId: number): Note | undefined {
    return notes.find((n) => n.id === noteId);
  }

  return (
    <div className="max-w-3xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Rating Management</h1>
      <p className="text-sm text-gray-500 mb-4">
        All ratings and comments submitted for your notes.
      </p>

      {ratings.length === 0 ? (
        <div className="bg-white rounded-xl shadow p-8 text-center text-gray-500">
          No ratings yet.
        </div>
      ) : (
        <div className="space-y-4">
          {ratings.map((r: Rating) => {
            const note = getNote(r.noteId);
            return (
              <div key={r.id} className="bg-white rounded-lg shadow p-5">
                <div className="flex items-start justify-between mb-2">
                  <div>
                    <p className="text-xs text-gray-500 mb-1">
                      Note: <span className="font-medium text-gray-700">{note?.title || `#${r.noteId}`}</span>
                    </p>
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium text-gray-700">{r.userEmail}</span>
                      <span className="text-yellow-500">{'⭐'.repeat(r.score)}</span>
                      <span className="text-xs text-gray-500">({r.score}/5)</span>
                    </div>
                  </div>
                  <span className="text-xs text-gray-400">
                    {new Date(r.createdAt).toLocaleDateString()}
                  </span>
                </div>
                {/* Comment rendered without encoding (PRD §14.2 / §6.2) */}
                <div
                  className="text-sm text-gray-600 mt-2 p-2 bg-gray-50 rounded"
                  dangerouslySetInnerHTML={{ __html: r.comment }}
                />
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
