-- Drive Service Database Initialization
-- PostgreSQL 18

-- Create database if not exists (run as superuser)
SELECT 'CREATE DATABASE drive'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'drive')\gexec
