import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { createNote, createAttachment, addAuditLog } from '../../utils/storage';
import { Card } from '../../components/ui/Card';
import { Input } from '../../components/ui/Input';
import { Textarea } from '../../components/ui/Textarea';
import { Button } from '../../components/ui/Button';
import { Alert } from '../../components/ui/Alert';
import { Select } from '../../components/ui/Select';
import { isAllowedFile, formatFileSize } from '../../utils/helpers';
import { Paperclip, X, ChevronLeft } from 'lucide-react';

interface PendingFile {
  file: File;
  id: string;
}

export function NoteCreatePage() {
  const { currentUser } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ title: '', content: '', visibility: 'private' as 'public' | 'private' });
  const [errors, setErrors] = useState<{ title?: string; content?: string }>({});
  const [error, setError] = useState('');
  const [pendingFiles, setPendingFiles] = useState<PendingFile[]>([]);
  const [loading, setLoading] = useState(false);

  if (!currentUser) return null;

  function addFile(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files || []);
    const valid: PendingFile[] = [];
    for (const file of files) {
      if (!isAllowedFile(file)) {
        setError(`File "${file.name}" is not allowed. Allowed: PDF, DOC, DOCX, TXT, PNG, JPG, JPEG`);
        continue;
      }
      if (file.size > 10 * 1024 * 1024) {
        setError(`File "${file.name}" exceeds 10MB limit.`);
        continue;
      }
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
      const note = createNote({
        title: form.title.trim(),
        content: form.content.trim(),
        userId: currentUser.id,
        visibility: form.visibility,
      });
      for (const { file } of pendingFiles) {
        createAttachment({
          noteId: note.id,
          filename: `${crypto.randomUUID()}_${file.name}`,
          originalFilename: file.name,
          fileType: file.type,
          size: file.size,
        });
      }
      addAuditLog(currentUser.id, 'NOTE_CREATED', `Created note: ${note.title}`);
      setLoading(false);
      navigate(`/notes/${note.id}`);
    }, 300);
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="flex items-center gap-3">
        <Link to="/notes" className="text-gray-400 hover:text-gray-600">
          <ChevronLeft size={20} />
        </Link>
        <h1 className="text-2xl font-bold text-gray-900">Create Note</h1>
      </div>

      <Card>
        <form onSubmit={handleSubmit} className="space-y-5">
          {error && <Alert type="error" message={error} onClose={() => setError('')} />}

          <Input
            label="Title *"
            value={form.title}
            onChange={e => setForm(f => ({ ...f, title: e.target.value }))}
            placeholder="Note title"
            error={errors.title}
            autoFocus
          />

          <Textarea
            label="Content *"
            value={form.content}
            onChange={e => setForm(f => ({ ...f, content: e.target.value }))}
            placeholder="Write your note here..."
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

          {/* File attachments */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Attachments</label>
            <label className="flex items-center gap-2 px-4 py-2 border border-dashed border-gray-300 rounded-md cursor-pointer hover:bg-gray-50 text-sm text-gray-600 w-fit">
              <Paperclip size={14} />
              Attach files
              <input type="file" multiple className="hidden" onChange={addFile} accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg" />
            </label>
            <p className="text-xs text-gray-500 mt-1">PDF, DOC, DOCX, TXT, PNG, JPG, JPEG — max 10MB each</p>

            {pendingFiles.length > 0 && (
              <ul className="mt-3 space-y-2">
                {pendingFiles.map(({ file, id }) => (
                  <li key={id} className="flex items-center gap-2 text-sm bg-gray-50 rounded px-3 py-2">
                    <Paperclip size={12} className="text-gray-400" />
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
            <Link to="/notes">
              <Button variant="secondary" type="button">Cancel</Button>
            </Link>
            <Button type="submit" loading={loading}>Create Note</Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
