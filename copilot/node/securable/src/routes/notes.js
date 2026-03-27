'use strict';

const express = require('express');
const router = express.Router();
const noteController = require('../controllers/noteController');
const requireAuth = require('../middleware/requireAuth');
const requireNoteOwner = require('../middleware/requireNoteOwner');
const { createNoteValidator, editNoteValidator } = require('../validators/noteValidators');

router.use(requireAuth);

router.get('/', noteController.getMyNotes);
router.get('/new', noteController.getCreateNotePage);
router.post('/', createNoteValidator, noteController.postCreateNote);

router.get('/:id', noteController.getNoteView);
router.get('/:id/edit', requireNoteOwner(), noteController.getEditNotePage);
router.put('/:id', requireNoteOwner(), editNoteValidator, noteController.putUpdateNote);
router.delete('/:id', requireNoteOwner({ adminAllowed: true }), noteController.deleteNote);

router.post('/:id/share', requireNoteOwner(), noteController.postGenerateShareLink);
router.delete('/:id/share', requireNoteOwner(), noteController.deleteRevokeShareLinks);

module.exports = router;
