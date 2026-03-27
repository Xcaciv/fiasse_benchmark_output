'use strict';

const fs = require('fs');
const path = require('path');
const { v4: uuidv4 } = require('uuid');
const fileType = require('file-type');
const { Attachment, Note } = require('../models');
const constants = require('../config/constants');
const { logger } = require('../config/logger');
const auditService = require('./auditService');

// Upload directory resolved at startup, never from user input
const UPLOAD_DIR = path.resolve(
  process.env.UPLOAD_PATH || path.join(__dirname, '..', 'uploads')
);

/**
 * Validate uploaded file by extension allowlist AND magic-byte MIME detection.
 * Both checks must pass (defense in depth).
 * @param {{ originalname: string, buffer: Buffer, mimetype: string }} file
 * @returns {Promise<{ valid: boolean, message: string, detectedMime: string|null }>}
 */
async function validateFile(file) {
  const ext = path.extname(file.originalname).toLowerCase();

  if (!constants.FILES.ALLOWED_EXTENSIONS.includes(ext)) {
    return { valid: false, message: `File extension '${ext}' is not allowed`, detectedMime: null };
  }

  // Trust boundary: use file-type library for magic byte detection, not client-supplied header
  const detected = await fileType.fromBuffer(file.buffer);
  const detectedMime = detected ? detected.mime : 'text/plain';

  // text/plain files have no magic bytes - allow only if extension matches
  const isTextFile = ext === '.txt';
  if (!isTextFile && !constants.FILES.ALLOWED_MIME_TYPES.includes(detectedMime)) {
    return {
      valid: false,
      message: `Detected file type '${detectedMime}' does not match allowed types`,
      detectedMime
    };
  }

  if (file.size > constants.FILES.MAX_SIZE_BYTES) {
    return {
      valid: false,
      message: `File size exceeds ${process.env.MAX_FILE_SIZE_MB || 10}MB limit`,
      detectedMime
    };
  }

  return { valid: true, message: '', detectedMime: detectedMime || 'text/plain' };
}

/**
 * Store validated file to uploads directory with UUID filename.
 * Path traversal prevented by UUID-based filename.
 * @param {{ originalname: string, buffer: Buffer, size: number }} file
 * @param {string} noteId
 * @param {string} detectedMime
 * @param {string} requestingUserId
 * @param {string} [correlationId]
 * @returns {Promise<Attachment>}
 */
async function storeFile(file, noteId, detectedMime, requestingUserId, correlationId) {
  const ext = path.extname(file.originalname).toLowerCase();
  // storedFilename is UUID-based - never derived from user input
  const storedFilename = `${uuidv4()}${ext}`;
  const storagePath = path.join(UPLOAD_DIR, storedFilename);

  await fs.promises.writeFile(storagePath, file.buffer);

  const attachment = await Attachment.create({
    noteId,
    originalFilename: path.basename(file.originalname),
    storedFilename,
    mimeType: detectedMime,
    size: file.size
  });

  await auditService.log('attachment.uploaded', {
    actorId: requestingUserId,
    targetId: attachment.id,
    targetType: 'attachment',
    outcome: 'success',
    metadata: { noteId, mimeType: detectedMime, size: file.size },
    correlationId
  });

  return attachment;
}

/**
 * Retrieve attachment record after verifying requester owns the parent note.
 * @param {string} fileId - Attachment UUID
 * @param {string} requestingUserId
 * @returns {Promise<Attachment|null>}
 */
async function getFile(fileId, requestingUserId) {
  const attachment = await Attachment.findByPk(fileId, {
    include: [{ model: Note, as: 'note' }]
  });

  if (!attachment) return null;

  // Integrity: ownership enforced at service layer, not just route
  if (attachment.note.ownerId !== requestingUserId) return null;

  return attachment;
}

/**
 * Build the safe absolute file path for an attachment (no user input in path).
 * @param {string} storedFilename
 * @returns {string}
 */
function buildFilePath(storedFilename) {
  // Prevent path traversal: use only the basename, never the raw value
  const safe = path.basename(storedFilename);
  return path.join(UPLOAD_DIR, safe);
}

/**
 * Delete an attachment file from disk.
 * Called during cascade delete; errors are logged, not rethrown.
 * @param {string} storedFilename
 */
async function deleteFileFromDisk(storedFilename) {
  const filePath = buildFilePath(storedFilename);
  try {
    await fs.promises.unlink(filePath);
  } catch (err) {
    // Resilience: file may already be gone; log but continue
    logger.warn('File deletion failed', {
      event: 'file.delete_failed',
      storedFilename,
      error: err.message
    });
  }
}

/**
 * Delete an attachment: auth check, disk delete, DB record delete.
 * @param {string} fileId
 * @param {string} requestingUserId
 * @param {string} [correlationId]
 * @returns {Promise<boolean>}
 */
async function deleteFile(fileId, requestingUserId, correlationId) {
  const attachment = await getFile(fileId, requestingUserId);
  if (!attachment) return false;

  await deleteFileFromDisk(attachment.storedFilename);
  await attachment.destroy();

  await auditService.log('attachment.deleted', {
    actorId: requestingUserId,
    targetId: fileId,
    targetType: 'attachment',
    outcome: 'success',
    correlationId
  });

  return true;
}

module.exports = {
  validateFile,
  storeFile,
  getFile,
  buildFilePath,
  deleteFile,
  deleteFileFromDisk
};
