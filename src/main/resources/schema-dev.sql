-- Phase 1 Dev Schema for H2
-- This creates the new identity model tables for development testing

CREATE TABLE IF NOT EXISTS persons (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    email VARCHAR(255),
    password VARCHAR(255),
    system_role VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS shops (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    account_id UUID
);

CREATE TABLE IF NOT EXISTS shop_memberships (
    id UUID PRIMARY KEY,
    person_id UUID NOT NULL,
    shop_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (person_id) REFERENCES persons(id),
    FOREIGN KEY (shop_id) REFERENCES shops(id)
);

CREATE TABLE IF NOT EXISTS membership_permission_grants (
    id UUID PRIMARY KEY,
    membership_id UUID NOT NULL,
    permission VARCHAR(100) NOT NULL,
    granted_at TIMESTAMP,
    FOREIGN KEY (membership_id) REFERENCES shop_memberships(id)
);

CREATE TABLE IF NOT EXISTS membership_permission_revokes (
    id UUID PRIMARY KEY,
    membership_id UUID NOT NULL,
    permission VARCHAR(100) NOT NULL,
    revoked_at TIMESTAMP,
    FOREIGN KEY (membership_id) REFERENCES shop_memberships(id)
);

CREATE TABLE IF NOT EXISTS legacy_id_mappings (
    id UUID PRIMARY KEY,
    legacy_id UUID NOT NULL,
    legacy_entity_type VARCHAR(50) NOT NULL,
    person_id UUID NOT NULL,
    mapped_at TIMESTAMP,
    FOREIGN KEY (person_id) REFERENCES persons(id)
);
