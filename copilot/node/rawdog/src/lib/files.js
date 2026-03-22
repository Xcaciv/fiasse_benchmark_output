const crypto = require('crypto');
const fs = require('fs/promises');
const path = require('path');
const multer = require('multer');
const { config } = require('../config');

const storage = multer.diskStorage({
  destination: function destination(req, file, cb) {
    cb(null, config.uploadDir);
  },
  filename: function filename(req, file, cb) {
    const extension = path.extname(file.originalname).toLowerCase();
    cb(null, `${crypto.randomBytes(16).toString('hex')}${extension}`);
  }
});

const upload = multer({
  storage,
  limits: {
    fileSize: config.maxFileSizeMb * 1024 * 1024
  },
  fileFilter: function fileFilter(req, file, cb) {
    const extension = path.extname(file.originalname).toLowerCase();

    if (!config.allowedExtensions.includes(extension)) {
      cb(new Error(`Unsupported file type: ${extension || 'unknown'}.`));
      return;
    }

    cb(null, true);
  }
});

function attachmentUploadMiddleware(req, res, next) {
  upload.array('attachments', 5)(req, res, function uploadCompleted(error) {
    if (error) {
      req.uploadError =
        error.code === 'LIMIT_FILE_SIZE'
          ? `Each file must be ${config.maxFileSizeMb} MB or smaller.`
          : error.message;
    }

    next();
  });
}

async function deleteStoredFiles(attachments) {
  await Promise.all(
    attachments.map(async (attachment) => {
      try {
        await fs.unlink(path.join(config.uploadDir, attachment.stored_filename));
      } catch (error) {
        if (error.code !== 'ENOENT') {
          throw error;
        }
      }
    })
  );
}

module.exports = {
  attachmentUploadMiddleware,
  deleteStoredFiles
};
