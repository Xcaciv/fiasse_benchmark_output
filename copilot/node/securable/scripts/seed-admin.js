'use strict';

const { initializeDatabase, getDb } = require('../src/database');
const { hashPassword } = require('../src/utils/passwords');

function getArgument(flag) {
  const index = process.argv.indexOf(flag);
  if (index === -1 || !process.argv[index + 1]) {
    return null;
  }

  return process.argv[index + 1];
}

async function main() {
  initializeDatabase();
  const db = getDb();

  const username = getArgument('--username') || process.env.ADMIN_USERNAME;
  const email = getArgument('--email') || process.env.ADMIN_EMAIL;
  const password = getArgument('--password') || process.env.ADMIN_PASSWORD;

  if (!username || !email || !password) {
    console.error('Usage: npm run seed:admin -- --username admin --email admin@example.com --password "StrongPassw0rd!"');
    process.exit(1);
  }

  const existingUser = db.prepare('SELECT id FROM users WHERE username = ? OR email = ?').get(username, email.toLowerCase());
  if (existingUser) {
    db.prepare('UPDATE users SET role = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?').run('admin', existingUser.id);
    console.log(`Updated existing user ${username} to admin.`);
    return;
  }

  const passwordHash = await hashPassword(password);
  db.prepare(
    'INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, ?)'
  ).run(username, email.toLowerCase(), passwordHash, 'admin');
  console.log(`Created admin user ${username}.`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
