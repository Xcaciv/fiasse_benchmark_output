const express = require('express');
const { db } = require('../db/database');
const { isAuthenticated } = require('../middleware/auth');

const router = express.Router();

router.get('/', isAuthenticated, (req, res) => {
  const { q } = req.query;
  
  if (!q || q.trim() === '') {
    return res.render('search/index', { 
      user: req.session.user, 
      notes: [],
      query: ''
    });
  }

  const searchTerm = `%${q}%`;
  
  const notes = db.prepare(`
    SELECT n.id, n.title, n.content, n.is_public, n.created_at, u.username as author,
      (SELECT AVG(rating) FROM ratings WHERE note_id = n.id) as avg_rating,
      (SELECT COUNT(*) FROM ratings WHERE note_id = n.id) as rating_count
    FROM notes n
    JOIN users u ON n.user_id = u.id
    WHERE n.user_id = ?
      OR n.is_public = 1
    ORDER BY n.updated_at DESC
  `).all(req.session.user.id);

  const filteredNotes = notes.filter(note => {
    const matchesSearch = 
      note.title.toLowerCase().includes(q.toLowerCase()) ||
      note.content.toLowerCase().includes(q.toLowerCase());
    
    if (note.user_id === req.session.user.id) {
      return true;
    }
    
    return matchesSearch && note.is_public;
  });

  const results = filteredNotes.map(note => {
    if (note.content.length > 200) {
      note.excerpt = note.content.substring(0, 200) + '...';
    } else {
      note.excerpt = note.content;
    }
    return note;
  });

  res.render('search/index', { 
    user: req.session.user, 
    notes: results,
    query: q
  });
});

router.get('/top-rated', (req, res) => {
  const notes = db.prepare(`
    SELECT n.id, n.title, n.content, n.is_public, n.created_at, u.username as author,
      AVG(r.rating) as avg_rating,
      COUNT(r.id) as rating_count
    FROM notes n
    JOIN users u ON n.user_id = u.id
    JOIN ratings r ON n.id = r.note_id
    WHERE n.is_public = 1
    GROUP BY n.id
    HAVING COUNT(r.id) >= 3
    ORDER BY avg_rating DESC, rating_count DESC
    LIMIT 50
  `).all();

  const results = notes.map(note => {
    note.avg_rating = parseFloat(note.avg_rating).toFixed(1);
    if (note.content.length > 200) {
      note.excerpt = note.content.substring(0, 200) + '...';
    } else {
      note.excerpt = note.content;
    }
    return note;
  });

  res.render('search/top-rated', { 
    user: req.session?.user || null, 
    notes: results
  });
});

module.exports = router;
