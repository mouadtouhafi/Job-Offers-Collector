package com.websolutions.companies.collection.locations;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Service
public class DetectCities {

    private static final String NOMINATIM_BASE = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "jobs-web/1.0 (contact: mouad-thfi4@gmail.com)";
    private static final Path JSON_FILE = Paths.get("models/country_cities.json");

    
    /*
     * This builds a single reusable HTTP client using OkHttp. 
     * The client is configured with a total call timeout of twenty seconds so that any request that takes too 
     * long fails cleanly instead of hanging forever. 
     * Defining it as a static final field ensures the same client (and its internal connection pool) is reused for all requests, 
     * which is more efficient and considered best practice.
     * */
    private static final OkHttpClient HTTP = new OkHttpClient.Builder() 
            .callTimeout(Duration.ofSeconds(20))
            .build();
    
    
    /*
     * This creates a Jackson ObjectMapper configured to ignore any JSON fields it doesn’t know about. 
     * When the program asks Nominatim for data, it receives JSON text; the ObjectMapper turns that JSON into Java objects. 
     * By telling it to ignore unknown properties, you protect our code from breaking if the API adds extra fields that you aren’t using.
     * */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    
    /*
     * These structures store city and country information in memory. 
     * The cache keeps quick lookups for the current runtime, while the two other maps hold the JSON file data and an 
     * index for fast city-to-country searches.
     * */
    private static final Map<String, String> CACHE_CITY_TO_COUNTRY = new HashMap<>();
    private static Map<String, List<String>> COUNTRY_TO_CITIES = null;
    private static Map<String, String> CITY_TO_COUNTRY_INDEX = null; 

    /**
     * Orchestrator:
     * 1) check local JSON,
     * 2) if missing: call API, then persist.
     * 
     * This method is the main entry point. 
     * It first cleans the city name, checks the in-memory cache, then searches the JSON file through findCountryInLocalJson. 
     * If not found, it calls fetchCountryFromApi to query Nominatim. 
     * When the API returns a valid result, the city and country are added to the local JSON file so future lookups are faster.
     */
    public Optional<String> getCountryForCity(String userCity) throws IOException, InterruptedException {
        String city = normalizeCity(userCity);
        if (city.isEmpty()) {
            System.out.println("⚠️ Empty city name received.");
            return Optional.empty();
        }

        System.out.println("\n🔎 Looking up city: " + city);
        String lower = city.toLowerCase(Locale.ROOT);

        /*
         * This checks whether the requested city already exists in the program’s in-memory cache, 
         * which is a HashMap named CACHE_CITY_TO_COUNTRY.
         * */
        if (CACHE_CITY_TO_COUNTRY.containsKey(lower)) {
            System.out.println("✅ Found in in-memory cache.");
            return Optional.ofNullable(CACHE_CITY_TO_COUNTRY.get(lower));
        }

        
        /*
         * This snippet first tries to find the country in the local JSON file by calling findCountryInLocalJson(city). 
         * If that method returns a non-empty Optional, meaning the city was found, it retrieves the country value, 
         * saves it into the in-memory cache for faster future access, and immediately returns it. 
         * This way, if the city already exists in the JSON, the program avoids calling the external API.
         * */
        Optional<String> local = findCountryInLocalJson(city);
        if (local.isPresent()) {
            String country = local.get();
            CACHE_CITY_TO_COUNTRY.put(lower, country);
            return local;
        }

        
        /*
         * This calls the API using fetchCountryFromApi(city) to get the country.
         * If the API returns a result, it extracts the country name, stores it in the in-memory cache for quick reuse, 
         * and adds it to the local JSON file with addCityToLocalStore(city, country) so it’s saved permanently. 
         * 	Finally, it returns the API result.
         * */
        Optional<String> api = fetchCountryFromApi(city);
        if (api.isPresent()) {
            String country = api.get();
            CACHE_CITY_TO_COUNTRY.put(lower, country);
            addCityToLocalStore(city, country);
        }
        return api;
    }

    /**
     * NEW: Check only the JSON file for the city (case-insensitive).
     * No network call. Returns Optional.of(country) if found.
     */
    public static Optional<String> findCountryInLocalJson(String userCity) throws IOException {
        String city = normalizeCity(userCity);
        if (city.isEmpty()) return Optional.empty();
        String lower = city.toLowerCase(Locale.ROOT);

        ensureIndexLoaded();
        String country = CITY_TO_COUNTRY_INDEX.get(lower);

        if (country != null) {
            System.out.println("✅ Local JSON hit: " + city + " -> " + country);
            return Optional.of(country);
        } else {
            System.out.println("ℹ️ Local JSON miss for: " + city);
            return Optional.empty();
        }
    }

    /**
     * NEW: Query only the Nominatim API for the city (no JSON write).
     * Returns Optional.of(country) if API finds it.
     */
    public static Optional<String> fetchCountryFromApi(String userCity) throws IOException, InterruptedException {
        String city = normalizeCity(userCity);
        if (city.isEmpty()) return Optional.empty();

        System.out.println("🌐 Querying Nominatim for: " + city);
        TimeUnit.MILLISECONDS.sleep(1100);

        HttpUrl url = HttpUrl.parse(NOMINATIM_BASE).newBuilder()
                .addQueryParameter("q", city)
                .addQueryParameter("format", "json")
                .addQueryParameter("addressdetails", "1")
                .addQueryParameter("limit", "1")
                .addQueryParameter("accept-language", "en")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build();

        try (Response resp = HTTP.newCall(request).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                System.out.println("❌ HTTP request failed with code: " + resp.code());
                return Optional.empty();
            }
            NominatimResult[] results = MAPPER.readValue(resp.body().bytes(), NominatimResult[].class);
            if (results.length == 0 || results[0].address == null) {
                System.out.println("❌ No results from API for: " + city);
                return Optional.empty();
            }
            String country = results[0].address.country;
            System.out.println("🌍 API result: " + city + " -> " + country);
            return Optional.ofNullable(country);
        }
    }

    /*
     * The purpose of ensureIndexLoaded() is to load the local JSON data into memory once and prepare it for fast lookups.
     * 
     * Our JSON file looks like this : 
     * 		{
     * 			"France": ["Lyon", "Bordeaux"],
     * 			"Sweden": ["Stockholm", "Göteborg"]
     * 		}		
     * 
     * But this file is just text on disk, not in memory yet, and reading from a file every time would be slow.
     * So when the program starts, it must load the file into memory and build an index that allows fast lookups.
     * 
     * When our program tries to find a city, it calls ensureIndexLoaded() to make sure the memory is ready.
     * - If nothing is loaded yet (COUNTRY_TO_CITIES == null) :
     *   It opens the JSON file, reads it, and stores it in memory as a Map<String, List<String>>.
     * - If the city→country index hasn’t been built (CITY_TO_COUNTRY_INDEX == null) :
     * 	 Then it loops through the above data and creates a reverse map in memory:
     * 		
     * 		CITY_TO_COUNTRY_INDEX = {
     * 			"lyon" = "France",
     * 			"bordeaux" = "France",
     * 			"stockholm" = "Sweden",
     * 			"göteborg" = "Sweden"
     * 		}
     * 	 Now, if we ask for "Lyon", it can instantly find "France" from this map, without searching the whole file.
     * 
     * */
    private static synchronized void ensureIndexLoaded() throws IOException {
        if (COUNTRY_TO_CITIES == null) {
            System.out.println("📂 Loading local JSON file...");
            COUNTRY_TO_CITIES = readCountryCitiesFile(JSON_FILE);
            System.out.println("✅ Loaded " + COUNTRY_TO_CITIES.size() + " countries.");
        }
        if (CITY_TO_COUNTRY_INDEX == null) {
            System.out.println("🧠 Building in-memory index...");
            CITY_TO_COUNTRY_INDEX = buildCityToCountryIndex(COUNTRY_TO_CITIES);
            System.out.println("✅ Indexed " + CITY_TO_COUNTRY_INDEX.size() + " cities.");
        }
    }

    
    /*
     * This read our JSON file from disk into memory as a Java map.
     * If the file does exist, it uses Jackson’s ObjectMapper (MAPPER.readValue(...)) to open and parse it. 
     * The JSON content is automatically converted into a Java object of type TreeMap<String, List<String>>, 
     * where each key is a country name and each value is the list of its cities.
     * */
    private static Map<String, List<String>> readCountryCitiesFile(Path file) throws IOException {
        if (!Files.exists(file)) {
            System.out.println("⚠️ No JSON file found, starting with empty dataset.");
            return new TreeMap<>();
        }
        return MAPPER.readValue(file.toFile(), new TypeReference<TreeMap<String, List<String>>>() {});
    }

    
    /*	We have for example the following Map : { "France": ["Lyon", "Bordeaux"], "Sweden": ["Stockholm", "Göteborg"] }
     * 
     * 1- countryToCities.entrySet() returns something like : 
     *    [ ("France" = ["Lyon", "Bordeaux"]), ("Sweden" = ["Stockholm", "Göteborg"]) ]
     * 
     * 2- .stream() turns that set into a stream — a sequence of these entries that we 
     * 	  can process one by one using Java’s Stream API.
     *    We get something like this : 
     *    	("France" = ["Lyon", "Bordeaux"])
     *    	("Sweden" = ["Stockholm", "Göteborg"])
     *    
     * 3- Each element e in this stream represents one of those pairs.
     * 
     * 4- For the first entry ("France" = ["Lyon", "Bordeaux"]): 
     * 	  e.getValue() gives the list ["Lyon", "Bordeaux"], then calling .stream() on it creates a stream of cities.
     * 
     * 5- The .filter(Objects::nonNull) part simply skips any null city names (in case the JSON contains an empty entry).
     * 
     * 6- The .flatMap(...) part takes all these individual city streams from each country and merges (“flattens”) 
     *    them into one continuous stream of cities.
     *    So after the flatMap call, instead of having a stream of two lists (one for France and one for Sweden), 
     *    we now have a single flat stream of all cities ("Lyon" "Bordeaux" "Stockholm" "Göteborg").
     *    
     * 7- .map(...) : Transforms each city name into a small key–value pair object (Map.Entry).
     *    The key is the city name converted to lowercase.
     *    The value is the country name from the outer entry (e.getKey()).
     *    
     *    flatMap(...) Combines all these small streams from every country into one continuous stream of entries:
     *    ("lyon", "France")
     *    ("bordeaux", "France")
     *    ("stockholm", "Sweden")
     *    ("göteborg", "Sweden")
     *    
     * */
    private static Map<String, String> buildCityToCountryIndex(Map<String, List<String>> countryToCities) {
        return countryToCities.entrySet().stream()
                .flatMap(e -> e.getValue().stream().filter(Objects::nonNull)
                        .map(city -> Map.entry(city.toLowerCase(Locale.ROOT), e.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    
    
    /*
     * This method adds a new city–country pair to both memory and the JSON file if it doesn’t already exist. 
     * It first ensures the current data is loaded, then checks if the country has a city list; 
     * if not, it creates one. 
     * It verifies whether the city is already present (ignoring case). 
     * If the city is new, it adds it to the list, sorts the list alphabetically, updates the reverse lookup map, 
     * and saves the updated data back to the JSON file. 
     * If the city already exists, it simply prints a message and makes no changes.
     * */
    private static synchronized void addCityToLocalStore(String city, String country) throws IOException {
        ensureIndexLoaded();

        COUNTRY_TO_CITIES.computeIfAbsent(country, k -> new ArrayList<>());
        List<String> cities = COUNTRY_TO_CITIES.get(country);
        boolean exists = cities.stream().anyMatch(c -> c.equalsIgnoreCase(city));

        if (!exists) {
            cities.add(city);
            cities.sort(String.CASE_INSENSITIVE_ORDER);
            CITY_TO_COUNTRY_INDEX.put(city.toLowerCase(Locale.ROOT), country);

            ensureParentDir(JSON_FILE);
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(JSON_FILE.toFile(), COUNTRY_TO_CITIES);

            System.out.println("💾 Persisted to JSON: \"" + city + "\" under \"" + country + "\".");
        } else {
            System.out.println("ℹ️ City already exists under " + country + " in JSON.");
        }
    }

    
    /*
     * This method makes sure that the folder where the JSON file should be saved actually exists. 
     * It gets the parent directory of the file path, checks whether it’s missing, and if so, 
     * tries to create it using mkdirs(). 
     * If the directory creation fails for any reason, it throws an IOException. 
     * In short, it guarantees that the necessary folder structure is ready before writing the JSON file.
     * */
    private static void ensureParentDir(Path file) throws IOException {
        File parent = file.toFile().getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs() && !parent.exists()) {
                throw new IOException("Failed to create directory: " + parent);
            }
        }
    }

    
    /*
     * This method cleans and standardizes the city name entered by the user. 
     * It first checks if the input is null and returns an empty string if so. 
     * Then it trims any extra spaces at the beginning or end and replaces multiple spaces with a single one. 
     * Finally, it splits the text by commas and keeps only the first part, so something like "Paris, France" becomes "Paris". 
     * This ensures all city names are in a clean, consistent format before being processed.
     * */
    private static String normalizeCity(String input) {
        if (input == null) return "";
        String s = input.trim().replaceAll("\\s+", " ");
        return s.split(",")[0].trim();
    }

    
    /*
     * This small inner class represents part of the JSON structure returned by the Nominatim API. 
     * The annotation @JsonIgnoreProperties(ignoreUnknown = true) tells Jackson to ignore any extra 
     * fields in the JSON that are not defined in this class. 
     * The class itself contains one field, address, which corresponds to the "address" object in the API response and 
     * is mapped to another class named Address. 
     * This setup lets Jackson automatically deserialize only the relevant part of the API response.
     * */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class NominatimResult {
        public Address address;
    }

    
    /*
     * This inner class models the "address" part of the JSON returned by the Nominatim API. 
     * The @JsonIgnoreProperties(ignoreUnknown = true) annotation tells Jackson to ignore any 
     * extra data fields not declared here. 
     * It contains two public variables: country, which stores the full country name (like "France"), 
     * and country_code, which stores the ISO country code (like "fr"). 
     * When the API response is read, these values are automatically filled in by Jackson during deserialization.
     * */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Address {
        public String country;
        public String country_code;
    }
}
