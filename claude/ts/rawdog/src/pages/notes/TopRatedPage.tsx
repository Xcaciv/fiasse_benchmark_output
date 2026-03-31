import { Link } from 'react-router-dom';
import { getNotes, getUserById, getAverageRating, getRatingsByNoteId } from '../../utils/storage';
import { Card } from '../../components/ui/Card';
import { AverageStars } from '../../components/ui/StarRating';
import { formatDate, truncate } from '../../utils/helpers';
import { Trophy } from 'lucide-react';

export function TopRatedPage() {
  const notes = getNotes()
    .filter(n => n.visibility === 'public')
    .map(note => ({
      note,
      ratings: getRatingsByNoteId(note.id),
      avg: getAverageRating(note.id),
    }))
    .filter(({ ratings }) => ratings.length >= 3)
    .sort((a, b) => b.avg - a.avg || b.ratings.length - a.ratings.length);

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="flex items-center gap-3">
        <Trophy size={28} className="text-yellow-500" />
        <h1 className="text-2xl font-bold text-gray-900">Top Rated Notes</h1>
      </div>
      <p className="text-gray-600 text-sm">Public notes with 3+ ratings, ranked by average rating.</p>

      {notes.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <Trophy size={48} className="mx-auto mb-4" />
          <p>No notes have 3+ ratings yet. Be the first to rate some notes!</p>
        </div>
      ) : (
        <div className="space-y-4">
          {notes.map(({ note, ratings, avg }, index) => {
            const author = getUserById(note.userId);
            return (
              <Card key={note.id} className="hover:shadow-md transition-shadow">
                <div className="flex gap-4">
                  <div className="flex-shrink-0 w-8 text-center">
                    {index === 0 && <span className="text-2xl">🥇</span>}
                    {index === 1 && <span className="text-2xl">🥈</span>}
                    {index === 2 && <span className="text-2xl">🥉</span>}
                    {index >= 3 && <span className="text-lg font-bold text-gray-400">#{index + 1}</span>}
                  </div>
                  <div className="flex-1 min-w-0 space-y-2">
                    <Link
                      to={`/notes/${note.id}`}
                      className="text-lg font-semibold text-gray-900 hover:text-indigo-600 block"
                    >
                      {note.title}
                    </Link>
                    <AverageStars value={avg} count={ratings.length} />
                    <p className="text-sm text-gray-600">{truncate(note.content, 200)}</p>
                    <div className="flex items-center gap-4 text-xs text-gray-500">
                      <span>By <strong className="text-gray-700">{author?.username || 'Unknown'}</strong></span>
                      <span>{formatDate(note.createdAt)}</span>
                    </div>
                  </div>
                </div>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
