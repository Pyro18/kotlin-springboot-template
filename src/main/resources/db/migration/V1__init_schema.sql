-- Create sequences
CREATE SEQUENCE IF NOT EXISTS user_sequence START WITH 1000 INCREMENT BY 50;

-- Create users table
CREATE TABLE users (
                       id BIGINT NOT NULL DEFAULT nextval('user_sequence'),
                       first_name VARCHAR(100) NOT NULL,
                       last_name VARCHAR(100) NOT NULL,
                       username VARCHAR(50) NOT NULL,
                       email VARCHAR(255) NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       active BOOLEAN NOT NULL DEFAULT TRUE,
                       role VARCHAR(20) NOT NULL DEFAULT 'USER',
                       bio VARCHAR(500),
                       profile_picture_url VARCHAR(500),
                       email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                       last_login_at TIMESTAMP,
                       failed_login_attempts INTEGER NOT NULL DEFAULT 0,
                       locked_until TIMESTAMP,
                       version BIGINT NOT NULL DEFAULT 0,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP,
                       CONSTRAINT pk_users PRIMARY KEY (id),
                       CONSTRAINT uk_users_username UNIQUE (username),
                       CONSTRAINT uk_users_email UNIQUE (email),
                       CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'MODERATOR', 'USER', 'GUEST'))
);

-- Create indexes for users table
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_username ON users(username);
CREATE INDEX idx_user_active ON users(active);
CREATE INDEX idx_user_created ON users(created_at DESC);
CREATE INDEX idx_user_role ON users(role);
CREATE INDEX idx_user_email_verified ON users(email_verified);
CREATE INDEX idx_user_last_login ON users(last_login_at DESC);

-- Create GIN index for full-text search (PostgreSQL specific)
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_user_search ON users USING GIN (
    (username || ' ' || email || ' ' || first_name || ' ' || last_name || ' ' || COALESCE(bio, '')) gin_trgm_ops
    );

-- Create user_sessions table
CREATE TABLE user_sessions (
                               id UUID DEFAULT gen_random_uuid(),
                               token VARCHAR(255) NOT NULL,
                               user_id BIGINT NOT NULL,
                               ip_address VARCHAR(45),
                               user_agent VARCHAR(500),
                               expires_at TIMESTAMP NOT NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT pk_user_sessions PRIMARY KEY (id),
                               CONSTRAINT uk_user_sessions_token UNIQUE (token),
                               CONSTRAINT fk_user_sessions_user FOREIGN KEY (user_id)
                                   REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for user_sessions table
CREATE INDEX idx_session_token ON user_sessions(token);
CREATE INDEX idx_session_expires ON user_sessions(expires_at);
CREATE INDEX idx_session_user_id ON user_sessions(user_id);

-- Create user_permissions table
CREATE TABLE user_permissions (
                                  user_id BIGINT NOT NULL,
                                  permission VARCHAR(100) NOT NULL,
                                  CONSTRAINT pk_user_permissions PRIMARY KEY (user_id, permission),
                                  CONSTRAINT fk_user_permissions_user FOREIGN KEY (user_id)
                                      REFERENCES users(id) ON DELETE CASCADE
);

-- Create audit_log table
CREATE TABLE audit_log (
                           id BIGSERIAL PRIMARY KEY,
                           user_id BIGINT,
                           action VARCHAR(50) NOT NULL,
                           entity_type VARCHAR(50) NOT NULL,
                           entity_id BIGINT,
                           old_value JSONB,
                           new_value JSONB,
                           ip_address VARCHAR(45),
                           user_agent VARCHAR(500),
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           CONSTRAINT fk_audit_log_user FOREIGN KEY (user_id)
                               REFERENCES users(id) ON DELETE SET NULL
);

-- Create indexes for audit_log table
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_created ON audit_log(created_at DESC);
CREATE INDEX idx_audit_log_action ON audit_log(action);

-- Create password_reset_tokens table
CREATE TABLE password_reset_tokens (
                                       id BIGSERIAL PRIMARY KEY,
                                       token VARCHAR(255) NOT NULL,
                                       user_id BIGINT NOT NULL,
                                       expires_at TIMESTAMP NOT NULL,
                                       used BOOLEAN NOT NULL DEFAULT FALSE,
                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       CONSTRAINT uk_password_reset_token UNIQUE (token),
                                       CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id)
                                           REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for password_reset_tokens table
CREATE INDEX idx_password_reset_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_expires ON password_reset_tokens(expires_at);

-- Create email_verification_tokens table
CREATE TABLE email_verification_tokens (
                                           id BIGSERIAL PRIMARY KEY,
                                           token VARCHAR(255) NOT NULL,
                                           user_id BIGINT NOT NULL,
                                           email VARCHAR(255) NOT NULL,
                                           expires_at TIMESTAMP NOT NULL,
                                           verified_at TIMESTAMP,
                                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           CONSTRAINT uk_email_verification_token UNIQUE (token),
                                           CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id)
                                               REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for email_verification_tokens table
CREATE INDEX idx_email_verification_token ON email_verification_tokens(token);
CREATE INDEX idx_email_verification_user_id ON email_verification_tokens(user_id);
CREATE INDEX idx_email_verification_expires ON email_verification_tokens(expires_at);

-- Create rate_limit table for tracking API rate limits
CREATE TABLE rate_limits (
                             id BIGSERIAL PRIMARY KEY,
                             identifier VARCHAR(255) NOT NULL, -- Can be user_id, IP, or API key
                             endpoint VARCHAR(255) NOT NULL,
                             request_count INTEGER NOT NULL DEFAULT 1,
                             window_start TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             window_end TIMESTAMP NOT NULL,
                             CONSTRAINT uk_rate_limit_identifier_endpoint UNIQUE (identifier, endpoint, window_start)
);

-- Create indexes for rate_limits table
CREATE INDEX idx_rate_limit_identifier ON rate_limits(identifier);
CREATE INDEX idx_rate_limit_window ON rate_limits(window_end);
CREATE INDEX idx_rate_limit_endpoint ON rate_limits(endpoint);

-- Create notifications table
CREATE TABLE notifications (
                               id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               type VARCHAR(50) NOT NULL,
                               title VARCHAR(255) NOT NULL,
                               message TEXT,
                               data JSONB,
                               read BOOLEAN NOT NULL DEFAULT FALSE,
                               read_at TIMESTAMP,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_notification_user FOREIGN KEY (user_id)
                                   REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for notifications table
CREATE INDEX idx_notification_user_id ON notifications(user_id);
CREATE INDEX idx_notification_read ON notifications(read);
CREATE INDEX idx_notification_created ON notifications(created_at DESC);
CREATE INDEX idx_notification_type ON notifications(type);

-- Insert default admin user (password: Admin123!)
INSERT INTO users (
    first_name,
    last_name,
    username,
    email,
    password,
    role,
    email_verified,
    bio
) VALUES (
             'Admin',
             'User',
             'admin',
             'admin@example.com',
             '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY/MrLJCRJQYPXC', -- Admin123!
             'ADMIN',
             TRUE,
             'System Administrator'
         );

-- Insert sample users for development
INSERT INTO users (first_name, last_name, username, email, password, role, email_verified, bio) VALUES
                                                                                                    ('John', 'Doe', 'johndoe', 'john.doe@example.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY/MrLJCRJQYPXC', 'USER', TRUE, 'Regular user'),
                                                                                                    ('Jane', 'Smith', 'janesmith', 'jane.smith@example.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY/MrLJCRJQYPXC', 'MODERATOR', TRUE, 'Content moderator'),
                                                                                                    ('Bob', 'Wilson', 'bobwilson', 'bob.wilson@example.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY/MrLJCRJQYPXC', 'USER', FALSE, 'New user'),
                                                                                                    ('Alice', 'Johnson', 'alicej', 'alice.j@example.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY/MrLJCRJQYPXC', 'USER', TRUE, 'Active member');

-- Create functions for automatic updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger for users table
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create function to clean up expired sessions
CREATE OR REPLACE FUNCTION cleanup_expired_sessions()
RETURNS INTEGER AS $$
DECLARE
deleted_count INTEGER;
BEGIN
DELETE FROM user_sessions WHERE expires_at < CURRENT_TIMESTAMP;
GET DIAGNOSTICS deleted_count = ROW_COUNT;
RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Create function to clean up expired tokens
CREATE OR REPLACE FUNCTION cleanup_expired_tokens()
RETURNS TABLE(
    password_reset_deleted INTEGER,
    email_verification_deleted INTEGER
) AS $$
DECLARE
pr_deleted INTEGER;
    ev_deleted INTEGER;
BEGIN
DELETE FROM password_reset_tokens WHERE expires_at < CURRENT_TIMESTAMP AND used = FALSE;
GET DIAGNOSTICS pr_deleted = ROW_COUNT;

DELETE FROM email_verification_tokens WHERE expires_at < CURRENT_TIMESTAMP AND verified_at IS NULL;
GET DIAGNOSTICS ev_deleted = ROW_COUNT;

RETURN QUERY SELECT pr_deleted, ev_deleted;
END;
$$ LANGUAGE plpgsql;

-- Create materialized view for user statistics (refresh periodically)
CREATE MATERIALIZED VIEW user_statistics AS
SELECT
    COUNT(*) as total_users,
    COUNT(*) FILTER (WHERE active = TRUE) as active_users,
    COUNT(*) FILTER (WHERE active = FALSE) as inactive_users,
    COUNT(*) FILTER (WHERE email_verified = TRUE) as verified_users,
    COUNT(*) FILTER (WHERE role = 'ADMIN') as admin_count,
    COUNT(*) FILTER (WHERE role = 'MODERATOR') as moderator_count,
    COUNT(*) FILTER (WHERE role = 'USER') as user_count,
    COUNT(*) FILTER (WHERE created_at >= CURRENT_DATE - INTERVAL '30 days') as new_users_30d,
    COUNT(*) FILTER (WHERE last_login_at >= CURRENT_DATE - INTERVAL '7 days') as active_users_7d
FROM users;

-- Create index on materialized view
CREATE UNIQUE INDEX idx_user_statistics ON user_statistics(total_users);

-- Grant permissions (adjust as needed)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO your_app_user;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO your_app_user;