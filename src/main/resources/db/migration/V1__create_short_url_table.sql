CREATE TABLE short_urls (
    id BIGINT NOT NULL AUTO_INCREMENT,
    short_code VARCHAR(32) NOT NULL,
    original_url VARCHAR(2048) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NULL,
    click_count BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_short_urls PRIMARY KEY (id),
    CONSTRAINT uk_short_urls_short_code UNIQUE (short_code),
    CONSTRAINT chk_short_urls_click_count CHECK (click_count >= 0)
);
