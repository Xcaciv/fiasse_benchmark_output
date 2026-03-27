function isAuthenticated(req, res, next) {
  if (req.session && req.session.userId) {
    return next();
  }
  req.flash('error', 'Please log in to access this page.');
  res.redirect('/auth/login');
}

function isAdmin(req, res, next) {
  if (req.session && req.session.role === 'admin') {
    return next();
  }
  res.status(403).render('error', { title: 'Forbidden', message: 'Admin access required.', user: req.session.user || null });
}

function isOwner(model) {
  return async (req, res, next) => {
    try {
      const record = await model.findByPk(req.params.id);
      if (!record) return res.status(404).render('error', { title: 'Not Found', message: 'Resource not found.', user: req.session.user || null });
      if (record.userId !== req.session.userId && req.session.role !== 'admin') {
        return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.', user: req.session.user || null });
      }
      req.record = record;
      next();
    } catch (err) {
      next(err);
    }
  };
}

module.exports = { isAuthenticated, isAdmin, isOwner };
