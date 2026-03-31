export interface User {
  id: string;
  username: string;
  email: string;
  passwordHash: string;
  role: 'user' | 'admin';
  createdAt: string;
}

export interface Note {
  id: string;
  title: string;
  content: string;
  userId: string;
  visibility: 'public' | 'private';
  createdAt: string;
  updatedAt: string;
}

export interface Attachment {
  id: string;
  noteId: string;
  filename: string;
  originalFilename: string;
  fileType: string;
  size: number;
  createdAt: string;
}

export interface Rating {
  id: string;
  noteId: string;
  userId: string;
  value: number;
  comment: string;
  createdAt: string;
  updatedAt: string;
}

export interface ShareLink {
  id: string;
  noteId: string;
  token: string;
  createdAt: string;
}

export interface AuditLog {
  id: string;
  userId: string;
  action: string;
  details: string;
  timestamp: string;
}

export interface ResetToken {
  id: string;
  userId: string;
  token: string;
  expiresAt: string;
  used: boolean;
}
