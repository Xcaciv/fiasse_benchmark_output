'use strict';

const path = require('path');
const fs = require('fs');
const { v4: uuidv4 } = require('uuid');
const multer = require('multer');
const security = require('../config/security');
const logger = require('../config/logger');

/**
 * File service — attachment upload and deletion.
 * SSEM Integrity: file type validated by extension AND mime-type (double check).
 * Stored filenames are UUID-based to prevent path traversal and filename collisions.
 * Original filename stored in metadata only.
 */

function getExtension(filename) {
  return path.extname(filename).toLowerCase().replace('.', '');
}

const storage = multer.diskStorage({
  destination(_req, _file, cb) {
    const dir = path.resolve(security.uploadDir);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
    cb(null, dir);
  },
  filename(_req, file, cb) {
    const ext = getExtension(file.originalname);
    cb(null, `${uuidv4()}.${ext}`);
  },
});

function fileFilter(_req, file, cb) {
  const ext = getExtension(file.originalname);
  const mimeOk = security.allowedMimeTypes.has(file.mimetype);
  const extOk = security.allowedExtensions.has(ext);

  if (!mimeOk || !extOk) {
    logger.warn({
      event: 'UPLOAD_REJECTED',
      mimetype: file.mimetype,
      ext,
      originalname: file.originalname,
    });
    return cb(new Error(`File type not allowed. Allowed: ${[...security.allowedExtensions].join(', ')}`));
  }
  cb(null, true);
}

const upload = multer({
  storage,
  fileFilter,
  limits: { fileSize: security.uploadMaxSizeBytes },
});

async function deleteFile(storedFilename) {
  const filePath = path.join(path.resolve(security.uploadDir), storedFilename);
  // Ensure path stays within upload directory (prevent traversal)
  const resolvedUploadDir = path.resolve(security.uploadDir);
  const resolvedFilePath = path.resolve(filePath);
  if (!resolvedFilePath.startsWith(resolvedUploadDir + path.sep)) {
    logger.error({ event: 'PATH_TRAVERSAL_ATTEMPT', storedFilename });
    throw new Error('Invalid file path.');
  }
  try {
    await fs.promises.unlink(resolvedFilePath);
  } catch (err) {
    if (err.code !== 'ENOENT') {
      throw err;
    }
    // File already gone — not an error
  }
}

function getFilePath(storedFilename) {
  const resolvedUploadDir = path.resolve(security.uploadDir);
  const resolved = path.resolve(resolvedUploadDir, storedFilename);
  if (!resolved.startsWith(resolvedUploadDir + path.sep)) {
    const err = new Error('Invalid file path.');
    err.status = 400;
    throw err;
  }
  return resolved;
}

module.exports = { upload, deleteFile, getFilePath };
