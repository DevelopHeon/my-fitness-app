CREATE TABLE ai_conversations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_ai_conversations_user_updated
    ON ai_conversations (user_id, updated_at DESC);

CREATE TABLE ai_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    query_type VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_ai_messages_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES ai_conversations(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_ai_messages_conversation_created
    ON ai_messages (conversation_id, created_at, id);

CREATE TABLE ai_request_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    user_message_id BIGINT NOT NULL,
    assistant_message_id BIGINT,
    query_type VARCHAR(30) NOT NULL,
    provider VARCHAR(40),
    model VARCHAR(100),
    prompt_version VARCHAR(50) NOT NULL,
    input_tokens INTEGER,
    output_tokens INTEGER,
    total_tokens INTEGER,
    latency_ms BIGINT,
    status VARCHAR(30) NOT NULL,
    error_code VARCHAR(80),
    context_types VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_ai_request_logs_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES ai_conversations(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_ai_request_logs_user_created
    ON ai_request_logs (user_id, created_at DESC);

CREATE INDEX idx_ai_request_logs_query_created
    ON ai_request_logs (query_type, created_at DESC);
