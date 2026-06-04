CREATE TABLE IF NOT EXISTS conversation_summaries (
  conversation_id     UUID NOT NULL REFERENCES conversations(id),
  covers_through_id   TEXT NOT NULL,
  summary             TEXT NOT NULL,
  token_count         INT NOT NULL,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (conversation_id, covers_through_id)
);
