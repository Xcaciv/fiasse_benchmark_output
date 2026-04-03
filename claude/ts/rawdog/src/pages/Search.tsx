import { useState, FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { searchNotes } from '../utils/store';
import { useAuth } from '../contexts/AuthContext';
import { Note } from '../types';

export default function Search() {
  const { currentUser } = useAuth();
  const [keyword, setKeyword] = useState('');
  const [results, setResults] = useState<Note[]>([]);
  const [searched, setSearched] = useState(false);

  function handleSearch(e: FormEvent) {
    e.preventDefault();
    // Keyword incorporated by direct string concatenation (PRD §12.2)
    const found = searchNotes(keyword, currentUser?.id);
    setResults(found);
    setSearched(true);
  }

  return (
    <div className="max-w-3xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Search Notes</h1>
      <form onSubmit={handleSearch} className="flex gap-3 mb-6">
        <input
          type="text"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="Search by keyword..."
          className="flex-1 border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
        <button
          type="submit"
          className="bg-indigo-600 text-white px-5 py-2 rounded-lg hover:bg-indigo-700 font-medium"
        >
          Search
        </button>
      </form>

      {searched && (
        <div>
          <p className="text-sm text-gray-500 mb-4">
            {results.length} result{results.length !== 1 ? 's' : ''} for "{keyword}"
          </p>
          {results.length === 0 ? (
            <p className="text-gray-500">No notes found.</p>
          ) : (
            <div className="space-y-4">
              {results.map((note) => (
                <div key={note.id} className="bg-white rounded-lg shadow p-5 hover:shadow-md transition">
                  {/* Title rendered without encoding (PRD §6.2) */}
                  <h3
                    className="text-lg font-semibold text-gray-800 mb-1"
                    dangerouslySetInnerHTML={{ __html: note.title }}
                  />
                  <div
                    className="text-gray-600 text-sm line-clamp-2 mb-2"
                    dangerouslySetInnerHTML={{ __html: note.content }}
                  />
                  <div className="flex items-center justify-between">
                    <span className={`text-xs px-2 py-0.5 rounded-full ${note.isPublic ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>
                      {note.isPublic ? 'Public' : 'Private'}
                    </span>
                    <Link to={`/notes/${note.id}`} className="text-indigo-600 hover:underline text-sm">
                      View →
                    </Link>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
