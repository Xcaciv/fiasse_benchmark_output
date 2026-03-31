import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { BookOpen, Share2, Star, Search, Shield, Users } from 'lucide-react';

export function HomePage() {
  const { currentUser } = useAuth();

  return (
    <div className="space-y-16">
      {/* Hero */}
      <div className="text-center py-16">
        <div className="flex justify-center mb-6">
          <div className="bg-indigo-600 rounded-2xl p-4">
            <BookOpen size={48} className="text-white" />
          </div>
        </div>
        <h1 className="text-4xl font-bold text-gray-900 mb-4">Welcome to Loose Notes</h1>
        <p className="text-xl text-gray-600 max-w-2xl mx-auto mb-8">
          A multi-user note-taking platform. Create, manage, share, and rate notes with ease.
        </p>
        {currentUser ? (
          <div className="flex gap-4 justify-center">
            <Link
              to="/notes"
              className="bg-indigo-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-indigo-700 transition-colors"
            >
              My Notes
            </Link>
            <Link
              to="/notes/create"
              className="bg-white text-indigo-600 border border-indigo-600 px-6 py-3 rounded-lg font-medium hover:bg-indigo-50 transition-colors"
            >
              Create Note
            </Link>
          </div>
        ) : (
          <div className="flex gap-4 justify-center">
            <Link
              to="/register"
              className="bg-indigo-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-indigo-700 transition-colors"
            >
              Get Started
            </Link>
            <Link
              to="/login"
              className="bg-white text-indigo-600 border border-indigo-600 px-6 py-3 rounded-lg font-medium hover:bg-indigo-50 transition-colors"
            >
              Sign In
            </Link>
          </div>
        )}
      </div>

      {/* Features */}
      <div>
        <h2 className="text-2xl font-bold text-gray-900 text-center mb-8">Everything you need to manage your notes</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[
            { icon: BookOpen, title: 'Rich Notes', desc: 'Create notes with titles and rich content. Attach files like PDFs, images, and documents.', color: 'bg-blue-100 text-blue-600' },
            { icon: Shield, title: 'Public & Private', desc: 'Control who sees your notes. Keep them private or make them public for everyone to discover.', color: 'bg-green-100 text-green-600' },
            { icon: Share2, title: 'Share Links', desc: 'Generate unique share links for any note. Anyone with the link can view your note.', color: 'bg-purple-100 text-purple-600' },
            { icon: Star, title: 'Ratings & Reviews', desc: 'Rate and comment on notes. See top-rated public notes from the community.', color: 'bg-yellow-100 text-yellow-600' },
            { icon: Search, title: 'Powerful Search', desc: 'Search through all public notes and your own private notes by title or content.', color: 'bg-red-100 text-red-600' },
            { icon: Users, title: 'Admin Controls', desc: 'Administrators can manage users, reassign notes, and view activity logs.', color: 'bg-indigo-100 text-indigo-600' },
          ].map(({ icon: Icon, title, desc, color }) => (
            <div key={title} className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm">
              <div className={`inline-flex rounded-lg p-3 mb-4 ${color}`}>
                <Icon size={24} />
              </div>
              <h3 className="font-semibold text-gray-900 mb-2">{title}</h3>
              <p className="text-sm text-gray-600">{desc}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Demo credentials */}
      {!currentUser && (
        <div className="bg-indigo-50 border border-indigo-200 rounded-lg p-6 text-center">
          <h3 className="font-semibold text-indigo-900 mb-2">Try the demo</h3>
          <p className="text-sm text-indigo-700 mb-3">Use these credentials to explore the app:</p>
          <div className="flex flex-wrap gap-4 justify-center text-sm">
            <div className="bg-white rounded px-4 py-2 border border-indigo-200">
              <span className="font-medium">Regular user:</span> alice / User123!
            </div>
            <div className="bg-white rounded px-4 py-2 border border-indigo-200">
              <span className="font-medium">Admin:</span> admin / Admin123!
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
