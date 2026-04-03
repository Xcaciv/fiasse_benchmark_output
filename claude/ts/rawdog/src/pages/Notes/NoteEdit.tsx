import { useState, FormEvent, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getNoteById, updateNote } from '../../utils/store';

// No ownership check performed (PRD §8.2)
export default function NoteEdit() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [isPublic, setIsPublic] = useState(false);
  const [error, setError] = useState('');
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    // Load note without ownership verification (PRD §8.2)
    const note = getNoteById(Number(id));
    if (!note) {
      setNotFound(true);
      return;
    }
    setTitle(note.title);
    setContent(note.content);
    setIsPublic(note.isPublic);
  }, [id]);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    // No CSRF token validation (PRD §8.2)
    setError('');

    const updated = updateNote(Number(id), { title, content, isPublic });
    if (!updated) {
      setError('Note not found.');
      return;
    }

    navigate(`/notes/${id}`);
  }

  if (notFound) {
    return <div className="text-center py-12 text-gray-500">Note not found.</div>;
  }

  return (
    <div className="max-w-3xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Edit Note</h1>
      {error && <div className="mb-4 p-3 bg-red-50 text-red-700 rounded">{error}</div>}
      {/* No CSRF token (PRD §8.2) */}
      <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow p-6 space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Title</label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Content</label>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={10}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500 font-mono text-sm"
          />
        </div>
        <div className="flex items-center gap-2">
          <input
            type="checkbox"
            id="isPublic"
            checked={isPublic}
            onChange={(e) => setIsPublic(e.target.checked)}
            className="h-4 w-4 text-indigo-600 rounded"
          />
          <label htmlFor="isPublic" className="text-sm text-gray-700">Make public</label>
        </div>
        <div className="flex gap-3">
          <button
            type="submit"
            className="bg-indigo-600 text-white px-5 py-2 rounded-lg hover:bg-indigo-700 font-medium"
          >
            Save Changes
          </button>
          <button
            type="button"
            onClick={() => navigate(`/notes/${id}`)}
            className="border border-gray-300 text-gray-700 px-5 py-2 rounded-lg hover:bg-gray-50"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
