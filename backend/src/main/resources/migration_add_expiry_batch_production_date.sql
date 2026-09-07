-- Manual schema change (ddl-auto=none, so nothing applies this automatically -
-- every teammate needs to run it once against their own local csi_erp_db,
-- the same way seed_roles.sql is run manually).
--
-- Adds ProductionDate to ExpiryBatch so every finished-product batch can
-- record when it was actually produced/put into stock, alongside its
-- already-existing ExpiryDate (which the backend now calculates
-- automatically from ProductionDate - see erp_backend.util.ExpiryCalculator
-- - rather than being entered by hand).
--
-- Safe to run more than once is NOT guaranteed by MySQL for ADD COLUMN
-- without IF NOT EXISTS (requires MySQL 8.0.29+) - this repo targets
-- MySQL 8.0.46, so IF NOT EXISTS is used below.

ALTER TABLE ExpiryBatch ADD COLUMN IF NOT EXISTS ProductionDate DATE NULL AFTER ProductID;
