-- Fingerprint of everything that shapes a suggestion (pantry, theme, note, count, diet,
-- allergies). A repeat request with the same fingerprint within a few minutes reuses the saved
-- recipes instead of paying for another AI call.
ALTER TABLE recipe_requests
    ADD COLUMN request_hash CHAR(64) NULL DEFAULT NULL AFTER status,
    ADD KEY idx_requests_hash (user_id, request_hash, created_at);
