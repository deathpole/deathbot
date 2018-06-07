CREATE SEQUENCE IF NOT EXISTS ACTIVATION_STATE_seq
INCREMENT 1
MINVALUE 1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

CREATE TABLE IF NOT EXISTS ACTIVATION_STATE
(
    ID INTEGER PRIMARY KEY NOT NULL DEFAULT nextval('ACTIVATION_STATE_seq'::regclass),
    GUILD_NAME VARCHAR(256) NOT NULL,
    ACTIVATED BOOLEAN DEFAULT FALSE NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS "ACTIVATION_STATE_ID_uindex" ON ACTIVATION_STATE (ID);

CREATE SEQUENCE IF NOT EXISTS ASSIGNABLE_RANKS_seq
INCREMENT 1
MINVALUE 1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

CREATE TABLE IF NOT EXISTS ASSIGNABLE_RANKS
(
    ID INTEGER PRIMARY KEY NOT NULL DEFAULT nextval('ASSIGNABLE_RANKS_seq'::regclass),
    GUILD_NAME VARCHAR(256) NOT NULL,
    ROLE_NAME VARCHAR(256) NOT NULL,
    SINGLE BOOLEAN DEFAULT TRUE NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS "ASSIGNABLE_RANKS_id_uindex" ON ASSIGNABLE_RANKS (ID);

CREATE SEQUENCE IF NOT EXISTS WELCOME_MESSAGE_seq
INCREMENT 1
MINVALUE 1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

CREATE TABLE IF NOT EXISTS WELCOME_MESSAGE
(
     ID INTEGER PRIMARY KEY NOT NULL DEFAULT nextval('WELCOME_MESSAGE_seq'::regclass),
    GUILD_NAME VARCHAR(256) NOT NULL,
    WELCOME_MESSAGE VARCHAR
);
CREATE UNIQUE INDEX IF NOT EXISTS "WELCOME_MESSAGE_GUILD_NAME_uindex" ON WELCOME_MESSAGE (ID);

CREATE SEQUENCE IF NOT EXISTS CUSTOM_REACTION_seq
INCREMENT 1
MINVALUE 1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

CREATE TABLE IF NOT EXISTS CUSTOM_REACTION
(
     ID INTEGER PRIMARY KEY NOT NULL DEFAULT nextval('CUSTOM_REACTION_seq'::regclass),
    GUILD_NAME varchar(256) NOT NULL,
    COMMAND varchar(256) NOT NULL,
    REACTION varchar,
    NUMBER_OF_PARAMS integer NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS "CUSTOM_REACTION_ID_uindex" ON CUSTOM_REACTION (ID);

CREATE SEQUENCE IF NOT EXISTS VOICE_ROLES_seq
INCREMENT 1
MINVALUE 1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

CREATE TABLE IF NOT EXISTS VOICE_ROLES
(
     ID INTEGER PRIMARY KEY NOT NULL DEFAULT nextval('VOICE_ROLES_seq'::regclass),
    GUILD_NAME varchar(256) NOT NULL,
    CHANNEL_NAME varchar(256) NOT NULL,
    ROLE varchar
);
CREATE UNIQUE INDEX IF NOT EXISTS "VOICE_ROLES_ID_uindex" ON VOICE_ROLES (ID);


CREATE SEQUENCE IF NOT EXISTS REMINDER_seq
INCREMENT 1
MINVALUE 1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

CREATE TABLE IF NOT EXISTS REMINDER
(
    ID INTEGER PRIMARY KEY NOT NULL DEFAULT nextval('REMINDER_seq'::regclass),
    GUILD_NAME varchar(256) NOT NULL,
    TITLE varchar(256) NOT NULL,
    TEXT varchar NOT NULL,
    CHAN varchar NOT NULL,
    CRONTAB varchar(64) NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS "REMINDER_ID_uindex" ON REMINDER (ID);

CREATE SEQUENCE IF NOT EXISTS ONJOIN_RANKS_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;

CREATE TABLE IF NOT EXISTS ONJOIN_RANKS
(
  ID INTEGER PRIMARY KEY NOT NULL DEFAULT nextval('ONJOIN_RANKS_seq'::regclass),
  GUILD_NAME VARCHAR(256) NOT NULL,
  ROLE_NAME VARCHAR(256) NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS "ONJOIN_RANKS_id_uindex" ON ONJOIN_RANKS (ID);


CREATE SEQUENCE IF NOT EXISTS DYNO_ACTIONS_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;

CREATE TABLE IF NOT EXISTS DYNO_ACTIONS
(
    id serial PRIMARY KEY NOT NULL,
    guild_name varchar(256) NOT NULL,
    action varchar NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS "DYNO_ACTIONS_id_uindex" ON DYNO_ACTIONS (ID);