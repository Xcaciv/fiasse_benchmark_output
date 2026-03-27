'use strict';

const { validationResult } = require('express-validator');
const { createNoteValidator, editNoteValidator } = require('../../src/validators/noteValidators');

async function runValidators(validators, body) {
  const req = { body, headers: {}, cookies: {} };
  for (const validator of validators) {
    await validator.run(req);
  }
  return validationResult(req);
}

describe('noteValidators - createNoteValidator', () => {
  it('accepts valid note data', async () => {
    const result = await runValidators(createNoteValidator, {
      title: 'My Note Title',
      content: 'This is the note content.',
    });
    expect(result.isEmpty()).toBe(true);
  });

  it('rejects empty title', async () => {
    const result = await runValidators(createNoteValidator, {
      title: '',
      content: 'Some content here.',
    });
    expect(result.isEmpty()).toBe(false);
    expect(result.array().some((e) => e.path === 'title')).toBe(true);
  });

  it('rejects title exceeding 255 characters', async () => {
    const result = await runValidators(createNoteValidator, {
      title: 'T'.repeat(256),
      content: 'Some content here.',
    });
    expect(result.isEmpty()).toBe(false);
    expect(result.array().some((e) => e.path === 'title')).toBe(true);
  });

  it('rejects empty content', async () => {
    const result = await runValidators(createNoteValidator, {
      title: 'Valid Title',
      content: '',
    });
    expect(result.isEmpty()).toBe(false);
    expect(result.array().some((e) => e.path === 'content')).toBe(true);
  });

  it('rejects content exceeding 50000 characters', async () => {
    const result = await runValidators(createNoteValidator, {
      title: 'Valid Title',
      content: 'x'.repeat(50001),
    });
    expect(result.isEmpty()).toBe(false);
  });
});

describe('noteValidators - editNoteValidator', () => {
  it('accepts valid visibility values', async () => {
    for (const vis of ['public', 'private']) {
      const result = await runValidators(editNoteValidator, {
        title: 'Title',
        content: 'Content',
        visibility: vis,
      });
      expect(result.isEmpty()).toBe(true);
    }
  });

  it('rejects invalid visibility value', async () => {
    const result = await runValidators(editNoteValidator, {
      title: 'Title',
      content: 'Content',
      visibility: 'hidden',
    });
    expect(result.isEmpty()).toBe(false);
    expect(result.array().some((e) => e.path === 'visibility')).toBe(true);
  });
});
