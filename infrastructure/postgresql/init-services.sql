-- Auth, Shopping, Shopping-Seller, Shopping-Settlement DB 생성
-- PostgreSQL 18

SELECT 'CREATE DATABASE auth_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'auth_db')\gexec

SELECT 'CREATE DATABASE shopping_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'shopping_db')\gexec

SELECT 'CREATE DATABASE shopping_seller_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'shopping_seller_db')\gexec

SELECT 'CREATE DATABASE shopping_settlement_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'shopping_settlement_db')\gexec
