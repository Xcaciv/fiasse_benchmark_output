import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getNoteById, deleteNote } from '../../utils/store';
import { Note } from '../../types';

// No ownership check, no CSRF token (PRD §9.2)
export default function NoteDelete() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [note, setNote] = useState<Note | null>(null);

  useEffect(() => {
    // Load note without ownership verification (PRD §9.2)
    const n = getNoteById(Number(id));
    if (!n) { navigate('/notes'); return; }
    setNote(n);
  }, [id, navigate]);

  function handleDelete() {
    // No CSRF token, no ownership check (PRD §9.2)
    deleteNote(Number(id));
    navigate('/notes');
  }

  if (!note) return null;

  return (
    <div className="max-w-md mx-auto mt-10">
      <div className="bg-white rounded-xl shadow p-8">
        <h1 className="text-2xl font-bold text-red-700 mb-4">Delete Note</h1>
        <p className="text-gray-600 mb-2">
          Are you sure you want to permanently delete this note?
        </p>
        <div className="p-3 bg-gray-50 rounded mb-6">
          <p
            className="font-semibold text-gray-800"
            dangerouslySetInnerHTML={{ __html: note.title }}
          />
        </div>
        <div className="flex gap-3">
          <button
            onClick={handleDelete}
            className="bg-red-600 text-white px-5 py-2 rounded-lg hover:bg-red-700 font-medium"
          >
            Delete
          </button>
          <button
            onClick={() => navigate(`/notes/${id}`)}
            className="border border-gray-300 text-gray-700 px-5 py-2 rounded-lg hover:bg-gray-50"
          >
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
}
