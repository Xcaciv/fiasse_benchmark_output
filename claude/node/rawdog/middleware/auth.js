const LocalStrategy = require('passport-local').Strategy;
const bcrypt = require('bcryptjs');
const { getDb } = require('../database/db');

function configurePassport(passport) {
  passport.use(new LocalStrategy(
    { usernameField: 'username' },
    (username, password, done) => {
      const db = getDb();
      const user = db.prepare(
        'SELECT * FROM users WHERE username = ? OR email = ?'
      ).get(username, username);

      if (!user) {
        return done(null, false, { message: 'Invalid username or password.' });
      }

      if (!bcrypt.compareSync(password, user.password_hash)) {
        return done(null, false, { message: 'Invalid username or password.' });
      }

      return done(null, user);
    }
  ));

  passport.serializeUser((user, done) => {
    done(null, user.id);
  });

  passport.deserializeUser((id, done) => {
    const db = getDb();
    const user = db.prepare('SELECT * FROM users WHERE id = ?').get(id);
    done(null, user || false);
  });
}

function ensureAuthenticated(req, res, next) {
  if (req.isAuthenticated()) {
    return next();
  }
  req.flash('error', 'Please log in to access this page.');
  res.redirect('/auth/login');
}

function ensureAdmin(req, res, next) {
  if (req.isAuthenticated() && req.user.role === 'admin') {
    return next();
  }
  res.status(403).render('error', {
    title: 'Forbidden',
    message: 'Admin access required.',
    status: 403
  });
}

module.exports = { configurePassport, ensureAuthenticated, ensureAdmin };
