require('dotenv').config();
const express = require('express');
const session = require('express-session');
const passport = require('passport');
const flash = require('connect-flash');
const path = require('path');
const fs = require('fs');

const { initDb } = require('./database/db');
const { configurePassport } = require('./middleware/auth');
const authRoutes = require('./routes/auth');
const notesRoutes = require('./routes/notes');
const adminRoutes = require('./routes/admin');
const profileRoutes = require('./routes/profile');
const shareRoutes = require('./routes/share');

const app = express();
const PORT = process.env.PORT || 3000;

// Ensure uploads directory exists
const uploadsDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}

// Initialize database
initDb();

// Configure passport
configurePassport(passport);

// View engine
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

// Middleware
app.use(express.urlencoded({ extended: true }));
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));
app.use('/uploads', express.static(uploadsDir));

app.use(session({
  secret: process.env.SESSION_SECRET || 'loose-notes-secret-key',
  resave: false,
  saveUninitialized: false,
  cookie: { maxAge: 24 * 60 * 60 * 1000 }
}));

app.use(passport.initialize());
app.use(passport.session());
app.use(flash());

// Global template variables
app.use((req, res, next) => {
  res.locals.user = req.user || null;
  res.locals.success = req.flash('success');
  res.locals.error = req.flash('error');
  next();
});

// Routes
app.use('/auth', authRoutes);
app.use('/notes', notesRoutes);
app.use('/admin', adminRoutes);
app.use('/profile', profileRoutes);
app.use('/share', shareRoutes);

// Home route
app.get('/', (req, res) => {
  if (req.isAuthenticated()) {
    return res.redirect('/notes');
  }
  res.render('index', { title: 'Welcome to Loose Notes' });
});

// 404 handler
app.use((req, res) => {
  res.status(404).render('error', {
    title: 'Page Not Found',
    message: 'The page you requested could not be found.',
    status: 404
  });
});

// Error handler
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).render('error', {
    title: 'Server Error',
    message: 'An internal server error occurred.',
    status: 500
  });
});

app.listen(PORT, () => {
  console.log(`Loose Notes running on http://localhost:${PORT}`);
});

module.exports = app;
