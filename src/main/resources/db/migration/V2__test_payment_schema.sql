-- Tests (main test, e.g. ОРТ)
CREATE TABLE IF NOT EXISTS tests
(
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255)   NOT NULL,
    description TEXT,
    icon_url    VARCHAR(500),
    price       NUMERIC(10, 2) NOT NULL DEFAULT 0,
    active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP      NOT NULL,
    updated_at  TIMESTAMP      NOT NULL
);

-- Sub-tests (levels within a test, e.g. "Бесплатный уровень", "1-уровень")
CREATE TABLE IF NOT EXISTS sub_tests
(
    id               BIGSERIAL PRIMARY KEY,
    test_id          BIGINT       NOT NULL REFERENCES tests (id) ON DELETE CASCADE,
    title            VARCHAR(255) NOT NULL,
    level_name       VARCHAR(100) NOT NULL,
    level_order      INT          NOT NULL DEFAULT 0,
    is_paid          BOOLEAN      NOT NULL DEFAULT FALSE,
    duration_minutes INT          NOT NULL DEFAULT 30,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL
);

-- Questions within a sub-test
CREATE TABLE IF NOT EXISTS questions
(
    id           BIGSERIAL PRIMARY KEY,
    sub_test_id  BIGINT  NOT NULL REFERENCES sub_tests (id) ON DELETE CASCADE,
    section_name VARCHAR(255),  -- optional section, e.g. "1-часть: Математика"
    text         TEXT    NOT NULL,
    image_url    VARCHAR(500),
    explanation  TEXT,          -- stored explanation for AI analysis fallback
    order_index  INT     NOT NULL DEFAULT 0,
    point_value  INT     NOT NULL DEFAULT 1,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP NOT NULL
);

-- Answer options for a question
CREATE TABLE IF NOT EXISTS answer_options
(
    id          BIGSERIAL PRIMARY KEY,
    question_id BIGINT       NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    label       VARCHAR(5)   NOT NULL, -- A, Б, В, Д
    text        TEXT         NOT NULL,
    is_correct  BOOLEAN      NOT NULL DEFAULT FALSE,
    order_index INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

-- User test access (granted after payment)
CREATE TABLE IF NOT EXISTS user_test_access
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    test_id    BIGINT    NOT NULL REFERENCES tests (id) ON DELETE CASCADE,
    granted_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (user_id, test_id)
);

-- Test sessions (a user taking a sub-test)
CREATE TABLE IF NOT EXISTS test_sessions
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users (id),
    sub_test_id     BIGINT      NOT NULL REFERENCES sub_tests (id),
    status          VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS', -- IN_PROGRESS, PAUSED, COMPLETED, ABANDONED, EXPIRED
    current_index   INT         NOT NULL DEFAULT 0,             -- current question index
    started_at      TIMESTAMP   NOT NULL,
    expires_at      TIMESTAMP   NOT NULL,
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP   NOT NULL
);

-- Individual answers within a session
CREATE TABLE IF NOT EXISTS user_answers
(
    id                 BIGSERIAL PRIMARY KEY,
    session_id         BIGINT  NOT NULL REFERENCES test_sessions (id) ON DELETE CASCADE,
    question_id        BIGINT  NOT NULL REFERENCES questions (id),
    selected_option_id BIGINT REFERENCES answer_options (id),
    is_correct         BOOLEAN NOT NULL DEFAULT FALSE,
    is_skipped         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP NOT NULL,
    updated_at         TIMESTAMP NOT NULL
);

-- Payments (Finik)
CREATE TABLE IF NOT EXISTS payments
(
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT         NOT NULL REFERENCES users (id),
    test_id        BIGINT         NOT NULL REFERENCES tests (id),
    payment_id     UUID           NOT NULL UNIQUE,
    amount         NUMERIC(10, 2) NOT NULL,
    payment_url    VARCHAR(1000),
    status         VARCHAR(20)    NOT NULL DEFAULT 'PENDING', -- PENDING, COMPLETED, EXPIRED, CANCELLED
    transaction_id VARCHAR(255) UNIQUE,
    receipt_number VARCHAR(255),
    webhook_data   TEXT,
    paid_at        TIMESTAMP,
    created_at     TIMESTAMP      NOT NULL,
    updated_at     TIMESTAMP      NOT NULL
);