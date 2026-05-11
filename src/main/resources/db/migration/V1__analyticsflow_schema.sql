CREATE TABLE IF NOT EXISTS stg_users (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(64) NOT NULL,
    row_number BIGINT NOT NULL,
    user_id VARCHAR(64),
    full_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(64),
    city VARCHAR(128),
    country VARCHAR(128),
    source_created_at VARCHAR(64),
    raw_payload TEXT,
    is_valid BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stg_product_categories (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(64) NOT NULL,
    row_number BIGINT NOT NULL,
    category_id VARCHAR(64),
    category_name VARCHAR(255),
    parent_category_id VARCHAR(64),
    description TEXT,
    source_created_at VARCHAR(64),
    raw_payload TEXT,
    is_valid BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stg_products (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(64) NOT NULL,
    row_number BIGINT NOT NULL,
    product_id VARCHAR(64),
    category_id VARCHAR(64),
    product_name VARCHAR(255),
    brand VARCHAR(128),
    price VARCHAR(64),
    is_active VARCHAR(16),
    source_created_at VARCHAR(64),
    raw_payload TEXT,
    is_valid BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stg_product_details (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(64) NOT NULL,
    row_number BIGINT NOT NULL,
    product_detail_id VARCHAR(64),
    product_id VARCHAR(64),
    sku VARCHAR(128),
    color VARCHAR(64),
    size VARCHAR(64),
    weight VARCHAR(64),
    material VARCHAR(128),
    manufacture_date VARCHAR(64),
    expiry_date VARCHAR(64),
    source_created_at VARCHAR(64),
    raw_payload TEXT,
    is_valid BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stg_orders (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(64) NOT NULL,
    row_number BIGINT NOT NULL,
    order_id VARCHAR(64),
    user_id VARCHAR(64),
    order_date VARCHAR(64),
    order_status VARCHAR(32),
    total_amount VARCHAR(64),
    channel VARCHAR(32),
    source_created_at VARCHAR(64),
    raw_payload TEXT,
    is_valid BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stg_order_items (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(64) NOT NULL,
    row_number BIGINT NOT NULL,
    order_item_id VARCHAR(64),
    order_id VARCHAR(64),
    product_id VARCHAR(64),
    quantity VARCHAR(64),
    unit_price VARCHAR(64),
    total_price VARCHAR(64),
    raw_payload TEXT,
    is_valid BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stg_transactions (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(64) NOT NULL,
    row_number BIGINT NOT NULL,
    transaction_id VARCHAR(64),
    order_id VARCHAR(64),
    user_id VARCHAR(64),
    transaction_date VARCHAR(64),
    payment_method VARCHAR(32),
    amount VARCHAR(64),
    currency VARCHAR(8),
    status VARCHAR(32),
    source_created_at VARCHAR(64),
    raw_payload TEXT,
    is_valid BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stg_deliveries (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(64) NOT NULL,
    row_number BIGINT NOT NULL,
    delivery_id VARCHAR(64),
    order_id VARCHAR(64),
    delivery_status VARCHAR(32),
    delivery_address TEXT,
    shipped_date VARCHAR(64),
    delivered_date VARCHAR(64),
    courier_name VARCHAR(128),
    source_created_at VARCHAR(64),
    raw_payload TEXT,
    is_valid BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(64) PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(64),
    city VARCHAR(128),
    country VARCHAR(128),
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_categories (
    category_id VARCHAR(64) PRIMARY KEY,
    category_name VARCHAR(255) NOT NULL,
    parent_category_id VARCHAR(64),
    description TEXT,
    created_at TIMESTAMP,
    CONSTRAINT fk_product_categories_parent
        FOREIGN KEY (parent_category_id) REFERENCES product_categories(category_id)
);

CREATE TABLE IF NOT EXISTS products (
    product_id VARCHAR(64) PRIMARY KEY,
    category_id VARCHAR(64) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    brand VARCHAR(128),
    price NUMERIC(19, 2) NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES product_categories(category_id)
);

CREATE TABLE IF NOT EXISTS product_details (
    product_detail_id VARCHAR(64) PRIMARY KEY,
    product_id VARCHAR(64) NOT NULL,
    sku VARCHAR(128) NOT NULL,
    color VARCHAR(64),
    size VARCHAR(64),
    weight NUMERIC(19, 4),
    material VARCHAR(128),
    manufacture_date DATE,
    expiry_date DATE,
    created_at TIMESTAMP,
    CONSTRAINT fk_product_details_product
        FOREIGN KEY (product_id) REFERENCES products(product_id)
);

CREATE TABLE IF NOT EXISTS orders (
    order_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    order_date TIMESTAMP NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS order_items (
    order_item_id VARCHAR(64) PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    total_price NUMERIC(19, 2) NOT NULL,
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id) REFERENCES products(product_id)
);

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id VARCHAR(64) PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    transaction_date TIMESTAMP NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT fk_transactions_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_transactions_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS deliveries (
    delivery_id VARCHAR(64) PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL,
    delivery_status VARCHAR(32) NOT NULL,
    delivery_address TEXT NOT NULL,
    shipped_date TIMESTAMP,
    delivered_date TIMESTAMP,
    courier_name VARCHAR(128) NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT fk_deliveries_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

CREATE TABLE IF NOT EXISTS sales_daily_summary (
    id BIGSERIAL PRIMARY KEY,
    summary_date DATE NOT NULL UNIQUE,
    total_orders BIGINT NOT NULL DEFAULT 0,
    total_items_sold BIGINT NOT NULL DEFAULT 0,
    total_gross_revenue NUMERIC(19, 2) NOT NULL DEFAULT 0,
    total_paid_revenue NUMERIC(19, 2) NOT NULL DEFAULT 0,
    total_success_transactions BIGINT NOT NULL DEFAULT 0,
    total_failed_transactions BIGINT NOT NULL DEFAULT 0,
    total_shipped_orders BIGINT NOT NULL DEFAULT 0,
    total_delivered_orders BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sales_product_summary (
    id BIGSERIAL PRIMARY KEY,
    product_id VARCHAR(64) NOT NULL UNIQUE,
    product_name VARCHAR(255) NOT NULL,
    category_id VARCHAR(64),
    category_name VARCHAR(255),
    brand VARCHAR(128),
    total_orders BIGINT NOT NULL DEFAULT 0,
    total_quantity_sold BIGINT NOT NULL DEFAULT 0,
    total_revenue NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sales_customer_summary (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    city VARCHAR(128),
    country VARCHAR(128),
    total_orders BIGINT NOT NULL DEFAULT 0,
    total_items_purchased BIGINT NOT NULL DEFAULT 0,
    total_spent NUMERIC(19, 2) NOT NULL DEFAULT 0,
    last_order_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS delivery_performance_summary (
    id BIGSERIAL PRIMARY KEY,
    summary_date DATE NOT NULL,
    courier_name VARCHAR(128) NOT NULL,
    total_shipments BIGINT NOT NULL DEFAULT 0,
    total_pending BIGINT NOT NULL DEFAULT 0,
    total_shipped BIGINT NOT NULL DEFAULT 0,
    total_in_transit BIGINT NOT NULL DEFAULT 0,
    total_delivered BIGINT NOT NULL DEFAULT 0,
    total_failed BIGINT NOT NULL DEFAULT 0,
    total_returned BIGINT NOT NULL DEFAULT 0,
    average_delivery_duration_hours NUMERIC(19, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_delivery_performance_summary UNIQUE (summary_date, courier_name)
);

CREATE TABLE IF NOT EXISTS payment_method_summary (
    id BIGSERIAL PRIMARY KEY,
    summary_date DATE NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    total_transactions BIGINT NOT NULL DEFAULT 0,
    total_success BIGINT NOT NULL DEFAULT 0,
    total_failed BIGINT NOT NULL DEFAULT 0,
    total_pending BIGINT NOT NULL DEFAULT 0,
    total_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_payment_method_summary UNIQUE (summary_date, payment_method, currency)
);

CREATE TABLE IF NOT EXISTS channel_sales_summary (
    id BIGSERIAL PRIMARY KEY,
    summary_date DATE NOT NULL,
    channel VARCHAR(32) NOT NULL,
    total_orders BIGINT NOT NULL DEFAULT 0,
    total_items_sold BIGINT NOT NULL DEFAULT 0,
    total_revenue NUMERIC(19, 2) NOT NULL DEFAULT 0,
    total_success_transactions BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_channel_sales_summary UNIQUE (summary_date, channel)
);

CREATE TABLE IF NOT EXISTS invalid_records (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(64) NOT NULL,
    source_file VARCHAR(128) NOT NULL,
    source_table VARCHAR(128) NOT NULL,
    row_number BIGINT,
    raw_payload TEXT,
    error_message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS etl_job (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(64) NOT NULL UNIQUE,
    sample_directory VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_rows BIGINT NOT NULL DEFAULT 0,
    processed_rows BIGINT NOT NULL DEFAULT 0,
    success_rows BIGINT NOT NULL DEFAULT 0,
    failed_rows BIGINT NOT NULL DEFAULT 0,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    duration_ms BIGINT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS etl_job_step (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(64) NOT NULL,
    step_name VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    read_count BIGINT NOT NULL DEFAULT 0,
    write_count BIGINT NOT NULL DEFAULT 0,
    skip_count BIGINT NOT NULL DEFAULT 0,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    duration_ms BIGINT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_etl_job_step UNIQUE (job_id, step_name)
);

CREATE TABLE IF NOT EXISTS report_metadata (
    id BIGSERIAL PRIMARY KEY,
    report_id VARCHAR(64) NOT NULL UNIQUE,
    job_id VARCHAR(64),
    report_type VARCHAR(64) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(1024) NOT NULL,
    format VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    generated_at TIMESTAMP,
    duration_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_stg_users_job_id ON stg_users(job_id);
CREATE INDEX IF NOT EXISTS idx_stg_product_categories_job_id ON stg_product_categories(job_id);
CREATE INDEX IF NOT EXISTS idx_stg_products_job_id ON stg_products(job_id);
CREATE INDEX IF NOT EXISTS idx_stg_product_details_job_id ON stg_product_details(job_id);
CREATE INDEX IF NOT EXISTS idx_stg_orders_job_id ON stg_orders(job_id);
CREATE INDEX IF NOT EXISTS idx_stg_order_items_job_id ON stg_order_items(job_id);
CREATE INDEX IF NOT EXISTS idx_stg_transactions_job_id ON stg_transactions(job_id);
CREATE INDEX IF NOT EXISTS idx_stg_deliveries_job_id ON stg_deliveries(job_id);

CREATE INDEX IF NOT EXISTS idx_users_user_id ON users(user_id);
CREATE INDEX IF NOT EXISTS idx_product_categories_category_id ON product_categories(category_id);
CREATE INDEX IF NOT EXISTS idx_products_product_id ON products(product_id);
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_product_details_product_id ON product_details(product_id);
CREATE INDEX IF NOT EXISTS idx_orders_order_id ON orders(order_id);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_order_date ON orders(order_date);
CREATE INDEX IF NOT EXISTS idx_orders_channel ON orders(channel);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);
CREATE INDEX IF NOT EXISTS idx_transactions_order_id ON transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_transaction_date ON transactions(transaction_date);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_deliveries_order_id ON deliveries(order_id);
CREATE INDEX IF NOT EXISTS idx_deliveries_delivery_status ON deliveries(delivery_status);

CREATE INDEX IF NOT EXISTS idx_sales_daily_summary_summary_date ON sales_daily_summary(summary_date);
CREATE INDEX IF NOT EXISTS idx_sales_product_summary_product_id ON sales_product_summary(product_id);
CREATE INDEX IF NOT EXISTS idx_sales_customer_summary_user_id ON sales_customer_summary(user_id);
CREATE INDEX IF NOT EXISTS idx_delivery_performance_summary_summary_date ON delivery_performance_summary(summary_date);
CREATE INDEX IF NOT EXISTS idx_payment_method_summary_summary_date ON payment_method_summary(summary_date);
CREATE INDEX IF NOT EXISTS idx_channel_sales_summary_summary_date ON channel_sales_summary(summary_date);

CREATE INDEX IF NOT EXISTS idx_invalid_records_job_id ON invalid_records(job_id);
CREATE INDEX IF NOT EXISTS idx_invalid_records_source_file ON invalid_records(source_file);
CREATE INDEX IF NOT EXISTS idx_etl_job_job_id ON etl_job(job_id);
CREATE INDEX IF NOT EXISTS idx_etl_job_status ON etl_job(status);
CREATE INDEX IF NOT EXISTS idx_etl_job_step_job_id ON etl_job_step(job_id);
CREATE INDEX IF NOT EXISTS idx_report_metadata_report_id ON report_metadata(report_id);
CREATE INDEX IF NOT EXISTS idx_report_metadata_job_id ON report_metadata(job_id);
