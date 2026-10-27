-- Identifies where an imported expense came from (a hash of the SMS), so the same message is
-- never added twice, even from a second device or after reinstalling the app.
-- Manual expenses leave it empty; MySQL allows many NULLs in a unique index.
ALTER TABLE expenses
    ADD COLUMN external_ref VARCHAR(64) NULL DEFAULT NULL AFTER source,
    ADD UNIQUE KEY uk_expenses_user_external_ref (user_id, external_ref);
