-- Phase 1 Dev Schema for H2
-- This creates the new identity model tables for development testing

CREATE TABLE IF NOT EXISTS persons (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    email VARCHAR(255),
    password_hash VARCHAR(255),
    profile_picture_url TEXT,
    theme_preference VARCHAR(50),
    oauth_provider VARCHAR(50),
    oauth_id VARCHAR(255),
    system_role VARCHAR(50),
    last_phone_change_at TIMESTAMP,
    last_email_change_at TIMESTAMP,
    email_verified BOOLEAN DEFAULT false,
    verification_token VARCHAR(255),
    verification_token_expires_at TIMESTAMP,
    reset_token VARCHAR(255),
    reset_token_expires_at TIMESTAMP,
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

-- Cash Register Tables
CREATE TABLE IF NOT EXISTS cash_registers (
    id UUID PRIMARY KEY,
    shop_id UUID NOT NULL,
    label VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP,
    FOREIGN KEY (shop_id) REFERENCES shops(id)
);

CREATE TABLE IF NOT EXISTS cash_register_sessions (
    id UUID PRIMARY KEY,
    register_id UUID NOT NULL,
    opened_by_id UUID NOT NULL,
    opened_at TIMESTAMP NOT NULL,
    opening_cash_amount DECIMAL(15,2) NOT NULL CHECK (opening_cash_amount >= 0),
    closed_by_id UUID,
    closed_at TIMESTAMP,
    closing_declared_amount DECIMAL(15,2) CHECK (closing_declared_amount IS NULL OR closing_declared_amount >= 0),
    closing_computed_amount DECIMAL(15,2),
    discrepancy DECIMAL(15,2),
    status VARCHAR(50) NOT NULL,
    FOREIGN KEY (register_id) REFERENCES cash_registers(id),
    FOREIGN KEY (opened_by_id) REFERENCES persons(id),
    FOREIGN KEY (closed_by_id) REFERENCES persons(id)
);

CREATE TABLE IF NOT EXISTS cash_movements (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    reason TEXT,
    reference_invoice_id UUID,
    effectue_par_id UUID NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    FOREIGN KEY (session_id) REFERENCES cash_register_sessions(id),
    FOREIGN KEY (effectue_par_id) REFERENCES persons(id)
);

-- Support & Ticketing Tables
CREATE TABLE IF NOT EXISTS support_tickets (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL,
    subject VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(50) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    resolved_at TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES persons(id)
);

CREATE TABLE IF NOT EXISTS support_ticket_messages (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL,
    sender_id UUID,
    message TEXT NOT NULL,
    is_system_message BOOLEAN DEFAULT false,
    created_at TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES support_tickets(id),
    FOREIGN KEY (sender_id) REFERENCES persons(id)
);

CREATE TABLE IF NOT EXISTS support_audit_logs (
    id UUID PRIMARY KEY,
    admin_id UUID NOT NULL,
    target_user_id UUID NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    reason TEXT NOT NULL,
    ticket_id UUID,
    timestamp TIMESTAMP,
    FOREIGN KEY (admin_id) REFERENCES persons(id),
    FOREIGN KEY (target_user_id) REFERENCES persons(id),
    FOREIGN KEY (ticket_id) REFERENCES support_tickets(id)
);
