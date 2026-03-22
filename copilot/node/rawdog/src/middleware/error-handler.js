const { logActivity } = require('../lib/activity');

async function errorHandler(error, req, res, next) {
  console.error(error);

  try {
    await logActivity(
      req.currentUser?.id || null,
      'application_error',
      `${error.message}\n${error.stack || ''}`.trim()
    );
  } catch (loggingError) {
    console.error(loggingError);
  }

  const statusCode = error.statusCode || 500;
  res.status(statusCode).render('error', {
    pageTitle: statusCode === 404 ? 'Not found' : 'Something went wrong',
    errorMessage:
      statusCode === 404
        ? error.message || 'The requested page could not be found.'
        : 'The application encountered an unexpected error.'
  });
}

module.exports = {
  errorHandler
};
