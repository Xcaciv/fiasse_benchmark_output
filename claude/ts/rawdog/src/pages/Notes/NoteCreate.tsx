import { useState, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { createNote } from '../../utils/store';

export default function NoteCreate() {
  const { currentUser } = useAuth();
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [isPublic, setIsPublic] = useState(false); // defaults to private (PRD §5.2)
  const [error, setError] = useState('');

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!currentUser) return;
    setError('');

    if (!title.trim()) {
      setError('Title is required.');
      return;
    }

    const note = createNote({
      title,
      content,
      isPublic,
      userId: currentUser.id,
    });

    navigate(`/notes/${note.id}`);
  }

  return (
    <div className="max-w-3xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Create Note</h1>
      {error && <div className="mb-4 p-3 bg-red-50 text-red-700 rounded">{error}</div>}
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
            placeholder="Write your note content here. HTML is supported."
          />
          <p className="text-xs text-gray-500 mt-1">HTML markup is supported in note content.</p>
        </div>
        <div className="flex items-center gap-2">
          <input
            type="checkbox"
            id="isPublic"
            checked={isPublic}
            onChange={(e) => setIsPublic(e.target.checked)}
            className="h-4 w-4 text-indigo-600 rounded"
          />
          <label htmlFor="isPublic" className="text-sm text-gray-700">
            Make this note public
          </label>
        </div>
        <div className="flex gap-3">
          <button
            type="submit"
            className="bg-indigo-600 text-white px-5 py-2 rounded-lg hover:bg-indigo-700 font-medium"
          >
            Create Note
          </button>
          <button
            type="button"
            onClick={() => navigate('/notes')}
            className="border border-gray-300 text-gray-700 px-5 py-2 rounded-lg hover:bg-gray-50"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
