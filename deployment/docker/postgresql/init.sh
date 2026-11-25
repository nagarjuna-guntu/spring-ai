#!/bin/bash
set -e

# 1. Create Both Databases (Connects to default maintenance DB first)
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
	CREATE DATABASE boarddb_games;
	CREATE DATABASE boarddb_players;
	CREATE DATABASE boarddb_chats;
	CREATE DATABASE boardgame_ingestion;
	CREATE DATABASE rag_ingestion;
EOSQL

# 2. Configure boardgame_ingestion DB (The "Brain" - Enable Vector Search)
# 🧠 Needs 'vector' for AI Search
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "boardgame_ingestion" <<-EOSQL
	CREATE EXTENSION IF NOT EXISTS vector;
	CREATE EXTENSION IF NOT EXISTS hstore;
	CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


	CREATE TABLE IF NOT EXISTS board_game_vectors (
    	id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    	content text,
    	metadata json,
    	embedding vector(768) --// 1536 is the default embedding dimension
    );

	CREATE INDEX ON board_game_vectors USING HNSW (embedding vector_cosine_ops);
EOSQL

# 2. Configure rag_ingestion DB (The "Brain" - Enable Vector Search)
# 🧠 Needs 'vector' for AI Search
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "rag_ingestion" <<-EOSQL
	CREATE EXTENSION IF NOT EXISTS vector;
	CREATE EXTENSION IF NOT EXISTS hstore;
	CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


	CREATE EXTENSION IF NOT EXISTS vector;
    CREATE EXTENSION IF NOT EXISTS hstore;
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

    CREATE TABLE IF NOT EXISTS board_game_vectors (
        id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
        content text,
        metadata json,
        embedding vector(768)
    );

    CREATE TABLE IF NOT EXISTS spring_doc_vectors (
        id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
        content text,
        metadata json,
        embedding vector(768)
    );

    CREATE INDEX IF NOT EXISTS board_game_vectors_embedding_idx
        ON board_game_vectors
        USING HNSW (embedding vector_cosine_ops);

    CREATE INDEX IF NOT EXISTS spring_doc_vectors_embedding_idx
        ON spring_doc_vectors
        USING HNSW (embedding vector_cosine_ops);
EOSQL

# 3. Configure other DBs (The "Ledger" - Standard Relational)
# 📝 Needs 'uuid-ossp' for IDs, but NO vector math
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "boarddb_games" <<-EOSQL
	CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "boarddb_players" <<-EOSQL
	CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "boarddb_chats" <<-EOSQL
	CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
EOSQL
