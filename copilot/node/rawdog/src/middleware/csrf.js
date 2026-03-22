const crypto = require('crypto');
const { config } = require('../config');

const csrfCookieName = 'loose-notes.csrf';

function parseCookies(cookieHeader = '') {
  return cookieHeader
    .split(';')
    .map((pair) => pair.trim())
    .filter(Boolean)
    .reduce((cookies, pair) => {
      const separatorIndex = pair.indexOf('=');

      if (separatorIndex === -1) {
        return cookies;
      }

      const name = pair.slice(0, separatorIndex).trim();
      const value = pair.slice(separatorIndex + 1).trim();
      cookies[name] = decodeURIComponent(value);
      return cookies;
    }, {});
}

function csrfProtection(req, res, next) {
  const useSecureCookies =
    process.env.NODE_ENV === 'production' && config.baseUrl.startsWith('https://');
  const cookies = parseCookies(req.headers.cookie);
  let csrfToken = cookies[csrfCookieName];

  if (!csrfToken) {
    csrfToken = crypto.randomBytes(32).toString('hex');
    res.cookie(csrfCookieName, csrfToken, {
      httpOnly: false,
      sameSite: 'lax',
      secure: useSecureCookies
    });
  }

  res.locals.csrfToken = csrfToken;

  if (req.method === 'GET' || req.method === 'HEAD' || req.method === 'OPTIONS') {
    next();
    return;
  }

  const submittedToken =
    req.body?.csrfToken || req.query?.csrfToken || req.headers['x-csrf-token'];

  if (submittedToken && submittedToken === csrfToken) {
    next();
    return;
  }

  res.status(403).render('error', {
    pageTitle: 'Invalid request',
    errorMessage: 'This form has expired or failed validation. Please refresh and try again.'
  });
}

module.exports = {
  csrfProtection
};
