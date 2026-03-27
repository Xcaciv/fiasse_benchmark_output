const Database = require('better-sqlite3');
const bcrypt = require('bcrypt');
const path = require('path');

const dbPath = path.join(__dirname, '../../data/loosenotes.db');
const db = new Database(dbPath);

async function seed() {
  console.log('Seeding database...');

  const adminPassword = await bcrypt.hash('Admin123!', 10);
  const userPassword = await bcrypt.hash('Password123!', 10);

  const insertUser = db.prepare(`
    INSERT INTO users (username, email, password_hash, role)
    VALUES (?, ?, ?, ?)
  `);

  const insertNote = db.prepare(`
    INSERT INTO notes (user_id, title, content, is_public)
    VALUES (?, ?, ?, ?)
  `);

  const insertRating = db.prepare(`
    INSERT INTO ratings (note_id, user_id, rating, comment)
    VALUES (?, ?, ?, ?)
  `);

  const insertActivity = db.prepare(`
    INSERT INTO activity_logs (user_id, action, description)
    VALUES (?, ?, ?)
  `);

  const adminId = insertUser.run('admin', 'admin@loosenotes.com', adminPassword, 'admin').lastInsertRowid;
  const johnId = insertUser.run('johndoe', 'john@example.com', userPassword, 'user').lastInsertRowid;
  const janeId = insertUser.run('janedoe', 'jane@example.com', userPassword, 'user').lastInsertRowid;

  console.log('Created users: admin, johndoe, janedoe');

  insertActivity.run(adminId, 'user_registered', 'Admin user created');
  insertActivity.run(johnId, 'user_registered', 'John Doe registered');
  insertActivity.run(janeId, 'user_registered', 'Jane Doe registered');

  const note1 = insertNote.run(johnId, 'Getting Started with Node.js', 
    'Node.js is a JavaScript runtime built on Chrome\'s V8 JavaScript engine. It uses an event-driven, non-blocking I/O model that makes it lightweight and efficient.\n\nKey features:\n- Fast performance\n- Large ecosystem (npm)\n- Asynchronous programming\n- Easy to learn for JavaScript developers', 1).lastInsertRowid;

  const note2 = insertNote.run(johnId, 'Express.js Best Practices',
    'Here are some best practices for building Express.js applications:\n\n1. Use middleware for cross-cutting concerns\n2. Keep routes modular\n3. Validate input data\n4. Use proper error handling\n5. Implement proper logging\n6. Use environment variables for configuration', 1).lastInsertRowid;

  const note3 = insertNote.run(johnId, 'My Private Thoughts',
    'This is a private note that only I can see.\n\nIt contains my personal thoughts and ideas that are not meant for public consumption.', 0).lastInsertRowid;

  const note4 = insertNote.run(janeId, 'Introduction to REST APIs',
    'REST (Representational State Transfer) is an architectural style for designing networked applications. RESTful APIs use HTTP requests to perform CRUD operations.\n\nHTTP Methods:\n- GET: Retrieve data\n- POST: Create new resources\n- PUT: Update existing resources\n- DELETE: Remove resources', 1).lastInsertRowid;

  const note5 = insertNote.run(janeId, 'Database Design Tips',
    'Good database design is crucial for application performance. Here are some tips:\n\n1. Normalize your data to reduce redundancy\n2. Use appropriate indexes for frequently queried fields\n3. Choose the right data types\n4. Consider denormalization for read-heavy workloads\n5. Plan for scalability', 1).lastInsertRowid;

  console.log('Created 5 notes');

  insertRating.run(note1, janeId, 5, 'Great introduction to Node.js!');
  insertRating.run(note1, adminId, 4, 'Very helpful, thanks!');
  insertRating.run(note1, johnId, 5, 'One of the best tutorials!');

  insertRating.run(note2, janeId, 4, 'Good best practices list');
  insertRating.run(note2, adminId, 5, 'Very comprehensive!');
  insertRating.run(note2, johnId, 4, 'Helpful tips');

  insertRating.run(note4, johnId, 5, 'Excellent explanation of REST!');
  insertRating.run(note4, adminId, 4, 'Clear and concise');
  insertRating.run(note4, janeId, 5, 'Very well written');

  insertRating.run(note5, johnId, 3, 'Good tips but could be more detailed');
  insertRating.run(note5, adminId, 4, 'Useful database design guide');

  console.log('Created ratings');

  insertActivity.run(johnId, 'note_created', 'Created "Getting Started with Node.js"');
  insertActivity.run(johnId, 'note_created', 'Created "Express.js Best Practices"');
  insertActivity.run(johnId, 'note_created', 'Created "My Private Thoughts"');
  insertActivity.run(janeId, 'note_created', 'Created "Introduction to REST APIs"');
  insertActivity.run(janeId, 'note_created', 'Created "Database Design Tips"');

  insertActivity.run(janeId, 'note_rated', 'Rated "Getting Started with Node.js"');
  insertActivity.run(adminId, 'note_rated', 'Rated "Getting Started with Node.js"');

  console.log('Seeding completed successfully!');
  db.close();
}

seed().catch(console.error);
