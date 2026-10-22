-- Demo accounts created by older seed data used plain text passwords.
-- Require a secure password reset before those accounts can sign in.
UPDATE users
SET status = 'LOCKED', password = 'RESET_REQUIRED_NON_HASHED_PASSWORD'
WHERE password NOT LIKE '$2a$%'
  AND password NOT LIKE '$2b$%'
  AND password NOT LIKE '$2y$%';
