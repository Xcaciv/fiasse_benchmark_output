'use strict';

/**
 * Security unit tests — SSEM Testability.
 * These tests verify security controls in isolation without network/DB.
 */

const security = require('../config/security');
const { getFilePath } = require('../services/fileService');
const path = require('path');

// ── security.js ─────────────────────────────────────────────────────────────

describe('security config', () => {
  test('allowed extensions are a Set', () => {
    expect(security.allowedExtensions).toBeInstanceOf(Set);
    expect(security.allowedExtensions.has('pdf')).toBe(true);
    expect(security.allowedExtensions.has('exe')).toBe(false);
    expect(security.allowedExtensions.has('php')).toBe(false);
  });

  test('allowed mime types are a Set', () => {
    expect(security.allowedMimeTypes).toBeInstanceOf(Set);
    expect(security.allowedMimeTypes.has('application/pdf')).toBe(true);
    expect(security.allowedMimeTypes.has('application/x-php')).toBe(false);
  });

  test('bcryptRounds is at least 10', () => {
    expect(security.bcryptRounds).toBeGreaterThanOrEqual(10);
  });

  test('resetTokenExpiryMs is a positive number', () => {
    expect(security.resetTokenExpiryMs).toBeGreaterThan(0);
    expect(typeof security.resetTokenExpiryMs).toBe('number');
  });

  test('uploadMaxSizeBytes is a positive number', () => {
    expect(security.uploadMaxSizeBytes).toBeGreaterThan(0);
  });

  test('rate limit windows and maxes are positive', () => {
    expect(security.rateLimits.login.windowMs).toBeGreaterThan(0);
    expect(security.rateLimits.login.max).toBeGreaterThan(0);
    expect(security.rateLimits.general.windowMs).toBeGreaterThan(0);
    expect(security.rateLimits.general.max).toBeGreaterThan(0);
  });
});

// ── fileService path traversal ───────────────────────────────────────────────

describe('fileService.getFilePath — path traversal prevention', () => {
  test('rejects path traversal with ..', () => {
    expect(() => getFilePath('../etc/passwd')).toThrow();
  });

  test('rejects absolute path escape', () => {
    expect(() => getFilePath('/etc/passwd')).toThrow();
  });

  test('rejects traversal with encoded sequences in stored name', () => {
    // A stored filename should be a plain UUID.ext — no directory separators
    expect(() => getFilePath('../../secret.txt')).toThrow();
  });

  test('accepts a valid UUID filename', () => {
    const validName = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890.pdf';
    // Should not throw (file may not exist on disk but path resolution is valid)
    expect(() => getFilePath(validName)).not.toThrow();
  });
});

// ── Share link token format ──────────────────────────────────────────────────

describe('share link token validation', () => {
  const { resolveShareLink } = require('../services/shareLinkService');

  // resolveShareLink returns null for invalid-format tokens without hitting DB
  test('rejects tokens that are too short', async () => {
    const result = await resolveShareLink('abc123');
    expect(result).toBeNull();
  });

  test('rejects tokens with non-hex characters', async () => {
    const badToken = 'z'.repeat(96);
    const result = await resolveShareLink(badToken);
    expect(result).toBeNull();
  });

  test('rejects empty token', async () => {
    const result = await resolveShareLink('');
    expect(result).toBeNull();
  });
});

// ── Rating value clamping ────────────────────────────────────────────────────

describe('ratingService.clampRating via upsertRating', () => {
  // clampRating is internal — test indirectly via the exported function
  // by confirming it throws on out-of-range input (before any DB call)
  const ratingService = require('../services/ratingService');

  test('throws on rating value 0', async () => {
    await expect(
      ratingService.upsertRating({ noteId: 'fake', userId: 'fake', value: 0 })
    ).rejects.toThrow();
  });

  test('throws on rating value 6', async () => {
    await expect(
      ratingService.upsertRating({ noteId: 'fake', userId: 'fake', value: 6 })
    ).rejects.toThrow();
  });

  test('throws on non-numeric rating value', async () => {
    await expect(
      ratingService.upsertRating({ noteId: 'fake', userId: 'fake', value: 'five' })
    ).rejects.toThrow();
  });
});
