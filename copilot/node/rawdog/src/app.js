const path = require('path');
const express = require('express');
const cookieSession = require('cookie-session');
const expressLayouts = require('express-ejs-layouts');
const helmet = require('helmet');
const { config, ensureDirectories } = require('./config');
const { initDatabase } = require('./db');
const { attachCurrentUser } = require('./middleware/auth');
const { flashMiddleware } = require('./middleware/flash');
const { populateLocals } = require('./middleware/locals');
const { csrfProtection } = require('./middleware/csrf');
const { errorHandler } = require('./middleware/error-handler');
const homeRoutes = require('./routes/home');
const authRoutes = require('./routes/auth');
const notesRoutes = require('./routes/notes');
const profileRoutes = require('./routes/profile');
const adminRoutes = require('./routes/admin');

async function createApp() {
  ensureDirectories();
  await initDatabase();

  const app = express();
  const useSecureCookies =
    process.env.NODE_ENV === 'production' && config.baseUrl.startsWith('https://');

  app.set('view engine', 'ejs');
  app.set('views', path.join(__dirname, 'views'));
  app.set('layout', 'layout');

  app.use(
    helmet({
      contentSecurityPolicy: false
    })
  );
  app.use(expressLayouts);
  app.use(express.urlencoded({ extended: true }));
  app.use(express.json());
  app.use(
    cookieSession({
      name: 'loose-notes.sid',
      keys: [config.sessionSecret],
      httpOnly: true,
      sameSite: 'lax',
      secure: useSecureCookies,
      maxAge: 8 * 60 * 60 * 1000
    })
  );
  app.use(express.static(path.join(config.rootDir, 'public')));
  app.use(attachCurrentUser);
  app.use(flashMiddleware);
  app.use(populateLocals);
  app.use(csrfProtection);

  app.use('/', homeRoutes);
  app.use('/auth', authRoutes);
  app.use('/', notesRoutes);
  app.use('/profile', profileRoutes);
  app.use('/admin', adminRoutes);

  app.use((req, res) => {
    res.status(404).render('error', {
      pageTitle: 'Not found',
      errorMessage: 'The page you requested could not be found.'
    });
  });

  app.use(errorHandler);

  return app;
}

module.exports = {
  createApp
};
