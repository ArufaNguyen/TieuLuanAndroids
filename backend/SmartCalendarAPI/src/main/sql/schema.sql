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
    password VARCHAR(255) NOT NULL,
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

CREATE TABLE discovery_jobs (
    id NVARCHAR(36) PRIMARY KEY,
    user_id INT NULL,
    status NVARCHAR(30) NOT NULL,
    file_name NVARCHAR(255),
    result_json NVARCHAR(MAX),
    error_message NVARCHAR(MAX),
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    completed_at DATETIME2,

    CONSTRAINT fk_discovery_jobs_users
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

CREATE TABLE api_knowledge (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NULL,
    discovery_job_id NVARCHAR(36) NULL,
    natural_key NVARCHAR(64) NOT NULL,
    tool_name NVARCHAR(255) NOT NULL,
    portal_url NVARCHAR(1000) NOT NULL,
    method NVARCHAR(20) NOT NULL,
    category NVARCHAR(100) NOT NULL,
    knowledge_json NVARCHAR(MAX) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),

    CONSTRAINT fk_api_knowledge_users
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_api_knowledge_discovery_jobs
        FOREIGN KEY (discovery_job_id)
        REFERENCES discovery_jobs(id)
);

CREATE UNIQUE INDEX ux_api_knowledge_scope_natural_key
    ON api_knowledge(user_id, natural_key);
