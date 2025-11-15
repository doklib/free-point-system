-- ============================================================================
-- Free Point System - Database Schema
-- ============================================================================
-- H2, MySQL 호환 스키마
-- 개발: JPA ddl-auto: create-drop (자동 생성)
-- ============================================================================

-- ============================================================================
-- 1. 포인트 트랜잭션 테이블 (point_transactions)
-- ============================================================================

CREATE TABLE point_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    point_key VARCHAR(50) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL,
    available_balance BIGINT NOT NULL,
    is_manual_grant BOOLEAN NOT NULL,
    expiration_date TIMESTAMP NULL,
    order_number VARCHAR(100) NULL,
    reference_point_key VARCHAR(50) NULL,
    description TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 인덱스
CREATE UNIQUE INDEX idx_point_key ON point_transactions(point_key);
CREATE INDEX idx_user_id ON point_transactions(user_id);
CREATE INDEX idx_user_type_created ON point_transactions(user_id, transaction_type, created_at);
CREATE INDEX idx_available_points ON point_transactions(user_id, transaction_type, available_balance, expiration_date, is_manual_grant, created_at);
CREATE INDEX idx_order_number ON point_transactions(order_number);

-- ============================================================================
-- 2. 사용자 포인트 요약 테이블 (user_point_summaries)
-- ============================================================================

CREATE TABLE user_point_summaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    total_balance BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 인덱스
CREATE UNIQUE INDEX idx_user_point_summary_user_id ON user_point_summaries(user_id);

-- ============================================================================
-- 3. 포인트 계정 테이블 (point_accounts)
-- ============================================================================

CREATE TABLE point_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    use_point_key VARCHAR(50) NOT NULL,
    earn_point_key VARCHAR(50) NOT NULL,
    used_amount BIGINT NOT NULL,
    canceled_amount BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 인덱스
CREATE INDEX idx_use_point_key ON point_accounts(use_point_key);
CREATE INDEX idx_earn_point_key ON point_accounts(earn_point_key);

-- ============================================================================
-- 4. 시스템 설정 테이블 (system_configs)
-- ============================================================================

CREATE TABLE system_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(500) NOT NULL,
    description TEXT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 인덱스
CREATE UNIQUE INDEX idx_config_key ON system_configs(config_key);

-- ============================================================================
-- 5. 멱등성 레코드 테이블 (idempotency_records)
-- ============================================================================

CREATE TABLE idempotency_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    response_body TEXT NOT NULL,
    status_code INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

-- 인덱스
CREATE UNIQUE INDEX idx_idempotency_key ON idempotency_records(idempotency_key);
CREATE INDEX idx_expires_at ON idempotency_records(expires_at);

-- ============================================================================
-- 초기 데이터
-- ============================================================================

INSERT INTO system_configs (config_key, config_value, description, updated_at) VALUES
('point.max.earn.per.transaction', '100000', '1회 최대 적립 가능 포인트 한도', CURRENT_TIMESTAMP),
('point.max.balance.per.user', '10000000', '개인별 최대 보유 가능 포인트 한도', CURRENT_TIMESTAMP),
('point.default.expiration.days', '365', '기본 포인트 만료일 (일)', CURRENT_TIMESTAMP),
('point.min.expiration.days', '1', '최소 포인트 만료일 (일)', CURRENT_TIMESTAMP),
('point.max.expiration.days', '1825', '최대 포인트 만료일 (일, 5년)', CURRENT_TIMESTAMP);

-- ============================================================================
-- 호환성 노트
-- ============================================================================
-- H2: AUTO_INCREMENT, TIMESTAMP 지원
-- MySQL: AUTO_INCREMENT, TIMESTAMP, ON UPDATE CURRENT_TIMESTAMP 지원
-- ==========================================================================
