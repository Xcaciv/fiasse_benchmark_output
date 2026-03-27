function isAuthenticated(req, res, next) {
  if (req.isAuthenticated()) {
    return next();
  }
  req.flash('error', 'Please log in to access that page.');
  res.redirect('/auth/login');
}

function isAdmin(req, res, next) {
  if (req.isAuthenticated() && req.user && req.user.isAdmin) {
    return next();
  }
  if (!req.isAuthenticated()) {
    req.flash('error', 'Please log in to access that page.');
    return res.redirect('/auth/login');
  }
  res.status(403).render('error', {
    title: 'Access Denied',
    statusCode: 403,
    message: 'You do not have permission to access this page.'
  });
}

module.exports = { isAuthenticated, isAdmin };
