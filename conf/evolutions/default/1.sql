# --- !Ups

CREATE TABLE Location(
  id bigint(20) NOT NULL AUTO_INCREMENT,
  name varchar(255) UNIQUE NOT NULL,
  angelId bigint(20) NOT NULL UNIQUE,
  kind varchar(255),
  PRIMARY KEY (id)
);