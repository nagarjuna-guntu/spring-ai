DROP TABLE IF EXISTS game;

CREATE TABLE game (
    id	        SERIAL PRIMARY KEY NOT NULL,
    title       VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL,
    complexity  FLOAT NOT NULL
);