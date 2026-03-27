const LocalStrategy = require('passport-local').Strategy;
const bcrypt = require('bcryptjs');
const { Op } = require('sequelize');
const db = require('../models');

module.exports = function(passport) {
  passport.use(new LocalStrategy(
    { usernameField: 'username', passwordField: 'password' },
    async (username, password, done) => {
      try {
        // Allow login with username or email (safe parameterized query)
        const user = await db.User.findOne({
          where: {
            [Op.or]: [
              { username: username },
              { email: username }
            ]
          }
        });

        if (!user) {
          return done(null, false, { message: 'Incorrect username or password.' });
        }

        const isMatch = await bcrypt.compare(password, user.password);
        if (!isMatch) {
          return done(null, false, { message: 'Incorrect username or password.' });
        }

        return done(null, user);
      } catch (err) {
        return done(err);
      }
    }
  ));

  passport.serializeUser((user, done) => {
    done(null, user.id);
  });

  passport.deserializeUser(async (id, done) => {
    try {
      const user = await db.User.findByPk(id);
      done(null, user);
    } catch (err) {
      done(err);
    }
  });
};
