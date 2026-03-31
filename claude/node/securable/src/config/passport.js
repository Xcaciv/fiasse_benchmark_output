'use strict';

/**
 * Passport.js local strategy configuration.
 * SSEM Authenticity: credentials verified against stored bcrypt hash;
 * password value is never logged or forwarded.
 */

const passport = require('passport');
const LocalStrategy = require('passport-local').Strategy;
const bcrypt = require('bcryptjs');
const { User } = require('../models');
const logger = require('./logger');

passport.use(
  new LocalStrategy(
    { usernameField: 'username', passwordField: 'password' },
    async (username, password, done) => {
      try {
        // Canonicalise: trim and lowercase username at trust boundary
        const canonical = username.trim().toLowerCase();
        const user = await User.findOne({ where: { username: canonical } });

        if (!user) {
          logger.audit('LOGIN_FAILURE', { reason: 'USER_NOT_FOUND', username: canonical });
          return done(null, false, { message: 'Invalid username or password.' });
        }

        const match = await bcrypt.compare(password, user.passwordHash);
        if (!match) {
          logger.audit('LOGIN_FAILURE', { reason: 'BAD_PASSWORD', userId: user.id });
          return done(null, false, { message: 'Invalid username or password.' });
        }

        if (!user.isActive) {
          logger.audit('LOGIN_FAILURE', { reason: 'ACCOUNT_INACTIVE', userId: user.id });
          return done(null, false, { message: 'Account is disabled.' });
        }

        logger.audit('LOGIN_SUCCESS', { userId: user.id });
        return done(null, user);
      } catch (err) {
        logger.error({ event: 'LOGIN_ERROR', message: err.message });
        return done(err);
      }
    }
  )
);

passport.serializeUser((user, done) => {
  done(null, user.id);
});

passport.deserializeUser(async (id, done) => {
  try {
    const user = await User.findByPk(id, {
      attributes: ['id', 'username', 'email', 'role', 'isActive'],
    });
    if (!user || !user.isActive) {
      return done(null, false);
    }
    done(null, user);
  } catch (err) {
    done(err);
  }
});

module.exports = passport;
