'use strict';
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const { v4: uuidv4 } = require('uuid');
const config = require('../config');

// Ensure uploads directory exists
fs.mkdirSync(config.uploadsPath, { recursive: true });

// Use memory storage so we can validate before writing to disk
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: config.maxFileSize },
});

function checkMagicBytes(buffer, expectedBytes) {
  if (!expectedBytes) return true; // text/plain has no magic bytes
  for (let i = 0; i < expectedBytes.length; i++) {
    if (buffer[i] !== expectedBytes[i]) return false;
  }
  return true;
}

function detectMimeType(buffer, ext) {
  for (const [mimeType, magicBytes] of Object.entries(config.allowedMimeTypes)) {
    if (magicBytes === null) continue; // skip text/plain for magic check
    if (checkMagicBytes(buffer, magicBytes)) {
      return mimeType;
    }
  }
  // Fall back to extension-based detection for text/plain
  if (ext === '.txt') return 'text/plain';
  return null;
}

function validateAndStoreFile(buffer, originalFilename) {
  const ext = path.extname(originalFilename).toLowerCase();

  if (!config.allowedExtensions.includes(ext)) {
    throw new Error(`File extension not allowed: ${ext}`);
  }

  const detectedMime = detectMimeType(buffer, ext);
  if (!detectedMime) {
    throw new Error('File type could not be verified or is not allowed');
  }

  if (config.forbiddenMimeTypes.includes(detectedMime)) {
    throw new Error('Archive files are not allowed');
  }

  if (!config.allowedMimeTypes.hasOwnProperty(detectedMime)) {
    throw new Error('File type is not permitted');
  }

  const storedFilename = uuidv4() + ext;
  const filePath = path.join(config.uploadsPath, storedFilename);
  fs.writeFileSync(filePath, buffer);

  return {
    storedFilename,
    mimeType: detectedMime,
    fileSize: buffer.length,
  };
}

function deleteFile(storedFilename) {
  const filePath = path.join(config.uploadsPath, storedFilename);
  try {
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
    }
  } catch (err) {
    // Log but don't throw — deletion failures should not block the request
    console.error('Failed to delete file:', storedFilename, err.message);
  }
}

function getFilePath(storedFilename) {
  return path.join(config.uploadsPath, storedFilename);
}

module.exports = { upload, validateAndStoreFile, deleteFile, getFilePath };
