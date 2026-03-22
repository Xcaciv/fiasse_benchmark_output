const { getDb } = require('../db');

async function logActivity(userId, actionType, details) {
  const db = await getDb();

  await db.run(
    `
      INSERT INTO activity_logs (user_id, action_type, details)
      VALUES (?, ?, ?)
    `,
    [userId || null, actionType, details]
  );
}

module.exports = {
  logActivity
};
