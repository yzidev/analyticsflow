package com.yzidev.analyticsflow.batch.staging;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.yzidev.analyticsflow.entity.staging.BaseStagingEntity;
import com.yzidev.analyticsflow.entity.staging.DeliveryStagingEntity;
import com.yzidev.analyticsflow.entity.staging.OrderItemStagingEntity;
import com.yzidev.analyticsflow.entity.staging.OrderStagingEntity;
import com.yzidev.analyticsflow.entity.staging.ProductCategoryStagingEntity;
import com.yzidev.analyticsflow.entity.staging.ProductDetailStagingEntity;
import com.yzidev.analyticsflow.entity.staging.ProductStagingEntity;
import com.yzidev.analyticsflow.entity.staging.TransactionStagingEntity;
import com.yzidev.analyticsflow.entity.staging.UserStagingEntity;
import com.yzidev.analyticsflow.common.enums.CsvDataset;

public final class StagingEntitySqlMappings {

	private StagingEntitySqlMappings() {
	}

	public static StagingEntitySqlMapping<UserStagingEntity> users() {
		return mapping(
				CsvDataset.USERS,
				List.of("user_id", "full_name", "email", "phone", "city", "country", "source_created_at"),
				List.of(
						UserStagingEntity::getUserId,
						UserStagingEntity::getFullName,
						UserStagingEntity::getEmail,
						UserStagingEntity::getPhone,
						UserStagingEntity::getCity,
						UserStagingEntity::getCountry,
						UserStagingEntity::getSourceCreatedAt));
	}

	public static StagingEntitySqlMapping<ProductCategoryStagingEntity> productCategories() {
		return mapping(
				CsvDataset.PRODUCT_CATEGORIES,
				List.of("category_id", "category_name", "parent_category_id", "description", "source_created_at"),
				List.of(
						ProductCategoryStagingEntity::getCategoryId,
						ProductCategoryStagingEntity::getCategoryName,
						ProductCategoryStagingEntity::getParentCategoryId,
						ProductCategoryStagingEntity::getDescription,
						ProductCategoryStagingEntity::getSourceCreatedAt));
	}

	public static StagingEntitySqlMapping<ProductStagingEntity> products() {
		return mapping(
				CsvDataset.PRODUCTS,
				List.of("product_id", "category_id", "product_name", "brand", "price", "is_active",
						"source_created_at"),
				List.of(
						ProductStagingEntity::getProductId,
						ProductStagingEntity::getCategoryId,
						ProductStagingEntity::getProductName,
						ProductStagingEntity::getBrand,
						ProductStagingEntity::getPrice,
						ProductStagingEntity::getActive,
						ProductStagingEntity::getSourceCreatedAt));
	}

	public static StagingEntitySqlMapping<ProductDetailStagingEntity> productDetails() {
		return mapping(
				CsvDataset.PRODUCT_DETAILS,
				List.of("product_detail_id", "product_id", "sku", "color", "size", "weight", "material",
						"manufacture_date", "expiry_date", "source_created_at"),
				List.of(
						ProductDetailStagingEntity::getProductDetailId,
						ProductDetailStagingEntity::getProductId,
						ProductDetailStagingEntity::getSku,
						ProductDetailStagingEntity::getColor,
						ProductDetailStagingEntity::getSize,
						ProductDetailStagingEntity::getWeight,
						ProductDetailStagingEntity::getMaterial,
						ProductDetailStagingEntity::getManufactureDate,
						ProductDetailStagingEntity::getExpiryDate,
						ProductDetailStagingEntity::getSourceCreatedAt));
	}

	public static StagingEntitySqlMapping<OrderStagingEntity> orders() {
		return mapping(
				CsvDataset.ORDERS,
				List.of("order_id", "user_id", "order_date", "order_status", "total_amount", "channel",
						"source_created_at"),
				List.of(
						OrderStagingEntity::getOrderId,
						OrderStagingEntity::getUserId,
						OrderStagingEntity::getOrderDate,
						OrderStagingEntity::getOrderStatus,
						OrderStagingEntity::getTotalAmount,
						OrderStagingEntity::getChannel,
						OrderStagingEntity::getSourceCreatedAt));
	}

	public static StagingEntitySqlMapping<OrderItemStagingEntity> orderItems() {
		return mapping(
				CsvDataset.ORDER_ITEMS,
				List.of("order_item_id", "order_id", "product_id", "quantity", "unit_price", "total_price"),
				List.of(
						OrderItemStagingEntity::getOrderItemId,
						OrderItemStagingEntity::getOrderId,
						OrderItemStagingEntity::getProductId,
						OrderItemStagingEntity::getQuantity,
						OrderItemStagingEntity::getUnitPrice,
						OrderItemStagingEntity::getTotalPrice));
	}

	public static StagingEntitySqlMapping<TransactionStagingEntity> transactions() {
		return mapping(
				CsvDataset.TRANSACTIONS,
				List.of("transaction_id", "order_id", "user_id", "transaction_date", "payment_method", "amount",
						"currency", "status", "source_created_at"),
				List.of(
						TransactionStagingEntity::getTransactionId,
						TransactionStagingEntity::getOrderId,
						TransactionStagingEntity::getUserId,
						TransactionStagingEntity::getTransactionDate,
						TransactionStagingEntity::getPaymentMethod,
						TransactionStagingEntity::getAmount,
						TransactionStagingEntity::getCurrency,
						TransactionStagingEntity::getStatus,
						TransactionStagingEntity::getSourceCreatedAt));
	}

	public static StagingEntitySqlMapping<DeliveryStagingEntity> deliveries() {
		return mapping(
				CsvDataset.DELIVERIES,
				List.of("delivery_id", "order_id", "delivery_status", "delivery_address", "shipped_date",
						"delivered_date", "courier_name", "source_created_at"),
				List.of(
						DeliveryStagingEntity::getDeliveryId,
						DeliveryStagingEntity::getOrderId,
						DeliveryStagingEntity::getDeliveryStatus,
						DeliveryStagingEntity::getDeliveryAddress,
						DeliveryStagingEntity::getShippedDate,
						DeliveryStagingEntity::getDeliveredDate,
						DeliveryStagingEntity::getCourierName,
						DeliveryStagingEntity::getSourceCreatedAt));
	}

	private static <T extends BaseStagingEntity> StagingEntitySqlMapping<T> mapping(
			CsvDataset dataset,
			List<String> datasetColumns,
			List<Function<T, Object>> datasetValues) {
		List<String> columns = new ArrayList<>();
		columns.add("job_id");
		columns.add("row_number");
		columns.addAll(datasetColumns);
		columns.add("raw_payload");
		columns.add("is_valid");
		columns.add("error_message");
		columns.add("created_at");

		List<Function<T, Object>> values = new ArrayList<>();
		values.add(BaseStagingEntity::getJobId);
		values.add(BaseStagingEntity::getRowNumber);
		values.addAll(datasetValues);
		values.add(BaseStagingEntity::getRawPayload);
		values.add(BaseStagingEntity::getValid);
		values.add(BaseStagingEntity::getErrorMessage);
		values.add(BaseStagingEntity::getCreatedAt);

		return new StagingEntitySqlMapping<>(
				dataset.fileName(),
				"analyticsflow_staging." + dataset.stagingTable(),
				columns,
				values);
	}
}
