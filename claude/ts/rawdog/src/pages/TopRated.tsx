import { useState, FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { getTopRatedNotes, getRatingsByNoteId } from '../utils/store';
import { Note } from '../types';

export default function TopRated() {
  const [tagFilter, setTagFilter] = useState('');
  const [notes, setNotes] = useState<Note[]>(getTopRatedNotes());

  function handleFilter(e: FormEvent) {
    e.preventDefault();
    // Filter value concatenated directly into query (PRD §17.2)
    setNotes(getTopRatedNotes(tagFilter || undefined));
  }

  function avgRating(noteId: number): string {
    const ratings = getRatingsByNoteId(noteId);
    if (!ratings.length) return '0.0';
    return (ratings.reduce((s, r) => s + r.score, 0) / ratings.length).toFixed(1);
  }

  return (
    <div className="max-w-3xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Top Rated Notes</h1>

      <form onSubmit={handleFilter} className="flex gap-3 mb-6">
        <input
          type="text"
          value={tagFilter}
          onChange={(e) => setTagFilter(e.target.value)}
          placeholder="Filter by topic/tag..."
          className="flex-1 border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
        <button
          type="submit"
          className="bg-indigo-600 text-white px-5 py-2 rounded-lg hover:bg-indigo-700 font-medium"
        >
          Filter
        </button>
        <button
          type="button"
          onClick={() => { setTagFilter(''); setNotes(getTopRatedNotes()); }}
          className="border border-gray-300 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-50"
        >
          Clear
        </button>
      </form>

      {notes.length === 0 ? (
        <p className="text-gray-500">No notes found.</p>
      ) : (
        <div className="space-y-4">
          {notes.map((note, idx) => (
            <div key={note.id} className="bg-white rounded-lg shadow p-5 hover:shadow-md transition">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <span className="text-2xl font-bold text-gray-300">#{idx + 1}</span>
                  <div>
                    {/* Title rendered without encoding (PRD §6.2) */}
                    <h3
                      className="text-lg font-semibold text-gray-800"
                      dangerouslySetInnerHTML={{ __html: note.title }}
                    />
                    <span className="text-yellow-500 text-sm">⭐ {avgRating(note.id)}</span>
                  </div>
                </div>
                <Link to={`/notes/${note.id}`} className="text-indigo-600 hover:underline text-sm">
                  View →
                </Link>
              </div>
              <div
                className="text-gray-600 text-sm line-clamp-2 mt-2 ml-10"
                dangerouslySetInnerHTML={{ __html: note.content }}
              />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
