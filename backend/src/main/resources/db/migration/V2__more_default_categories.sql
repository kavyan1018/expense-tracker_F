-- New default expense categories (keep in step with CategoryService.DEFAULTS).
-- 1. Rename the old defaults so existing transactions, budgets and rules keep their category.
UPDATE categories SET name = '🛒 Groceries' WHERE type = 'EXPENSE' AND name = 'Groceries';
UPDATE categories SET name = '🏠 Rent & Housing' WHERE type = 'EXPENSE' AND name = 'Rent';
UPDATE categories SET name = '💡 Utilities & Bills' WHERE type = 'EXPENSE' AND name = 'Utilities';
UPDATE categories SET name = '🚗 Transportation' WHERE type = 'EXPENSE' AND name = 'Transport';
UPDATE categories SET name = '🏥 Healthcare' WHERE type = 'EXPENSE' AND name = 'Health';
UPDATE categories SET name = '🎓 Education' WHERE type = 'EXPENSE' AND name = 'Education';
UPDATE categories SET name = '🎬 Entertainment' WHERE type = 'EXPENSE' AND name = 'Entertainment';
UPDATE categories SET name = '🛍️ Shopping' WHERE type = 'EXPENSE' AND name = 'Shopping';
UPDATE categories SET name = '📦 Miscellaneous' WHERE type = 'EXPENSE' AND name = 'Other';

-- 2. Give every existing user the categories they don't have yet.
INSERT INTO categories (user_id, name, type, color)
SELECT u.id, '🍔 Food & Dining', 'EXPENSE', '#f97316' FROM users u
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.user_id = u.id AND c.type = 'EXPENSE' AND c.name = '🍔 Food & Dining');
INSERT INTO categories (user_id, name, type, color)
SELECT u.id, '🛒 Groceries', 'EXPENSE', '#16a34a' FROM users u
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.user_id = u.id AND c.type = 'EXPENSE' AND c.name = '🛒 Groceries');
INSERT INTO categories (user_id, name, type, color)
SELECT u.id, '🚗 Transportation', 'EXPENSE', '#ea580c' FROM users u
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.user_id = u.id AND c.type = 'EXPENSE' AND c.name = '🚗 Transportation');
INSERT INTO categories (user_id, name, type, color)
SELECT u.id, '🏠 Rent & Housing', 'EXPENSE', '#7c3aed' FROM users u
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.user_id = u.id AND c.type = 'EXPENSE' AND c.name = '🏠 Rent & Housing');
INSERT INTO categories (user_id, name, type, color)
SELECT u.id, '💡 Utilities & Bills', 'EXPENSE', '#0891b2' FROM users u
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.user_id = u.id AND c.type = 'EXPENSE' AND c.name = '💡 Utilities & Bills');
INSERT INTO categories (user_id, name, type, color)
SELECT u.id, '📱 Mobile & Recharge', 'EXPENSE', '#0284c7' FROM users u
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.user_id = u.id AND c.type = 'EXPENSE' AND c.name = '📱 Mobile & Recharge');
INSERT INTO categories (user_id, name, type, color)
SELECT u.id, '🛍️ Shopping', 'EXPENSE', '#ca8a04' FROM users u
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.user_id = u.id AND c.type = 'EXPENSE' AND c.name = '🛍️ Shopping');
INSERT INTO categories (user_id, name, type, color)
SELECT u.id, '☕ Tea & Coffee', 'EXPENSE', '#92400e' FROM users u
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.user_id = u.id AND c.type = 'EXPENSE' AND c.name = '☕ Tea & Coffee');
INSERT INTO categories (user_id, name, type, color)
SELECT u.id, '🏥 Healthcare', 'EXPENSE', '#dc2626' FROM users u
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.user_id = u.id AND c.type = 'EXPENSE' AND c.name = '🏥 Healthcare');
INSERT INTO categories (user_id, name, type, color)
SELECT u.id, '🎬 Entertainment', 'EXPENSE', '#db2777' FROM users u
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.user_id = u.id AND c.type = 'EXPENSE' AND c.name = '🎬 Entertainment');
INSERT INTO categories (user_id, name, type, color)
SELECT u.id, '🎓 Education', 'EXPENSE', '#2563eb' FROM users u
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.user_id = u.id AND c.type = 'EXPENSE' AND c.name = '🎓 Education');
INSERT INTO categories (user_id, name, type, color)
SELECT u.id, '💳 EMIs & Loans', 'EXPENSE', '#be123c' FROM users u
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.user_id = u.id AND c.type = 'EXPENSE' AND c.name = '💳 EMIs & Loans');
INSERT INTO categories (user_id, name, type, color)
SELECT u.id, '👨‍👩‍👧 Family & Personal', 'EXPENSE', '#c026d3' FROM users u
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.user_id = u.id AND c.type = 'EXPENSE' AND c.name = '👨‍👩‍👧 Family & Personal');
INSERT INTO categories (user_id, name, type, color)
SELECT u.id, '✈️ Travel', 'EXPENSE', '#0d9488' FROM users u
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.user_id = u.id AND c.type = 'EXPENSE' AND c.name = '✈️ Travel');
INSERT INTO categories (user_id, name, type, color)
SELECT u.id, '💰 Savings & Investments', 'EXPENSE', '#65a30d' FROM users u
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.user_id = u.id AND c.type = 'EXPENSE' AND c.name = '💰 Savings & Investments');
INSERT INTO categories (user_id, name, type, color)
SELECT u.id, '📦 Miscellaneous', 'EXPENSE', '#64748b' FROM users u
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.user_id = u.id AND c.type = 'EXPENSE' AND c.name = '📦 Miscellaneous');
