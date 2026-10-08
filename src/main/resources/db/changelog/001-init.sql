CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE clients (
  id UUID PRIMARY KEY,
  first_name VARCHAR(120) NOT NULL,
  last_name VARCHAR(120) NOT NULL,
  email VARCHAR(320) NOT NULL UNIQUE,
  description TEXT,
  social_links JSONB NOT NULL DEFAULT '[]'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE documents (
  id UUID PRIMARY KEY,
  client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  content TEXT NOT NULL,
  summary TEXT NULL,
  embedding VECTOR(384) NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_documents_embedding_hnsw
  ON documents USING hnsw (embedding vector_cosine_ops);

CREATE INDEX idx_documents_title_trgm
  ON documents USING gin (title gin_trgm_ops);

CREATE INDEX idx_documents_content_trgm
  ON documents USING gin (content gin_trgm_ops);

CREATE INDEX idx_documents_client_id ON documents (client_id);
CREATE INDEX idx_documents_created_at ON documents (created_at);
