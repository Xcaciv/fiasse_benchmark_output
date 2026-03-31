// Demo-only password handling. NOT suitable for production.
// In production, use a backend with bcrypt/argon2.

export function hashPassword(password: string): string {
  return btoa(encodeURIComponent(password) + ':ln_demo_salt_v1');
}

export function verifyPassword(password: string, hash: string): boolean {
  return hashPassword(password) === hash;
}

export function generateToken(): string {
  return crypto.randomUUID().replace(/-/g, '') + crypto.randomUUID().replace(/-/g, '');
}

export function generateId(): string {
  return crypto.randomUUID();
}

export function isValidEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

export function isValidPassword(password: string): boolean {
  // Min 8 chars, at least one uppercase, one lowercase, one digit
  return password.length >= 8 &&
    /[A-Z]/.test(password) &&
    /[a-z]/.test(password) &&
    /[0-9]/.test(password);
}
