'use strict';

const { body, validationResult } = require('express-validator');

// Password strength: min 8 chars, upper, lower, digit, special
const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&^#])[A-Za-z\d@$!%*?&^#]{8,}$/;
const PASSWORD_MSG = 'Password must be at least 8 characters with uppercase, lowercase, number, and special character (@$!%*?&^#)';

/**
 * Collect validation errors and redirect back with a flash message.
 * Placed at the end of each validation chain array.
 */
const handleValidationErrors = (req, res, next) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    req.session.flash = {
      type: 'error',
      message: errors.array().map((e) => e.msg).join('. '),
    };
    req.session.formErrors = errors.array();
    return res.redirect('back');
  }
  next();
};

const registerValidation = [
  body('username')
    .trim()
    .isLength({ min: 3, max: 30 }).withMessage('Username must be 3–30 characters')
    .isAlphanumeric().withMessage('Username must contain only letters and numbers'),
  body('email')
    .trim()
    .isEmail().withMessage('A valid email address is required')
    .normalizeEmail(),
  body('password')
    .matches(PASSWORD_REGEX).withMessage(PASSWORD_MSG),
  handleValidationErrors,
];

const loginValidation = [
  body('username').trim().notEmpty().withMessage('Username is required'),
  body('password').notEmpty().withMessage('Password is required'),
  handleValidationErrors,
];

const noteValidation = [
  body('title')
    .trim()
    .isLength({ min: 1, max: 255 }).withMessage('Title must be 1–255 characters'),
  body('content')
    .trim()
    .isLength({ min: 1, max: 50000 }).withMessage('Content must be 1–50,000 characters'),
  handleValidationErrors,
];

const ratingValidation = [
  body('stars')
    .isInt({ min: 1, max: 5 }).withMessage('Stars must be an integer between 1 and 5'),
  body('comment')
    .optional({ checkFalsy: true })
    .trim()
    .isLength({ max: 1000 }).withMessage('Comment must not exceed 1,000 characters'),
  handleValidationErrors,
];

const passwordResetValidation = [
  body('password').matches(PASSWORD_REGEX).withMessage(PASSWORD_MSG),
  handleValidationErrors,
];

const forgotPasswordValidation = [
  body('email')
    .trim()
    .isEmail().withMessage('A valid email address is required')
    .normalizeEmail(),
  handleValidationErrors,
];

const profileValidation = [
  body('username')
    .trim()
    .isLength({ min: 3, max: 30 }).withMessage('Username must be 3–30 characters')
    .isAlphanumeric().withMessage('Username must contain only letters and numbers'),
  body('email')
    .trim()
    .isEmail().withMessage('A valid email address is required')
    .normalizeEmail(),
  body('password')
    .optional({ checkFalsy: true })
    .matches(PASSWORD_REGEX).withMessage(PASSWORD_MSG),
  handleValidationErrors,
];

module.exports = {
  registerValidation,
  loginValidation,
  noteValidation,
  ratingValidation,
  passwordResetValidation,
  forgotPasswordValidation,
  profileValidation,
  handleValidationErrors,
};
