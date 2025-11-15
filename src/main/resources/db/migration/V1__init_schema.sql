CREATE TABLE IF NOT EXISTS trader_order (
    client_order_id VARCHAR(64) PRIMARY KEY,
    trade_date DATE NOT NULL,
    country_code VARCHAR(8) NOT NULL,
    status VARCHAR(32) NOT NULL,
    sub_status VARCHAR(64) NOT NULL,
    original_client_order_id VARCHAR(64),
    security_id VARCHAR(64) NOT NULL,
    order_quantity NUMERIC(20, 4) NOT NULL,
    clean_price NUMERIC(18, 6),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS trader_sub_order (
    id BIGSERIAL PRIMARY KEY,
    country_code VARCHAR(8) NOT NULL,
    client_order_id VARCHAR(64) NOT NULL REFERENCES trader_order(client_order_id) ON DELETE CASCADE,
    account_id VARCHAR(64) NOT NULL,
    issue_ipo_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS regional_allocation (
    client_order_id VARCHAR(64) PRIMARY KEY REFERENCES trader_order(client_order_id) ON DELETE CASCADE,
    order_quantity NUMERIC(20, 4) NOT NULL,
    hk_order_quantity NUMERIC(20, 4) NOT NULL DEFAULT 0,
    sg_order_quantity NUMERIC(20, 4) NOT NULL DEFAULT 0,
    limit_value NUMERIC(20, 4),
    limit_type VARCHAR(32),
    size_limit NUMERIC(20, 4),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS regional_allocation_breakdown (
    id BIGSERIAL PRIMARY KEY,
    client_order_id VARCHAR(64) NOT NULL REFERENCES trader_order(client_order_id) ON DELETE CASCADE,
    order_quantity NUMERIC(20, 4) NOT NULL,
    regional_allocation_status VARCHAR(16) NOT NULL DEFAULT 'NEW',
    country_code VARCHAR(8) NOT NULL,
    final_allocation NUMERIC(20, 4),
    allocation_percentage NUMERIC(7, 4),
    estimated_order_size NUMERIC(20, 4),
    yield_limit NUMERIC(9, 4),
    spread_limit NUMERIC(9, 4),
    size_limit NUMERIC(20, 4),
    account_number VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS client_allocation_breakdown (
    id BIGSERIAL PRIMARY KEY,
    client_order_id VARCHAR(64) NOT NULL REFERENCES trader_order(client_order_id) ON DELETE CASCADE,
    order_quantity NUMERIC(20, 4) NOT NULL,
    allocation_status VARCHAR(16) NOT NULL DEFAULT 'NEW',
    country_code VARCHAR(8) NOT NULL,
    final_allocation NUMERIC(20, 4),
    allocation_percentage NUMERIC(7, 4),
    estimated_order_size NUMERIC(20, 4),
    yield_limit NUMERIC(9, 4),
    spread_limit NUMERIC(9, 4),
    size_limit NUMERIC(20, 4),
    account_number VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS client_allocation_amend_log (
    id BIGSERIAL PRIMARY KEY,
    revision INTEGER NOT NULL,
    ref_id VARCHAR(64) NOT NULL,
    obj_type VARCHAR(32) NOT NULL,
    before_obj JSONB,
    after_obj JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS trader_order_status_audit (
    id BIGSERIAL PRIMARY KEY,
    client_order_id VARCHAR(64) NOT NULL REFERENCES trader_order(client_order_id) ON DELETE CASCADE,
    from_status VARCHAR(32),
    from_sub_status VARCHAR(64),
    to_status VARCHAR(32) NOT NULL,
    to_sub_status VARCHAR(64) NOT NULL,
    changed_by VARCHAR(64) NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    note VARCHAR(256)
);

CREATE TABLE IF NOT EXISTS final_priced_allocation_breakdown (
    id BIGSERIAL PRIMARY KEY,
    client_order_id VARCHAR(64) NOT NULL REFERENCES trader_order(client_order_id) ON DELETE CASCADE,
    limit_type VARCHAR(32),
    final_price NUMERIC(18, 6),
    country_code VARCHAR(8) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS final_regional_allocation (
    id BIGSERIAL PRIMARY KEY,
    client_order_id VARCHAR(64) NOT NULL REFERENCES trader_order(client_order_id) ON DELETE CASCADE,
    asia_allocation NUMERIC(20, 4),
    allocation NUMERIC(20, 4),
    market VARCHAR(32) NOT NULL,
    effective_order NUMERIC(20, 4),
    pro_rata NUMERIC(7, 4),
    allocation_amount NUMERIC(20, 4),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(client_order_id, market)
);

CREATE INDEX IF NOT EXISTS idx_trader_sub_order_client_order ON trader_sub_order (client_order_id);
CREATE INDEX IF NOT EXISTS idx_regional_alloc_breakdown_order ON regional_allocation_breakdown (client_order_id);
CREATE INDEX IF NOT EXISTS idx_client_alloc_breakdown_order ON client_allocation_breakdown (client_order_id);
CREATE INDEX IF NOT EXISTS idx_final_priced_alloc_order ON final_priced_allocation_breakdown (client_order_id);
CREATE INDEX IF NOT EXISTS idx_final_regional_alloc_order ON final_regional_allocation (client_order_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_client_alloc_amend_ref_rev ON client_allocation_amend_log (ref_id, revision);

