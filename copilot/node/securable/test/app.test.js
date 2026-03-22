'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const fs = require('node:fs');

const request = require('supertest');

const { app } = require('../app');
const { dataDirectories, getDb } = require('../src/database');
const { hashPassword } = require('../src/utils/passwords');

function extractCsrfToken(html) {
  const match = html.match(/name="_csrf" value="([^"]+)"/);
  return match ? match[1] : null;
}

test('health endpoint responds with ok', async () => {
  const response = await request(app).get('/health');
  assert.equal(response.statusCode, 200);
  assert.equal(response.body.status, 'ok');
});

test('user can register and see dashboard', async () => {
  const agent = request.agent(app);
  const registerPage = await agent.get('/register');
  assert.equal(registerPage.statusCode, 200);

  const csrfToken = extractCsrfToken(registerPage.text);
  assert.ok(csrfToken);

  const response = await agent.post('/register').type('form').send({
    _csrf: csrfToken,
    username: `tester${Date.now()}`,
    email: `tester${Date.now()}@example.com`,
    password: 'ExamplePassw0rd!',
    confirmPassword: 'ExamplePassw0rd!'
  });

  assert.equal(response.statusCode, 302);
  assert.equal(response.headers.location, '/dashboard');

  const dashboard = await agent.get('/dashboard');
  assert.equal(dashboard.statusCode, 200);
  assert.match(dashboard.text, /Your notes/);
});

test('authenticated user can create a note', async () => {
  const agent = request.agent(app);
  const uniqueSuffix = Date.now();

  const registerPage = await agent.get('/register');
  const registerCsrf = extractCsrfToken(registerPage.text);

  await agent.post('/register').type('form').send({
    _csrf: registerCsrf,
    username: `author${uniqueSuffix}`,
    email: `author${uniqueSuffix}@example.com`,
    password: 'ExamplePassw0rd!',
    confirmPassword: 'ExamplePassw0rd!'
  }).expect(302);

  const createPage = await agent.get('/notes/new');
  assert.equal(createPage.statusCode, 200);
  const noteCsrf = extractCsrfToken(createPage.text);
  assert.ok(noteCsrf);

  const createResponse = await agent
    .post(`/notes?_csrf=${encodeURIComponent(noteCsrf)}`)
    .field('title', 'My secure note')
    .field('content', 'This note verifies multipart-safe CSRF handling.')
    .field('visibility', 'private');

  assert.equal(createResponse.statusCode, 302);
  assert.match(createResponse.headers.location, /^\/notes\/\d+$/);

  const notePage = await agent.get(createResponse.headers.location);
  assert.equal(notePage.statusCode, 200);
  assert.match(notePage.text, /My secure note/);
});

test('dashboard counts attachments and ratings without duplication', async () => {
  const authorAgent = request.agent(app);
  const raterAgent = request.agent(app);
  const uniqueSuffix = `counts${Date.now()}`;

  const authorRegisterPage = await authorAgent.get('/register');
  const authorRegisterCsrf = extractCsrfToken(authorRegisterPage.text);

  await authorAgent.post('/register').type('form').send({
    _csrf: authorRegisterCsrf,
    username: `author-${uniqueSuffix}`,
    email: `author-${uniqueSuffix}@example.com`,
    password: 'ExamplePassw0rd!',
    confirmPassword: 'ExamplePassw0rd!'
  }).expect(302);

  const createPage = await authorAgent.get('/notes/new');
  const createCsrf = extractCsrfToken(createPage.text);
  const createResponse = await authorAgent
    .post(`/notes?_csrf=${encodeURIComponent(createCsrf)}`)
    .field('title', 'Dashboard count note')
    .field('content', 'One attachment and one rating should each count once.')
    .field('visibility', 'public')
    .attach('attachments', Buffer.from('attachment body'), 'evidence.txt');

  assert.equal(createResponse.statusCode, 302);

  const notePath = createResponse.headers.location;
  const noteIdMatch = notePath.match(/^\/notes\/(\d+)$/);
  assert.ok(noteIdMatch);

  const raterRegisterPage = await raterAgent.get('/register');
  const raterRegisterCsrf = extractCsrfToken(raterRegisterPage.text);

  await raterAgent.post('/register').type('form').send({
    _csrf: raterRegisterCsrf,
    username: `rater-${uniqueSuffix}`,
    email: `rater-${uniqueSuffix}@example.com`,
    password: 'ExamplePassw0rd!',
    confirmPassword: 'ExamplePassw0rd!'
  }).expect(302);

  const notePage = await raterAgent.get(notePath);
  const ratingCsrf = extractCsrfToken(notePage.text);

  await raterAgent.post(`/notes/${noteIdMatch[1]}/ratings`).type('form').send({
    _csrf: ratingCsrf,
    value: '5',
    comment: 'Great note.'
  }).expect(302);

  const dashboard = await authorAgent.get('/dashboard');
  assert.equal(dashboard.statusCode, 200);
  assert.match(dashboard.text, />1<\/td>\s*<td>1<\/td>/);
});

test('forgot-password issues a reset token and the token can be used exactly once', async () => {
  const agent = request.agent(app);
  const uniqueSuffix = `reset${Date.now()}`;
  const email = `${uniqueSuffix}@example.com`;

  const registerPage = await agent.get('/register');
  const registerCsrf = extractCsrfToken(registerPage.text);

  await agent.post('/register').type('form').send({
    _csrf: registerCsrf,
    username: uniqueSuffix,
    email,
    password: 'ExamplePassw0rd!',
    confirmPassword: 'ExamplePassw0rd!'
  }).expect(302);

  const forgotPasswordPage = await agent.get('/forgot-password');
  const forgotPasswordCsrf = extractCsrfToken(forgotPasswordPage.text);

  const mailFilesBefore = new Set(fs.readdirSync(dataDirectories.mail));

  await agent.post('/forgot-password').type('form').send({
    _csrf: forgotPasswordCsrf,
    email
  }).expect(302);

  const newMailFile = fs
    .readdirSync(dataDirectories.mail)
    .find((fileName) => !mailFilesBefore.has(fileName));
  assert.ok(newMailFile);

  const mailText = fs.readFileSync(path.join(dataDirectories.mail, newMailFile), 'utf8');
  const normalizedMailText = mailText.replace(/=\r?\n/g, '').replace(/=3D/g, '=');
  const resetLinkMatch = normalizedMailText.match(/http:\/\/[^/\s"]+\/reset-password\/([a-f0-9]{64})/i);
  assert.ok(resetLinkMatch);

  const resetPage = await agent.get(`/reset-password/${resetLinkMatch[1]}`);
  const resetCsrf = extractCsrfToken(resetPage.text);
  assert.ok(resetCsrf);

  await agent.post(`/reset-password/${resetLinkMatch[1]}`).type('form').send({
    _csrf: resetCsrf,
    password: 'NewExamplePassw0rd!',
    confirmPassword: 'NewExamplePassw0rd!'
  }).expect(302);

  const reusedTokenPage = await agent.get(`/reset-password/${resetLinkMatch[1]}`);
  assert.equal(reusedTokenPage.statusCode, 400);
  assert.match(reusedTokenPage.text, /invalid|expired/i);
});

test('admin can reassign a note to another existing user', async () => {
  const uniqueSuffix = `admin${Date.now()}`;
  const db = getDb();
  const adminPassword = 'AdminExamplePassw0rd!';
  const adminHash = await hashPassword(adminPassword);
  const adminInsert = db
    .prepare('INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, ?)')
    .run(`admin-${uniqueSuffix}`, `admin-${uniqueSuffix}@example.com`, adminHash, 'admin');

  const ownerPassword = 'OwnerExamplePassw0rd!';
  const newOwnerPassword = 'NewOwnerExamplePassw0rd!';
  const ownerHash = await hashPassword(ownerPassword);
  const newOwnerHash = await hashPassword(newOwnerPassword);
  const ownerInsert = db
    .prepare('INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, ?)')
    .run(`owner-${uniqueSuffix}`, `owner-${uniqueSuffix}@example.com`, ownerHash, 'user');
  const newOwnerInsert = db
    .prepare('INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, ?)')
    .run(`recipient-${uniqueSuffix}`, `recipient-${uniqueSuffix}@example.com`, newOwnerHash, 'user');

  const noteInsert = db
    .prepare('INSERT INTO notes (user_id, title, content, visibility) VALUES (?, ?, ?, ?)')
    .run(ownerInsert.lastInsertRowid, 'Reassign me', 'Admin ownership transfer test.', 'private');

  const adminAgent = request.agent(app);
  const loginPage = await adminAgent.get('/login');
  const loginCsrf = extractCsrfToken(loginPage.text);

  await adminAgent.post('/login').type('form').send({
    _csrf: loginCsrf,
    username: `admin-${uniqueSuffix}`,
    password: adminPassword
  }).expect(302);

  const reassignPage = await adminAgent.get(`/admin/notes/${noteInsert.lastInsertRowid}/reassign`);
  assert.equal(reassignPage.statusCode, 200);
  const reassignCsrf = extractCsrfToken(reassignPage.text);

  await adminAgent.post(`/admin/notes/${noteInsert.lastInsertRowid}/reassign`).type('form').send({
    _csrf: reassignCsrf,
    newOwnerId: String(newOwnerInsert.lastInsertRowid)
  }).expect(302);

  const updatedNote = db
    .prepare('SELECT user_id FROM notes WHERE id = ?')
    .get(noteInsert.lastInsertRowid);
  assert.equal(updatedNote.user_id, newOwnerInsert.lastInsertRowid);
  assert.ok(adminInsert.lastInsertRowid);
});
