-- Dev bootstrap: default admin uses mobile login (19988888888 / 123456).
-- Password BCrypt: 123456

UPDATE sys_user
SET username = '19988888888',
    mobile = '19988888888',
    password = '$2a$10$1.CaH9sDhy1/.pjBfztKSuaYXNgNrYz2Y9Y4Ds8dRkn1u/MuPI0lO',
    update_time = NOW()
WHERE id = 1;
