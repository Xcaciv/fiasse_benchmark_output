import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import {
  getNoteById, getUserById,
  getAttachmentsByNoteId, deleteAttachment,
  getRatingsByNoteId, upsertRating, getRatingByUserAndNote, getAverageRating,
  getShareLinkByNoteId, createOrReplaceShareLink, revokeShareLink,
  deleteNote, addAuditLog,
} from '../../utils/storage';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';
import { Alert } from '../../components/ui/Alert';
import { StarRating, AverageStars } from '../../components/ui/StarRating';
import { Textarea } from '../../components/ui/Textarea';
import { formatDateTime, formatDate, formatFileSize, getFileIcon } from '../../utils/helpers';
import { Edit, Trash2, Share2, Globe, Lock, Link as LinkIcon, X, ChevronLeft, Paperclip, Star } from 'lucide-react';

export function NoteDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { currentUser } = useAuth();
  const navigate = useNavigate();

  const note = id ? getNoteById(id) : undefined;
  const myExistingRating = note && currentUser ? getRatingByUserAndNote(currentUser.id, note.id) : undefined;

  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [ratingValue, setRatingValue] = useState(myExistingRating?.value ?? 0);
  const [ratingComment, setRatingComment] = useState(myExistingRating?.comment ?? '');
  const [ratingSuccess, setRatingSuccess] = useState('');
  const [ratingError, setRatingError] = useState('');
  const [shareCopied, setShareCopied] = useState(false);
  const [tick, setTick] = useState(0);

  // Sync rating form when the note/user changes
  useEffect(() => {
    if (myExistingRating) {
      setRatingValue(myExistingRating.value);
      setRatingComment(myExistingRating.comment);
    }
  }, [id, currentUser?.id]);

  if (!note) return (
    <div className="text-center py-16">
      <h2 className="text-xl font-semibold text-gray-900 mb-2">Note not found</h2>
      <Link to="/notes" className="text-indigo-600 hover:underline">Back to notes</Link>
    </div>
  );

  const owner = getUserById(note.userId);
  const isOwner = currentUser?.id === note.userId;
  const isAdmin = currentUser?.role === 'admin';
  const canView = note.visibility === 'public' || isOwner || isAdmin;

  if (!canView) return (
    <div className="text-center py-16">
      <Lock size={48} className="mx-auto text-gray-400 mb-4" />
      <h2 className="text-xl font-semibold text-gray-900 mb-2">This note is private</h2>
      <Link to="/notes" className="text-indigo-600 hover:underline">Back to notes</Link>
    </div>
  );

  // Re-read fresh data on each render (tick forces re-read after mutations)
  void tick;
  const attachments = getAttachmentsByNoteId(note.id);
  const ratings = getRatingsByNoteId(note.id);
  const avgRating = getAverageRating(note.id);
  const shareLink = getShareLinkByNoteId(note.id);
  const myRating = currentUser ? getRatingByUserAndNote(currentUser.id, note.id) : undefined;
  const shareUrl = shareLink ? `${window.location.origin}/share/${shareLink.token}` : '';

  function refresh() { setTick(t => t + 1); }

  function handleDelete() {
    deleteNote(note!.id);
    addAuditLog(currentUser!.id, 'NOTE_DELETED', `Deleted note: ${note!.title}`);
    navigate('/notes');
  }

  function handleGenerateShareLink() {
    createOrReplaceShareLink(note!.id);
    addAuditLog(currentUser!.id, 'SHARE_LINK_CREATED', `Created share link for note: ${note!.title}`);
    refresh();
  }

  function handleRevokeShareLink() {
    revokeShareLink(note!.id);
    refresh();
  }

  function handleCopyShareLink() {
    navigator.clipboard.writeText(shareUrl).then(() => {
      setShareCopied(true);
      setTimeout(() => setShareCopied(false), 2000);
    });
  }

  function handleDeleteAttachment(attId: string) {
    deleteAttachment(attId);
    refresh();
  }

  function handleRatingSubmit(e: React.FormEvent) {
    e.preventDefault();
    setRatingError('');
    if (!currentUser) {
      setRatingError('Please sign in to rate notes.');
      return;
    }
    if (ratingValue === 0) {
      setRatingError('Please select a star rating.');
      return;
    }
    upsertRating({ noteId: note!.id, userId: currentUser.id, value: ratingValue, comment: ratingComment });
    setRatingSuccess(myRating ? 'Rating updated!' : 'Rating submitted!');
    refresh();
    setTimeout(() => setRatingSuccess(''), 3000);
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="flex items-center gap-3">
        <button onClick={() => navigate(-1)} className="text-gray-400 hover:text-gray-600">
          <ChevronLeft size={20} />
        </button>
        <div className="flex-1" />
        {(isOwner || isAdmin) && (
          <div className="flex gap-2">
            <Link to={`/notes/${note.id}/edit`}>
              <Button variant="secondary" size="sm">
                <Edit size={14} /> Edit
              </Button>
            </Link>
            <Button variant="danger" size="sm" onClick={() => setShowDeleteModal(true)}>
              <Trash2 size={14} /> Delete
            </Button>
          </div>
        )}
      </div>

      {/* Note header */}
      <Card>
        <div className="space-y-3">
          <div className="flex items-start justify-between gap-4">
            <h1 className="text-2xl font-bold text-gray-900">{note.title}</h1>
            <Badge color={note.visibility === 'public' ? 'green' : 'gray'} className="flex-shrink-0">
              {note.visibility === 'public' ? <><Globe size={10} className="inline mr-1" />Public</> : <><Lock size={10} className="inline mr-1" />Private</>}
            </Badge>
          </div>
          <div className="flex items-center gap-4 text-sm text-gray-500 flex-wrap">
            <span>By <strong className="text-gray-700">{owner?.username || 'Unknown'}</strong></span>
            <span>Created {formatDate(note.createdAt)}</span>
            {note.updatedAt !== note.createdAt && <span>Updated {formatDate(note.updatedAt)}</span>}
          </div>
          {ratings.length > 0 && <AverageStars value={avgRating} count={ratings.length} />}
        </div>
      </Card>

      {/* Content */}
      <Card>
        <div className="prose max-w-none">
          <pre className="whitespace-pre-wrap font-sans text-sm text-gray-800 leading-relaxed">{note.content}</pre>
        </div>
      </Card>

      {/* Attachments */}
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
                  <p className="text-xs text-gray-500">{formatFileSize(att.size)} · {formatDate(att.createdAt)}</p>
                </div>
                {(isOwner || isAdmin) && (
                  <button onClick={() => handleDeleteAttachment(att.id)} className="text-red-400 hover:text-red-600 flex-shrink-0">
                    <X size={14} />
                  </button>
                )}
              </li>
            ))}
          </ul>
        </Card>
      )}

      {/* Share Link */}
      {(isOwner || isAdmin) && (
        <Card>
          <h2 className="font-semibold text-gray-900 mb-3 flex items-center gap-2">
            <Share2 size={16} /> Share Link
          </h2>
          {shareLink ? (
            <div className="space-y-3">
              <div className="flex items-center gap-2 bg-gray-50 rounded-md p-3">
                <LinkIcon size={14} className="text-gray-400 flex-shrink-0" />
                <span className="text-sm text-gray-600 flex-1 truncate">{shareUrl}</span>
                <Button variant="secondary" size="sm" onClick={handleCopyShareLink}>
                  {shareCopied ? 'Copied!' : 'Copy'}
                </Button>
              </div>
              <div className="flex gap-2">
                <Button variant="secondary" size="sm" onClick={handleGenerateShareLink}>
                  Regenerate
                </Button>
                <Button variant="danger" size="sm" onClick={handleRevokeShareLink}>
                  Revoke
                </Button>
              </div>
            </div>
          ) : (
            <Button variant="secondary" size="sm" onClick={handleGenerateShareLink}>
              <Share2 size={14} /> Generate Share Link
            </Button>
          )}
        </Card>
      )}

      {/* Ratings */}
      <Card>
        <h2 className="font-semibold text-gray-900 mb-4 flex items-center gap-2">
          <Star size={16} /> Ratings &amp; Reviews
        </h2>

        {/* Submit rating */}
        {currentUser && (
          <form onSubmit={handleRatingSubmit} className="mb-6 p-4 bg-gray-50 rounded-lg space-y-3">
            <h3 className="text-sm font-medium text-gray-700">{myRating ? 'Update your rating' : 'Rate this note'}</h3>
            {ratingError && <Alert type="error" message={ratingError} />}
            {ratingSuccess && <Alert type="success" message={ratingSuccess} />}
            <div className="flex items-center gap-3">
              <StarRating value={ratingValue} onChange={setRatingValue} size={24} />
              {ratingValue > 0 && <span className="text-sm text-gray-600">{['', 'Poor', 'Fair', 'Good', 'Very Good', 'Excellent'][ratingValue]}</span>}
            </div>
            <Textarea
              value={ratingComment}
              onChange={e => setRatingComment(e.target.value)}
              placeholder="Add a comment (optional)"
              rows={2}
            />
            <Button type="submit" size="sm">
              {myRating ? 'Update Rating' : 'Submit Rating'}
            </Button>
          </form>
        )}

        {/* Ratings list */}
        {ratings.length === 0 ? (
          <p className="text-sm text-gray-500">No ratings yet. Be the first to rate!</p>
        ) : (
          <div className="space-y-4">
            {[...ratings].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()).map(rating => {
              const rater = getUserById(rating.userId);
              return (
                <div key={rating.id} className="border-b last:border-0 pb-4 last:pb-0">
                  <div className="flex items-center gap-3 mb-1">
                    <span className="font-medium text-sm text-gray-900">{rater?.username || 'Unknown'}</span>
                    <StarRating value={rating.value} readOnly size={14} />
                    <span className="text-xs text-gray-400 ml-auto">{formatDateTime(rating.createdAt)}</span>
                  </div>
                  {rating.comment && <p className="text-sm text-gray-600">{rating.comment}</p>}
                </div>
              );
            })}
          </div>
        )}
      </Card>

      {/* Delete confirmation modal */}
      <Modal isOpen={showDeleteModal} onClose={() => setShowDeleteModal(false)} title="Delete Note">
        <div className="space-y-4">
          <p className="text-gray-600">
            Are you sure you want to delete <strong>"{note.title}"</strong>? This will also remove all attachments, ratings, and share links.
            <strong> This action cannot be undone.</strong>
          </p>
          <div className="flex gap-3 justify-end">
            <Button variant="secondary" onClick={() => setShowDeleteModal(false)}>Cancel</Button>
            <Button variant="danger" onClick={handleDelete}>Delete Note</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
