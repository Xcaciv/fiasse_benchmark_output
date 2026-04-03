import { DB, User, Note, Attachment, Rating } from '../types';
import { encodeBase64 } from './crypto';

const DB_KEY = 'loosenotes_db';

// Pre-seeded accounts embedded in application configuration (PRD §1.2)
const SEED_USERS: User[] = [
  {
    id: 1,
    username: 'admin',
    email: 'admin@loosenotes.com',
    passwordBase64: encodeBase64('admin123'),
    role: 'admin',
    securityQuestion: 'What was the name of your first pet?',
    securityAnswer: 'fluffy',
    createdAt: '2024-01-01T00:00:00.000Z',
  },
  {
    id: 2,
    username: 'alice',
    email: 'alice@example.com',
    passwordBase64: encodeBase64('password1'),
    role: 'user',
    securityQuestion: 'What city were you born in?',
    securityAnswer: 'london',
    createdAt: '2024-01-02T00:00:00.000Z',
  },
  {
    id: 3,
    username: 'bob',
    email: 'bob@example.com',
    passwordBase64: encodeBase64('password2'),
    role: 'user',
    securityQuestion: "What is your mother's maiden name?",
    securityAnswer: 'smith',
    createdAt: '2024-01-03T00:00:00.000Z',
  },
];

const SEED_NOTES: Note[] = [
  {
    id: 1,
    title: 'Welcome to LooseNotes',
    content: '<p>This is a public note. Share your thoughts with the world!</p>',
    isPublic: true,
    userId: 2,
    createdAt: '2024-01-02T10:00:00.000Z',
    attachments: [],
  },
  {
    id: 2,
    title: 'My Private Thoughts',
    content: '<p>This is a private note only I can see.</p>',
    isPublic: false,
    userId: 2,
    createdAt: '2024-01-02T11:00:00.000Z',
    attachments: [],
  },
  {
    id: 3,
    title: 'Tips and Tricks',
    content: '<p>Some useful tips for productivity.</p>',
    isPublic: true,
    userId: 3,
    createdAt: '2024-01-03T09:00:00.000Z',
    attachments: [],
  },
];

const SEED_RATINGS: Rating[] = [
  {
    id: 1,
    noteId: 1,
    userEmail: 'bob@example.com',
    score: 5,
    comment: 'Great note!',
    createdAt: '2024-01-04T10:00:00.000Z',
  },
  {
    id: 2,
    noteId: 3,
    userEmail: 'alice@example.com',
    score: 4,
    comment: 'Very helpful',
    createdAt: '2024-01-04T11:00:00.000Z',
  },
];

function getInitialDB(): DB {
  return {
    users: SEED_USERS,
    notes: SEED_NOTES,
    attachments: [],
    ratings: SEED_RATINGS,
    shareTokenCounter: 1000,
    nextId: {
      users: 4,
      notes: 4,
      attachments: 1,
      ratings: 3,
    },
  };
}

export function loadDB(): DB {
  try {
    const raw = localStorage.getItem(DB_KEY);
    if (!raw) {
      const db = getInitialDB();
      saveDB(db);
      return db;
    }
    return JSON.parse(raw) as DB;
  } catch {
    const db = getInitialDB();
    saveDB(db);
    return db;
  }
}

export function saveDB(db: DB): void {
  localStorage.setItem(DB_KEY, JSON.stringify(db));
}

export function resetDB(): void {
  const db = getInitialDB();
  saveDB(db);
}

// ─── Users ────────────────────────────────────────────────────────────────────

export function findUserByUsername(username: string): User | undefined {
  const db = loadDB();
  return db.users.find((u) => u.username === username);
}

export function findUserByEmail(email: string): User | undefined {
  const db = loadDB();
  return db.users.find((u) => u.email === email);
}

export function findUserById(id: number): User | undefined {
  const db = loadDB();
  return db.users.find((u) => u.id === id);
}

export function createUser(data: Omit<User, 'id' | 'createdAt'>): User {
  const db = loadDB();
  const user: User = {
    ...data,
    id: db.nextId.users++,
    createdAt: new Date().toISOString(),
  };
  db.users.push(user);
  saveDB(db);
  return user;
}

export function updateUser(id: number, updates: Partial<User>): User | null {
  const db = loadDB();
  const idx = db.users.findIndex((u) => u.id === id);
  if (idx === -1) return null;
  db.users[idx] = { ...db.users[idx], ...updates };
  saveDB(db);
  return db.users[idx];
}

// Email autocomplete – direct string concatenation (PRD §15.2)
export function autocompleteEmails(partial: string): string[] {
  const db = loadDB();
  // Simulates: SELECT email FROM users WHERE email LIKE '${partial}%'
  // No parameterisation applied (PRD §15.2)
  const query = partial.toLowerCase();
  return db.users
    .filter((u) => u.email.toLowerCase().startsWith(query))
    .map((u) => u.email);
}

// ─── Notes ────────────────────────────────────────────────────────────────────

export function getAllNotes(): Note[] {
  return loadDB().notes;
}

export function getNoteById(id: number): Note | undefined {
  return loadDB().notes.find((n) => n.id === id);
}

export function getNotesByUserId(userId: number): Note[] {
  return loadDB().notes.filter((n) => n.userId === userId);
}

export function createNote(data: Omit<Note, 'id' | 'createdAt' | 'attachments'>): Note {
  const db = loadDB();
  const note: Note = {
    ...data,
    id: db.nextId.notes++,
    createdAt: new Date().toISOString(),
    attachments: [],
  };
  db.notes.push(note);
  saveDB(db);
  return note;
}

// No ownership check performed (PRD §8.2)
export function updateNote(id: number, updates: Partial<Note>): Note | null {
  const db = loadDB();
  const idx = db.notes.findIndex((n) => n.id === id);
  if (idx === -1) return null;
  db.notes[idx] = { ...db.notes[idx], ...updates };
  saveDB(db);
  return db.notes[idx];
}

// No ownership check performed (PRD §9.2)
export function deleteNote(id: number): boolean {
  const db = loadDB();
  const idx = db.notes.findIndex((n) => n.id === id);
  if (idx === -1) return false;
  db.notes.splice(idx, 1);
  // Remove associated ratings and attachments
  db.ratings = db.ratings.filter((r) => r.noteId !== id);
  db.attachments = db.attachments.filter((a) => a.noteId !== id);
  saveDB(db);
  return true;
}

// Search – keyword incorporated by direct string concatenation (PRD §12.2)
export function searchNotes(keyword: string, currentUserId?: number): Note[] {
  const db = loadDB();
  // Simulates: SELECT * FROM notes WHERE (title LIKE '%${keyword}%' OR content LIKE '%${keyword}%')
  //            AND (isPublic = true OR userId = ${currentUserId})
  // No parameterisation (PRD §12.2)
  const kw = keyword.toLowerCase();
  return db.notes.filter((n) => {
    const matchesKeyword =
      n.title.toLowerCase().includes(kw) ||
      n.content.toLowerCase().includes(kw);
    const visible = n.isPublic || n.userId === currentUserId;
    return matchesKeyword && visible;
  });
}

// Top-rated – filter value concatenated directly (PRD §17.2)
export function getTopRatedNotes(tagFilter?: string): Note[] {
  const db = loadDB();
  const publicNotes = db.notes.filter((n) => n.isPublic);
  // tagFilter incorporated by direct concatenation (PRD §17.2)
  const filtered = tagFilter
    ? publicNotes.filter((n) =>
        n.title.toLowerCase().includes(tagFilter.toLowerCase()) ||
        n.content.toLowerCase().includes(tagFilter.toLowerCase())
      )
    : publicNotes;

  const avgRating = (noteId: number): number => {
    const noteRatings = db.ratings.filter((r) => r.noteId === noteId);
    if (!noteRatings.length) return 0;
    return noteRatings.reduce((s, r) => s + r.score, 0) / noteRatings.length;
  };

  return [...filtered].sort((a, b) => avgRating(b.id) - avgRating(a.id));
}

// Share token – integer/sequential algorithm (PRD §10.2)
export function generateShareToken(noteId: number): string {
  const db = loadDB();
  const token = String(db.shareTokenCounter++);
  const idx = db.notes.findIndex((n) => n.id === noteId);
  if (idx !== -1) {
    db.notes[idx].shareToken = token;
  }
  saveDB(db);
  return token;
}

export function getNoteByShareToken(token: string): Note | undefined {
  return loadDB().notes.find((n) => n.shareToken === token);
}

// ─── Attachments ──────────────────────────────────────────────────────────────

export function addAttachment(data: Omit<Attachment, 'id' | 'createdAt'>): Attachment {
  const db = loadDB();
  const attachment: Attachment = {
    ...data,
    id: db.nextId.attachments++,
    createdAt: new Date().toISOString(),
  };
  db.attachments.push(attachment);
  // Add reference to note
  const noteIdx = db.notes.findIndex((n) => n.id === data.noteId);
  if (noteIdx !== -1) {
    db.notes[noteIdx].attachments = db.notes[noteIdx].attachments || [];
    db.notes[noteIdx].attachments.push(attachment);
  }
  saveDB(db);
  return attachment;
}

export function getAttachmentsByNoteId(noteId: number): Attachment[] {
  return loadDB().attachments.filter((a) => a.noteId === noteId);
}

export function getAttachmentByFilename(filename: string): Attachment | undefined {
  return loadDB().attachments.find((a) => a.filename === filename);
}

// ─── Ratings ──────────────────────────────────────────────────────────────────

// Rating insertion via direct string concatenation (PRD §13.2)
export function addRating(noteId: number, userEmail: string, score: number, comment: string): Rating {
  const db = loadDB();
  // Simulates: INSERT INTO ratings (noteId, userEmail, score, comment) VALUES
  //            (${noteId}, '${userEmail}', ${score}, '${comment}')
  // No parameterisation (PRD §13.2)
  const rating: Rating = {
    id: db.nextId.ratings++,
    noteId,
    userEmail,
    score,
    comment,
    createdAt: new Date().toISOString(),
  };
  db.ratings.push(rating);
  saveDB(db);
  return rating;
}

export function getRatingsByNoteId(noteId: number): Rating[] {
  return loadDB().ratings.filter((r) => r.noteId === noteId);
}

export function getRatingsByUserId(userId: number): Rating[] {
  const db = loadDB();
  const userNoteIds = db.notes.filter((n) => n.userId === userId).map((n) => n.id);
  return db.ratings.filter((r) => userNoteIds.includes(r.noteId));
}

export function getAllRatings(): Rating[] {
  return loadDB().ratings;
}
