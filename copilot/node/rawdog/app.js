require('dotenv').config();
const express = require('express');
const session = require('express-session');
const flash = require('connect-flash');
const methodOverride = require('method-override');
const path = require('path');
const expressLayouts = require('express-ejs-layouts');
const bcrypt = require('bcryptjs');

const { sequelize, User, ActivityLog } = require('./models');

const authRoutes = require('./routes/auth');
const notesRoutes = require('./routes/notes');
const adminRoutes = require('./routes/admin');
const profileRoutes = require('./routes/profile');
const shareRoutes = require('./routes/share');

const app = express();
const PORT = process.env.PORT || 3000;

// View engine
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));
app.use(expressLayouts);
app.set('layout', 'layout');

// Static files
app.use(express.static(path.join(__dirname, 'public')));

// Body parsing
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Method override (_method query param)
app.use(methodOverride('_method'));

// Session
app.use(session({
  secret: process.env.SESSION_SECRET || 'loose-notes-secret-key',
  resave: false,
  saveUninitialized: false,
  cookie: { maxAge: 24 * 60 * 60 * 1000 }, // 1 day
}));

// Flash
app.use(flash());

// Locals middleware
app.use((req, res, next) => {
  // Flash messages
  const successMsg = req.flash('success');
  const errorMsg = req.flash('error');
  const infoMsg = req.flash('info');
  res.locals.flash = {
    success: successMsg.length ? successMsg[0] : null,
    error: errorMsg.length ? errorMsg[0] : null,
    info: infoMsg.length ? infoMsg[0] : null,
  };

  // Current user info for navbar
  if (req.session.userId) {
    res.locals.currentUser = {
      id: req.session.userId,
      username: req.session.username,
      role: req.session.role,
    };
  } else {
    res.locals.currentUser = null;
  }

  next();
});

// Routes
app.use('/auth', authRoutes);
app.use('/notes', notesRoutes);
app.use('/admin', adminRoutes);
app.use('/profile', profileRoutes);
app.use('/share', shareRoutes);

// Root redirect
app.get('/', (req, res) => {
  if (req.session.userId) return res.redirect('/notes');
  res.redirect('/auth/login');
});

// 404 handler
app.use((req, res) => {
  res.status(404).render('error', { title: 'Not Found', message: 'Page not found.', user: res.locals.currentUser });
});

// Error handler
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).render('error', { title: 'Server Error', message: 'Something went wrong.', user: res.locals.currentUser });
});

// Initialize DB and start server
async function start() {
  try {
    await sequelize.authenticate();
    await sequelize.sync({ force: false });
    console.log('Database synced.');

    // Seed admin user if no users exist
    const count = await User.count();
    if (count === 0) {
      const hashed = await bcrypt.hash('Admin1234!', 12);
      await User.create({
        username: 'admin',
        email: 'admin@example.com',
        password: hashed,
        role: 'admin',
      });
      await ActivityLog.create({ userId: null, action: 'seed', details: 'Admin user seeded on first run.' });
      console.log('Admin user created: admin / Admin1234!');
    }

    app.listen(PORT, () => {
      console.log(`Loose Notes running at http://localhost:${PORT}`);
    });
  } catch (err) {
    console.error('Startup error:', err);
    process.exit(1);
  }
}

start();
