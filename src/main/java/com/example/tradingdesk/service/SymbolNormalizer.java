package com.example.tradingdesk.service;

import java.util.Locale;

public final class SymbolNormalizer {

    private SymbolNormalizer() {
    }

    public static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
