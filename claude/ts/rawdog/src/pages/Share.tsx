import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getNoteByShareToken, getRatingsByNoteId, getAttachmentsByNoteId } from '../utils/store';
import { Note, Rating, Attachment } from '../types';

// Share endpoint serves note without requiring session (PRD §10.2)
export default function Share() {
  const { token } = useParams<{ token: string }>();
  const [note, setNote] = useState<Note | null>(null);
  const [ratings, setRatings] = useState<Rating[]>([]);
  const [attachments, setAttachments] = useState<Attachment[]>([]);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    const n = getNoteByShareToken(token || '');
    if (!n) {
      setNotFound(true);
      return;
    }
    setNote(n);
    setRatings(getRatingsByNoteId(n.id));
    setAttachments(getAttachmentsByNoteId(n.id));
  }, [token]);

  if (notFound) {
    return (
      <div className="max-w-2xl mx-auto mt-10 text-center">
        <div className="bg-white rounded-xl shadow p-8">
          <h1 className="text-xl font-bold text-gray-700">Note Not Found</h1>
          <p className="text-gray-500 mt-2">This share link is invalid or has expired.</p>
        </div>
      </div>
    );
  }

  if (!note) return <div className="text-center py-12">Loading...</div>;

  const avgRating = ratings.length
    ? (ratings.reduce((s, r) => s + r.score, 0) / ratings.length).toFixed(1)
    : '—';

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="bg-white rounded-xl shadow p-6">
        <div className="mb-4 p-2 bg-indigo-50 rounded text-sm text-indigo-700">
          Shared note (no login required)
        </div>
        {/* Content rendered without encoding (PRD §6.2) */}
        <h1
          className="text-2xl font-bold text-gray-800 mb-2"
          dangerouslySetInnerHTML={{ __html: note.title }}
        />
        <div className="flex items-center gap-3 mb-4 text-sm text-gray-500">
          <span>{new Date(note.createdAt).toLocaleDateString()}</span>
          <span>⭐ {avgRating}</span>
        </div>
        <div
          className="prose max-w-none text-gray-700"
          dangerouslySetInnerHTML={{ __html: note.content }}
        />
      </div>

      {attachments.length > 0 && (
        <div className="bg-white rounded-xl shadow p-6">
          <h2 className="text-lg font-semibold text-gray-800 mb-3">Attachments</h2>
          <ul className="space-y-2">
            {attachments.map((att) => (
              <li key={att.id} className="text-sm text-gray-700">{att.originalName}</li>
            ))}
          </ul>
        </div>
      )}

      {ratings.length > 0 && (
        <div className="bg-white rounded-xl shadow p-6">
          <h2 className="text-lg font-semibold text-gray-800 mb-3">Comments</h2>
          <ul className="space-y-3">
            {ratings.map((r) => (
              <li key={r.id} className="border-b border-gray-100 pb-2">
                <div className="text-sm font-medium">{r.userEmail} — {'⭐'.repeat(r.score)}</div>
                {/* Comment rendered without encoding (PRD §6.2) */}
                <div
                  className="text-sm text-gray-600 mt-1"
                  dangerouslySetInnerHTML={{ __html: r.comment }}
                />
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
