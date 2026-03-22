'use strict';
const LocalStrategy = require('passport-local').Strategy;
const bcrypt = require('bcryptjs');

// Authenticity: Passport local strategy with injected User model for testability
function configurePassport(passport, User) {
  passport.use(new LocalStrategy(
    { usernameField: 'username', passwordField: 'password' },
    async (username, password, done) => {
      try {
        // Trust boundary: canonicalize username before lookup
        const normalizedUsername = username.trim().toLowerCase();
        const user = await User.findOne({ where: { username: normalizedUsername } });
        if (!user) {
          return done(null, false, { message: 'Invalid credentials' });
        }
        const isMatch = await bcrypt.compare(password, user.passwordHash);
        if (!isMatch) {
          return done(null, false, { message: 'Invalid credentials' });
        }
        return done(null, user);
      } catch (err) {
        return done(err);
      }
    }
  ));

  passport.serializeUser((user, done) => done(null, user.id));

  passport.deserializeUser(async (id, done) => {
    try {
      const user = await User.findByPk(id);
      done(null, user);
    } catch (err) {
      done(err);
    }
  });
}

module.exports = { configurePassport };
