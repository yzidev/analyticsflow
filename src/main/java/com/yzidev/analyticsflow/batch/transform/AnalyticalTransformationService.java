package com.yzidev.analyticsflow.batch.transform;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticalTransformationService {

	private final JdbcTemplate jdbcTemplate;

	public AnalyticalTransformationService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional
	public TransformationResult transform() {
		long read = count("users")
				+ count("product_categories")
				+ count("products")
				+ count("product_details")
				+ count("orders")
				+ count("order_items")
				+ count("transactions")
				+ count("deliveries");
		long written = transformSalesDailySummary()
				+ transformSalesProductSummary()
				+ transformSalesCustomerSummary()
				+ transformDeliveryPerformanceSummary()
				+ transformPaymentMethodSummary()
				+ transformChannelSalesSummary();
		return new TransformationResult(read, written);
	}

	private long count(String tableName) {
		Long total = jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
		return total == null ? 0L : total;
	}

	private int transformSalesDailySummary() {
		return jdbcTemplate.update(
				"""
				with summary_dates as (
				    select order_date::date as summary_date from orders
				    union
				    select transaction_date::date as summary_date from transactions
				    union
				    select coalesce(delivered_date, shipped_date, created_at)::date as summary_date
				    from deliveries
				    where coalesce(delivered_date, shipped_date, created_at) is not null
				),
				order_metrics as (
				    select order_date::date as summary_date,
				           count(*) as total_orders,
				           coalesce(sum(total_amount), 0) as total_gross_revenue
				    from orders
				    group by order_date::date
				),
				item_metrics as (
				    select o.order_date::date as summary_date,
				           coalesce(sum(oi.quantity), 0) as total_items_sold
				    from orders o
				    join order_items oi on oi.order_id = o.order_id
				    group by o.order_date::date
				),
				transaction_metrics as (
				    select transaction_date::date as summary_date,
				           coalesce(sum(case when status = 'SUCCESS' then amount else 0 end), 0) as total_paid_revenue,
				           count(*) filter (where status = 'SUCCESS') as total_success_transactions,
				           count(*) filter (where status = 'FAILED') as total_failed_transactions
				    from transactions
				    group by transaction_date::date
				),
				delivery_metrics as (
				    select coalesce(delivered_date, shipped_date, created_at)::date as summary_date,
				           count(*) filter (where delivery_status in ('SHIPPED', 'IN_TRANSIT', 'DELIVERED')) as total_shipped_orders,
				           count(*) filter (where delivery_status = 'DELIVERED') as total_delivered_orders
				    from deliveries
				    where coalesce(delivered_date, shipped_date, created_at) is not null
				    group by coalesce(delivered_date, shipped_date, created_at)::date
				)
				insert into sales_daily_summary (
				    summary_date, total_orders, total_items_sold, total_gross_revenue,
				    total_paid_revenue, total_success_transactions, total_failed_transactions,
				    total_shipped_orders, total_delivered_orders, created_at, updated_at
				)
				select d.summary_date,
				       coalesce(o.total_orders, 0),
				       coalesce(i.total_items_sold, 0),
				       coalesce(o.total_gross_revenue, 0),
				       coalesce(t.total_paid_revenue, 0),
				       coalesce(t.total_success_transactions, 0),
				       coalesce(t.total_failed_transactions, 0),
				       coalesce(del.total_shipped_orders, 0),
				       coalesce(del.total_delivered_orders, 0),
				       current_timestamp,
				       current_timestamp
				from summary_dates d
				left join order_metrics o on o.summary_date = d.summary_date
				left join item_metrics i on i.summary_date = d.summary_date
				left join transaction_metrics t on t.summary_date = d.summary_date
				left join delivery_metrics del on del.summary_date = d.summary_date
				on conflict (summary_date) do update set
				    total_orders = excluded.total_orders,
				    total_items_sold = excluded.total_items_sold,
				    total_gross_revenue = excluded.total_gross_revenue,
				    total_paid_revenue = excluded.total_paid_revenue,
				    total_success_transactions = excluded.total_success_transactions,
				    total_failed_transactions = excluded.total_failed_transactions,
				    total_shipped_orders = excluded.total_shipped_orders,
				    total_delivered_orders = excluded.total_delivered_orders,
				    updated_at = current_timestamp
				""");
	}

	private int transformSalesProductSummary() {
		return jdbcTemplate.update(
				"""
				insert into sales_product_summary (
				    product_id, product_name, category_id, category_name, brand,
				    total_orders, total_quantity_sold, total_revenue, created_at, updated_at
				)
				select p.product_id,
				       p.product_name,
				       p.category_id,
				       pc.category_name,
				       p.brand,
				       count(distinct oi.order_id),
				       coalesce(sum(oi.quantity), 0),
				       coalesce(sum(oi.total_price), 0),
				       current_timestamp,
				       current_timestamp
				from products p
				left join product_categories pc on pc.category_id = p.category_id
				left join order_items oi on oi.product_id = p.product_id
				group by p.product_id, p.product_name, p.category_id, pc.category_name, p.brand
				on conflict (product_id) do update set
				    product_name = excluded.product_name,
				    category_id = excluded.category_id,
				    category_name = excluded.category_name,
				    brand = excluded.brand,
				    total_orders = excluded.total_orders,
				    total_quantity_sold = excluded.total_quantity_sold,
				    total_revenue = excluded.total_revenue,
				    updated_at = current_timestamp
				""");
	}

	private int transformSalesCustomerSummary() {
		return jdbcTemplate.update(
				"""
				with order_metrics as (
				    select user_id,
				           count(*) as total_orders,
				           coalesce(sum(total_amount), 0) as total_spent,
				           max(order_date) as last_order_date
				    from orders
				    group by user_id
				),
				item_metrics as (
				    select o.user_id,
				           coalesce(sum(oi.quantity), 0) as total_items_purchased
				    from orders o
				    join order_items oi on oi.order_id = o.order_id
				    group by o.user_id
				)
				insert into sales_customer_summary (
				    user_id, full_name, email, city, country, total_orders,
				    total_items_purchased, total_spent, last_order_date, created_at, updated_at
				)
				select u.user_id,
				       u.full_name,
				       u.email,
				       u.city,
				       u.country,
				       coalesce(o.total_orders, 0),
				       coalesce(i.total_items_purchased, 0),
				       coalesce(o.total_spent, 0),
				       o.last_order_date,
				       current_timestamp,
				       current_timestamp
				from users u
				left join order_metrics o on o.user_id = u.user_id
				left join item_metrics i on i.user_id = u.user_id
				on conflict (user_id) do update set
				    full_name = excluded.full_name,
				    email = excluded.email,
				    city = excluded.city,
				    country = excluded.country,
				    total_orders = excluded.total_orders,
				    total_items_purchased = excluded.total_items_purchased,
				    total_spent = excluded.total_spent,
				    last_order_date = excluded.last_order_date,
				    updated_at = current_timestamp
				""");
	}

	private int transformDeliveryPerformanceSummary() {
		return jdbcTemplate.update(
				"""
				insert into delivery_performance_summary (
				    summary_date, courier_name, total_shipments, total_pending, total_shipped,
				    total_in_transit, total_delivered, total_failed, total_returned,
				    average_delivery_duration_hours, created_at, updated_at
				)
				select coalesce(delivered_date, shipped_date, created_at)::date as summary_date,
				       courier_name,
				       count(*) as total_shipments,
				       count(*) filter (where delivery_status = 'PENDING') as total_pending,
				       count(*) filter (where delivery_status = 'SHIPPED') as total_shipped,
				       count(*) filter (where delivery_status = 'IN_TRANSIT') as total_in_transit,
				       count(*) filter (where delivery_status = 'DELIVERED') as total_delivered,
				       count(*) filter (where delivery_status = 'FAILED') as total_failed,
				       count(*) filter (where delivery_status = 'RETURNED') as total_returned,
				       round((avg(extract(epoch from (delivered_date - shipped_date)) / 3600)
				           filter (where delivered_date is not null and shipped_date is not null))::numeric, 2),
				       current_timestamp,
				       current_timestamp
				from deliveries
				where coalesce(delivered_date, shipped_date, created_at) is not null
				group by coalesce(delivered_date, shipped_date, created_at)::date, courier_name
				on conflict (summary_date, courier_name) do update set
				    total_shipments = excluded.total_shipments,
				    total_pending = excluded.total_pending,
				    total_shipped = excluded.total_shipped,
				    total_in_transit = excluded.total_in_transit,
				    total_delivered = excluded.total_delivered,
				    total_failed = excluded.total_failed,
				    total_returned = excluded.total_returned,
				    average_delivery_duration_hours = excluded.average_delivery_duration_hours,
				    updated_at = current_timestamp
				""");
	}

	private int transformPaymentMethodSummary() {
		return jdbcTemplate.update(
				"""
				insert into payment_method_summary (
				    summary_date, payment_method, currency, total_transactions, total_success,
				    total_failed, total_pending, total_amount, created_at, updated_at
				)
				select transaction_date::date,
				       payment_method,
				       currency,
				       count(*) as total_transactions,
				       count(*) filter (where status = 'SUCCESS') as total_success,
				       count(*) filter (where status = 'FAILED') as total_failed,
				       count(*) filter (where status = 'PENDING') as total_pending,
				       coalesce(sum(amount), 0) as total_amount,
				       current_timestamp,
				       current_timestamp
				from transactions
				group by transaction_date::date, payment_method, currency
				on conflict (summary_date, payment_method, currency) do update set
				    total_transactions = excluded.total_transactions,
				    total_success = excluded.total_success,
				    total_failed = excluded.total_failed,
				    total_pending = excluded.total_pending,
				    total_amount = excluded.total_amount,
				    updated_at = current_timestamp
				""");
	}

	private int transformChannelSalesSummary() {
		return jdbcTemplate.update(
				"""
				with order_metrics as (
				    select order_date::date as summary_date,
				           channel,
				           count(*) as total_orders,
				           coalesce(sum(total_amount), 0) as total_revenue
				    from orders
				    group by order_date::date, channel
				),
				item_metrics as (
				    select o.order_date::date as summary_date,
				           o.channel,
				           coalesce(sum(oi.quantity), 0) as total_items_sold
				    from orders o
				    join order_items oi on oi.order_id = o.order_id
				    group by o.order_date::date, o.channel
				),
				transaction_metrics as (
				    select o.order_date::date as summary_date,
				           o.channel,
				           count(*) filter (where t.status = 'SUCCESS') as total_success_transactions
				    from orders o
				    join transactions t on t.order_id = o.order_id
				    group by o.order_date::date, o.channel
				)
				insert into channel_sales_summary (
				    summary_date, channel, total_orders, total_items_sold,
				    total_revenue, total_success_transactions, created_at, updated_at
				)
				select o.summary_date,
				       o.channel,
				       o.total_orders,
				       coalesce(i.total_items_sold, 0),
				       o.total_revenue,
				       coalesce(t.total_success_transactions, 0),
				       current_timestamp,
				       current_timestamp
				from order_metrics o
				left join item_metrics i on i.summary_date = o.summary_date and i.channel = o.channel
				left join transaction_metrics t on t.summary_date = o.summary_date and t.channel = o.channel
				on conflict (summary_date, channel) do update set
				    total_orders = excluded.total_orders,
				    total_items_sold = excluded.total_items_sold,
				    total_revenue = excluded.total_revenue,
				    total_success_transactions = excluded.total_success_transactions,
				    updated_at = current_timestamp
				""");
	}
}
