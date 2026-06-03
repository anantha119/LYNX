-- Adds Auth0 subject identifier to users table.
-- Run once against the Cloud SQL instance via Cloud SQL Auth Proxy.
ALTER TABLE users ADD COLUMN IF NOT EXISTS auth0_sub TEXT UNIQUE;
