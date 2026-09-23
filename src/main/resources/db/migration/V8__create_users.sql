CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    google_subject VARCHAR(255) NOT NULL,
    email VARCHAR(320),
    display_name VARCHAR(255),
    profile_image_url VARCHAR(1024),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    last_login_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_users_google_subject UNIQUE (google_subject)
);

CREATE INDEX idx_users_email
    ON users (email);
