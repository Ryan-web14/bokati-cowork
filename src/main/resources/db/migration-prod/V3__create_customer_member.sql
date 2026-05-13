
-- =========================================================
-- 1) role / permission
-- =========================================================
CREATE TABLE IF NOT EXISTS role (
                                     id             BIGINT PRIMARY KEY,
                                     name           VARCHAR(250) NOT NULL UNIQUE,
                                     display_name   VARCHAR(255),
                                     description    TEXT,
                                     is_system_role BOOLEAN NOT NULL DEFAULT FALSE,
                                     version        INTEGER,
                                     created_at     TIMESTAMP,
                                     updated_at     TIMESTAMP,
                                     is_active      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_role_name ON role(name);
CREATE INDEX IF NOT EXISTS idx_role_is_active ON role(is_active);

CREATE TABLE IF NOT EXISTS permission (
                                           id                   BIGINT PRIMARY KEY,
                                           name                 VARCHAR(100) NOT NULL,
                                           display_name         VARCHAR(100),
                                           module               VARCHAR(50) NOT NULL,
                                           action               VARCHAR(20) NOT NULL,
                                           is_system_permission BOOLEAN NOT NULL DEFAULT FALSE,
                                           is_active            BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS role_permission (
                                                role_id       BIGINT NOT NULL,
                                                permission_id BIGINT NOT NULL,
                                                created_by    VARCHAR(255) NOT NULL,
                                                is_active     BOOLEAN NOT NULL DEFAULT TRUE,
                                                CONSTRAINT pk_role_permission PRIMARY KEY (role_id, permission_id),
                                                CONSTRAINT role_fk FOREIGN KEY (role_id) REFERENCES role(id),
                                                CONSTRAINT permission_fk FOREIGN KEY (permission_id) REFERENCES permission(id)
);

-- =========================================================
-- 2) users
-- =========================================================
CREATE TABLE IF NOT EXISTS users (
                                     id                    BIGINT PRIMARY KEY,
                                     user_id               VARCHAR(120) NOT NULL UNIQUE,
                                     email                 VARCHAR(350) NOT NULL UNIQUE,
                                     passwordHash          VARCHAR(250) NOT NULL,
                                     user_profile_id       BIGINT UNIQUE,
                                     last_login            TIMESTAMP,
                                     created_at            TIMESTAMP NOT NULL,
                                     updated_at            TIMESTAMP,
                                     deleted               BOOLEAN NOT NULL DEFAULT FALSE,
                                     is_account_expired    BOOLEAN,
                                     is_account_locked     BOOLEAN,
                                     is_account_enabled    BOOLEAN,
                                     failed_login_attempts INTEGER
);

CREATE INDEX IF NOT EXISTS idx_users_user_id ON users(user_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_deleted ON users(deleted);

-- =========================================================
-- 3) audit_log
-- =========================================================
CREATE TABLE IF NOT EXISTS audit_log (
                                         id            BIGINT PRIMARY KEY,
                                         created_at    TIMESTAMP NOT NULL,
                                         actor_id      BIGINT NOT NULL,
                                         actor_email   VARCHAR(255) NOT NULL,
                                         action        VARCHAR(255),
                                         ressource     VARCHAR(350),
                                         audit_status  VARCHAR(60),
                                         module        VARCHAR(120) NOT NULL,
                                         ip_address    VARCHAR(60),
                                         user_agent    TEXT,
                                         session_id    VARCHAR(120),
                                         error_code    VARCHAR(120),
                                         error_message TEXT,
                                         metadata_json JSONB,
                                         diff_json     JSONB
);

CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_actor_id ON audit_log(actor_id);

-- =========================================================
-- 4) user_sessions
-- =========================================================
CREATE TABLE IF NOT EXISTS user_sessions (
    id             BIGINT PRIMARY KEY,
    session_id     UUID NOT NULL UNIQUE,
    user_id        BIGINT NOT NULL,
    refresh_token  TEXT,
    created_at     TIMESTAMP NOT NULL,
    expired_at     TIMESTAMP NOT NULL,
    last_seen      TIMESTAMP NOT NULL,
    expires_in     BIGINT NOT NULL,
    ip_address     VARCHAR(60),
    user_agent     TEXT,
    device_type    VARCHAR(120),
    is_active      BOOLEAN NOT NULL,
    revoked        BOOLEAN NOT NULL,
    role_snapshot  VARCHAR(120),
    location_guess VARCHAR(120),
    CONSTRAINT user_sessions_user_fk FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id ON user_sessions(user_id);

-- =========================================================
-- 5) one_time_token
-- =========================================================
CREATE TABLE IF NOT EXISTS one_time_token (
  id         BIGINT PRIMARY KEY,
  token      VARCHAR(255),
  user_id    BIGINT NOT NULL,
  is_used    BOOLEAN,
  created_at TIMESTAMP,
  expired_at TIMESTAMP,
  expiration BIGINT,
  CONSTRAINT one_time_token_user_fk FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_one_time_token_user_id ON one_time_token(user_id);

-- =========================================================
-- 6) password_reset_token
-- =========================================================
CREATE TABLE IF NOT EXISTS password_reset_token (
                                                    id             BIGINT PRIMARY KEY,
                                                    password_token VARCHAR(255),
                                                    user_id        BIGINT NOT NULL,
                                                    expiry_date    TIMESTAMP NOT NULL,
                                                    used           BOOLEAN NOT NULL,
                                                    created_at     TIMESTAMP NOT NULL,
                                                    expiration     BIGINT,
                                                    CONSTRAINT fk_user_token
                                                        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_password_reset_token_user_id ON password_reset_token(user_id);

-- =========================================================
-- 7) refresh_token
-- =========================================================
CREATE TABLE IF NOT EXISTS refresh_token (
                                             id         BIGINT PRIMARY KEY,
                                             token      TEXT,
                                             user_id    BIGINT NOT NULL,
                                             expiration TIMESTAMP,
                                             revoked    BOOLEAN NOT NULL DEFAULT FALSE,
                                             CONSTRAINT fk_user_refresh_token FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON refresh_token(user_id);

CREATE TABLE IF NOT EXISTS role_user (
                                         user_id     BIGINT NOT NULL,
                                         role_id     BIGINT NOT NULL,
                                         assigned_by VARCHAR(255) NOT NULL,
                                         assigned_at TIMESTAMP NOT NULL,
                                         updated_at  TIMESTAMP,
                                         CONSTRAINT pk_role_user PRIMARY KEY (user_id, role_id),
                                         CONSTRAINT fk_role_user_user FOREIGN KEY (user_id) REFERENCES users(id),
                                         CONSTRAINT fk_role_user_role FOREIGN KEY (role_id) REFERENCES role(id)
);

CREATE INDEX IF NOT EXISTS idx_role_user_user_id ON role_user(user_id);
CREATE INDEX IF NOT EXISTS idx_role_user_role_id ON role_user(role_id);



CREATE TABLE IF NOT EXISTS address (
    id BIGINT PRIMARY KEY,
    address VARCHAR(255),
    city VARCHAR(120),
    country VARCHAR(120),
    postal_code VARCHAR(12)
);


CREATE TABLE IF NOT EXISTS customer (
    id BIGINT PRIMARY KEY,
    customer_id VARCHAR(300) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    firstname VARCHAR(250),
    lastname VARCHAR(250),
    fullname VARCHAR(500),
    address_id BIGINT NOT NULL,
    company_name VARCHAR(150),
    billing_email VARCHAR(150),
    phone VARCHAR(30),
    whatsapp_phone VARCHAR(12),
    status VARCHAR(20) NOT NULL,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_customer_address FOREIGN KEY (address_id) REFERENCES address(id) ON DELETE CASCADE

);

CREATE TABLE IF NOT EXISTS member (
    id BIGINT PRIMARY KEY,
    member_id VARCHAR(50) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    firstname VARCHAR(250),
    lastname VARCHAR(250),
    address_id BIGINT NOT NULL,
    email VARCHAR(200),
    phone VARCHAR(12),
    whatsapp_phone VARCHAR(12),
    status VARCHAR(20) NOT NULL,
    portal_access BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_member_customer FOREIGN KEY (customer_id) REFERENCES customer(id),

    CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_member_address FOREIGN KEY (address_id) REFERENCES address(id) ON DELETE CASCADE

);

CREATE TABLE IF NOT EXISTS member_profile (
    id BIGINT PRIMARY KEY,
    member_id BIGINT NOT NULL UNIQUE,
    birth_date DATE,
    job_title VARCHAR(120),
    company_role VARCHAR(120),
    address VARCHAR(255),
    city VARCHAR(120),
    country VARCHAR(120),
    emergency_contact_name VARCHAR(150),
    emergency_contact_phone VARCHAR(30),
    photo_url TEXT,

    CONSTRAINT fk_profile_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE INDEX IF NOT EXISTS idx_member_customer_id ON member(customer_id);
CREATE INDEX IF NOT EXISTS  idx_member_email ON member(email);
CREATE INDEX IF NOT EXISTS idx_member_phone ON member(phone);


CREATE OR REPLACE FUNCTION set_customer_full_name()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    -- Si les deux sont null → full_name = NULL
    IF NEW.first_name IS NULL AND NEW.last_name IS NULL THEN
        NEW.full_name := NULL;

    -- Si seulement prénom
    ELSIF NEW.last_name IS NULL THEN
        NEW.full_name := trim(NEW.first_name);

    -- Si seulement nom
    ELSIF NEW.first_name IS NULL THEN
        NEW.full_name := trim(NEW.last_name);

    -- Les deux présents
    ELSE
        NEW.full_name := trim(NEW.first_name) || ' ' || trim(NEW.last_name);
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION set_customer_full_name()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.full_name := NULLIF(
        trim(concat_ws(' ', NEW.first_name, NEW.last_name)),
        ''
    );

    RETURN NEW;
END;
$$;

