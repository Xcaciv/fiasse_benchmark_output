import { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import {
  getNoteById, updateNote, createAttachment,
  getAttachmentsByNoteId, deleteAttachment, addAuditLog,
} from '../../utils/storage';
import { Card } from '../../components/ui/Card';
import { Input } from '../../components/ui/Input';
import { Textarea } from '../../components/ui/Textarea';
import { Button } from '../../components/ui/Button';
import { Alert } from '../../components/ui/Alert';
import { Select } from '../../components/ui/Select';
import { isAllowedFile, formatFileSize, getFileIcon } from '../../utils/helpers';
import { Paperclip, X, ChevronLeft } from 'lucide-react';

interface PendingFile { file: File; id: string; }

export function NoteEditPage() {
  const { id } = useParams<{ id: string }>();
  const { currentUser } = useAuth();
  const navigate = useNavigate();

  const note = id ? getNoteById(id) : undefined;

  const [form, setForm] = useState({
    title: note?.title || '',
    content: note?.content || '',
    visibility: note?.visibility || 'private' as 'public' | 'private',
  });
  const [errors, setErrors] = useState<{ title?: string; content?: string }>({});
  const [error, setError] = useState('');
  const [pendingFiles, setPendingFiles] = useState<PendingFile[]>([]);
  const [loading, setLoading] = useState(false);

  if (!note) return (
    <div className="text-center py-16">
      <h2 className="text-xl font-semibold">Note not found</h2>
      <Link to="/notes" className="text-indigo-600 hover:underline">Back to notes</Link>
    </div>
  );

  const isOwner = currentUser?.id === note.userId;
  const isAdmin = currentUser?.role === 'admin';
  if (!isOwner && !isAdmin) return (
    <div className="text-center py-16">
      <h2 className="text-xl font-semibold">Access denied</h2>
      <Link to="/notes" className="text-indigo-600 hover:underline">Back to notes</Link>
    </div>
  );

  const existingAttachments = getAttachmentsByNoteId(note.id);

  function addFile(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files || []);
    const valid: PendingFile[] = [];
    for (const file of files) {
      if (!isAllowedFile(file)) { setError(`File "${file.name}" type not allowed.`); continue; }
      if (file.size > 10 * 1024 * 1024) { setError(`File "${file.name}" exceeds 10MB.`); continue; }
      valid.push({ file, id: crypto.randomUUID() });
    }
    setPendingFiles(f => [...f, ...valid]);
    e.target.value = '';
  }

  function validate() {
    const errs: typeof errors = {};
    if (!form.title.trim()) errs.title = 'Title is required.';
    if (!form.content.trim()) errs.content = 'Content is required.';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    if (!validate()) return;
    setLoading(true);
    setTimeout(() => {
      updateNote({ ...note, title: form.title.trim(), content: form.content.trim(), visibility: form.visibility });
      for (const { file } of pendingFiles) {
        createAttachment({ noteId: note.id, filename: `${crypto.randomUUID()}_${file.name}`, originalFilename: file.name, fileType: file.type, size: file.size });
      }
      addAuditLog(currentUser!.id, 'NOTE_UPDATED', `Updated note: ${form.title}`);
      setLoading(false);
      navigate(`/notes/${note.id}`);
    }, 300);
  }

  function handleDeleteExistingAttachment(attId: string) {
    deleteAttachment(attId);
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="flex items-center gap-3">
        <Link to={`/notes/${note.id}`} className="text-gray-400 hover:text-gray-600">
          <ChevronLeft size={20} />
        </Link>
        <h1 className="text-2xl font-bold text-gray-900">Edit Note</h1>
      </div>

      <Card>
        <form onSubmit={handleSubmit} className="space-y-5">
          {error && <Alert type="error" message={error} onClose={() => setError('')} />}

          <Input
            label="Title *"
            value={form.title}
            onChange={e => setForm(f => ({ ...f, title: e.target.value }))}
            error={errors.title}
            autoFocus
          />

          <Textarea
            label="Content *"
            value={form.content}
            onChange={e => setForm(f => ({ ...f, content: e.target.value }))}
            rows={12}
            error={errors.content}
          />

          <Select
            label="Visibility"
            value={form.visibility}
            onChange={e => setForm(f => ({ ...f, visibility: e.target.value as 'public' | 'private' }))}
            options={[
              { value: 'private', label: 'Private — only visible to you' },
              { value: 'public', label: 'Public — visible to everyone' },
            ]}
          />

          {/* Existing attachments */}
          {existingAttachments.length > 0 && (
            <div>
              <p className="text-sm font-medium text-gray-700 mb-2">Current Attachments</p>
              <ul className="space-y-2">
                {existingAttachments.map(att => (
                  <li key={att.id} className="flex items-center gap-3 p-2 bg-gray-50 rounded text-sm">
                    <span>{getFileIcon(att.fileType)}</span>
                    <span className="flex-1 truncate">{att.originalFilename}</span>
                    <span className="text-gray-400 text-xs">{formatFileSize(att.size)}</span>
                    <button type="button" onClick={() => handleDeleteExistingAttachment(att.id)} className="text-red-400 hover:text-red-600">
                      <X size={14} />
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* Add new attachments */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Add Attachments</label>
            <label className="flex items-center gap-2 px-4 py-2 border border-dashed border-gray-300 rounded-md cursor-pointer hover:bg-gray-50 text-sm text-gray-600 w-fit">
              <Paperclip size={14} />
              Attach files
              <input type="file" multiple className="hidden" onChange={addFile} accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg" />
            </label>
            {pendingFiles.length > 0 && (
              <ul className="mt-2 space-y-2">
                {pendingFiles.map(({ file, id }) => (
                  <li key={id} className="flex items-center gap-2 text-sm bg-blue-50 rounded px-3 py-2">
                    <Paperclip size={12} className="text-blue-400" />
                    <span className="flex-1 truncate">{file.name}</span>
                    <span className="text-gray-400 text-xs">{formatFileSize(file.size)}</span>
                    <button type="button" onClick={() => setPendingFiles(f => f.filter(pf => pf.id !== id))} className="text-red-400 hover:text-red-600">
                      <X size={14} />
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="flex gap-3 justify-end pt-2 border-t">
            <Link to={`/notes/${note.id}`}>
              <Button variant="secondary" type="button">Cancel</Button>
            </Link>
            <Button type="submit" loading={loading}>Save Changes</Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
