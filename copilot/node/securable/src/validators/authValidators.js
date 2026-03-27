'use strict';

const { body, validationResult } = require('express-validator');

const registerValidator = [
  body('username')
    .trim()
    .isLength({ min: 3, max: 50 }).withMessage('Username must be 3–50 characters.')
    .matches(/^[a-zA-Z0-9_]+$/).withMessage('Username may only contain letters, numbers, and underscores.'),
  body('email')
    .trim()
    .isEmail().withMessage('Please provide a valid email address.')
    .normalizeEmail(),
  body('password')
    .trim()
    .isLength({ min: 8, max: 128 }).withMessage('Password must be 8–128 characters.'),
];

const loginValidator = [
  body('username').trim().notEmpty().withMessage('Username or email is required.'),
  body('password').trim().notEmpty().withMessage('Password is required.'),
];

const forgotPasswordValidator = [
  body('email')
    .trim()
    .isEmail().withMessage('Please provide a valid email address.')
    .normalizeEmail(),
];

const resetPasswordValidator = [
  body('password')
    .trim()
    .isLength({ min: 8, max: 128 }).withMessage('Password must be 8–128 characters.'),
  body('token')
    .notEmpty().withMessage('Reset token is required.')
    .isHexadecimal().withMessage('Invalid reset token.')
    .isLength({ min: 64, max: 64 }).withMessage('Invalid reset token length.'),
];

function handleValidationErrors(req, res, view, locals = {}) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    const messages = errors.array().map((e) => e.msg);
    req.flash('error', messages.join(' '));
    return res.redirect('back');
  }
  return null;
}

module.exports = {
  registerValidator,
  loginValidator,
  forgotPasswordValidator,
  resetPasswordValidator,
  handleValidationErrors,
};
