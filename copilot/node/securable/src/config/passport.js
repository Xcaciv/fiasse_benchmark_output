'use strict';

const LocalStrategy = require('passport-local').Strategy;
const bcrypt = require('bcrypt');
const { User } = require('../models');
const { Op } = require('sequelize');
const { logActivity } = require('../services/auditService');
const logger = require('../utils/logger');

function configurePassport(passport) {
  passport.use(new LocalStrategy(
    { usernameField: 'username', passwordField: 'password' },
    async (usernameOrEmail, password, done) => {
      try {
        const user = await User.scope('withAuth').findOne({
          where: {
            [Op.or]: [
              { username: usernameOrEmail },
              { email: usernameOrEmail.toLowerCase() },
            ],
          },
        });

        if (!user) {
          return done(null, false, { message: 'Invalid credentials' });
        }

        const match = await bcrypt.compare(password, user.passwordHash);
        if (!match) {
          await logActivity({
            userId: user.id,
            action: 'user.login.failure',
            details: { reason: 'bad_password' },
          });
          return done(null, false, { message: 'Invalid credentials' });
        }

        await logActivity({ userId: user.id, action: 'user.login.success' });
        return done(null, user);
      } catch (err) {
        logger.error('Passport strategy error', { error: err.message });
        return done(err);
      }
    }
  ));

  passport.serializeUser((user, done) => done(null, user.id));

  passport.deserializeUser(async (id, done) => {
    try {
      const user = await User.findByPk(id);
      done(null, user || false);
    } catch (err) {
      done(err);
    }
  });
}

module.exports = configurePassport;
