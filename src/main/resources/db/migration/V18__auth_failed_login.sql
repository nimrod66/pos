-- Durable failed-login tracking for brute-force lockout.
CREATE TABLE public.auth_failed_login (
    email        varchar(255) PRIMARY KEY,
    failure_count integer NOT NULL,
    last_failure timestamp NOT NULL
);
