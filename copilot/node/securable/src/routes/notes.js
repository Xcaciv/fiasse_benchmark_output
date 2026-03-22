'use strict';
const express = require('express');
const { body } = require('express-validator');
const { requireAuthenticated } = require('../middleware/requireAuth');
const noteController = require('../controllers/noteController');

const router = express.Router();

const noteValidation = [
  body('title').trim().notEmpty().isLength({ max: 255 }).withMessage('Title is required (max 255 chars)'),
  body('content').trim().notEmpty().withMessage('Content is required')
];

router.get('/top-rated', noteController.topRated);
router.get('/', requireAuthenticated, noteController.listMyNotes);
router.get('/create', requireAuthenticated, (req, res) => res.render('notes/create', { errors: [], csrfToken: req.csrfToken() }));
router.post('/', requireAuthenticated, noteValidation, noteController.createNote);
router.get('/:id', noteController.viewNote);
router.get('/:id/edit', requireAuthenticated, noteController.editNoteForm);
router.post('/:id/edit', requireAuthenticated, noteValidation, noteController.updateNote);
router.post('/:id/delete', requireAuthenticated, noteController.deleteNote);

module.exports = router;
