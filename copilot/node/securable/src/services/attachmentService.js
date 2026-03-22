'use strict';
const fs = require('fs').promises;
const path = require('path');
const { v4: uuidv4 } = require('uuid');
const { ALLOWED_EXTENSIONS, ALLOWED_MIME_TYPES } = require('../config/constants');

// Integrity: Validate file extension and MIME type — trust boundary for uploads
function isAllowedFile(originalName, mimeType) {
  const ext = path.extname(originalName).toLowerCase();
  return ALLOWED_EXTENSIONS.includes(ext) && ALLOWED_MIME_TYPES.includes(mimeType);
}

async function saveAttachment(file, noteId, { Attachment }) {
  if (!isAllowedFile(file.originalname, file.mimetype)) {
    return { success: false, error: 'File type not allowed' };
  }
  const attachment = await Attachment.create({
    noteId,
    originalFilename: file.originalname,
    storedFilename: file.filename,
    mimeType: file.mimetype,
    fileSizeBytes: file.size
  });
  return { success: true, attachment };
}

async function deleteAttachment(attachmentId, noteId, userId, { Attachment, Note }) {
  const attachment = await Attachment.findOne({ where: { id: attachmentId, noteId } });
  if (!attachment) return { success: false, error: 'Attachment not found' };
  const note = await Note.findOne({ where: { id: noteId, userId } });
  if (!note) return { success: false, error: 'Access denied' };
  const uploadDir = process.env.UPLOAD_DIR || './uploads';
  const filePath = path.join(uploadDir, attachment.storedFilename);
  await fs.unlink(filePath).catch(() => {});
  await attachment.destroy();
  return { success: true };
}

async function getAttachment(attachmentId, requestingUserId, isAdmin, { Attachment, Note }) {
  const attachment = await Attachment.findByPk(attachmentId, { include: [Note] });
  if (!attachment) return null;
  const note = attachment.Note;
  const canAccess = isAdmin || note.isPublic || (requestingUserId && note.userId === requestingUserId);
  return canAccess ? attachment : null;
}

module.exports = { saveAttachment, deleteAttachment, getAttachment, isAllowedFile };
