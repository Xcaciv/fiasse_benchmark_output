'use strict';

const { generateToken, hashToken } = require('../../src/utils/tokenUtils');

describe('tokenUtils', () => {
  describe('generateToken()', () => {
    it('returns a 64-character hex string', () => {
      const token = generateToken();
      expect(typeof token).toBe('string');
      expect(token).toHaveLength(64);
      expect(/^[a-f0-9]{64}$/.test(token)).toBe(true);
    });

    it('produces unique values on each call', () => {
      const t1 = generateToken();
      const t2 = generateToken();
      expect(t1).not.toBe(t2);
    });

    it('returns different values across multiple calls', () => {
      const tokens = new Set(Array.from({ length: 10 }, () => generateToken()));
      expect(tokens.size).toBe(10);
    });
  });

  describe('hashToken()', () => {
    it('returns a 64-character hex SHA-256 hash', () => {
      const hash = hashToken('test-token');
      expect(typeof hash).toBe('string');
      expect(hash).toHaveLength(64);
      expect(/^[a-f0-9]{64}$/.test(hash)).toBe(true);
    });

    it('is deterministic — same input yields same hash', () => {
      const token = 'consistent-token';
      expect(hashToken(token)).toBe(hashToken(token));
    });

    it('produces different hashes for different inputs', () => {
      expect(hashToken('token-a')).not.toBe(hashToken('token-b'));
    });

    it('correctly hashes the known SHA-256 value of "abc"', () => {
      const expected = 'ba7816bf8f01cfea414140de5dae2ec73b00361bbef0469f96edbb74a8e6750c';
      expect(hashToken('abc')).toBe(expected);
    });
  });
});
