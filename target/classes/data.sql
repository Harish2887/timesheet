-- Insert roles if they don't exist
INSERT INTO roles (name) 
SELECT 'ROLE_ADMIN' 
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_ADMIN');

INSERT INTO roles (name) 
SELECT 'ROLE_USER_EMP' 
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_USER_EMP');

INSERT INTO roles (name) 
SELECT 'ROLE_USER_PAY' 
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_USER_PAY');

INSERT INTO roles (name) 
SELECT 'ROLE_USER_SUB' 
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_USER_SUB');

-- Create default admin user if no users exist
INSERT INTO users (username, email, password)
SELECT 'admin', 'admin@example.com', '$2a$10$ixlPY3AAd4ty1l6E2IsQ9OFZi2ba9ZQE0bP7RFcGIWNhyFrrT3YUi' -- password: adminPass123
WHERE NOT EXISTS (SELECT 1 FROM users);

-- Assign ROLE_ADMIN to the admin user
INSERT INTO user_roles (user_id, role_id)
SELECT (SELECT id FROM users WHERE username = 'admin'), (SELECT id FROM roles WHERE name = 'ROLE_ADMIN')
WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = (SELECT id FROM users WHERE username = 'admin') AND role_id = (SELECT id FROM roles WHERE name = 'ROLE_ADMIN')); 