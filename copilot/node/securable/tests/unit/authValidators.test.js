'use strict';

const { validationResult } = require('express-validator');
const { registerValidator } = require('../../src/validators/authValidators');

async function runValidators(validators, body) {
  const req = { body, headers: {}, cookies: {} };
  req.flash = () => {};
  for (const validator of validators) {
    await validator.run(req);
  }
  return validationResult(req);
}

describe('authValidators - registerValidator', () => {
  it('accepts valid registration data', async () => {
    const result = await runValidators(registerValidator, {
      username: 'valid_user',
      email: 'user@example.com',
      password: 'securepassword1',
    });
    expect(result.isEmpty()).toBe(true);
  });

  it('rejects username shorter than 3 characters', async () => {
    const result = await runValidators(registerValidator, {
      username: 'ab',
      email: 'user@example.com',
      password: 'securepassword1',
    });
    expect(result.isEmpty()).toBe(false);
    expect(result.array().some((e) => e.path === 'username')).toBe(true);
  });

  it('rejects username with special characters', async () => {
    const result = await runValidators(registerValidator, {
      username: 'bad user!',
      email: 'user@example.com',
      password: 'securepassword1',
    });
    expect(result.isEmpty()).toBe(false);
    expect(result.array().some((e) => e.path === 'username')).toBe(true);
  });

  it('rejects username longer than 50 characters', async () => {
    const result = await runValidators(registerValidator, {
      username: 'a'.repeat(51),
      email: 'user@example.com',
      password: 'securepassword1',
    });
    expect(result.isEmpty()).toBe(false);
  });

  it('rejects invalid email format', async () => {
    const result = await runValidators(registerValidator, {
      username: 'valid_user',
      email: 'not-an-email',
      password: 'securepassword1',
    });
    expect(result.isEmpty()).toBe(false);
    expect(result.array().some((e) => e.path === 'email')).toBe(true);
  });

  it('rejects password shorter than 8 characters', async () => {
    const result = await runValidators(registerValidator, {
      username: 'valid_user',
      email: 'user@example.com',
      password: 'short',
    });
    expect(result.isEmpty()).toBe(false);
    expect(result.array().some((e) => e.path === 'password')).toBe(true);
  });

  it('rejects password longer than 128 characters', async () => {
    const result = await runValidators(registerValidator, {
      username: 'valid_user',
      email: 'user@example.com',
      password: 'a'.repeat(129),
    });
    expect(result.isEmpty()).toBe(false);
  });
});
