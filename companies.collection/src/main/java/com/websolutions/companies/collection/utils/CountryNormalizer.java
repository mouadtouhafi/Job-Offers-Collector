package com.websolutions.companies.collection.utils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class CountryNormalizer {
	
	private static final Map<String, String> COUNTRY_MAP = new HashMap<>();
	static {
		COUNTRY_MAP.put("czechia", "Czech Republic");
		COUNTRY_MAP.put("deutschland", "Germany");
	}
	
	public String find(String check) {
		return COUNTRY_MAP.entrySet().stream()
				.filter(entry -> entry.getKey().equalsIgnoreCase(check))
				.map(Map.Entry::getValue)
				.findFirst()
				.orElse("NOT FOUND");
	}
}