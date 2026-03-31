'use strict';

const { validationResult } = require('express-validator');

/**
 * Validation result handler.
 * Converts express-validator errors into flash messages and redirects back.
 * Trust boundary: this is the last gate before controller logic receives input.
 */
function handleValidationErrors(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    const messages = errors.array().map((e) => e.msg);
    req.flash('error', messages);
    return res.redirect('back');
  }
  next();
}

/**
 * Same as handleValidationErrors but renders a view instead of redirecting.
 * Use this for forms with complex state that can't be reconstructed on redirect.
 */
function handleValidationErrorsRender(view, locals = {}) {
  return (req, res, next) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(422).render(view, {
        ...locals,
        errors: errors.array(),
        body: req.body,
      });
    }
    next();
  };
}

module.exports = { handleValidationErrors, handleValidationErrorsRender };
