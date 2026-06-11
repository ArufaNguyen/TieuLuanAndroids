CREATE DATABASE SmartCalendarDB;
GO

USE SmartCalendarDB;
GO

CREATE TABLE users (
    id INT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(100) NOT NULL UNIQUE,
    email NVARCHAR(255) NOT NULL UNIQUE,
    full_name NVARCHAR(255)
);

CREATE TABLE accounts (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    username NVARCHAR(100) NOT NULL UNIQUE,
    login_name NVARCHAR(100) NOT NULL UNIQUE,
    password_hash NVARCHAR(255) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),

    CONSTRAINT fk_accounts_users
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

CREATE TABLE sessions (
    id INT IDENTITY(1,1) PRIMARY KEY,
    session_token NVARCHAR(255) NOT NULL UNIQUE,
    account_id INT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    expires_at DATETIME2 NOT NULL,
    active BIT NOT NULL DEFAULT 1,

    CONSTRAINT fk_sessions_accounts
        FOREIGN KEY (account_id)
        REFERENCES accounts(id)
);

CREATE TABLE tags (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    color NVARCHAR(50),
    user_id INT NULL,

    CONSTRAINT fk_tags_users
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

CREATE TABLE events (
    id INT IDENTITY(1,1) PRIMARY KEY,
    title NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX),
    start_time DATETIME2 NOT NULL,
    end_time DATETIME2 NOT NULL,
    tag_id INT NULL,
    user_id INT NULL,

    CONSTRAINT fk_events_tags
        FOREIGN KEY (tag_id)
        REFERENCES tags(id),

    CONSTRAINT fk_events_users
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);
