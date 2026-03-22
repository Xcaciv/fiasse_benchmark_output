const { createApp } = require('./app');
const { config } = require('./config');

createApp()
  .then((app) => {
    app.listen(config.port, () => {
      console.log(`Loose Notes is running at ${config.baseUrl}`);
      console.log(`Default admin username: ${config.defaultAdmin.username}`);
      console.log('Default admin password is configured through DEFAULT_ADMIN_PASSWORD.');
    });
  })
  .catch((error) => {
    console.error('Failed to start Loose Notes.');
    console.error(error);
    process.exit(1);
  });
