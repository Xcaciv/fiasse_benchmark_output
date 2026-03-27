'use strict';

const { body } = require('express-validator');

const createNoteValidator = [
  body('title')
    .trim()
    .escape()
    .isLength({ min: 1, max: 255 }).withMessage('Title must be 1–255 characters.'),
  body('content')
    .trim()
    .isLength({ min: 1, max: 50000 }).withMessage('Content must be 1–50,000 characters.'),
];

const editNoteValidator = [
  ...createNoteValidator,
  body('visibility')
    .optional()
    .isIn(['public', 'private']).withMessage('Visibility must be public or private.'),
];

module.exports = { createNoteValidator, editNoteValidator };
