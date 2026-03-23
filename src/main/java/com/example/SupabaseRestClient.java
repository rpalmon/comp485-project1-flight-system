package com.example;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SupabaseRestClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final HttpClient client = HttpClient.newHttpClient();
    private final String baseUrl;
    private final String apiKey;

    public SupabaseRestClient(String baseUrl, String apiKey) {
        this.baseUrl = requireValue(baseUrl, "Supabase URL");
        this.apiKey = requireValue(apiKey, "Anon key");
    }

    public String loadRows(
            String table,
            String selectColumns,
            Integer limit,
            String orderBy,
            String direction,
            String filterColumn,
            String filterOperator,
            String filterValue
    ) throws IOException, InterruptedException {
        List<QueryFilter> filters = new ArrayList<>();
        if (filterColumn != null && !filterColumn.isBlank() && filterValue != null && !filterValue.isBlank()) {
            filters.add(new QueryFilter(filterColumn, filterOperator, filterValue));
        }

        return loadRows(table, selectColumns, limit, orderBy, direction, filters);
    }

    public String loadRows(
            String table,
            String selectColumns,
            Integer limit,
            String orderBy,
            String direction,
            List<QueryFilter> filters
    ) throws IOException, InterruptedException {
        List<String> queryParts = new ArrayList<>();
        queryParts.add("select=" + encode(selectColumns == null || selectColumns.isBlank() ? "*" : selectColumns));

        if (limit != null && limit > 0) {
            queryParts.add("limit=" + limit);
        }

        if (orderBy != null && !orderBy.isBlank()) {
            String orderValue = orderBy.trim() + "." + (direction == null || direction.isBlank() ? "desc" : direction.trim());
            queryParts.add("order=" + encode(orderValue));
        }

        for (QueryFilter filter : filters == null ? Collections.<QueryFilter>emptyList() : filters) {
            if (filter.column() == null || filter.column().isBlank() || filter.value() == null || filter.value().isBlank()) {
                continue;
            }

            String filterExpression = (filter.operator() == null || filter.operator().isBlank() ? "eq" : filter.operator().trim())
                    + "." + filter.value().trim();
            queryParts.add(encode(filter.column().trim()) + "=" + encode(filterExpression));
        }

        URI uri = URI.create(endpoint(table) + "?" + String.join("&", queryParts));
        return send("GET", uri, null);
    }

    public String insert(String table, String payloadJson) throws IOException, InterruptedException {
        return send("POST", URI.create(endpoint(table)), requireValue(payloadJson, "Insert JSON"));
    }

    public String update(String table, String matchColumn, String matchValue, String payloadJson) throws IOException, InterruptedException {
        URI uri = URI.create(endpoint(table) + "?" + matchQuery(matchColumn, matchValue));
        return send("PATCH", uri, requireValue(payloadJson, "Update JSON"));
    }

    public String delete(String table, String matchColumn, String matchValue) throws IOException, InterruptedException {
        URI uri = URI.create(endpoint(table) + "?" + matchQuery(matchColumn, matchValue));
        return send("DELETE", uri, null);
    }

    private String endpoint(String table) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedTable = requireValue(table, "Table name").trim();
        return normalizedBaseUrl + "/rest/v1/" + normalizedTable;
    }

    private String matchQuery(String matchColumn, String matchValue) {
        String normalizedColumn = requireValue(matchColumn, "Match column").trim();
        String normalizedValue = requireValue(matchValue, "Match value").trim();
        return "select=*"
                + "&" + encode(normalizedColumn)
                + "=" + encode("eq." + normalizedValue);
    }

    private String send(String method, URI uri, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Prefer", "return=representation");

        if (body != null) {
            builder.header("Content-Type", "application/json");
        }

        HttpRequest request = switch (method) {
            case "GET" -> builder.GET().build();
            case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body)).build();
            case "PATCH" -> builder.method("PATCH", HttpRequest.BodyPublishers.ofString(body)).build();
            case "DELETE" -> builder.DELETE().build();
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            String errorBody = response.body() == null || response.body().isBlank()
                    ? response.statusCode() + " " + response.uri()
                    : response.body();
            throw new IOException(errorBody);
        }

        return response.body() == null || response.body().isBlank() ? "[]" : response.body();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String requireValue(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value;
    }

    public record QueryFilter(String column, String operator, String value) {
    }
}
