'use strict';

const path = require('node:path');

const { createLogger: createWinstonLogger, format, transports } = require('winston');

const { dataDirectories } = require('./database');

function createLogger() {
  return createWinstonLogger({
    level: 'info',
    format: format.combine(format.timestamp(), format.json()),
    transports: [
      new transports.File({
        filename: path.join(dataDirectories.logs, 'application.log')
      }),
      new transports.Console({
        format: format.combine(format.colorize(), format.simple())
      })
    ]
  });
}

module.exports = { createLogger };
