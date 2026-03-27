'use strict';

function registerRoutes(app, db) {
  require('./authRoutes')(app, db);
  require('./noteRoutes')(app, db);
  require('./attachmentRoutes')(app, db);
  require('./ratingRoutes')(app, db);
  require('./shareRoutes')(app, db);
  require('./searchRoutes')(app, db);
  require('./profileRoutes')(app, db);
  require('./adminRoutes')(app, db);
  require('./publicRoutes')(app, db);
}

module.exports = registerRoutes;
