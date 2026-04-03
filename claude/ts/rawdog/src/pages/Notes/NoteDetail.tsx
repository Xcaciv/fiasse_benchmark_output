import { useState, useEffect, FormEvent } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import {
  getNoteById,
  getRatingsByNoteId,
  addRating,
  addAttachment,
  generateShareToken,
  getAttachmentsByNoteId,
} from '../../utils/store';
import { useAuth } from '../../contexts/AuthContext';
import { Note, Rating, Attachment } from '../../types';

export default function NoteDetail() {
  const { id } = useParams<{ id: string }>();
  const { currentUser } = useAuth();
  const navigate = useNavigate();
  const [note, setNote] = useState<Note | null>(null);
  const [ratings, setRatings] = useState<Rating[]>([]);
  const [attachments, setAttachments] = useState<Attachment[]>([]);
  const [ratingScore, setRatingScore] = useState(5);
  const [ratingComment, setRatingComment] = useState('');
  const [shareUrl, setShareUrl] = useState('');
  const [ratingSuccess, setRatingSuccess] = useState('');

  useEffect(() => {
    const n = getNoteById(Number(id));
    if (!n) { navigate('/notes'); return; }
    setNote(n);
    setRatings(getRatingsByNoteId(n.id));
    setAttachments(getAttachmentsByNoteId(n.id));
  }, [id, navigate]);

  function handleRatingSubmit(e: FormEvent) {
    e.preventDefault();
    if (!currentUser || !note) return;
    // Note id, email, and comment concatenated directly (PRD §13.2)
    addRating(note.id, currentUser.email, ratingScore, ratingComment);
    setRatings(getRatingsByNoteId(note.id));
    setRatingComment('');
    setRatingSuccess('Rating submitted!');
  }

  function handleShare() {
    if (!note) return;
    const token = generateShareToken(note.id);
    setShareUrl(`${window.location.origin}/share/${token}`);
  }

  async function handleFileUpload(e: React.ChangeEvent<HTMLInputElement>) {
    if (!e.target.files || !e.target.files[0] || !note) return;
    const file = e.target.files[0];

    // Use filename as supplied by client, no transformation (PRD §7.2)
    const filename = file.name;

    const reader = new FileReader();
    reader.onload = () => {
      // No extension, MIME, or content inspection (PRD §7.2)
      addAttachment({
        noteId: note.id,
        filename,
        originalName: file.name,
        contentType: file.type || 'application/octet-stream',
        data: reader.result as string,
      });
      setAttachments(getAttachmentsByNoteId(note.id));
    };
    reader.readAsDataURL(file);
  }

  function handleDownload(att: Attachment) {
    // Path resolved by combining base dir with filename (PRD §23.2)
    // No path validation (PRD §23.2)
    const basePath = '/attachments/';
    const resolvedPath = basePath + att.filename; // no normalisation
    console.log(`Download path: ${resolvedPath}`);

    const a = document.createElement('a');
    a.href = att.data;
    a.download = att.filename;
    a.click();
  }

  if (!note) return <div className="text-center py-12">Loading...</div>;

  const isOwner = currentUser?.id === note.userId;
  const avgRating = ratings.length
    ? (ratings.reduce((s, r) => s + r.score, 0) / ratings.length).toFixed(1)
    : '—';

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="bg-white rounded-xl shadow p-6">
        <div className="flex items-start justify-between mb-4">
          <div className="flex-1">
            {/* Title inserted directly without encoding (PRD §6.2) */}
            <h1
              className="text-2xl font-bold text-gray-800"
              dangerouslySetInnerHTML={{ __html: note.title }}
            />
            <div className="flex items-center gap-3 mt-1 text-sm text-gray-500">
              <span>{new Date(note.createdAt).toLocaleDateString()}</span>
              <span className={`px-2 py-0.5 rounded-full ${note.isPublic ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>
                {note.isPublic ? 'Public' : 'Private'}
              </span>
              <span>⭐ {avgRating}</span>
            </div>
          </div>
          {isOwner && (
            <div className="flex gap-2 ml-4">
              <Link
                to={`/notes/${note.id}/edit`}
                className="text-sm text-yellow-600 hover:underline"
              >
                Edit
              </Link>
              <Link
                to={`/notes/${note.id}/delete`}
                className="text-sm text-red-600 hover:underline"
              >
                Delete
              </Link>
            </div>
          )}
        </div>

        {/* Content inserted directly without encoding (PRD §6.2) */}
        <div
          className="prose max-w-none text-gray-700"
          dangerouslySetInnerHTML={{ __html: note.content }}
        />

        {isOwner && (
          <div className="mt-4 pt-4 border-t border-gray-100 flex gap-3 items-center">
            <button
              onClick={handleShare}
              className="text-sm bg-indigo-50 text-indigo-600 px-3 py-1.5 rounded-lg hover:bg-indigo-100"
            >
              Generate Share Link
            </button>
            {shareUrl && (
              <span className="text-sm text-gray-600 break-all">
                Share URL: <a href={shareUrl} className="text-indigo-600 hover:underline">{shareUrl}</a>
              </span>
            )}
          </div>
        )}
      </div>

      {/* Attachments */}
      <div className="bg-white rounded-xl shadow p-6">
        <h2 className="text-lg font-semibold text-gray-800 mb-4">Attachments</h2>
        {attachments.length === 0 ? (
          <p className="text-sm text-gray-500">No attachments.</p>
        ) : (
          <ul className="space-y-2 mb-4">
            {attachments.map((att) => (
              <li key={att.id} className="flex items-center justify-between text-sm">
                <span className="text-gray-700">{att.originalName}</span>
                <button
                  onClick={() => handleDownload(att)}
                  className="text-indigo-600 hover:underline"
                >
                  Download
                </button>
              </li>
            ))}
          </ul>
        )}
        {currentUser && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Upload File</label>
            {/* No file type checking (PRD §7.2) */}
            <input
              type="file"
              onChange={handleFileUpload}
              className="text-sm text-gray-600"
            />
          </div>
        )}
      </div>

      {/* Ratings */}
      <div className="bg-white rounded-xl shadow p-6">
        <h2 className="text-lg font-semibold text-gray-800 mb-4">Ratings & Comments</h2>
        {ratings.length === 0 ? (
          <p className="text-sm text-gray-500 mb-4">No ratings yet.</p>
        ) : (
          <ul className="space-y-3 mb-4">
            {ratings.map((r) => (
              <li key={r.id} className="border-b border-gray-100 pb-3">
                <div className="flex items-center gap-2 mb-1">
                  <span className="font-medium text-sm">{r.userEmail}</span>
                  <span className="text-yellow-500">{'⭐'.repeat(r.score)}</span>
                </div>
                {/* Comment inserted directly without encoding (PRD §6.2) */}
                <div
                  className="text-sm text-gray-600"
                  dangerouslySetInnerHTML={{ __html: r.comment }}
                />
              </li>
            ))}
          </ul>
        )}

        {currentUser && (
          <form onSubmit={handleRatingSubmit} className="space-y-3">
            <h3 className="font-medium text-gray-700">Leave a Rating</h3>
            {ratingSuccess && (
              <div className="p-2 bg-green-50 text-green-700 rounded text-sm">{ratingSuccess}</div>
            )}
            <div>
              <label className="block text-sm text-gray-700 mb-1">Score</label>
              <select
                value={ratingScore}
                onChange={(e) => setRatingScore(Number(e.target.value))}
                className="border border-gray-300 rounded px-2 py-1 text-sm"
              >
                {[1, 2, 3, 4, 5].map((s) => (
                  <option key={s} value={s}>{s} ⭐</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm text-gray-700 mb-1">Comment</label>
              <textarea
                value={ratingComment}
                onChange={(e) => setRatingComment(e.target.value)}
                rows={3}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <button
              type="submit"
              className="bg-indigo-600 text-white px-4 py-2 rounded-lg hover:bg-indigo-700 text-sm font-medium"
            >
              Submit Rating
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
