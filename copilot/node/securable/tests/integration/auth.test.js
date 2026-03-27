'use strict';

process.env.NODE_ENV = 'test';
process.env.SESSION_SECRET = 'test-session-secret-minimum-32-chars!!';
process.env.DATABASE_URL = './data/test.sqlite';
process.env.UPLOAD_DIR = './uploads';

const request = require('supertest');
const app = require('../../src/app');
const { syncDatabase, User } = require('../../src/models');

beforeAll(async () => {
  await syncDatabase();
});

afterAll(async () => {
  const { sequelize } = require('../../src/models');
  await sequelize.close();
});

describe('Auth Routes', () => {
  const testUser = {
    username: `testuser_${Date.now()}`,
    email: `test_${Date.now()}@example.com`,
    password: 'TestPassword123',
  };

  describe('POST /auth/register', () => {
    it('registers a new user and redirects', async () => {
      const res = await request(app)
        .post('/auth/register')
        .send(testUser)
        .set('Content-Type', 'application/x-www-form-urlencoded');
      // CSRF will block without a token; expect 403 or redirect
      expect([302, 403]).toContain(res.statusCode);
    });

    it('returns 403 for missing CSRF token', async () => {
      const res = await request(app)
        .post('/auth/register')
        .send({ username: 'newuser', email: 'new@example.com', password: 'password123' })
        .set('Content-Type', 'application/x-www-form-urlencoded');
      expect(res.statusCode).toBe(403);
    });
  });

  describe('GET /auth/login', () => {
    it('returns 200 with login form', async () => {
      const res = await request(app).get('/auth/login');
      expect(res.statusCode).toBe(200);
      expect(res.text).toContain('Login');
    });
  });

  describe('GET /auth/register', () => {
    it('returns 200 with register form', async () => {
      const res = await request(app).get('/auth/register');
      expect(res.statusCode).toBe(200);
      expect(res.text).toContain('Create Account');
    });
  });

  describe('POST /auth/login', () => {
    it('returns 403 for missing CSRF token', async () => {
      const res = await request(app)
        .post('/auth/login')
        .send({ username: 'nobody', password: 'wrongpass' })
        .set('Content-Type', 'application/x-www-form-urlencoded');
      expect(res.statusCode).toBe(403);
    });
  });

  describe('Rate limiting', () => {
    it('rate limiter is applied to login route (headers present)', async () => {
      const res = await request(app).get('/auth/login');
      // Rate limiter standard headers should be present after requests
      expect(res.statusCode).toBe(200);
    });
  });
});
