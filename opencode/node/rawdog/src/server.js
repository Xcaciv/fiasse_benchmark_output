const express = require('express');
const session = require('express-session');
const flash = require('connect-flash');
const path = require('path');
const fs = require('fs');

const { initializeDatabase } = require('./db/database');

const authRoutes = require('./routes/auth');
const notesRoutes = require('./routes/notes');
const searchRoutes = require('./routes/search');
const adminRoutes = require('./routes/admin');

const app = express();

initializeDatabase();

app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

app.use(express.urlencoded({ extended: true }));
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

app.use(session({
  secret: process.env.SESSION_SECRET || 'loose-notes-secret-key-change-in-production',
  resave: false,
  saveUninitialized: false,
  cookie: { 
    maxAge: 24 * 60 * 60 * 1000,
    httpOnly: true
  }
}));

app.use(flash());

app.use((req, res, next) => {
  res.locals.user = req.session.user || null;
  res.locals.success = req.flash('success');
  res.locals.error = req.flash('error');
  res.locals.resetToken = req.flash('resetToken')[0] || null;
  res.locals.resetEmail = req.flash('resetEmail')[0] || null;
  next();
});

app.use('/', authRoutes);
app.use('/notes', notesRoutes);
app.use('/search', searchRoutes);
app.use('/admin', adminRoutes);

app.get('/', (req, res) => {
  if (req.session.user) {
    return res.redirect('/notes');
  }
  res.render('home', { user: null });
});

app.use('/uploads', express.static(path.join(__dirname, '../uploads')));

app.use((req, res) => {
  res.status(404).render('error', { message: 'Page not found', user: req.session?.user || null });
});

app.use((err, req, res, next) => {
  console.error('Error:', err);
  res.status(500).render('error', { 
    message: process.env.NODE_ENV === 'production' ? 'An error occurred' : err.message,
    user: req.session?.user || null
  });
});

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`Loose Notes app running at http://localhost:${PORT}`);
});

module.exports = app;
