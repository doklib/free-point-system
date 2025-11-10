-- Initial data for SystemConfig table
INSERT INTO system_configs (config_key, config_value, description, updated_at) VALUES
('point.max.earn.per.transaction', '100000', '1회 최대 적립 가능 포인트 한도', CURRENT_TIMESTAMP),
('point.max.balance.per.user', '10000000', '개인별 최대 보유 가능 포인트 한도', CURRENT_TIMESTAMP),
('point.default.expiration.days', '365', '기본 포인트 만료일 (일)', CURRENT_TIMESTAMP),
('point.min.expiration.days', '1', '최소 포인트 만료일 (일)', CURRENT_TIMESTAMP),
('point.max.expiration.days', '1825', '최대 포인트 만료일 (일, 5년)', CURRENT_TIMESTAMP);
