import pg from 'pg';
import fs from 'fs';
import 'dotenv/config';

const pool = new pg.Pool({
  host: process.env.DB_HOST,
  port: parseInt(process.env.DB_PORT || '5432'),
  database: process.env.DB_NAME,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
});

async function run() {
  const sql = fs.readFileSync('migration/02_create_conversation_summaries.sql', 'utf8');
  await pool.query(sql);
  console.log('Migration completed successfully.');
  process.exit(0);
}

run().catch(console.error);
