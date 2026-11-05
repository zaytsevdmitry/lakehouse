-- Airflow metadata database.
-- It is created in the same PostgreSQL instance as the yarina services, but in
-- a separate database so that `airflow db migrate` never touches their schema.
CREATE DATABASE airflow;
