// CSRF is handled globally in app.js via csurf middleware.
// This file exists for compatibility only.

function noop(req, res, next) {
  next();
}

module.exports = { initCsrf: noop, verifyCsrf: noop };
