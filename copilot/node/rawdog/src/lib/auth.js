const bcrypt = require('bcryptjs');

const crypto = require('crypto');
const { config } = require('../config');

const authCookieName = 'loose-notes.auth';

function parseCookies(cookieHeader = '') {
  return cookieHeader
    .split(';')
    .map((pair) => pair.trim())
    .filter(Boolean)
    .reduce((cookies, pair) => {
      const separatorIndex = pair.indexOf('=');

      if (separatorIndex === -1) {
        return cookies;
      }

      const name = pair.slice(0, separatorIndex).trim();
      const value = pair.slice(separatorIndex + 1).trim();
      cookies[name] = decodeURIComponent(value);
      return cookies;
    }, {});
}

function signAuthPayload(userId, expiresAt) {
  const payload = `${userId}.${expiresAt}`;
  const signature = crypto
    .createHmac('sha256', process.env.SESSION_SECRET || 'change-me-in-production')
    .update(payload)
    .digest('hex');

  return `${payload}.${signature}`;
}

function verifyAuthPayload(value) {
  if (!value) {
    return null;
  }

  const [userId, expiresAt, signature] = value.split('.');

  if (!userId || !expiresAt || !signature) {
    return null;
  }

  const expectedValue = signAuthPayload(userId, expiresAt);
  const expectedSignature = expectedValue.split('.').pop();

  if (expectedSignature !== signature) {
    return null;
  }

  if (Number(expiresAt) < Date.now()) {
    return null;
  }

  return {
    userId: Number(userId),
    expiresAt: Number(expiresAt)
  };
}

async function hashPassword(password) {
  return bcrypt.hash(password, 12);
}

async function verifyPassword(password, passwordHash) {
  return bcrypt.compare(password, passwordHash);
}

function createSession(req, user) {
  req.session = req.session || {};
  req.session.userId = user.id;
}

function setAuthCookie(res, userId) {
  const maxAge = 8 * 60 * 60 * 1000;
  const expiresAt = Date.now() + maxAge;
  const cookieValue = signAuthPayload(userId, expiresAt);
  const useSecureCookies =
    process.env.NODE_ENV === 'production' && config.baseUrl.startsWith('https://');

  res.cookie(authCookieName, cookieValue, {
    httpOnly: true,
    sameSite: 'lax',
    secure: useSecureCookies,
    maxAge
  });
}

function clearAuthCookie(res) {
  res.clearCookie(authCookieName);
}

function getAuthenticatedUserId(req) {
  const cookies = parseCookies(req.headers.cookie);
  const payload = verifyAuthPayload(cookies[authCookieName]);
  return payload?.userId || null;
}

function saveSession(req) {
  return Promise.resolve();
}

async function redirectWithSession(req, res, location) {
  await saveSession(req);
  res.redirect(location);
}

function destroySession(req) {
  req.session = null;
  return Promise.resolve();
}

module.exports = {
  hashPassword,
  verifyPassword,
  createSession,
  setAuthCookie,
  clearAuthCookie,
  getAuthenticatedUserId,
  saveSession,
  redirectWithSession,
  destroySession
};
