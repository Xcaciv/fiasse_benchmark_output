'use strict';

const path = require('path');
const fs = require('fs');
const multer = require('multer');
const { v4: uuidv4 } = require('uuid');

const UPLOAD_DIR = path.resolve(process.env.UPLOAD_DIR || './uploads');
const MAX_SIZE   = (parseInt(process.env.MAX_FILE_SIZE_MB || '10', 10)) * 1024 * 1024;

// Allowed file extensions and corresponding MIME types (allowlist, not denylist)
const ALLOWED = new Map([
  ['.pdf',  ['application/pdf']],
  ['.doc',  ['application/msword']],
  ['.docx', ['application/vnd.openxmlformats-officedocument.wordprocessingml.document']],
  ['.txt',  ['text/plain']],
  ['.png',  ['image/png']],
  ['.jpg',  ['image/jpeg']],
  ['.jpeg', ['image/jpeg']]
]);

fs.mkdirSync(UPLOAD_DIR, { recursive: true });

const storage = multer.diskStorage({
  destination: (_req, _file, cb) => cb(null, UPLOAD_DIR),
  filename: (_req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    // Store with UUID to prevent filename collisions and path traversal
    cb(null, `${uuidv4()}${ext}`);
  }
});

function fileFilter(_req, file, cb) {
  const ext      = path.extname(file.originalname).toLowerCase();
  const mimeList = ALLOWED.get(ext);

  if (!mimeList || !mimeList.includes(file.mimetype)) {
    return cb(Object.assign(new Error(`File type not allowed: ${ext}`), { code: 'INVALID_TYPE' }), false);
  }
  cb(null, true);
}

const upload = multer({
  storage,
  fileFilter,
  limits: { fileSize: MAX_SIZE, files: 5 }
});

module.exports = { upload, UPLOAD_DIR };
