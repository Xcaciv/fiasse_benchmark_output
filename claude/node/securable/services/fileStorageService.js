'use strict';

const path = require('path');
const fs = require('fs');
const { v4: uuidv4 } = require('uuid');
const security = require('../config/security');
const logger = require('../config/logger');

const uploadDir = path.resolve(process.env.UPLOAD_DIR || './uploads');

// Ensure upload directory exists at startup
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

// [TRUST BOUNDARY] File storage service — Integrity + Confidentiality
// storedFilename is always UUID-based; originalFilename is display-only.

function validateFileType(mimetype, originalname) {
  const ext = path.extname(originalname).toLowerCase();
  if (!security.ALLOWED_EXTENSIONS.has(ext)) {
    return { valid: false, reason: `File extension '${ext}' is not allowed.` };
  }
  if (!security.ALLOWED_MIME_TYPES.has(mimetype)) {
    return { valid: false, reason: `MIME type '${mimetype}' is not allowed.` };
  }
  return { valid: true };
}

function buildStoredFilename(originalname) {
  const ext = path.extname(originalname).toLowerCase();
  return `${uuidv4()}${ext}`;
}

// Resolve the absolute path on disk — never use originalFilename in path operations
function resolveStoragePath(storedFilename) {
  // Path traversal guard: storedFilename is UUID+ext, contains no slashes
  const resolved = path.resolve(uploadDir, storedFilename);
  if (!resolved.startsWith(uploadDir + path.sep) && resolved !== uploadDir) {
    throw new Error('Invalid stored filename: path traversal detected');
  }
  return resolved;
}

async function deleteFile(storedFilename) {
  try {
    const filePath = resolveStoragePath(storedFilename);
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
    }
  } catch (err) {
    logger.error('Failed to delete file', { storedFilename, error: err.message });
  }
}

module.exports = {
  validateFileType,
  buildStoredFilename,
  resolveStoragePath,
  deleteFile,
  uploadDir,
};
