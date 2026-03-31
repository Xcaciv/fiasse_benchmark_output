import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getNotes, getUserById, getAverageRating, getRatingsByNoteId } from '../../utils/storage';
import { Card } from '../../components/ui/Card';
import { Input } from '../../components/ui/Input';
import { Badge } from '../../components/ui/Badge';
import { AverageStars } from '../../components/ui/StarRating';
import { formatDate, truncate } from '../../utils/helpers';
import { Search, Globe, Lock } from 'lucide-react';

export function SearchPage() {
  const { currentUser } = useAuth();
  const [query, setQuery] = useState('');
  const [searched, setSearched] = useState(false);

  function getResults() {
    if (!query.trim()) return [];
    const q = query.toLowerCase();
    return getNotes().filter(note => {
      const matchesQuery = note.title.toLowerCase().includes(q) || note.content.toLowerCase().includes(q);
      if (!matchesQuery) return false;
      // Include if: owned by current user, OR public
      if (note.userId === currentUser?.id) return true;
      if (note.visibility === 'public') return true;
      return false;
    }).sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  }

  const results = searched ? getResults() : [];

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSearched(true);
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Search Notes</h1>

      <Card>
        <form onSubmit={handleSubmit} className="flex gap-3">
          <div className="flex-1">
            <Input
              value={query}
              onChange={e => { setQuery(e.target.value); if (!e.target.value) setSearched(false); }}
              placeholder="Search by title or content..."
              autoFocus
            />
          </div>
          <button
            type="submit"
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 transition-colors text-sm font-medium"
          >
            <Search size={16} /> Search
          </button>
        </form>
      </Card>

      {searched && (
        <div>
          <p className="text-sm text-gray-500 mb-4">
            {results.length === 0
              ? `No results for "${query}"`
              : `${results.length} result${results.length !== 1 ? 's' : ''} for "${query}"`}
          </p>

          {results.length > 0 && (
            <div className="space-y-4">
              {results.map(note => {
                const author = getUserById(note.userId);
                const ratings = getRatingsByNoteId(note.id);
                const avg = getAverageRating(note.id);
                const isOwn = note.userId === currentUser?.id;

                return (
                  <Card key={note.id} className="hover:shadow-md transition-shadow">
                    <div className="space-y-2">
                      <div className="flex items-start justify-between gap-4">
                        <Link
                          to={`/notes/${note.id}`}
                          className="text-lg font-semibold text-gray-900 hover:text-indigo-600"
                        >
                          {note.title}
                        </Link>
                        <div className="flex gap-2 flex-shrink-0">
                          {isOwn && (
                            <Badge color="indigo">Mine</Badge>
                          )}
                          <Badge color={note.visibility === 'public' ? 'green' : 'gray'}>
                            {note.visibility === 'public'
                              ? <><Globe size={10} className="inline mr-1" />Public</>
                              : <><Lock size={10} className="inline mr-1" />Private</>}
                          </Badge>
                        </div>
                      </div>
                      <p className="text-sm text-gray-600">{truncate(note.content, 200)}</p>
                      <div className="flex items-center gap-4 flex-wrap text-xs text-gray-500">
                        <span>By <strong className="text-gray-700">{author?.username || 'Unknown'}</strong></span>
                        <span>{formatDate(note.createdAt)}</span>
                        {ratings.length > 0 && <AverageStars value={avg} count={ratings.length} size={12} />}
                      </div>
                    </div>
                  </Card>
                );
              })}
            </div>
          )}
        </div>
      )}

      {!searched && (
        <div className="text-center py-12 text-gray-400">
          <Search size={48} className="mx-auto mb-4" />
          <p className="text-sm">Search across your notes and all public notes</p>
        </div>
      )}
    </div>
  );
}
