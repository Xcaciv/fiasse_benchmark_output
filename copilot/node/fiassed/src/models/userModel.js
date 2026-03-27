'use strict';

function createUser(db, { id, username, email, passwordHash }) {
  const now = Date.now();
  const stmt = db.prepare(
    'INSERT INTO users (id, username, email, password_hash, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)'
  );
  stmt.run(id, username, email, passwordHash, now, now);
  return findById(db, id);
}

function findByUsername(db, username) {
  return db.prepare('SELECT * FROM users WHERE username = ?').get(username) || null;
}

function findByEmail(db, email) {
  return db.prepare('SELECT * FROM users WHERE email = ?').get(email) || null;
}

function findById(db, id) {
  return db.prepare('SELECT * FROM users WHERE id = ?').get(id) || null;
}

function updatePassword(db, userId, passwordHash) {
  const result = db.prepare(
    'UPDATE users SET password_hash = ?, updated_at = ? WHERE id = ?'
  ).run(passwordHash, Date.now(), userId);
  return result.changes;
}

function updateUser(db, userId, updates) {
  const fields = [];
  const values = [];
  if (updates.email !== undefined) { fields.push('email = ?'); values.push(updates.email); }
  if (updates.username !== undefined) { fields.push('username = ?'); values.push(updates.username); }
  if (fields.length === 0) return 0;
  fields.push('updated_at = ?');
  values.push(Date.now(), userId);
  const result = db.prepare(`UPDATE users SET ${fields.join(', ')} WHERE id = ?`).run(...values);
  return result.changes;
}

function getAllUsers(db) {
  return db.prepare('SELECT id, username, email, role, created_at FROM users ORDER BY created_at DESC').all();
}

function updateRole(db, userId, role) {
  const result = db.prepare(
    'UPDATE users SET role = ?, updated_at = ? WHERE id = ?'
  ).run(role, Date.now(), userId);
  return result.changes;
}

function deleteUser(db, userId) {
  const result = db.prepare('DELETE FROM users WHERE id = ?').run(userId);
  return result.changes;
}

module.exports = { createUser, findByUsername, findByEmail, findById, updatePassword, updateUser, getAllUsers, updateRole, deleteUser };
