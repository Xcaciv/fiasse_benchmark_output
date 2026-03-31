const express = require('express');
const router = express.Router();
const Note = require('../models/Note');
const Attachment = require('../models/Attachment');
const Rating = require('../models/Rating');
const ShareLink = require('../models/ShareLink');
const logger = require('../utils/logger');

router.get('/:token', async (req, res) => {
  try {
    const shareLink = await ShareLink.findByToken(req.params.token);
    
    if (!shareLink) {
      return res.status(404).render('error', { 
        user: null, 
        message: 'Share link not found or has expired' 
      });
    }
    
    const note = await Note.findById(shareLink.note_id);
    
    if (!note) {
      return res.status(404).render('error', { 
        user: null, 
        message: 'Note not found' 
      });
    }
    
    const attachments = await Attachment.findByNoteId(note.id);
    const ratings = await Rating.findByNoteId(note.id);
    
    res.render('share/view', { 
      user: req.user, 
      note, 
      attachments, 
      ratings,
      shareToken: req.params.token
    });
  } catch (error) {
    logger.error('Error viewing shared note', { error: error.message, token: req.params.token });
    res.status(500).render('error', { 
      user: null, 
      message: 'Failed to load shared note' 
    });
  }
});

module.exports = router;
