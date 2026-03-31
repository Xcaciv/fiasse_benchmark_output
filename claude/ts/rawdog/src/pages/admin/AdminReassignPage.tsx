import { useState } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getUsers, getNotes, updateNote, addAuditLog } from '../../utils/storage';
import { Card } from '../../components/ui/Card';
import { Select } from '../../components/ui/Select';
import { Button } from '../../components/ui/Button';
import { Alert } from '../../components/ui/Alert';
import { ChevronLeft, ArrowRight } from 'lucide-react';

export function AdminReassignPage() {
  const { currentUser } = useAuth();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const preSelectedUserId = searchParams.get('userId') || '';

  const allUsers = getUsers();
  const allNotes = getNotes();

  const [fromUserId, setFromUserId] = useState(preSelectedUserId);
  const [selectedNoteId, setSelectedNoteId] = useState('');
  const [toUserId, setToUserId] = useState('');
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const fromUserNotes = fromUserId
    ? allNotes.filter(n => n.userId === fromUserId)
    : [];

  const userOptions = allUsers.map(u => ({ value: u.id, label: `${u.username} (${u.email})` }));
  const noteOptions = fromUserNotes.map(n => ({ value: n.id, label: n.title }));
  const toUserOptions = allUsers
    .filter(u => u.id !== fromUserId)
    .map(u => ({ value: u.id, label: `${u.username} (${u.email})` }));

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    if (!fromUserId || !selectedNoteId || !toUserId) {
      setError('Please select a source user, note, and target user.');
      return;
    }
    const note = allNotes.find(n => n.id === selectedNoteId);
    const toUser = allUsers.find(u => u.id === toUserId);
    if (!note || !toUser) {
      setError('Invalid selection.');
      return;
    }
    setLoading(true);
    setTimeout(() => {
      updateNote({ ...note, userId: toUserId });
      addAuditLog(
        currentUser!.id,
        'NOTE_REASSIGNED',
        `Admin reassigned note "${note.title}" to user ${toUser.username}`
      );
      setLoading(false);
      setSuccess(`Note "${note.title}" has been reassigned to ${toUser.username}.`);
      setSelectedNoteId('');
    }, 300);
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="flex items-center gap-3">
        <Link to="/admin/users" className="text-gray-400 hover:text-gray-600">
          <ChevronLeft size={20} />
        </Link>
        <h1 className="text-2xl font-bold text-gray-900">Reassign Note Ownership</h1>
      </div>

      <Card>
        <form onSubmit={handleSubmit} className="space-y-5">
          {error && <Alert type="error" message={error} onClose={() => setError('')} />}
          {success && <Alert type="success" message={success} onClose={() => setSuccess('')} />}

          <Select
            label="Source User (current owner)"
            value={fromUserId}
            onChange={e => { setFromUserId(e.target.value); setSelectedNoteId(''); }}
            options={[{ value: '', label: '— Select a user —' }, ...userOptions]}
          />

          {fromUserId && (
            <Select
              label="Note to Reassign"
              value={selectedNoteId}
              onChange={e => setSelectedNoteId(e.target.value)}
              options={[
                { value: '', label: noteOptions.length ? '— Select a note —' : 'No notes for this user' },
                ...noteOptions,
              ]}
            />
          )}

          {selectedNoteId && (
            <>
              <div className="flex items-center justify-center py-2">
                <ArrowRight size={20} className="text-gray-400" />
              </div>

              <Select
                label="Transfer to User"
                value={toUserId}
                onChange={e => setToUserId(e.target.value)}
                options={[
                  { value: '', label: '— Select target user —' },
                  ...toUserOptions,
                ]}
              />
            </>
          )}

          <div className="flex gap-3 justify-end pt-2 border-t">
            <Link to="/admin/users">
              <Button variant="secondary" type="button">Cancel</Button>
            </Link>
            <Button type="submit" loading={loading} disabled={!fromUserId || !selectedNoteId || !toUserId}>
              Reassign Note
            </Button>
          </div>
        </form>
      </Card>

      {fromUserId && fromUserNotes.length > 0 && (
        <Card>
          <h2 className="font-semibold text-gray-900 mb-3">Notes owned by {allUsers.find(u => u.id === fromUserId)?.username}</h2>
          <ul className="space-y-2 text-sm text-gray-600">
            {fromUserNotes.map(n => (
              <li key={n.id} className="flex items-center justify-between py-1 border-b last:border-0">
                <span className="font-medium text-gray-900">{n.title}</span>
                <span className={`text-xs px-2 py-0.5 rounded ${n.visibility === 'public' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                  {n.visibility}
                </span>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  );
}
