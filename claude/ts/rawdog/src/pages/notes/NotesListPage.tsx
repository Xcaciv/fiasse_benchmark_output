import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getNotes, deleteNote, getAverageRating, getRatingsByNoteId } from '../../utils/storage';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';
import { AverageStars } from '../../components/ui/StarRating';
import { formatDate, truncate } from '../../utils/helpers';
import { Plus, Edit, Trash2, Eye, Lock, Globe } from 'lucide-react';

export function NotesListPage() {
  const { currentUser } = useAuth();
  const [filter, setFilter] = useState<'all' | 'public' | 'private'>('all');
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const [, forceUpdate] = useState(0);

  if (!currentUser) return null;

  const allNotes = getNotes()
    .filter(n => n.userId === currentUser.id)
    .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());

  const notes = filter === 'all' ? allNotes :
    allNotes.filter(n => n.visibility === filter);

  function confirmDelete(id: string) {
    setDeleteId(id);
  }

  function handleDelete() {
    if (!deleteId) return;
    deleteNote(deleteId);
    setDeleteId(null);
    forceUpdate(x => x + 1);
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">My Notes</h1>
        <Link to="/notes/create">
          <Button>
            <Plus size={16} /> New Note
          </Button>
        </Link>
      </div>

      {/* Filter tabs */}
      <div className="flex gap-1 border-b border-gray-200">
        {(['all', 'public', 'private'] as const).map(f => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors capitalize ${
              filter === f
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            {f === 'all' ? `All (${allNotes.length})` : f === 'public' ? `Public (${allNotes.filter(n => n.visibility === 'public').length})` : `Private (${allNotes.filter(n => n.visibility === 'private').length})`}
          </button>
        ))}
      </div>

      {notes.length === 0 ? (
        <div className="text-center py-16">
          <div className="text-gray-400 mb-4">
            <Edit size={48} className="mx-auto" />
          </div>
          <h3 className="text-lg font-medium text-gray-900 mb-2">No notes yet</h3>
          <p className="text-gray-500 mb-4">Create your first note to get started.</p>
          <Link to="/notes/create">
            <Button>
              <Plus size={16} /> Create Note
            </Button>
          </Link>
        </div>
      ) : (
        <div className="grid gap-4">
          {notes.map(note => {
            const ratings = getRatingsByNoteId(note.id);
            const avg = getAverageRating(note.id);
            return (
              <Card key={note.id} className="hover:shadow-md transition-shadow">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1 flex-wrap">
                      <Link
                        to={`/notes/${note.id}`}
                        className="text-lg font-semibold text-gray-900 hover:text-indigo-600 truncate"
                      >
                        {note.title}
                      </Link>
                      <Badge color={note.visibility === 'public' ? 'green' : 'gray'}>
                        {note.visibility === 'public' ? <><Globe size={10} className="inline mr-1" />Public</> : <><Lock size={10} className="inline mr-1" />Private</>}
                      </Badge>
                    </div>
                    <p className="text-sm text-gray-600 mb-2">{truncate(note.content, 200)}</p>
                    <div className="flex items-center gap-4 flex-wrap text-xs text-gray-500">
                      <span>Updated {formatDate(note.updatedAt)}</span>
                      {ratings.length > 0 && (
                        <AverageStars value={avg} count={ratings.length} size={12} />
                      )}
                    </div>
                  </div>
                  <div className="flex gap-2 flex-shrink-0">
                    <Link to={`/notes/${note.id}`}>
                      <Button variant="ghost" size="sm" title="View">
                        <Eye size={14} />
                      </Button>
                    </Link>
                    <Link to={`/notes/${note.id}/edit`}>
                      <Button variant="ghost" size="sm" title="Edit">
                        <Edit size={14} />
                      </Button>
                    </Link>
                    <Button
                      variant="ghost"
                      size="sm"
                      title="Delete"
                      className="text-red-500 hover:bg-red-50"
                      onClick={() => confirmDelete(note.id)}
                    >
                      <Trash2 size={14} />
                    </Button>
                  </div>
                </div>
              </Card>
            );
          })}
        </div>
      )}

      <Modal isOpen={!!deleteId} onClose={() => setDeleteId(null)} title="Delete Note">
        <div className="space-y-4">
          <p className="text-gray-600">
            Are you sure you want to delete this note? This will also remove all attachments, ratings, and share links.
            <strong> This action cannot be undone.</strong>
          </p>
          <div className="flex gap-3 justify-end">
            <Button variant="secondary" onClick={() => setDeleteId(null)}>Cancel</Button>
            <Button variant="danger" onClick={handleDelete}>Delete Note</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
