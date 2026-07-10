-- Password reset tokens are now stored hashed (SHA-256) instead of in plaintext.
-- Any token issued before this change is still stored in plaintext and can no longer
-- be matched (lookups now hash the incoming token), so invalidate every pending token.
-- Users with an outstanding reset link must simply request a new one.
UPDATE password_reset_token SET used = true WHERE used = false;
