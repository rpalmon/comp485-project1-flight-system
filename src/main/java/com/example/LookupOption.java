package com.example;

import java.util.Locale;

public final class LookupOption {
    private final String id;
    private final String label;
    private final String searchText;

    public LookupOption(String id, String label, String searchText) {
        this.id = id == null ? "" : id.trim();
        this.label = label == null ? "" : label.trim();
        this.searchText = searchText == null ? this.label.toLowerCase(Locale.ROOT) : searchText.toLowerCase(Locale.ROOT);
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public boolean matchesExact(String query) {
        String normalized = normalize(query);
        return !normalized.isBlank()
                && (normalize(label).equals(normalized) || searchText.equals(normalized));
    }

    public boolean matchesLoose(String query) {
        String normalized = normalize(query);
        return !normalized.isBlank()
                && (normalize(label).contains(normalized) || searchText.contains(normalized));
    }

    @Override
    public String toString() {
        return label;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
