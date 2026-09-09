-- Demo/test user accounts used across the team for RBAC testing (one
-- account per role, plus Hashini's own admin login). Like seed_roles.sql,
-- this is NOT applied automatically (ddl-auto=none) - every teammate needs
-- to run it ONCE against their own local csi_erp_db, the same way
-- seed_roles.sql is run manually.
--
-- Why this file exists: these accounts were created directly in Hashini's
-- own local database (via the app's own "Create User" screen), which only
-- ever writes to HER MySQL install. Git tracks code, not database rows -
-- pulling the latest code does not create these rows on anyone else's
-- machine, which is why "admin" / "admin123" (from the original seed) kept
-- working for the team but admin.hashini, qc.priyantha, inventory.kumari
-- and sales.nadeesha did not: they simply didn't exist yet in anyone
-- else's database.
--
-- PasswordHash values below are bcrypt hashes copied as-is - never
-- plaintext, and safe to commit (bcrypt is specifically designed to resist
-- being reversed even with the hash exposed). The actual passwords aren't
-- in this file or anywhere in git - ask Hashini directly if you don't
-- already have them.
--
-- ON DUPLICATE KEY UPDATE makes this safe to re-run - matches on the
-- existing UNIQUE key on Username, so running it twice on the same
-- database just re-confirms the same values instead of erroring or
-- duplicating rows.

INSERT INTO AppUser (Username, PasswordHash, FullName, RoleID, Status) VALUES
('admin.hashini',     '$2a$10$lVG5lnv.eyA3O1I8krZ1iem6U2BqSnrX5ALqOwaoKM/KiUeYVILZy', 'Hashini Umesha',   1, 'Active'),
('qc.priyantha',      '$2a$10$Bf7HSaq8884tKolyzmCfx.0lw/Hg.DsTvjtoqP7RZ34k1t5BGhl/e', 'Priyantha Silva',  2, 'Active'),
('inventory.kumari',  '$2a$10$Px/BJRKNZmBHvnM/1bBNoODpf0kgg3apFWqQeintCihAg5r20SifG', 'Kumari Fernando',  3, 'Active'),
('sales.nadeesha',    '$2a$10$.7XF4lGhV.bv/WbsxsjiIOiNEQAjQIt8porwo4BItaCv/dsQQbTBq', 'Nadeesha Perera',  4, 'Active')
ON DUPLICATE KEY UPDATE
    PasswordHash = VALUES(PasswordHash),
    FullName = VALUES(FullName),
    RoleID = VALUES(RoleID),
    Status = VALUES(Status);
