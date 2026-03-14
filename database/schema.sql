-- =============================================
-- Base de datos: proyecto_n8n
-- Asistente de vestimenta por clima
-- =============================================

CREATE DATABASE IF NOT EXISTS proyecto_n8n
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE proyecto_n8n;

-- =============================================
-- Tabla: users
-- Almacena usuarios opcionales del sistema
-- =============================================
CREATE TABLE IF NOT EXISTS `users` (
  `id`         INT           NOT NULL AUTO_INCREMENT,
  `name`       VARCHAR(100)  NULL,
  `email`      VARCHAR(150)  NULL,
  `created_at` TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- Tabla: weather_queries
-- Almacena el historial de consultas de clima
-- =============================================
CREATE TABLE IF NOT EXISTS `weather_queries` (
  `id`                      INT           NOT NULL AUTO_INCREMENT,
  `origin`                  VARCHAR(150)  NOT NULL,
  `destination`             VARCHAR(150)  NOT NULL,
  `travel_date`             DATE          NOT NULL,
  `transport`               VARCHAR(20)   NOT NULL,
  `origin_temperature`      DECIMAL(5,2)  NULL,
  `destination_temperature` DECIMAL(5,2)  NULL,
  `origin_condition`        VARCHAR(100)  NULL,
  `destination_condition`   VARCHAR(100)  NULL,
  `recommended_clothes`     TEXT          NOT NULL,
  `user_id`                 INT           NULL,
  `created_at`              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_user`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
