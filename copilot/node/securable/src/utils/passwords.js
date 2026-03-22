'use strict';

const crypto = require('node:crypto');
const util = require('node:util');

const pbkdf2 = util.promisify(crypto.pbkdf2);

const ITERATIONS = 310000;
const KEY_LENGTH = 32;
const DIGEST = 'sha256';

async function hashPassword(password) {
  const salt = crypto.randomBytes(16).toString('hex');
  const derivedKey = await pbkdf2(password, salt, ITERATIONS, KEY_LENGTH, DIGEST);
  return `${ITERATIONS}:${salt}:${derivedKey.toString('hex')}`;
}

async function verifyPassword(password, storedHash) {
  const [iterationsValue, salt, originalHash] = storedHash.split(':');
  const iterations = Number(iterationsValue);
  const derivedKey = await pbkdf2(password, salt, iterations, KEY_LENGTH, DIGEST);
  const originalBuffer = Buffer.from(originalHash, 'hex');

  return (
    originalBuffer.length === derivedKey.length &&
    crypto.timingSafeEqual(originalBuffer, derivedKey)
  );
}

function isStrongPassword(password) {
  return (
    typeof password === 'string' &&
    password.length >= 12 &&
    /[a-z]/.test(password) &&
    /[A-Z]/.test(password) &&
    /\d/.test(password) &&
    /[^A-Za-z0-9]/.test(password)
  );
}

module.exports = {
  hashPassword,
  isStrongPassword,
  verifyPassword
};
