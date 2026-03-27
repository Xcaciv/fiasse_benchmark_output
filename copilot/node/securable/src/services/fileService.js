'use strict';

const path = require('path');
const fs = require('fs').promises;
const { v4: uuidv4 } = require('uuid');
const multer = require('multer');
const { Attachment, Note } = require('../models/index');
const { logAction, AUDIT_ACTIONS } = require('./auditService');
const { logger } = require('../config/logger');

const ALLOWED_EXTENSIONS = new Set(['pdf', 'doc', 'docx', 'txt', 'png', 'jpg', 'jpeg']);

const ALLOWED_MIME_TYPES = new Set([
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'text/plain',
  'image/png',
  'image/jpeg',
]);

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

const getUploadDir = () => path.resolve(process.env.UPLOAD_DIR || './uploads');

const getExtension = (filename) => path.extname(filename).toLowerCase().slice(1);

/**
 * Validate file type and size. Returns { valid, reason }.
 * Trust boundary: called before any file is accepted or stored.
 */
const validateFile = (file) => {
  const ext = getExtension(file.originalname);
  if (!ALLOWED_EXTENSIONS.has(ext)) {
    return { valid: false, reason: 'File type not allowed' };
  }
  if (!ALLOWED_MIME_TYPES.has(file.mimetype)) {
    return { valid: false, reason: 'MIME type not permitted' };
  }
  if (file.size > MAX_FILE_SIZE) {
    return { valid: false, reason: 'File exceeds 10 MB limit' };
  }
  return { valid: true };
};

/**
 * Resolve a stored filename to an absolute path.
 * Guards against path traversal by verifying the result stays inside uploads/.
 */
const getSecureFilePath = (storedName) => {
  const uploadDir = getUploadDir();
  const resolved = path.resolve(uploadDir, storedName);
  if (!resolved.startsWith(uploadDir + path.sep) && resolved !== uploadDir) {
    const err = new Error('Path traversal detected');
    err.status = 400;
    throw err;
  }
  return resolved;
};

/**
 * Move the uploaded temp file to the uploads dir with a UUID filename,
 * then create the Attachment record in the database.
 */
const storeFile = async ({ file, noteId, userId, ipAddress }) => {
  const validation = validateFile(file);
  if (!validation.valid) {
    const err = new Error(validation.reason); err.status = 400; throw err;
  }

  const ext = getExtension(file.originalname);
  const storedName = `${uuidv4()}.${ext}`;
  const destPath = getSecureFilePath(storedName);

  await fs.rename(file.path, destPath);

  const attachment = await Attachment.create({
    noteId,
    originalName: file.originalname,
    storedName,
    mimeType: file.mimetype,
    sizeBytes: file.size,
  });

  logAction({
    actorId: userId,
    action: AUDIT_ACTIONS.FILE_UPLOAD,
    resourceType: 'Attachment',
    resourceId: attachment.id,
    metadata: { noteId, originalName: file.originalname, sizeBytes: file.size },
    ipAddress,
  });

  return attachment;
};

/**
 * Delete an attachment record and its file from disk.
 * Ownership is verified via the parent note.
 */
const deleteFile = async ({ attachmentId, userId, isAdmin, ipAddress }) => {
  const attachment = await Attachment.findByPk(attachmentId, {
    include: [{ model: Note }],
  });

  if (!attachment) {
    const err = new Error('Attachment not found'); err.status = 404; throw err;
  }
  if (!isAdmin && attachment.Note.userId !== userId) {
    const err = new Error('Forbidden'); err.status = 403; throw err;
  }

  const filePath = getSecureFilePath(attachment.storedName);
  await attachment.destroy();

  // File removal is best-effort; DB record already gone
  await fs.unlink(filePath).catch((unlinkErr) => {
    logger.warn('Could not remove file from disk', {
      storedName: attachment.storedName,
      error: unlinkErr.message,
    });
  });

  logAction({
    actorId: userId,
    action: AUDIT_ACTIONS.FILE_DELETE,
    resourceType: 'Attachment',
    resourceId: attachmentId,
    metadata: { noteId: attachment.noteId },
    ipAddress,
  });
};

/**
 * Return attachment and its resolved path for download.
 * Access allowed for owner or when parent note is public.
 */
const getAttachmentForDownload = async ({ attachmentId, requestingUserId }) => {
  const attachment = await Attachment.findByPk(attachmentId, {
    include: [{ model: Note }],
  });

  if (!attachment) {
    const err = new Error('Attachment not found'); err.status = 404; throw err;
  }

  const note = attachment.Note;
  const isOwner = note.userId === requestingUserId;
  const isPublic = note.visibility === 'public';

  if (!isOwner && !isPublic) {
    const err = new Error('Forbidden'); err.status = 403; throw err;
  }

  const filePath = getSecureFilePath(attachment.storedName);
  return { attachment, filePath };
};

/**
 * Build and return a configured multer instance.
 * fileFilter rejects disallowed types before writing to disk.
 */
const createMulterUpload = () => {
  const storage = multer.diskStorage({
    destination: (_req, _file, cb) => cb(null, getUploadDir()),
    filename: (_req, _file, cb) => cb(null, `tmp_${uuidv4()}`),
  });

  return multer({
    storage,
    limits: { fileSize: MAX_FILE_SIZE },
    fileFilter: (_req, file, cb) => {
      const ext = getExtension(file.originalname);
      if (!ALLOWED_EXTENSIONS.has(ext) || !ALLOWED_MIME_TYPES.has(file.mimetype)) {
        return cb(new Error('File type not allowed'));
      }
      cb(null, true);
    },
  });
};

module.exports = {
  validateFile,
  storeFile,
  deleteFile,
  getAttachmentForDownload,
  createMulterUpload,
  ALLOWED_EXTENSIONS,
  MAX_FILE_SIZE,
};
