'use strict';

const { body } = require('express-validator');

const ratingValidator = [
  body('value')
    .isInt({ min: 1, max: 5 }).withMessage('Rating must be an integer between 1 and 5.')
    .toInt(),
  body('comment')
    .optional()
    .trim()
    .escape()
    .isLength({ max: 1000 }).withMessage('Comment must be at most 1,000 characters.'),
];

module.exports = { ratingValidator };
