import type { User, Note, Attachment, Rating, ShareLink, AuditLog, ResetToken } from '../types';
import { hashPassword, generateId, generateToken } from './auth';

const KEYS = {
  USERS: 'ln_users',
  NOTES: 'ln_notes',
  ATTACHMENTS: 'ln_attachments',
  RATINGS: 'ln_ratings',
  SHARE_LINKS: 'ln_share_links',
  AUDIT_LOGS: 'ln_audit_logs',
  RESET_TOKENS: 'ln_reset_tokens',
  INITIALIZED: 'ln_initialized',
};

function load<T>(key: string): T[] {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function save<T>(key: string, items: T[]): void {
  localStorage.setItem(key, JSON.stringify(items));
}

// ---- Users ----
export function getUsers(): User[] { return load<User>(KEYS.USERS); }
export function saveUsers(users: User[]): void { save(KEYS.USERS, users); }
export function getUserById(id: string): User | undefined {
  return getUsers().find(u => u.id === id);
}
export function getUserByEmail(email: string): User | undefined {
  return getUsers().find(u => u.email.toLowerCase() === email.toLowerCase());
}
export function getUserByUsername(username: string): User | undefined {
  return getUsers().find(u => u.username.toLowerCase() === username.toLowerCase());
}
export function createUser(data: Omit<User, 'id' | 'createdAt'>): User {
  const user: User = { ...data, id: generateId(), createdAt: new Date().toISOString() };
  const users = getUsers();
  users.push(user);
  saveUsers(users);
  return user;
}
export function updateUser(updated: User): void {
  const users = getUsers().map(u => u.id === updated.id ? updated : u);
  saveUsers(users);
}

// ---- Notes ----
export function getNotes(): Note[] { return load<Note>(KEYS.NOTES); }
export function saveNotes(notes: Note[]): void { save(KEYS.NOTES, notes); }
export function getNoteById(id: string): Note | undefined {
  return getNotes().find(n => n.id === id);
}
export function createNote(data: Omit<Note, 'id' | 'createdAt' | 'updatedAt'>): Note {
  const now = new Date().toISOString();
  const note: Note = { ...data, id: generateId(), createdAt: now, updatedAt: now };
  const notes = getNotes();
  notes.push(note);
  saveNotes(notes);
  return note;
}
export function updateNote(updated: Note): void {
  const notes = getNotes().map(n => n.id === updated.id ? { ...updated, updatedAt: new Date().toISOString() } : n);
  saveNotes(notes);
}
export function deleteNote(id: string): void {
  saveNotes(getNotes().filter(n => n.id !== id));
  // Cascade delete attachments, ratings, share links
  saveAttachments(getAttachments().filter(a => a.noteId !== id));
  saveRatings(getRatings().filter(r => r.noteId !== id));
  saveShareLinks(getShareLinks().filter(s => s.noteId !== id));
}

// ---- Attachments ----
export function getAttachments(): Attachment[] { return load<Attachment>(KEYS.ATTACHMENTS); }
export function saveAttachments(items: Attachment[]): void { save(KEYS.ATTACHMENTS, items); }
export function getAttachmentsByNoteId(noteId: string): Attachment[] {
  return getAttachments().filter(a => a.noteId === noteId);
}
export function createAttachment(data: Omit<Attachment, 'id' | 'createdAt'>): Attachment {
  const att: Attachment = { ...data, id: generateId(), createdAt: new Date().toISOString() };
  const atts = getAttachments();
  atts.push(att);
  saveAttachments(atts);
  return att;
}
export function deleteAttachment(id: string): void {
  saveAttachments(getAttachments().filter(a => a.id !== id));
}

// ---- Ratings ----
export function getRatings(): Rating[] { return load<Rating>(KEYS.RATINGS); }
export function saveRatings(items: Rating[]): void { save(KEYS.RATINGS, items); }
export function getRatingsByNoteId(noteId: string): Rating[] {
  return getRatings().filter(r => r.noteId === noteId);
}
export function getRatingByUserAndNote(userId: string, noteId: string): Rating | undefined {
  return getRatings().find(r => r.userId === userId && r.noteId === noteId);
}
export function upsertRating(data: Omit<Rating, 'id' | 'createdAt' | 'updatedAt'>): Rating {
  const existing = getRatingByUserAndNote(data.userId, data.noteId);
  const now = new Date().toISOString();
  if (existing) {
    const updated: Rating = { ...existing, value: data.value, comment: data.comment, updatedAt: now };
    saveRatings(getRatings().map(r => r.id === updated.id ? updated : r));
    return updated;
  }
  const rating: Rating = { ...data, id: generateId(), createdAt: now, updatedAt: now };
  const ratings = getRatings();
  ratings.push(rating);
  saveRatings(ratings);
  return rating;
}
export function getAverageRating(noteId: string): number {
  const ratings = getRatingsByNoteId(noteId);
  if (ratings.length === 0) return 0;
  return ratings.reduce((sum, r) => sum + r.value, 0) / ratings.length;
}

// ---- Share Links ----
export function getShareLinks(): ShareLink[] { return load<ShareLink>(KEYS.SHARE_LINKS); }
export function saveShareLinks(items: ShareLink[]): void { save(KEYS.SHARE_LINKS, items); }
export function getShareLinkByNoteId(noteId: string): ShareLink | undefined {
  return getShareLinks().find(s => s.noteId === noteId);
}
export function getShareLinkByToken(token: string): ShareLink | undefined {
  return getShareLinks().find(s => s.token === token);
}
export function createOrReplaceShareLink(noteId: string): ShareLink {
  const token = generateToken();
  const link: ShareLink = { id: generateId(), noteId, token, createdAt: new Date().toISOString() };
  const links = getShareLinks().filter(s => s.noteId !== noteId);
  links.push(link);
  saveShareLinks(links);
  return link;
}
export function revokeShareLink(noteId: string): void {
  saveShareLinks(getShareLinks().filter(s => s.noteId !== noteId));
}

// ---- Audit Logs ----
export function getAuditLogs(): AuditLog[] { return load<AuditLog>(KEYS.AUDIT_LOGS); }
export function addAuditLog(userId: string, action: string, details: string): void {
  const log: AuditLog = { id: generateId(), userId, action, details, timestamp: new Date().toISOString() };
  const logs = getAuditLogs();
  logs.unshift(log);
  // Keep last 500 entries
  save(KEYS.AUDIT_LOGS, logs.slice(0, 500));
}

// ---- Reset Tokens ----
export function getResetTokens(): ResetToken[] { return load<ResetToken>(KEYS.RESET_TOKENS); }
export function createResetToken(userId: string): ResetToken {
  const token: ResetToken = {
    id: generateId(),
    userId,
    token: generateToken(),
    expiresAt: new Date(Date.now() + 3600000).toISOString(), // 1 hour
    used: false,
  };
  const tokens = getResetTokens().filter(t => t.userId !== userId);
  tokens.push(token);
  save(KEYS.RESET_TOKENS, tokens);
  return token;
}
export function getResetTokenByToken(token: string): ResetToken | undefined {
  return getResetTokens().find(t => t.token === token);
}
export function markResetTokenUsed(id: string): void {
  const tokens = getResetTokens().map(t => t.id === id ? { ...t, used: true } : t);
  save(KEYS.RESET_TOKENS, tokens);
}

// ---- Seed Data ----
export function initializeSeedData(): void {
  if (localStorage.getItem(KEYS.INITIALIZED)) return;

  const adminId = generateId();
  const userId1 = generateId();
  const userId2 = generateId();

  const users: User[] = [
    {
      id: adminId,
      username: 'admin',
      email: 'admin@example.com',
      passwordHash: hashPassword('Admin123!'),
      role: 'admin',
      createdAt: new Date(Date.now() - 30 * 86400000).toISOString(),
    },
    {
      id: userId1,
      username: 'alice',
      email: 'alice@example.com',
      passwordHash: hashPassword('User123!'),
      role: 'user',
      createdAt: new Date(Date.now() - 20 * 86400000).toISOString(),
    },
    {
      id: userId2,
      username: 'bob',
      email: 'bob@example.com',
      passwordHash: hashPassword('User123!'),
      role: 'user',
      createdAt: new Date(Date.now() - 10 * 86400000).toISOString(),
    },
  ];
  saveUsers(users);

  const note1Id = generateId();
  const note2Id = generateId();
  const note3Id = generateId();
  const note4Id = generateId();
  const note5Id = generateId();

  const notes: Note[] = [
    {
      id: note1Id,
      title: 'Getting Started with React Hooks',
      content: `React Hooks are a way to use state and other React features without writing a class component.\n\nThe most commonly used hooks are:\n- useState: for local state\n- useEffect: for side effects\n- useContext: for consuming context\n- useRef: for mutable refs\n- useMemo/useCallback: for performance optimization\n\nHooks have rules: only call them at the top level, and only in React functions.`,
      userId: userId1,
      visibility: 'public',
      createdAt: new Date(Date.now() - 18 * 86400000).toISOString(),
      updatedAt: new Date(Date.now() - 18 * 86400000).toISOString(),
    },
    {
      id: note2Id,
      title: 'TypeScript Best Practices',
      content: `TypeScript adds static typing to JavaScript. Here are some best practices:\n\n1. Use strict mode\n2. Prefer interfaces over type aliases for object shapes\n3. Use generics to write reusable code\n4. Avoid using 'any' - use 'unknown' instead\n5. Use union types for flexible yet type-safe APIs\n6. Leverage TypeScript's inference - don't over-annotate\n7. Use readonly where immutability is desired`,
      userId: userId1,
      visibility: 'public',
      createdAt: new Date(Date.now() - 15 * 86400000).toISOString(),
      updatedAt: new Date(Date.now() - 15 * 86400000).toISOString(),
    },
    {
      id: note3Id,
      title: 'Tailwind CSS Tips',
      content: `Tailwind CSS is a utility-first CSS framework.\n\nKey tips:\n- Use @apply for repeated utility combinations\n- Configure your theme in tailwind.config.js\n- Use responsive prefixes (sm:, md:, lg:) for responsive design\n- Dark mode support with the dark: prefix\n- JIT mode for smaller bundle sizes\n\nCommon patterns:\n- flex items-center justify-between\n- grid grid-cols-3 gap-4\n- text-sm font-medium text-gray-700`,
      userId: userId2,
      visibility: 'public',
      createdAt: new Date(Date.now() - 12 * 86400000).toISOString(),
      updatedAt: new Date(Date.now() - 12 * 86400000).toISOString(),
    },
    {
      id: note4Id,
      title: 'My Private Shopping List',
      content: `Things to buy:\n- Groceries\n- New keyboard\n- Books on system design`,
      userId: userId1,
      visibility: 'private',
      createdAt: new Date(Date.now() - 8 * 86400000).toISOString(),
      updatedAt: new Date(Date.now() - 8 * 86400000).toISOString(),
    },
    {
      id: note5Id,
      title: 'Database Design Principles',
      content: `Good database design is fundamental to application performance.\n\nKey principles:\n1. Normalization: eliminate redundancy (1NF, 2NF, 3NF)\n2. Use appropriate data types\n3. Index frequently queried columns\n4. Use foreign keys for referential integrity\n5. Consider denormalization for read-heavy workloads\n6. Use transactions for data consistency\n7. Plan for scalability from the start`,
      userId: userId2,
      visibility: 'public',
      createdAt: new Date(Date.now() - 5 * 86400000).toISOString(),
      updatedAt: new Date(Date.now() - 5 * 86400000).toISOString(),
    },
  ];
  saveNotes(notes);

  const ratings: Rating[] = [
    { id: generateId(), noteId: note1Id, userId: userId2, value: 5, comment: 'Very clear explanation!', createdAt: new Date(Date.now() - 17 * 86400000).toISOString(), updatedAt: new Date(Date.now() - 17 * 86400000).toISOString() },
    { id: generateId(), noteId: note1Id, userId: adminId, value: 4, comment: 'Good overview of hooks.', createdAt: new Date(Date.now() - 16 * 86400000).toISOString(), updatedAt: new Date(Date.now() - 16 * 86400000).toISOString() },
    { id: generateId(), noteId: note1Id, userId: userId1, value: 5, comment: '', createdAt: new Date(Date.now() - 15 * 86400000).toISOString(), updatedAt: new Date(Date.now() - 15 * 86400000).toISOString() },
    { id: generateId(), noteId: note2Id, userId: userId2, value: 5, comment: 'Excellent TypeScript tips!', createdAt: new Date(Date.now() - 14 * 86400000).toISOString(), updatedAt: new Date(Date.now() - 14 * 86400000).toISOString() },
    { id: generateId(), noteId: note2Id, userId: adminId, value: 4, comment: 'Helpful reference.', createdAt: new Date(Date.now() - 13 * 86400000).toISOString(), updatedAt: new Date(Date.now() - 13 * 86400000).toISOString() },
    { id: generateId(), noteId: note2Id, userId: userId1, value: 5, comment: 'I always come back to this.', createdAt: new Date(Date.now() - 12 * 86400000).toISOString(), updatedAt: new Date(Date.now() - 12 * 86400000).toISOString() },
    { id: generateId(), noteId: note3Id, userId: userId1, value: 4, comment: 'Useful CSS tips.', createdAt: new Date(Date.now() - 11 * 86400000).toISOString(), updatedAt: new Date(Date.now() - 11 * 86400000).toISOString() },
    { id: generateId(), noteId: note3Id, userId: adminId, value: 3, comment: 'Could be more detailed.', createdAt: new Date(Date.now() - 10 * 86400000).toISOString(), updatedAt: new Date(Date.now() - 10 * 86400000).toISOString() },
    { id: generateId(), noteId: note3Id, userId: userId2, value: 5, comment: 'Love Tailwind!', createdAt: new Date(Date.now() - 9 * 86400000).toISOString(), updatedAt: new Date(Date.now() - 9 * 86400000).toISOString() },
    { id: generateId(), noteId: note5Id, userId: userId1, value: 5, comment: 'Great principles!', createdAt: new Date(Date.now() - 4 * 86400000).toISOString(), updatedAt: new Date(Date.now() - 4 * 86400000).toISOString() },
    { id: generateId(), noteId: note5Id, userId: adminId, value: 4, comment: 'Solid content.', createdAt: new Date(Date.now() - 3 * 86400000).toISOString(), updatedAt: new Date(Date.now() - 3 * 86400000).toISOString() },
    { id: generateId(), noteId: note5Id, userId: userId2, value: 5, comment: 'Very thorough.', createdAt: new Date(Date.now() - 2 * 86400000).toISOString(), updatedAt: new Date(Date.now() - 2 * 86400000).toISOString() },
  ];
  saveRatings(ratings);

  const auditLogs: AuditLog[] = [
    { id: generateId(), userId: adminId, action: 'USER_REGISTERED', details: 'Admin account created', timestamp: new Date(Date.now() - 30 * 86400000).toISOString() },
    { id: generateId(), userId: userId1, action: 'USER_REGISTERED', details: 'User alice registered', timestamp: new Date(Date.now() - 20 * 86400000).toISOString() },
    { id: generateId(), userId: userId2, action: 'USER_REGISTERED', details: 'User bob registered', timestamp: new Date(Date.now() - 10 * 86400000).toISOString() },
  ];
  save(KEYS.AUDIT_LOGS, auditLogs);

  localStorage.setItem(KEYS.INITIALIZED, 'true');
}
