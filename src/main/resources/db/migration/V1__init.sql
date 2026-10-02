CREATE TABLE tags (
    id   BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE tasks (
    id       BIGSERIAL PRIMARY KEY,
    title    TEXT NOT NULL,
    done     BOOLEAN NOT NULL DEFAULT FALSE,
    priority TEXT NOT NULL DEFAULT 'MEDIUM'
                 CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    tag_id   BIGINT REFERENCES tags (id) ON DELETE SET NULL
);

CREATE INDEX idx_tasks_tag_id ON tasks (tag_id);
