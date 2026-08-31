-- Predefined roles for CSI ERP (run once against csi_erp_db in MySQL Workbench)
-- Satisfies lecturer requirement: "predefined roles should be entered into the
-- database in advance rather than requiring users to manually enter roles one by one."

INSERT INTO Role (RoleName) VALUES
    ('Admin'),
    ('QC Officer'),
    ('Inventory Manager'),
    ('Sales Officer');

-- Verify:
SELECT * FROM Role;
