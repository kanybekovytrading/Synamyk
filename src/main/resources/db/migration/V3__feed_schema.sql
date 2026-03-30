-- Add correct_answers counter to test_sessions for rating queries
ALTER TABLE test_sessions ADD COLUMN IF NOT EXISTS correct_answers INT NOT NULL DEFAULT 0;

-- News articles
CREATE TABLE IF NOT EXISTS news_articles
(
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(500)  NOT NULL,
    cover_image_url VARCHAR(1000),
    content         TEXT          NOT NULL,
    published_at    TIMESTAMP     NOT NULL,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL
);