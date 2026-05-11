package com.yzidev.analyticsflow.common.enums;

import java.util.List;

public enum CsvDataset {
	USERS("users.csv", "stg_users", List.of("user_id", "full_name", "email", "phone", "city", "country", "created_at")),
	PRODUCT_CATEGORIES("product_categories.csv", "stg_product_categories",
			List.of("category_id", "category_name", "parent_category_id", "description", "created_at")),
	PRODUCTS("products.csv", "stg_products",
			List.of("product_id", "category_id", "product_name", "brand", "price", "is_active", "created_at")),
	PRODUCT_DETAILS("product_details.csv", "stg_product_details",
			List.of("product_detail_id", "product_id", "sku", "color", "size", "weight", "material",
					"manufacture_date", "expiry_date", "created_at")),
	ORDERS("orders.csv", "stg_orders",
			List.of("order_id", "user_id", "order_date", "order_status", "total_amount", "channel", "created_at")),
	ORDER_ITEMS("order_items.csv", "stg_order_items",
			List.of("order_item_id", "order_id", "product_id", "quantity", "unit_price", "total_price")),
	TRANSACTIONS("transactions.csv", "stg_transactions",
			List.of("transaction_id", "order_id", "user_id", "transaction_date", "payment_method", "amount",
					"currency", "status", "created_at")),
	DELIVERIES("deliveries.csv", "stg_deliveries",
			List.of("delivery_id", "order_id", "delivery_status", "delivery_address", "shipped_date",
					"delivered_date", "courier_name", "created_at"));

	private final String fileName;
	private final String stagingTable;
	private final List<String> expectedHeader;

	CsvDataset(String fileName, String stagingTable, List<String> expectedHeader) {
		this.fileName = fileName;
		this.stagingTable = stagingTable;
		this.expectedHeader = expectedHeader;
	}

	public String fileName() {
		return fileName;
	}

	public String stagingTable() {
		return stagingTable;
	}

	public List<String> expectedHeader() {
		return expectedHeader;
	}
}
