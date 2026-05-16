CREATE UNIQUE INDEX IF NOT EXISTS uq_stg_users_job_row
    ON analyticsflow_staging.stg_users(job_id, row_number);

CREATE UNIQUE INDEX IF NOT EXISTS uq_stg_product_categories_job_row
    ON analyticsflow_staging.stg_product_categories(job_id, row_number);

CREATE UNIQUE INDEX IF NOT EXISTS uq_stg_products_job_row
    ON analyticsflow_staging.stg_products(job_id, row_number);

CREATE UNIQUE INDEX IF NOT EXISTS uq_stg_product_details_job_row
    ON analyticsflow_staging.stg_product_details(job_id, row_number);

CREATE UNIQUE INDEX IF NOT EXISTS uq_stg_orders_job_row
    ON analyticsflow_staging.stg_orders(job_id, row_number);

CREATE UNIQUE INDEX IF NOT EXISTS uq_stg_order_items_job_row
    ON analyticsflow_staging.stg_order_items(job_id, row_number);

CREATE UNIQUE INDEX IF NOT EXISTS uq_stg_transactions_job_row
    ON analyticsflow_staging.stg_transactions(job_id, row_number);

CREATE UNIQUE INDEX IF NOT EXISTS uq_stg_deliveries_job_row
    ON analyticsflow_staging.stg_deliveries(job_id, row_number);

CREATE UNIQUE INDEX IF NOT EXISTS uq_invalid_records_job_table_row
    ON analyticsflow_support.invalid_records(job_id, source_table, row_number)
    WHERE row_number IS NOT NULL;
