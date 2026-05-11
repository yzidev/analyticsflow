package com.yzidev.analyticsflow.batch.staging;

import java.util.ArrayList;
import java.util.List;

public final class CsvLineParser {

	private CsvLineParser() {
	}

	public static List<String> parse(String line) {
		List<String> values = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean quoted = false;

		for (int index = 0; index < line.length(); index++) {
			char character = line.charAt(index);
			if (character == '"') {
				if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
					current.append('"');
					index++;
				}
				else {
					quoted = !quoted;
				}
			}
			else if (character == ',' && !quoted) {
				values.add(current.toString());
				current.setLength(0);
			}
			else {
				current.append(character);
			}
		}

		if (quoted) {
			throw new IllegalArgumentException("Malformed CSV row: unterminated quoted value");
		}

		values.add(current.toString());
		return values;
	}
}
