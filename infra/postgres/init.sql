-- PostgreSQL initialization script
-- Runs once when the container is first created

-- Enable the uuid-ossp extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enable pgcrypto for additional crypto functions (optional but useful)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
