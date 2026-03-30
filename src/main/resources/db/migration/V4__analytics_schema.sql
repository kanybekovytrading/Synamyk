-- Video lessons for "Видео уроки" tab in Анализ screen
CREATE TABLE IF NOT EXISTS video_lessons
(
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(500)  NOT NULL,
    description   TEXT,
    thumbnail_url VARCHAR(1000),
    video_url     VARCHAR(1000) NOT NULL,
    test_id       BIGINT REFERENCES tests (id),
    order_index   INT           NOT NULL DEFAULT 0,
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL,
    updated_at    TIMESTAMP     NOT NULL
);