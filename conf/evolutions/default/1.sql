# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table "DATABASE_UPDATE" ("GUTE_DATE" TIMESTAMP NOT NULL,"FOLDER" VARCHAR NOT NULL,"ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY);
create table "LOCATION" ("NAME" VARCHAR NOT NULL,"ANGEL_ID" BIGINT NOT NULL,"KIND" VARCHAR NOT NULL,"ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY);
create table "MARKET" ("NAME" VARCHAR NOT NULL,"ANGEL_ID" BIGINT NOT NULL,"ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY);
create table "STARTUP_LOCATION" ("STARTUP_ID" BIGINT NOT NULL,"LOCATION_ID" BIGINT NOT NULL,"ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY);
create table "STARTUP_MARKET" ("MARKET_ID" BIGINT NOT NULL,"STARTUP_ID" BIGINT NOT NULL,"ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY);
create table "STARTUP" ("NAME" VARCHAR NOT NULL,"ANGEL_ID" BIGINT NOT NULL,"QUALITY" INTEGER NOT NULL,"CREATION_DATE" DATE NOT NULL,"ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY);
create table "USERS" ("USERNAME" VARCHAR NOT NULL,"PASSWORD" VARCHAR NOT NULL,"FIRST_NAME" VARCHAR NOT NULL,"LAST_NAME" VARCHAR NOT NULL,"ROLE" VARCHAR NOT NULL,"ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY);

# --- !Downs

drop table "DATABASE_UPDATE";
drop table "LOCATION";
drop table "MARKET";
drop table "STARTUP_LOCATION";
drop table "STARTUP_MARKET";
drop table "STARTUP";
drop table "USERS";

