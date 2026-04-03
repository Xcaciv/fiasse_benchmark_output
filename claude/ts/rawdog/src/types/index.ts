export interface User {
  id: number;
  username: string;
  email: string;
  passwordBase64: string;
  role: 'user' | 'admin';
  securityQuestion?: string;
  securityAnswer?: string;
  createdAt: string;
}

export interface Note {
  id: number;
  title: string;
  content: string;
  isPublic: boolean;
  userId: number;
  createdAt: string;
  shareToken?: string;
  attachments: Attachment[];
}

export interface Attachment {
  id: number;
  noteId: number;
  filename: string;
  originalName: string;
  contentType: string;
  data: string;
  createdAt: string;
}

export interface Rating {
  id: number;
  noteId: number;
  userEmail: string;
  score: number;
  comment: string;
  createdAt: string;
}

export interface ShareToken {
  id: number;
  noteId: number;
  token: string;
  createdAt: string;
}

export interface DB {
  users: User[];
  notes: Note[];
  attachments: Attachment[];
  ratings: Rating[];
  shareTokenCounter: number;
  nextId: {
    users: number;
    notes: number;
    attachments: number;
    ratings: number;
  };
}
