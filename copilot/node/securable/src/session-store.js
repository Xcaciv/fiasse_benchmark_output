'use strict';

const { getDb } = require('./database');

class SQLiteSessionStore {
  constructor({ session }) {
    this.Store = session.Store;
    const BaseStore = this.Store;
    const db = getDb();

    return new (class extends BaseStore {
      get(sid, callback) {
        try {
          db.prepare('DELETE FROM sessions WHERE expires_at <= CURRENT_TIMESTAMP').run();
          const row = db.prepare('SELECT sess, expires_at FROM sessions WHERE sid = ?').get(sid);

          if (!row) {
            return callback(null, null);
          }

          if (new Date(row.expires_at) <= new Date()) {
            db.prepare('DELETE FROM sessions WHERE sid = ?').run(sid);
            return callback(null, null);
          }

          return callback(null, JSON.parse(row.sess));
        } catch (error) {
          return callback(error);
        }
      }

      set(sid, sessionData, callback) {
        try {
          const expiresAt = sessionData.cookie?.expires
            ? new Date(sessionData.cookie.expires).toISOString()
            : new Date(Date.now() + (sessionData.cookie?.maxAge || 0)).toISOString();

          db.prepare(
            `INSERT INTO sessions (sid, sess, expires_at)
             VALUES (?, ?, ?)
             ON CONFLICT(sid) DO UPDATE SET sess = excluded.sess, expires_at = excluded.expires_at`
          ).run(sid, JSON.stringify(sessionData), expiresAt);

          return callback && callback(null);
        } catch (error) {
          return callback && callback(error);
        }
      }

      destroy(sid, callback) {
        try {
          db.prepare('DELETE FROM sessions WHERE sid = ?').run(sid);
          return callback && callback(null);
        } catch (error) {
          return callback && callback(error);
        }
      }

      touch(sid, sessionData, callback) {
        return this.set(sid, sessionData, callback);
      }
    })();
  }
}

module.exports = {
  SQLiteSessionStore
};
