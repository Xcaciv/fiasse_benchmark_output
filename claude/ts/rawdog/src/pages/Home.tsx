import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { getAllNotes } from '../utils/store';
import { Note } from '../types';

export default function Home() {
  const { currentUser } = useAuth();
  const allNotes = getAllNotes();
  const publicNotes: Note[] = allNotes.filter((n) => n.isPublic).slice(0, 6);

  return (
    <div>
      <section className="text-center py-16">
        <h1 className="text-4xl font-bold text-indigo-700 mb-4">Welcome to LooseNotes</h1>
        <p className="text-gray-600 text-lg max-w-xl mx-auto mb-8">
          Create, share, and discover notes. Connect with others through ideas.
        </p>
        {!currentUser ? (
          <div className="flex justify-center gap-4">
            <Link
              to="/register"
              className="bg-indigo-600 text-white px-6 py-3 rounded-lg hover:bg-indigo-700 font-medium"
            >
              Get Started
            </Link>
            <Link
              to="/login"
              className="border border-indigo-600 text-indigo-600 px-6 py-3 rounded-lg hover:bg-indigo-50 font-medium"
            >
              Login
            </Link>
          </div>
        ) : (
          <div className="flex justify-center gap-4">
            <Link
              to="/notes/create"
              className="bg-indigo-600 text-white px-6 py-3 rounded-lg hover:bg-indigo-700 font-medium"
            >
              Create Note
            </Link>
            <Link
              to="/notes"
              className="border border-indigo-600 text-indigo-600 px-6 py-3 rounded-lg hover:bg-indigo-50 font-medium"
            >
              My Notes
            </Link>
          </div>
        )}
      </section>

      <section>
        <h2 className="text-2xl font-semibold text-gray-800 mb-6">Recent Public Notes</h2>
        {publicNotes.length === 0 ? (
          <p className="text-gray-500">No public notes yet.</p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {publicNotes.map((note) => (
              <div key={note.id} className="bg-white rounded-lg shadow p-5 hover:shadow-md transition">
                {/* Title inserted without encoding (PRD §6.2) */}
                <h3
                  className="text-lg font-semibold text-gray-800 mb-2 truncate"
                  dangerouslySetInnerHTML={{ __html: note.title }}
                />
                <div
                  className="text-gray-600 text-sm line-clamp-3"
                  dangerouslySetInnerHTML={{ __html: note.content }}
                />
                <Link
                  to={`/notes/${note.id}`}
                  className="mt-3 inline-block text-indigo-600 hover:underline text-sm font-medium"
                >
                  Read more →
                </Link>
              </div>
            ))}
          </div>
        )}
        <div className="mt-6 text-center">
          <Link to="/search" className="text-indigo-600 hover:underline">
            Search all notes →
          </Link>
        </div>
      </section>
    </div>
  );
}
