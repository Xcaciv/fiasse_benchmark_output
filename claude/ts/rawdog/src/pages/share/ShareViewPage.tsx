import { useParams, Link } from 'react-router-dom';
import { getShareLinkByToken, getNoteById, getUserById, getAttachmentsByNoteId, getRatingsByNoteId, getAverageRating } from '../../utils/storage';
import { Card } from '../../components/ui/Card';
import { AverageStars } from '../../components/ui/StarRating';
import { formatDate, formatFileSize, getFileIcon } from '../../utils/helpers';
import { BookOpen, Globe, Paperclip } from 'lucide-react';

export function ShareViewPage() {
  const { token } = useParams<{ token: string }>();
  const shareLink = token ? getShareLinkByToken(token) : undefined;
  const note = shareLink ? getNoteById(shareLink.noteId) : undefined;

  if (!shareLink || !note) {
    return (
      <div className="min-h-[80vh] flex items-center justify-center">
        <div className="text-center">
          <BookOpen size={48} className="mx-auto text-gray-300 mb-4" />
          <h2 className="text-xl font-semibold text-gray-900 mb-2">Note not found</h2>
          <p className="text-gray-500 mb-4">This share link may have been revoked or is invalid.</p>
          <Link to="/" className="text-indigo-600 hover:underline">Go to homepage</Link>
        </div>
      </div>
    );
  }

  const author = getUserById(note.userId);
  const attachments = getAttachmentsByNoteId(note.id);
  const ratings = getRatingsByNoteId(note.id);
  const avg = getAverageRating(note.id);

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="flex items-center gap-2 text-sm text-gray-500">
        <Globe size={14} />
        <span>Shared note</span>
      </div>

      <Card>
        <div className="space-y-3">
          <h1 className="text-2xl font-bold text-gray-900">{note.title}</h1>
          <div className="flex items-center gap-4 text-sm text-gray-500 flex-wrap">
            <span>By <strong className="text-gray-700">{author?.username || 'Unknown'}</strong></span>
            <span>Created {formatDate(note.createdAt)}</span>
          </div>
          {ratings.length > 0 && <AverageStars value={avg} count={ratings.length} />}
        </div>
      </Card>

      <Card>
        <pre className="whitespace-pre-wrap font-sans text-sm text-gray-800 leading-relaxed">{note.content}</pre>
      </Card>

      {attachments.length > 0 && (
        <Card>
          <h2 className="font-semibold text-gray-900 mb-3 flex items-center gap-2">
            <Paperclip size={16} /> Attachments ({attachments.length})
          </h2>
          <ul className="space-y-2">
            {attachments.map(att => (
              <li key={att.id} className="flex items-center gap-3 p-3 bg-gray-50 rounded-md">
                <span className="text-lg">{getFileIcon(att.fileType)}</span>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-900 truncate">{att.originalFilename}</p>
                  <p className="text-xs text-gray-500">{formatFileSize(att.size)}</p>
                </div>
              </li>
            ))}
          </ul>
        </Card>
      )}

      <div className="text-center">
        <Link to="/" className="text-sm text-indigo-600 hover:underline">
          Sign in to create your own notes →
        </Link>
      </div>
    </div>
  );
}
