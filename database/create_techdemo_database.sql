/*
 * Run this file once while connected to the SQL Server instance (for example,
 * in SQL Server Management Studio). Flyway will create the tables and load the
 * sample data when the Spring Boot application starts.
 */
USE [master];
GO

IF DB_ID(N'TechDemo') IS NULL
BEGIN
    CREATE DATABASE [TechDemo];
END;
GO

ALTER DATABASE [TechDemo] SET RECOVERY SIMPLE;
GO

USE [TechDemo];
GO

SELECT
    DB_NAME() AS database_name,
    N'Database is ready. Start the application so Flyway can apply V1 and V2.' AS message;
GO
