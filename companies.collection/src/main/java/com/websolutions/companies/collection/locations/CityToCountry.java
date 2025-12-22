package com.websolutions.companies.collection.locations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CityToCountry {
	
	/*
	 * This program must be run one time so the Json file is created.
	 * */
	
	/*
	 * The program will send internet requests to “Nominatim,” a free online service.
	 * We need to tell it who we are (the user agent), and where to send our requests (the base URL).
	 * NOMINATIM_BASE: the website (API) we’ll use to find what country each city belongs to.
	 * USER_AGENT: a short “signature” that identifies your program to that website.
	 * */
    private static final String NOMINATIM_BASE = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "jobs-web/1.0 (contact: mouad-thf3@example.com)";
    
    
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
     * By telling it to ignore unknown properties, you protect your code from breaking if the API adds extra fields that you aren’t using.
     * */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    
    /*
     * This is a simple in-memory cache that maps a cleaned city string to the country name returned by the API. 
     * If the same city appears multiple times in your list, the program can fetch the answer from this cache instead of calling the API again. 
     * That makes the program faster and reduces the number of web requests, which is important because Nominatim asks 
     * clients to limit their request rate.
     * */
    private static final Map<String, String> CACHE = new HashMap<>();

    
    
    /*
     * This begins the core lookup method, getCountry, which takes a city query and returns an Optional<String> containing the 
     * country name if one is found. 
     * It first normalizes the input by trimming leading and trailing spaces and collapsing any run of whitespace into a single space. 
     * It then checks the cache; if the city has already been looked up during this run, it immediately returns the cached value, 
     * avoiding another network call.
     * */
    public static Optional<String> getCountry(String query) throws IOException, InterruptedException {
        String key = query.trim().replaceAll("\\s+", " ");
        if (CACHE.containsKey(key)) return Optional.ofNullable(CACHE.get(key));

        /*
         * This line pauses the program for about 1.1 seconds before making the request. 
         * Nominatim’s usage policy expects clients to limit themselves to roughly one request per second. 
         * Sleeping slightly over one second provides a small safety margin to remain within that limit and 
         * avoid being rate-limited or blocked.
         * */
        TimeUnit.MILLISECONDS.sleep(1100);
        
        
        
        /*
         * Here the program constructs the full request URL using OkHttp’s HttpUrl builder, which safely handles 
         * URL encoding and parameter concatenation. 
         * It sets the query (q) to the normalized city name, 
         * asks for a JSON response (format=json), 
         * requests extra address details so that the country appears (addressdetails=1), 
         * limits the response to a single best match for efficiency (limit=1), 
         * and requests English-language labels in the response (accept-language=en). 
         * 
         * Building the URL this way avoids mistakes that can happen with manual string concatenation.
         * */
        HttpUrl url = HttpUrl.parse(NOMINATIM_BASE).newBuilder()
                .addQueryParameter("q", key)
                .addQueryParameter("format", "json")
                .addQueryParameter("addressdetails", "1")
                .addQueryParameter("limit", "1")
                .addQueryParameter("accept-language", "en")
                .build();

        
        /*
         * This creates the HTTP request with the URL we just built and attaches the User-Agent header. 
         * The request object represents exactly what will be sent over the network. 
         * Including the user agent is mandatory for Nominatim and signals that our client is well-behaved.
         * */
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build();
        
        
        
        /*
         * This sends the HTTP request and waits for the response, using a try-with-resources block so the response is 
         * automatically closed afterward. 
         * It then checks whether the HTTP status indicates success and that there is a body to read. 
         * If the call failed or the body is missing, the method caches a null result for this key (so repeated failures 
         * won’t keep hitting the API) and returns an empty Optional to signal that no country could be determined.
         * */
        try (Response resp = HTTP.newCall(request).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                CACHE.put(key, null);
                return Optional.empty();
            }
            
            
            /*
             * This block reads the response body bytes and asks Jackson to deserialize them into an array of NominatimResult 
             * objects (defined below). 
             * If there are no results or the first result lacks an address, it caches null and returns empty. 
             * Otherwise it extracts the country name from the first result, stores it in the cache, 
             * and returns it inside an Optional. 
             * Limiting to the first result keeps things fast, though it does mean we trust Nominatim’s best match for ambiguous city names.
             * */
            NominatimResult[] results = MAPPER.readValue(resp.body().bytes(), NominatimResult[].class);
            if (results.length == 0 || results[0].address == null) {
                CACHE.put(key, null);
                return Optional.empty();
            }
            String country = results[0].address.country;
            CACHE.put(key, country);
            return Optional.ofNullable(country);
        }
    }
    
    
    /*
     * These two tiny classes describe the parts of the JSON we care about. 
     * NominatimResult contains an address, and Address contains country and country_code. 
     * The @JsonIgnoreProperties(ignoreUnknown = true) annotation tells Jackson to ignore any other 
     * fields in the JSON that aren’t declared here. 
     * This keeps the deserialization simple and resilient to API changes while letting us access the specific fields we need.
     * */

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class NominatimResult {
        public Address address;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Address {
        public String country;
        public String country_code;
    }

    // Example usage on your pasted list
    public static void main(String[] args) throws Exception {
        String raw = """
        Andover, Massachusetts
        Rochester, New York
        Rochester, New York
        Victor, New York
        Rochester, New York
        Canton, Massachusetts
        Rochester, New York
        Carlsbad, California
        St. louis, Missouri
        Stamford, Connecticut
        Andover, Massachusetts
        Rochester, New York
        Rochester, New York
        Victor, New York
        Rochester, New York
        Canton, Massachusetts
        San jose, California
        Chicago, Illinois
        Chicago, Illinois
        Carrollton, Texas
        Stamford, Connecticut
        Chicago, Illinois
        Chicago, Illinois
        Newark, New Jersey
        Rochester, New York
        Bothell, Washington
        Seattle, Washington
        Pennington, New Jersey
        Plano, Texas
        Charlotte, North Carolina
        BORDEAUX
        BORDEAUX
        BORDEAUX
        LYON
        LYON
        BORDEAUX
        MONTPELLIER
        LYON
        LYON
        LYON
        LYON
        NANTES
        NANTES
        NANTES
        NANTES
        STRASBOURG
        STRASBOURG
        STRASBOURG
        Göteborg
        Stockholm
        Linköping
        Göteborg, Gotland, Stockholm
        Linköping
        Lund
        Stockholm
        Stockholm
        Göteborg
        Göteborg, Stockholm
        Göteborg
        Ludvika, Västerås
        Göteborg
        Göteborg
        Linköping
        Jönköping
        Stockholm
        Göteborg, Gotland, Stockholm
        Göteborg, Gotland, Stockholm
        Göteborg
        Linköping
        Västerås
        Göteborg, Gotland, Stockholm
        Jönköping
        Lund
        Stockholm
        Örebro
        Jönköping
        Göteborg
        Göteborg
        Linköping
        Stockholm
        Stockholm
        Stockholm
        Göteborg, Stockholm
        Göteborg, Stockholm
        Göteborg, Stockholm
        Stockholm
        Göteborg
        Ludvika, Västerås
        Göteborg
        Västerås
        Göteborg, Gotland, Stockholm
        Stockholm
        Göteborg
        Göteborg
        """;
        
        /*
         * This line turns the large text block into a list of individual lines, 
         * trims each line to remove leading and trailing whitespace, and discards any blank lines. 
         * The result is a clean list of non-empty strings that the rest of the program can iterate over without extra checks.
         * */
        List<String> queries = raw.lines().map(String::trim).filter(s -> !s.isBlank()).toList();

        
        
        /*
         * This loop normalizes each input line into a single city name. 
         * If a line contains multiple comma-separated items (for example, “Göteborg, Gotland, Stockholm”), 
         * it keeps only the first part (“Göteborg”), trims it, and then performs a very simple title-case 
         * operation that uppercases just the first character. 
         * The normalized city is added to a list. 
         * This step standardizes the inputs so the API receives consistent, city-only queries, 
         * which improves matching and avoids sending noisy context that might confuse the lookup. 
         * */
        List<String> normalizedCities = new ArrayList<>();
        for (String q : queries) {
            String firstToken = q.split(",")[0].trim();
            if (firstToken.isEmpty()) continue;
            String city = firstToken.substring(0, 1).toUpperCase() + firstToken.substring(1);
            normalizedCities.add(city);
        }

        
        
        /*
         * This creates a mapping from a country name to a set of its cities. 
         * Using a set ensures that each city appears only once under a given country, even if it appeared many times in the input. 
         * The map is initially a HashMap because insertion and lookup are fast; 
         * later, the code converts the final result into sorted forms for nicer output.
         * */
        Map<String, Set<String>> countryToCities = new HashMap<>();

        
        /*
         * This loop performs the actual lookups. 
         * For each normalized city, it calls getCountry to ask Nominatim which country the city belongs to. 
         * If the lookup fails or returns nothing, the city is skipped; 
         * alternatively, we could store such cases under an “Unknown” bucket. 
         * If a country is found, the code ensures there is a TreeSet for that country (creating one if necessary) and adds the city to it. 
         * A TreeSet is used because it keeps the stored city names unique and sorted alphabetically, so the final output is clean and ordered.
         * */
        for (String city : normalizedCities) {
            Optional<String> countryOpt = getCountry(city);
            if (countryOpt.isEmpty()) {
                countryOpt = Optional.of("Unknown");
                continue;
            }
            String country = countryOpt.get();
            countryToCities.computeIfAbsent(country, k -> new TreeSet<>())
                           .add(city);
        }

        /*
         * This converts the intermediate map into a structure that is convenient to write as JSON. 
         * JSON doesn’t have a “set” type, so each set of cities is turned into a list. 
         * The outer map is changed to a TreeMap, which keeps its keys (the country names) sorted alphabetically. 
         * Because the inner collections were TreeSets, the city lists are already in alphabetical order. 
         * The result is a tidy, readable mapping from countries to their city lists.
         * */
        Map<String, List<String>> output = new TreeMap<>();
        for (Map.Entry<String, Set<String>> e : countryToCities.entrySet()) {
            output.put(e.getKey(), new ArrayList<>(e.getValue()));
        }

        /*
         * This creates a File object for a file named countries_to_cities.json and uses Jackson’s ObjectMapper 
         * to write the output map to that file in human-friendly, indented JSON. 
         * This is where the program produces its main artifact: a clean, grouped, and sorted list of cities by country that we can open, 
         * share, or feed into other tools.
         * */
        Path path = Paths.get("models/country_cities.json");
        File outFile = new File(path.toString());
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(outFile, output);

        /* System.out.println("Wrote JSON to: " + outFile.getAbsolutePath()); */
        /* System.out.println("Countries: " + output.size()); */
        output.forEach((ctry, cities) ->
                System.out.println(ctry + " -> " + cities.size() + " cities"));
    }
}
