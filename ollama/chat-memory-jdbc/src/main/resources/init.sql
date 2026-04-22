CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY
(
    id                  UUID  PRIMARY KEY,
    conversation_id     VARCHAR(255) NOT NULL,
    content             TEXT,
    metadata            JSONB,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_spring_ai_chat_memory_conv_id
ON SPRING_AI_CHAT_MEMORY(conversation_id);
