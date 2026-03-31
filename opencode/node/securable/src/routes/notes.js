const express = require('express');
const router = express.Router();
const Note = require('../models/Note');
const Attachment = require('../models/Attachment');
const Rating = require('../models/Rating');
const ShareLink = require('../models/ShareLink');
const ActivityLog = require('../models/ActivityLog');
const { requireAuth } = require('../middleware/auth');
const { noteValidation, ratingValidation } = require('../middleware/validation');
const upload = require('../middleware/upload');
const logger = require('../utils/logger');
const path = require('path');

router.get('/', requireAuth, async (req, res) => {
  try {
    const search = req.query.search || null;
    const notes = await Note.getByUserId(req.user.id, search);
    res.render('notes/index', { user: req.user, notes, search });
  } catch (error) {
    logger.error('Error fetching notes', { error: error.message });
    req.flash('error', 'Failed to load notes');
    res.redirect('/');
  }
});

router.get('/new', requireAuth, (req, res) => {
  res.render('notes/new', { user: req.user, note: null });
});

router.post('/', requireAuth, noteValidation, async (req, res) => {
  try {
    const { title, content, is_public } = req.body;
    const noteId = await Note.create(req.user.id, title, content, is_public === 'on');
    
    await ActivityLog.log(req.user.id, 'NOTE_CREATED', 'note', noteId, null, req.ip);
    
    req.flash('success', 'Note created successfully');
    res.redirect(`/notes/${noteId}`);
  } catch (error) {
    logger.error('Error creating note', { error: error.message });
    req.flash('error', 'Failed to create note');
    res.redirect('/notes/new');
  }
});

router.get('/:id', async (req, res) => {
  try {
    const note = await Note.findById(req.params.id, req.user?.id);
    
    if (!note) {
      req.flash('error', 'Note not found');
      return res.redirect('/notes');
    }
    
    const attachments = await Attachment.findByNoteId(note.id);
    const ratings = await Rating.findByNoteId(note.id);
    const userRating = req.user ? await Rating.findByUserAndNote(req.user.id, note.id) : null;
    const shareLinks = note.user_id === req.user?.id ? await ShareLink.findByNoteId(note.id) : [];
    
    const isOwner = req.user && req.user.id === note.user_id;
    
    res.render('notes/show', { 
      user: req.user, 
      note, 
      attachments, 
      ratings, 
      userRating, 
      shareLinks,
      isOwner 
    });
  } catch (error) {
    logger.error('Error fetching note', { error: error.message, noteId: req.params.id });
    req.flash('error', 'Failed to load note');
    res.redirect('/notes');
  }
});

router.get('/:id/edit', requireAuth, async (req, res) => {
  try {
    const note = await Note.findByIdWithOwner(req.params.id);
    
    if (!note) {
      req.flash('error', 'Note not found');
      return res.redirect('/notes');
    }
    
    if (note.user_id !== req.user.id && req.user.role !== 'admin') {
      req.flash('error', 'You do not have permission to edit this note');
      return res.redirect('/notes');
    }
    
    res.render('notes/edit', { user: req.user, note });
  } catch (error) {
    logger.error('Error fetching note for edit', { error: error.message });
    req.flash('error', 'Failed to load note');
    res.redirect('/notes');
  }
});

router.put('/:id', requireAuth, noteValidation, async (req, res) => {
  try {
    const note = await Note.findByIdWithOwner(req.params.id);
    
    if (!note) {
      req.flash('error', 'Note not found');
      return res.redirect('/notes');
    }
    
    if (note.user_id !== req.user.id && req.user.role !== 'admin') {
      req.flash('error', 'You do not have permission to edit this note');
      return res.redirect('/notes');
    }
    
    const { title, content, is_public } = req.body;
    await Note.update(req.params.id, req.user.id, { title, content, is_public: is_public === 'on' });
    
    await ActivityLog.log(req.user.id, 'NOTE_UPDATED', 'note', req.params.id, null, req.ip);
    
    req.flash('success', 'Note updated successfully');
    res.redirect(`/notes/${req.params.id}`);
  } catch (error) {
    logger.error('Error updating note', { error: error.message });
    req.flash('error', 'Failed to update note');
    res.redirect(`/notes/${req.params.id}/edit`);
  }
});

router.delete('/:id', requireAuth, async (req, res) => {
  try {
    const note = await Note.findByIdWithOwner(req.params.id);
    
    if (!note) {
      req.flash('error', 'Note not found');
      return res.redirect('/notes');
    }
    
    const isAdmin = req.user.role === 'admin';
    if (note.user_id !== req.user.id && !isAdmin) {
      req.flash('error', 'You do not have permission to delete this note');
      return res.redirect('/notes');
    }
    
    await Note.delete(req.params.id, req.user.id, isAdmin);
    
    await ActivityLog.log(req.user.id, 'NOTE_DELETED', 'note', req.params.id, null, req.ip);
    
    req.flash('success', 'Note deleted successfully');
    res.redirect('/notes');
  } catch (error) {
    logger.error('Error deleting note', { error: error.message });
    req.flash('error', 'Failed to delete note');
    res.redirect('/notes');
  }
});

router.post('/:id/attachments', requireAuth, upload.array('attachments', 5), async (req, res) => {
  try {
    const note = await Note.findByIdWithOwner(req.params.id);
    
    if (!note || note.user_id !== req.user.id) {
      req.flash('error', 'You do not have permission to add attachments to this note');
      return res.redirect(`/notes/${req.params.id}`);
    }
    
    if (req.files && req.files.length > 0) {
      for (const file of req.files) {
        await Attachment.create(
          req.params.id,
          file.originalname,
          file.filename,
          file.mimetype,
          file.size
        );
      }
      
      await ActivityLog.log(req.user.id, 'ATTACHMENT_ADDED', 'note', req.params.id, `${req.files.length} files`, req.ip);
    }
    
    req.flash('success', 'Attachments added successfully');
    res.redirect(`/notes/${req.params.id}`);
  } catch (error) {
    logger.error('Error adding attachments', { error: error.message });
    req.flash('error', 'Failed to add attachments');
    res.redirect(`/notes/${req.params.id}`);
  }
});

router.delete('/:id/attachments/:attachmentId', requireAuth, async (req, res) => {
  try {
    const note = await Note.findByIdWithOwner(req.params.id);
    
    if (!note || note.user_id !== req.user.id) {
      req.flash('error', 'You do not have permission to delete attachments');
      return res.redirect(`/notes/${req.params.id}`);
    }
    
    await Attachment.delete(req.params.attachmentId);
    
    req.flash('success', 'Attachment deleted successfully');
    res.redirect(`/notes/${req.params.id}`);
  } catch (error) {
    logger.error('Error deleting attachment', { error: error.message });
    req.flash('error', 'Failed to delete attachment');
    res.redirect(`/notes/${req.params.id}`);
  }
});

router.get('/download/:attachmentId', async (req, res) => {
  try {
    const attachment = await Attachment.findById(req.params.attachmentId);
    
    if (!attachment) {
      return res.status(404).send('File not found');
    }
    
    const note = await Note.findById(attachment.note_id, req.user?.id);
    
    if (!note) {
      return res.status(404).send('File not found');
    }
    
    const isOwner = req.user && req.user.id === note.user_id;
    if (!note.is_public && !isOwner) {
      return res.status(403).send('Access denied');
    }
    
    const uploadDir = process.env.UPLOAD_DIR || './uploads';
    const filePath = path.join(uploadDir, attachment.stored_filename);
    
    res.download(filePath, attachment.original_filename);
  } catch (error) {
    logger.error('Error downloading attachment', { error: error.message });
    res.status(500).send('Error downloading file');
  }
});

router.post('/:id/rate', requireAuth, ratingValidation, async (req, res) => {
  try {
    const note = await Note.findById(req.params.id);
    
    if (!note) {
      req.flash('error', 'Note not found');
      return res.redirect('/notes');
    }
    
    const { value, comment } = req.body;
    await Rating.create(req.params.id, req.user.id, parseInt(value), comment || null);
    
    await ActivityLog.log(req.user.id, 'RATING_ADDED', 'note', req.params.id, `Value: ${value}`, req.ip);
    
    req.flash('success', 'Rating submitted successfully');
    res.redirect(`/notes/${req.params.id}`);
  } catch (error) {
    logger.error('Error submitting rating', { error: error.message });
    req.flash('error', 'Failed to submit rating');
    res.redirect(`/notes/${req.params.id}`);
  }
});

router.get('/:id/share', requireAuth, async (req, res) => {
  try {
    const note = await Note.findByIdWithOwner(req.params.id);
    
    if (!note || note.user_id !== req.user.id) {
      req.flash('error', 'You do not have permission to share this note');
      return res.redirect('/notes');
    }
    
    const links = await ShareLink.findByNoteId(note.id);
    res.render('notes/share', { user: req.user, note, shareLinks: links });
  } catch (error) {
    logger.error('Error loading share page', { error: error.message });
    req.flash('error', 'Failed to load share options');
    res.redirect('/notes');
  }
});

router.post('/:id/share', requireAuth, async (req, res) => {
  try {
    const note = await Note.findByIdWithOwner(req.params.id);
    
    if (!note || note.user_id !== req.user.id) {
      req.flash('error', 'You do not have permission to share this note');
      return res.redirect('/notes');
    }
    
    const expiresIn = req.body.expires_in ? parseInt(req.body.expires_in) : null;
    const expiresAt = expiresIn ? new Date(Date.now() + expiresIn * 1000).toISOString() : null;
    
    const link = await ShareLink.regenerate(req.params.id, expiresAt);
    
    await ActivityLog.log(req.user.id, 'SHARE_LINK_CREATED', 'note', req.params.id, `Token: ${link.token}`, req.ip);
    
    req.flash('success', 'Share link generated');
    res.redirect(`/notes/${req.params.id}/share`);
  } catch (error) {
    logger.error('Error creating share link', { error: error.message });
    req.flash('error', 'Failed to create share link');
    res.redirect(`/notes/${req.params.id}`);
  }
});

router.post('/:id/share/:linkId/deactivate', requireAuth, async (req, res) => {
  try {
    const note = await Note.findByIdWithOwner(req.params.id);
    
    if (!note || note.user_id !== req.user.id) {
      req.flash('error', 'You do not have permission to manage share links');
      return res.redirect('/notes');
    }
    
    await ShareLink.deactivate(req.params.linkId);
    
    req.flash('success', 'Share link deactivated');
    res.redirect(`/notes/${req.params.id}/share`);
  } catch (error) {
    logger.error('Error deactivating share link', { error: error.message });
    req.flash('error', 'Failed to deactivate share link');
    res.redirect(`/notes/${req.params.id}/share`);
  }
});

module.exports = router;
