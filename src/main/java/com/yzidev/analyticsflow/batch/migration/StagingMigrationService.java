package com.yzidev.analyticsflow.batch.migration;

import java.time.Duration;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StagingMigrationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(StagingMigrationService.class);

	private static final String AMOUNT_PATTERN = "^-?\\d+(\\.\\d+)?$";
	private static final String DATE_PATTERN = "^\\d{4}-\\d{2}-\\d{2}$";
	private static final String INTEGER_PATTERN = "^\\d+$";
	private static final String TIMESTAMP_PATTERN = "^\\d{4}-\\d{2}-\\d{2}([ T]\\d{2}:\\d{2}:\\d{2})?$";

	private final JdbcTemplate jdbcTemplate;

	public StagingMigrationService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(transactionManager = "transactionManager")
	public MigrationResult migrateMasterData(String jobId) {
		String stepName = "MIGRATE_MASTER_DATA";
		LOGGER.info("migration_step_started jobId={} step={}", jobId, stepName);
		long read = countWithProgress(jobId, stepName, "stg_users", "analyticsflow_staging.stg_users")
				+ countWithProgress(jobId, stepName, "stg_product_categories",
						"analyticsflow_staging.stg_product_categories")
				+ countWithProgress(jobId, stepName, "stg_products", "analyticsflow_staging.stg_products")
				+ countWithProgress(jobId, stepName, "stg_product_details",
						"analyticsflow_staging.stg_product_details");
		long invalid = updateWithProgress(jobId, stepName, "insert_invalid_users", () -> insertInvalidUsers(jobId))
				+ updateWithProgress(jobId, stepName, "insert_invalid_product_categories",
						() -> insertInvalidProductCategories(jobId))
				+ updateWithProgress(jobId, stepName, "insert_invalid_products",
						() -> insertInvalidProducts(jobId))
				+ updateWithProgress(jobId, stepName, "insert_invalid_product_details",
						() -> insertInvalidProductDetails(jobId));
		long written = updateWithProgress(jobId, stepName, "insert_users", () -> insertUsers(jobId))
				+ updateWithProgress(jobId, stepName, "insert_product_categories",
						() -> insertProductCategories(jobId))
				+ updateWithProgress(jobId, stepName, "insert_products", () -> insertProducts(jobId))
				+ updateWithProgress(jobId, stepName, "insert_product_details",
						() -> insertProductDetails(jobId));
		return migrationResult(jobId, stepName, read, written, invalid);
	}

	@Transactional(transactionManager = "transactionManager")
	public MigrationResult migrateOrderData(String jobId) {
		String stepName = "MIGRATE_ORDER_DATA";
		LOGGER.info("migration_step_started jobId={} step={}", jobId, stepName);
		long read = countWithProgress(jobId, stepName, "stg_orders", "analyticsflow_staging.stg_orders")
				+ countWithProgress(jobId, stepName, "stg_order_items", "analyticsflow_staging.stg_order_items");
		long invalid = updateWithProgress(jobId, stepName, "insert_invalid_orders",
				() -> insertInvalidOrders(jobId))
				+ updateWithProgress(jobId, stepName, "insert_invalid_order_items",
						() -> insertInvalidOrderItems(jobId));
		long written = updateWithProgress(jobId, stepName, "insert_orders", () -> insertOrders(jobId))
				+ updateWithProgress(jobId, stepName, "insert_order_items", () -> insertOrderItems(jobId));
		return migrationResult(jobId, stepName, read, written, invalid);
	}

	@Transactional(transactionManager = "transactionManager")
	public MigrationResult migrateTransactionAndDeliveryData(String jobId) {
		String stepName = "MIGRATE_TRANSACTION_AND_DELIVERY_DATA";
		LOGGER.info("migration_step_started jobId={} step={}", jobId, stepName);
		long read = countWithProgress(jobId, stepName, "stg_transactions",
				"analyticsflow_staging.stg_transactions")
				+ countWithProgress(jobId, stepName, "stg_deliveries", "analyticsflow_staging.stg_deliveries");
		long invalid = updateWithProgress(jobId, stepName, "insert_invalid_transactions",
				() -> insertInvalidTransactions(jobId))
				+ updateWithProgress(jobId, stepName, "insert_invalid_deliveries",
						() -> insertInvalidDeliveries(jobId));
		long written = updateWithProgress(jobId, stepName, "insert_transactions",
				() -> insertTransactions(jobId))
				+ updateWithProgress(jobId, stepName, "insert_deliveries", () -> insertDeliveries(jobId));
		return migrationResult(jobId, stepName, read, written, invalid);
	}

	private long countWithProgress(String jobId, String stepName, String phase, String tableName) {
		return longPhase(jobId, stepName, "count_" + phase, () -> count(jobId, tableName));
	}

	private int updateWithProgress(String jobId, String stepName, String phase, IntSupplier operation) {
		long startedAt = System.nanoTime();
		LOGGER.info("migration_phase_started jobId={} step={} phase={}", jobId, stepName, phase);
		int affected = operation.getAsInt();
		LOGGER.info(
				"migration_phase_finished jobId={} step={} phase={} affected={} durationMs={}",
				jobId,
				stepName,
				phase,
				affected,
				durationMs(startedAt));
		return affected;
	}

	private long longPhase(String jobId, String stepName, String phase, LongSupplier operation) {
		long startedAt = System.nanoTime();
		LOGGER.info("migration_phase_started jobId={} step={} phase={}", jobId, stepName, phase);
		long result = operation.getAsLong();
		LOGGER.info(
				"migration_phase_finished jobId={} step={} phase={} result={} durationMs={}",
				jobId,
				stepName,
				phase,
				result,
				durationMs(startedAt));
		return result;
	}

	private MigrationResult migrationResult(String jobId, String stepName, long read, long written, long invalid) {
		LOGGER.info(
				"migration_step_finished jobId={} step={} read={} written={} invalid={}",
				jobId,
				stepName,
				read,
				written,
				invalid);
		return new MigrationResult(read, written, invalid);
	}

	private long durationMs(long startedAt) {
		return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
	}

	private long count(String jobId, String tableName) {
		Long total = jdbcTemplate.queryForObject(
				"select count(*) from " + tableName + " where job_id = ? and is_valid = true",
				Long.class,
				jobId);
		return total == null ? 0L : total;
	}

	private int insertInvalidUsers(String jobId) {
		return invalid(
				"users.csv",
				"stg_users",
				"""
				select ?, 'users.csv', 'stg_users', row_number, raw_payload,
				       case
				           when nullif(trim(user_id), '') is null then 'user_id is required'
				           when nullif(trim(full_name), '') is null then 'full_name is required'
				           when not first_row then 'Duplicate user_id in staging file'
				           when not valid_created_at then 'created_at must be a valid timestamp'
				           else 'Invalid user row'
				       end
				from (
				    select s.*,
				           row_number() over (partition by user_id order by row_number) = 1 as first_row,
				           source_created_at is null or trim(source_created_at) = '' or source_created_at ~ ? as valid_created_at
				    from analyticsflow_staging.stg_users s
				    where job_id = ? and is_valid = true
				) s
				where nullif(trim(user_id), '') is null
				   or nullif(trim(full_name), '') is null
				   or not first_row
				   or not valid_created_at
				""",
				jobId,
				TIMESTAMP_PATTERN,
				jobId);
	}

	private int insertUsers(String jobId) {
		return jdbcTemplate.update(
				"""
				insert into analyticsflow_oltp.users (user_id, full_name, email, phone, city, country, created_at)
				select user_id, full_name, email, phone, city, country,
				       case when nullif(trim(source_created_at), '') is null then null
				            else replace(source_created_at, 'T', ' ')::timestamp end
				from (
				    select s.*, row_number() over (partition by user_id order by row_number) as duplicate_number
				    from analyticsflow_staging.stg_users s
				    where job_id = ? and is_valid = true
				) s
				where duplicate_number = 1
				  and nullif(trim(user_id), '') is not null
				  and nullif(trim(full_name), '') is not null
				  and (source_created_at is null or trim(source_created_at) = '' or source_created_at ~ ?)
				on conflict (user_id) do update set
				    full_name = excluded.full_name,
				    email = excluded.email,
				    phone = excluded.phone,
				    city = excluded.city,
				    country = excluded.country,
				    created_at = excluded.created_at
				""",
				jobId,
				TIMESTAMP_PATTERN);
	}

	private int insertInvalidProductCategories(String jobId) {
		return invalid(
				"product_categories.csv",
				"stg_product_categories",
				"""
				select ?, 'product_categories.csv', 'stg_product_categories', row_number, raw_payload,
				       case
				           when nullif(trim(category_id), '') is null then 'category_id is required'
				           when nullif(trim(category_name), '') is null then 'category_name is required'
				           when not first_row then 'Duplicate category_id in staging file'
				           when parent_category_id is not null and trim(parent_category_id) <> '' and not parent_exists then 'parent_category_id does not exist'
				           when not valid_created_at then 'created_at must be a valid timestamp'
				           else 'Invalid product category row'
				       end
				from (
				    select s.*,
				           row_number() over (partition by category_id order by row_number) = 1 as first_row,
				           source_created_at is null or trim(source_created_at) = '' or source_created_at ~ ? as valid_created_at,
				           exists (
				               select 1
				               from analyticsflow_oltp.product_categories pc
				               where pc.category_id = s.parent_category_id
				           )
				           or exists (
				               select 1
				               from analyticsflow_staging.stg_product_categories parent
				               where parent.job_id = s.job_id
				                 and parent.is_valid = true
				                 and parent.category_id = s.parent_category_id
				           ) as parent_exists
				    from analyticsflow_staging.stg_product_categories s
				    where job_id = ? and is_valid = true
				) s
				where nullif(trim(category_id), '') is null
				   or nullif(trim(category_name), '') is null
				   or not first_row
				   or (parent_category_id is not null and trim(parent_category_id) <> '' and not parent_exists)
				   or not valid_created_at
				""",
				jobId,
				TIMESTAMP_PATTERN,
				jobId);
	}

	private int insertProductCategories(String jobId) {
		return jdbcTemplate.update(
				"""
				insert into analyticsflow_oltp.product_categories (category_id, category_name, parent_category_id, description, created_at)
				select category_id, category_name, nullif(parent_category_id, ''), description,
				       case when nullif(trim(source_created_at), '') is null then null
				            else replace(source_created_at, 'T', ' ')::timestamp end
				from (
				    select s.*, row_number() over (partition by category_id order by row_number) as duplicate_number
				    from analyticsflow_staging.stg_product_categories s
				    where job_id = ? and is_valid = true
				) s
				where duplicate_number = 1
				  and nullif(trim(category_id), '') is not null
				  and nullif(trim(category_name), '') is not null
				  and (source_created_at is null or trim(source_created_at) = '' or source_created_at ~ ?)
				  and (
				      parent_category_id is null
				      or trim(parent_category_id) = ''
				      or exists (select 1 from analyticsflow_oltp.product_categories pc where pc.category_id = s.parent_category_id)
				      or exists (
				          select 1 from analyticsflow_staging.stg_product_categories parent
				          where parent.job_id = s.job_id and parent.is_valid = true and parent.category_id = s.parent_category_id
				      )
				  )
				on conflict (category_id) do update set
				    category_name = excluded.category_name,
				    parent_category_id = excluded.parent_category_id,
				    description = excluded.description,
				    created_at = excluded.created_at
				""",
				jobId,
				TIMESTAMP_PATTERN);
	}

	private int insertInvalidProducts(String jobId) {
		return invalid(
				"products.csv",
				"stg_products",
				"""
				select ?, 'products.csv', 'stg_products', row_number, raw_payload,
				       case
				           when nullif(trim(product_id), '') is null then 'product_id is required'
				           when nullif(trim(category_id), '') is null then 'category_id is required'
				           when nullif(trim(product_name), '') is null then 'product_name is required'
				           when nullif(trim(price), '') is null or price !~ ? then 'price must be numeric'
				           when lower(trim(is_active)) not in ('true', 't', '1', 'yes', 'y', 'false', 'f', '0', 'no', 'n') then 'is_active must be boolean'
				           when not first_row then 'Duplicate product_id in staging file'
				           when not category_exists then 'category_id does not exist'
				           when not valid_created_at then 'created_at must be a valid timestamp'
				           else 'Invalid product row'
				       end
				from (
				    select s.*,
				           row_number() over (partition by product_id order by row_number) = 1 as first_row,
				           source_created_at is null or trim(source_created_at) = '' or source_created_at ~ ? as valid_created_at,
				           exists (select 1 from analyticsflow_oltp.product_categories pc where pc.category_id = s.category_id) as category_exists
				    from analyticsflow_staging.stg_products s
				    where job_id = ? and is_valid = true
				) s
				where nullif(trim(product_id), '') is null
				   or nullif(trim(category_id), '') is null
				   or nullif(trim(product_name), '') is null
				   or nullif(trim(price), '') is null
				   or price !~ ?
				   or lower(trim(is_active)) not in ('true', 't', '1', 'yes', 'y', 'false', 'f', '0', 'no', 'n')
				   or not first_row
				   or not category_exists
				   or not valid_created_at
				""",
				jobId,
				AMOUNT_PATTERN,
				TIMESTAMP_PATTERN,
				jobId,
				AMOUNT_PATTERN);
	}

	private int insertProducts(String jobId) {
		return jdbcTemplate.update(
				"""
				insert into analyticsflow_oltp.products (product_id, category_id, product_name, brand, price, is_active, created_at)
				select product_id, category_id, product_name, brand, price::numeric(19, 2),
				       lower(trim(is_active)) in ('true', 't', '1', 'yes', 'y'),
				       case when nullif(trim(source_created_at), '') is null then null
				            else replace(source_created_at, 'T', ' ')::timestamp end
				from (
				    select s.*, row_number() over (partition by product_id order by row_number) as duplicate_number
				    from analyticsflow_staging.stg_products s
				    where job_id = ? and is_valid = true
				) s
				where duplicate_number = 1
				  and nullif(trim(product_id), '') is not null
				  and nullif(trim(category_id), '') is not null
				  and nullif(trim(product_name), '') is not null
				  and nullif(trim(price), '') is not null
				  and price ~ ?
				  and lower(trim(is_active)) in ('true', 't', '1', 'yes', 'y', 'false', 'f', '0', 'no', 'n')
				  and (source_created_at is null or trim(source_created_at) = '' or source_created_at ~ ?)
				  and exists (select 1 from analyticsflow_oltp.product_categories pc where pc.category_id = s.category_id)
				on conflict (product_id) do update set
				    category_id = excluded.category_id,
				    product_name = excluded.product_name,
				    brand = excluded.brand,
				    price = excluded.price,
				    is_active = excluded.is_active,
				    created_at = excluded.created_at
				""",
				jobId,
				AMOUNT_PATTERN,
				TIMESTAMP_PATTERN);
	}

	private int insertInvalidProductDetails(String jobId) {
		return invalid(
				"product_details.csv",
				"stg_product_details",
				"""
				select ?, 'product_details.csv', 'stg_product_details', row_number, raw_payload,
				       case
				           when nullif(trim(product_detail_id), '') is null then 'product_detail_id is required'
				           when nullif(trim(product_id), '') is null then 'product_id is required'
				           when nullif(trim(sku), '') is null then 'sku is required'
				           when weight is not null and trim(weight) <> '' and weight !~ ? then 'weight must be numeric'
				           when manufacture_date is not null and trim(manufacture_date) <> '' and manufacture_date !~ ? then 'manufacture_date must be yyyy-MM-dd'
				           when expiry_date is not null and trim(expiry_date) <> '' and expiry_date !~ ? then 'expiry_date must be yyyy-MM-dd'
				           when not first_row then 'Duplicate product_detail_id in staging file'
				           when not product_exists then 'product_id does not exist'
				           when not valid_created_at then 'created_at must be a valid timestamp'
				           else 'Invalid product detail row'
				       end
				from (
				    select s.*,
				           row_number() over (partition by product_detail_id order by row_number) = 1 as first_row,
				           source_created_at is null or trim(source_created_at) = '' or source_created_at ~ ? as valid_created_at,
				           exists (select 1 from analyticsflow_oltp.products p where p.product_id = s.product_id) as product_exists
				    from analyticsflow_staging.stg_product_details s
				    where job_id = ? and is_valid = true
				) s
				where nullif(trim(product_detail_id), '') is null
				   or nullif(trim(product_id), '') is null
				   or nullif(trim(sku), '') is null
				   or (weight is not null and trim(weight) <> '' and weight !~ ?)
				   or (manufacture_date is not null and trim(manufacture_date) <> '' and manufacture_date !~ ?)
				   or (expiry_date is not null and trim(expiry_date) <> '' and expiry_date !~ ?)
				   or not first_row
				   or not product_exists
				   or not valid_created_at
				""",
				jobId,
				AMOUNT_PATTERN,
				DATE_PATTERN,
				DATE_PATTERN,
				TIMESTAMP_PATTERN,
				jobId,
				AMOUNT_PATTERN,
				DATE_PATTERN,
				DATE_PATTERN);
	}

	private int insertProductDetails(String jobId) {
		return jdbcTemplate.update(
				"""
				insert into analyticsflow_oltp.product_details (
				    product_detail_id, product_id, sku, color, size, weight, material,
				    manufacture_date, expiry_date, created_at
				)
				select product_detail_id, product_id, sku, color, size,
				       case when nullif(trim(weight), '') is null then null else weight::numeric(19, 4) end,
				       material,
				       case when nullif(trim(manufacture_date), '') is null then null else manufacture_date::date end,
				       case when nullif(trim(expiry_date), '') is null then null else expiry_date::date end,
				       case when nullif(trim(source_created_at), '') is null then null
				            else replace(source_created_at, 'T', ' ')::timestamp end
				from (
				    select s.*, row_number() over (partition by product_detail_id order by row_number) as duplicate_number
				    from analyticsflow_staging.stg_product_details s
				    where job_id = ? and is_valid = true
				) s
				where duplicate_number = 1
				  and nullif(trim(product_detail_id), '') is not null
				  and nullif(trim(product_id), '') is not null
				  and nullif(trim(sku), '') is not null
				  and (weight is null or trim(weight) = '' or weight ~ ?)
				  and (manufacture_date is null or trim(manufacture_date) = '' or manufacture_date ~ ?)
				  and (expiry_date is null or trim(expiry_date) = '' or expiry_date ~ ?)
				  and (source_created_at is null or trim(source_created_at) = '' or source_created_at ~ ?)
				  and exists (select 1 from analyticsflow_oltp.products p where p.product_id = s.product_id)
				on conflict (product_detail_id) do update set
				    product_id = excluded.product_id,
				    sku = excluded.sku,
				    color = excluded.color,
				    size = excluded.size,
				    weight = excluded.weight,
				    material = excluded.material,
				    manufacture_date = excluded.manufacture_date,
				    expiry_date = excluded.expiry_date,
				    created_at = excluded.created_at
				""",
				jobId,
				AMOUNT_PATTERN,
				DATE_PATTERN,
				DATE_PATTERN,
				TIMESTAMP_PATTERN);
	}

	private int insertInvalidOrders(String jobId) {
		return invalid(
				"orders.csv",
				"stg_orders",
				"""
				select ?, 'orders.csv', 'stg_orders', row_number, raw_payload,
				       case
				           when nullif(trim(order_id), '') is null then 'order_id is required'
				           when nullif(trim(user_id), '') is null then 'user_id is required'
				           when nullif(trim(order_date), '') is null or order_date !~ ? then 'order_date must be a valid timestamp'
				           when normalize_order_status not in ('PENDING', 'PAID', 'PROCESSING', 'COMPLETED', 'CANCELLED', 'REFUNDED', 'FAILED') then 'order_status is invalid'
				           when nullif(trim(total_amount), '') is null or total_amount !~ ? then 'total_amount must be numeric'
				           when normalize_channel not in ('WEB', 'MOBILE', 'MARKETPLACE', 'OFFLINE') then 'channel is invalid'
				           when not first_row then 'Duplicate order_id in staging file'
				           when not user_exists then 'user_id does not exist'
				           when not valid_created_at then 'created_at must be a valid timestamp'
				           else 'Invalid order row'
				       end
				from (
				    select s.*,
				           upper(replace(replace(trim(order_status), '-', '_'), ' ', '_')) as normalize_order_status,
				           upper(replace(replace(trim(channel), '-', '_'), ' ', '_')) as normalize_channel,
				           row_number() over (partition by order_id order by row_number) = 1 as first_row,
				           source_created_at is null or trim(source_created_at) = '' or source_created_at ~ ? as valid_created_at,
				           exists (select 1 from analyticsflow_oltp.users u where u.user_id = s.user_id) as user_exists
				    from analyticsflow_staging.stg_orders s
				    where job_id = ? and is_valid = true
				) s
				where nullif(trim(order_id), '') is null
				   or nullif(trim(user_id), '') is null
				   or nullif(trim(order_date), '') is null
				   or order_date !~ ?
				   or normalize_order_status not in ('PENDING', 'PAID', 'PROCESSING', 'COMPLETED', 'CANCELLED', 'REFUNDED', 'FAILED')
				   or nullif(trim(total_amount), '') is null
				   or total_amount !~ ?
				   or normalize_channel not in ('WEB', 'MOBILE', 'MARKETPLACE', 'OFFLINE')
				   or not first_row
				   or not user_exists
				   or not valid_created_at
				""",
				jobId,
				TIMESTAMP_PATTERN,
				AMOUNT_PATTERN,
				TIMESTAMP_PATTERN,
				jobId,
				TIMESTAMP_PATTERN,
				AMOUNT_PATTERN);
	}

	private int insertOrders(String jobId) {
		return jdbcTemplate.update(
				"""
				insert into analyticsflow_oltp.orders (order_id, user_id, order_date, order_status, total_amount, channel, created_at)
				select order_id, user_id, replace(order_date, 'T', ' ')::timestamp,
				       upper(replace(replace(trim(order_status), '-', '_'), ' ', '_')),
				       total_amount::numeric(19, 2),
				       upper(replace(replace(trim(channel), '-', '_'), ' ', '_')),
				       case when nullif(trim(source_created_at), '') is null then null
				            else replace(source_created_at, 'T', ' ')::timestamp end
				from (
				    select s.*, row_number() over (partition by order_id order by row_number) as duplicate_number
				    from analyticsflow_staging.stg_orders s
				    where job_id = ? and is_valid = true
				) s
				where duplicate_number = 1
				  and nullif(trim(order_id), '') is not null
				  and nullif(trim(user_id), '') is not null
				  and order_date ~ ?
				  and upper(replace(replace(trim(order_status), '-', '_'), ' ', '_')) in ('PENDING', 'PAID', 'PROCESSING', 'COMPLETED', 'CANCELLED', 'REFUNDED', 'FAILED')
				  and total_amount ~ ?
				  and upper(replace(replace(trim(channel), '-', '_'), ' ', '_')) in ('WEB', 'MOBILE', 'MARKETPLACE', 'OFFLINE')
				  and (source_created_at is null or trim(source_created_at) = '' or source_created_at ~ ?)
				  and exists (select 1 from analyticsflow_oltp.users u where u.user_id = s.user_id)
				on conflict (order_id) do update set
				    user_id = excluded.user_id,
				    order_date = excluded.order_date,
				    order_status = excluded.order_status,
				    total_amount = excluded.total_amount,
				    channel = excluded.channel,
				    created_at = excluded.created_at
				""",
				jobId,
				TIMESTAMP_PATTERN,
				AMOUNT_PATTERN,
				TIMESTAMP_PATTERN);
	}

	private int insertInvalidOrderItems(String jobId) {
		return invalid(
				"order_items.csv",
				"stg_order_items",
				"""
				select ?, 'order_items.csv', 'stg_order_items', row_number, raw_payload,
				       case
				           when nullif(trim(order_item_id), '') is null then 'order_item_id is required'
				           when nullif(trim(order_id), '') is null then 'order_id is required'
				           when nullif(trim(product_id), '') is null then 'product_id is required'
				           when nullif(trim(quantity), '') is null or quantity !~ ? then 'quantity must be integer'
				           when nullif(trim(unit_price), '') is null or unit_price !~ ? then 'unit_price must be numeric'
				           when nullif(trim(total_price), '') is null or total_price !~ ? then 'total_price must be numeric'
				           when not first_row then 'Duplicate order_item_id in staging file'
				           when not order_exists then 'order_id does not exist'
				           when not product_exists then 'product_id does not exist'
				           else 'Invalid order item row'
				       end
				from (
				    select s.*,
				           row_number() over (partition by order_item_id order by row_number) = 1 as first_row,
				           exists (select 1 from analyticsflow_oltp.orders o where o.order_id = s.order_id) as order_exists,
				           exists (select 1 from analyticsflow_oltp.products p where p.product_id = s.product_id) as product_exists
				    from analyticsflow_staging.stg_order_items s
				    where job_id = ? and is_valid = true
				) s
				where nullif(trim(order_item_id), '') is null
				   or nullif(trim(order_id), '') is null
				   or nullif(trim(product_id), '') is null
				   or nullif(trim(quantity), '') is null
				   or quantity !~ ?
				   or nullif(trim(unit_price), '') is null
				   or unit_price !~ ?
				   or nullif(trim(total_price), '') is null
				   or total_price !~ ?
				   or not first_row
				   or not order_exists
				   or not product_exists
				""",
				jobId,
				INTEGER_PATTERN,
				AMOUNT_PATTERN,
				AMOUNT_PATTERN,
				jobId,
				INTEGER_PATTERN,
				AMOUNT_PATTERN,
				AMOUNT_PATTERN);
	}

	private int insertOrderItems(String jobId) {
		return jdbcTemplate.update(
				"""
				insert into analyticsflow_oltp.order_items (order_item_id, order_id, product_id, quantity, unit_price, total_price)
				select order_item_id, order_id, product_id, quantity::integer,
				       unit_price::numeric(19, 2), total_price::numeric(19, 2)
				from (
				    select s.*, row_number() over (partition by order_item_id order by row_number) as duplicate_number
				    from analyticsflow_staging.stg_order_items s
				    where job_id = ? and is_valid = true
				) s
				where duplicate_number = 1
				  and nullif(trim(order_item_id), '') is not null
				  and nullif(trim(order_id), '') is not null
				  and nullif(trim(product_id), '') is not null
				  and quantity ~ ?
				  and unit_price ~ ?
				  and total_price ~ ?
				  and exists (select 1 from analyticsflow_oltp.orders o where o.order_id = s.order_id)
				  and exists (select 1 from analyticsflow_oltp.products p where p.product_id = s.product_id)
				on conflict (order_item_id) do update set
				    order_id = excluded.order_id,
				    product_id = excluded.product_id,
				    quantity = excluded.quantity,
				    unit_price = excluded.unit_price,
				    total_price = excluded.total_price
				""",
				jobId,
				INTEGER_PATTERN,
				AMOUNT_PATTERN,
				AMOUNT_PATTERN);
	}

	private int insertInvalidTransactions(String jobId) {
		return invalid(
				"transactions.csv",
				"stg_transactions",
				"""
				select ?, 'transactions.csv', 'stg_transactions', row_number, raw_payload,
				       case
				           when nullif(trim(transaction_id), '') is null then 'transaction_id is required'
				           when nullif(trim(order_id), '') is null then 'order_id is required'
				           when nullif(trim(user_id), '') is null then 'user_id is required'
				           when nullif(trim(transaction_date), '') is null or transaction_date !~ ? then 'transaction_date must be a valid timestamp'
				           when normalize_payment_method not in ('BANK_TRANSFER', 'CREDIT_CARD', 'DEBIT_CARD', 'EWALLET', 'QRIS', 'VIRTUAL_ACCOUNT', 'CASH') then 'payment_method is invalid'
				           when nullif(trim(amount), '') is null or amount !~ ? then 'amount must be numeric'
				           when nullif(trim(currency), '') is null then 'currency is required'
				           when normalize_status not in ('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED', 'REFUNDED', 'CANCELLED') then 'status is invalid'
				           when not first_row then 'Duplicate transaction_id in staging file'
				           when not order_exists then 'order_id does not exist'
				           when not user_exists then 'user_id does not exist'
				           when not valid_created_at then 'created_at must be a valid timestamp'
				           else 'Invalid transaction row'
				       end
				from (
				    select s.*,
				           upper(replace(replace(trim(payment_method), '-', '_'), ' ', '_')) as normalize_payment_method,
				           upper(replace(replace(trim(status), '-', '_'), ' ', '_')) as normalize_status,
				           row_number() over (partition by transaction_id order by row_number) = 1 as first_row,
				           source_created_at is null or trim(source_created_at) = '' or source_created_at ~ ? as valid_created_at,
				           exists (select 1 from analyticsflow_oltp.orders o where o.order_id = s.order_id) as order_exists,
				           exists (select 1 from analyticsflow_oltp.users u where u.user_id = s.user_id) as user_exists
				    from analyticsflow_staging.stg_transactions s
				    where job_id = ? and is_valid = true
				) s
				where nullif(trim(transaction_id), '') is null
				   or nullif(trim(order_id), '') is null
				   or nullif(trim(user_id), '') is null
				   or nullif(trim(transaction_date), '') is null
				   or transaction_date !~ ?
				   or normalize_payment_method not in ('BANK_TRANSFER', 'CREDIT_CARD', 'DEBIT_CARD', 'EWALLET', 'QRIS', 'VIRTUAL_ACCOUNT', 'CASH')
				   or nullif(trim(amount), '') is null
				   or amount !~ ?
				   or nullif(trim(currency), '') is null
				   or normalize_status not in ('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED', 'REFUNDED', 'CANCELLED')
				   or not first_row
				   or not order_exists
				   or not user_exists
				   or not valid_created_at
				""",
				jobId,
				TIMESTAMP_PATTERN,
				AMOUNT_PATTERN,
				TIMESTAMP_PATTERN,
				jobId,
				TIMESTAMP_PATTERN,
				AMOUNT_PATTERN);
	}

	private int insertTransactions(String jobId) {
		return jdbcTemplate.update(
				"""
				insert into analyticsflow_oltp.transactions (
				    transaction_id, order_id, user_id, transaction_date, payment_method,
				    amount, currency, status, created_at
				)
				select transaction_id, order_id, user_id, replace(transaction_date, 'T', ' ')::timestamp,
				       upper(replace(replace(trim(payment_method), '-', '_'), ' ', '_')),
				       amount::numeric(19, 2), currency,
				       upper(replace(replace(trim(status), '-', '_'), ' ', '_')),
				       case when nullif(trim(source_created_at), '') is null then null
				            else replace(source_created_at, 'T', ' ')::timestamp end
				from (
				    select s.*, row_number() over (partition by transaction_id order by row_number) as duplicate_number
				    from analyticsflow_staging.stg_transactions s
				    where job_id = ? and is_valid = true
				) s
				where duplicate_number = 1
				  and nullif(trim(transaction_id), '') is not null
				  and nullif(trim(order_id), '') is not null
				  and nullif(trim(user_id), '') is not null
				  and transaction_date ~ ?
				  and upper(replace(replace(trim(payment_method), '-', '_'), ' ', '_')) in ('BANK_TRANSFER', 'CREDIT_CARD', 'DEBIT_CARD', 'EWALLET', 'QRIS', 'VIRTUAL_ACCOUNT', 'CASH')
				  and amount ~ ?
				  and nullif(trim(currency), '') is not null
				  and upper(replace(replace(trim(status), '-', '_'), ' ', '_')) in ('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED', 'REFUNDED', 'CANCELLED')
				  and (source_created_at is null or trim(source_created_at) = '' or source_created_at ~ ?)
				  and exists (select 1 from analyticsflow_oltp.orders o where o.order_id = s.order_id)
				  and exists (select 1 from analyticsflow_oltp.users u where u.user_id = s.user_id)
				on conflict (transaction_id) do update set
				    order_id = excluded.order_id,
				    user_id = excluded.user_id,
				    transaction_date = excluded.transaction_date,
				    payment_method = excluded.payment_method,
				    amount = excluded.amount,
				    currency = excluded.currency,
				    status = excluded.status,
				    created_at = excluded.created_at
				""",
				jobId,
				TIMESTAMP_PATTERN,
				AMOUNT_PATTERN,
				TIMESTAMP_PATTERN);
	}

	private int insertInvalidDeliveries(String jobId) {
		return invalid(
				"deliveries.csv",
				"stg_deliveries",
				"""
				select ?, 'deliveries.csv', 'stg_deliveries', row_number, raw_payload,
				       case
				           when nullif(trim(delivery_id), '') is null then 'delivery_id is required'
				           when nullif(trim(order_id), '') is null then 'order_id is required'
				           when normalize_delivery_status not in ('PENDING', 'SHIPPED', 'IN_TRANSIT', 'DELIVERED', 'FAILED', 'RETURNED', 'CANCELLED') then 'delivery_status is invalid'
				           when nullif(trim(delivery_address), '') is null then 'delivery_address is required'
				           when shipped_date is not null and trim(shipped_date) <> '' and shipped_date !~ ? then 'shipped_date must be a valid timestamp'
				           when delivered_date is not null and trim(delivered_date) <> '' and delivered_date !~ ? then 'delivered_date must be a valid timestamp'
				           when nullif(trim(courier_name), '') is null then 'courier_name is required'
				           when not first_row then 'Duplicate delivery_id in staging file'
				           when not order_exists then 'order_id does not exist'
				           when not valid_created_at then 'created_at must be a valid timestamp'
				           else 'Invalid delivery row'
				       end
				from (
				    select s.*,
				           upper(replace(replace(trim(delivery_status), '-', '_'), ' ', '_')) as normalize_delivery_status,
				           row_number() over (partition by delivery_id order by row_number) = 1 as first_row,
				           source_created_at is null or trim(source_created_at) = '' or source_created_at ~ ? as valid_created_at,
				           exists (select 1 from analyticsflow_oltp.orders o where o.order_id = s.order_id) as order_exists
				    from analyticsflow_staging.stg_deliveries s
				    where job_id = ? and is_valid = true
				) s
				where nullif(trim(delivery_id), '') is null
				   or nullif(trim(order_id), '') is null
				   or normalize_delivery_status not in ('PENDING', 'SHIPPED', 'IN_TRANSIT', 'DELIVERED', 'FAILED', 'RETURNED', 'CANCELLED')
				   or nullif(trim(delivery_address), '') is null
				   or (shipped_date is not null and trim(shipped_date) <> '' and shipped_date !~ ?)
				   or (delivered_date is not null and trim(delivered_date) <> '' and delivered_date !~ ?)
				   or nullif(trim(courier_name), '') is null
				   or not first_row
				   or not order_exists
				   or not valid_created_at
				""",
				jobId,
				TIMESTAMP_PATTERN,
				TIMESTAMP_PATTERN,
				TIMESTAMP_PATTERN,
				jobId,
				TIMESTAMP_PATTERN,
				TIMESTAMP_PATTERN);
	}

	private int insertDeliveries(String jobId) {
		return jdbcTemplate.update(
				"""
				insert into analyticsflow_oltp.deliveries (
				    delivery_id, order_id, delivery_status, delivery_address, shipped_date,
				    delivered_date, courier_name, created_at
				)
				select delivery_id, order_id,
				       upper(replace(replace(trim(delivery_status), '-', '_'), ' ', '_')),
				       delivery_address,
				       case when nullif(trim(shipped_date), '') is null then null
				            else replace(shipped_date, 'T', ' ')::timestamp end,
				       case when nullif(trim(delivered_date), '') is null then null
				            else replace(delivered_date, 'T', ' ')::timestamp end,
				       courier_name,
				       case when nullif(trim(source_created_at), '') is null then null
				            else replace(source_created_at, 'T', ' ')::timestamp end
				from (
				    select s.*, row_number() over (partition by delivery_id order by row_number) as duplicate_number
				    from analyticsflow_staging.stg_deliveries s
				    where job_id = ? and is_valid = true
				) s
				where duplicate_number = 1
				  and nullif(trim(delivery_id), '') is not null
				  and nullif(trim(order_id), '') is not null
				  and upper(replace(replace(trim(delivery_status), '-', '_'), ' ', '_')) in ('PENDING', 'SHIPPED', 'IN_TRANSIT', 'DELIVERED', 'FAILED', 'RETURNED', 'CANCELLED')
				  and nullif(trim(delivery_address), '') is not null
				  and (shipped_date is null or trim(shipped_date) = '' or shipped_date ~ ?)
				  and (delivered_date is null or trim(delivered_date) = '' or delivered_date ~ ?)
				  and nullif(trim(courier_name), '') is not null
				  and (source_created_at is null or trim(source_created_at) = '' or source_created_at ~ ?)
				  and exists (select 1 from analyticsflow_oltp.orders o where o.order_id = s.order_id)
				on conflict (delivery_id) do update set
				    order_id = excluded.order_id,
				    delivery_status = excluded.delivery_status,
				    delivery_address = excluded.delivery_address,
				    shipped_date = excluded.shipped_date,
				    delivered_date = excluded.delivered_date,
				    courier_name = excluded.courier_name,
				    created_at = excluded.created_at
				""",
				jobId,
				TIMESTAMP_PATTERN,
				TIMESTAMP_PATTERN,
				TIMESTAMP_PATTERN);
	}

	private int invalid(String sourceFile, String sourceTable, String selectSql, Object... arguments) {
		String sql = """
				insert into analyticsflow_support.invalid_records (job_id, source_file, source_table, row_number, raw_payload, error_message)
				""" + selectSql + """
				on conflict (job_id, source_table, row_number) where row_number is not null do nothing
				""";
		return jdbcTemplate.update(sql, arguments);
	}
}
