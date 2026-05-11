package com.yzidev.analyticsflow.batch.staging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CsvLineParserTest {

	@Test
	void parsesQuotedCommasAndEscapedQuotes() {
		assertThat(CsvLineParser.parse("u-1,\"Jane, D\",jane@example.com,\"hello \"\"there\"\"\""))
				.containsExactly("u-1", "Jane, D", "jane@example.com", "hello \"there\"");
	}

	@Test
	void rejectsUnterminatedQuotedValue() {
		assertThatThrownBy(() -> CsvLineParser.parse("u-1,\"Jane D,jane@example.com"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("unterminated quoted value");
	}
}
