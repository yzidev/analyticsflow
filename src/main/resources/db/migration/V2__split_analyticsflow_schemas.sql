CREATE SCHEMA IF NOT EXISTS analyticsflow_staging;
CREATE SCHEMA IF NOT EXISTS analyticsflow_oltp;
CREATE SCHEMA IF NOT EXISTS analyticsflow_olap;
CREATE SCHEMA IF NOT EXISTS analyticsflow_support;

ALTER TABLE IF EXISTS public.stg_users SET SCHEMA analyticsflow_staging;
ALTER TABLE IF EXISTS public.stg_product_categories SET SCHEMA analyticsflow_staging;
ALTER TABLE IF EXISTS public.stg_products SET SCHEMA analyticsflow_staging;
ALTER TABLE IF EXISTS public.stg_product_details SET SCHEMA analyticsflow_staging;
ALTER TABLE IF EXISTS public.stg_orders SET SCHEMA analyticsflow_staging;
ALTER TABLE IF EXISTS public.stg_order_items SET SCHEMA analyticsflow_staging;
ALTER TABLE IF EXISTS public.stg_transactions SET SCHEMA analyticsflow_staging;
ALTER TABLE IF EXISTS public.stg_deliveries SET SCHEMA analyticsflow_staging;

ALTER TABLE IF EXISTS public.users SET SCHEMA analyticsflow_oltp;
ALTER TABLE IF EXISTS public.product_categories SET SCHEMA analyticsflow_oltp;
ALTER TABLE IF EXISTS public.products SET SCHEMA analyticsflow_oltp;
ALTER TABLE IF EXISTS public.product_details SET SCHEMA analyticsflow_oltp;
ALTER TABLE IF EXISTS public.orders SET SCHEMA analyticsflow_oltp;
ALTER TABLE IF EXISTS public.order_items SET SCHEMA analyticsflow_oltp;
ALTER TABLE IF EXISTS public.transactions SET SCHEMA analyticsflow_oltp;
ALTER TABLE IF EXISTS public.deliveries SET SCHEMA analyticsflow_oltp;

ALTER TABLE IF EXISTS public.sales_daily_summary SET SCHEMA analyticsflow_olap;
ALTER TABLE IF EXISTS public.sales_product_summary SET SCHEMA analyticsflow_olap;
ALTER TABLE IF EXISTS public.sales_customer_summary SET SCHEMA analyticsflow_olap;
ALTER TABLE IF EXISTS public.delivery_performance_summary SET SCHEMA analyticsflow_olap;
ALTER TABLE IF EXISTS public.payment_method_summary SET SCHEMA analyticsflow_olap;
ALTER TABLE IF EXISTS public.channel_sales_summary SET SCHEMA analyticsflow_olap;

ALTER TABLE IF EXISTS public.invalid_records SET SCHEMA analyticsflow_support;
ALTER TABLE IF EXISTS public.etl_job SET SCHEMA analyticsflow_support;
ALTER TABLE IF EXISTS public.etl_job_step SET SCHEMA analyticsflow_support;
ALTER TABLE IF EXISTS public.report_metadata SET SCHEMA analyticsflow_support;
