'use strict';

const passport = require('passport');
const LocalStrategy = require('passport-local').Strategy;
const bcrypt = require('bcrypt');
const { User } = require('../models');
const logger = require('./logger');

// [TRUST BOUNDARY] Authentication strategy — Authenticity pillar
// Credentials validated here; session only stores non-sensitive user ID
function configurePassport() {
  passport.use(new LocalStrategy(
    { usernameField: 'username', passwordField: 'password' },
    async (username, password, done) => {
      try {
        // Canonicalize username to prevent case-variation bypasses
        const normalizedUsername = username.trim().toLowerCase();
        const user = await User.findOne({ where: { username: normalizedUsername } });

        if (!user) {
          // Uniform timing response to prevent username enumeration
          await bcrypt.compare(password, '$2b$12$invalidhashfortimingequaliz');
          return done(null, false, { message: 'Invalid credentials' });
        }

        const passwordMatch = await bcrypt.compare(password, user.passwordHash);
        if (!passwordMatch) {
          return done(null, false, { message: 'Invalid credentials' });
        }

        if (user.isLocked) {
          return done(null, false, { message: 'Account locked. Contact support.' });
        }

        return done(null, user);
      } catch (err) {
        // Do not expose internal error detail to strategy consumer
        logger.error('Passport strategy error', { error: err.message });
        return done(err);
      }
    }
  ));

  // Session stores only user ID — Confidentiality: no sensitive data in session
  passport.serializeUser((user, done) => done(null, user.id));

  passport.deserializeUser(async (id, done) => {
    try {
      const user = await User.findByPk(id, {
        attributes: ['id', 'username', 'email', 'role', 'isLocked'],
      });
      done(null, user || false);
    } catch (err) {
      logger.error('Session deserialization error', { error: err.message });
      done(err);
    }
  });
}

module.exports = { configurePassport };
