'use strict';

const { validationResult } = require('express-validator');

// [TRUST BOUNDARY] Validation middleware — Integrity pillar
// Collects express-validator errors and returns them uniformly.
// Used after rule arrays defined in route files.
function handleValidation(req, res, next) {
  const errors = validationResult(req);
  if (errors.isEmpty()) {
    return next();
  }

  const messages = errors.array().map((e) => e.msg);

  if (req.accepts('html')) {
    req.flash('error', messages[0]);
    return res.redirect('back');
  }

  return res.status(422).json({ errors: messages });
}

module.exports = { handleValidation };
