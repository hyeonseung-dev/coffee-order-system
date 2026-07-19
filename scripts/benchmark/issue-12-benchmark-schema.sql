-- Issue #12 전용 benchmark 스키마.
-- 일반 개발 DB의 초기화 데이터와 측정 fixture가 섞이지 않도록 별도 DB만 재생성한다.

DROP DATABASE IF EXISTS coffee_order_issue12_benchmark;
CREATE DATABASE coffee_order_issue12_benchmark
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE coffee_order_issue12_benchmark;

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    name VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE TABLE menu (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    name VARCHAR(100) NOT NULL,
    price BIGINT NOT NULL,
    status ENUM ('ACTIVE', 'INACTIVE') NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_price BIGINT NOT NULL,
    ordered_at DATETIME(6) NOT NULL,
    status ENUM ('COMPLETED') NOT NULL,
    menu_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_orders_menu_id (menu_id),
    INDEX idx_orders_user_id (user_id),
    CONSTRAINT fk_benchmark_orders_menu FOREIGN KEY (menu_id) REFERENCES menu (id),
    CONSTRAINT fk_benchmark_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB;
