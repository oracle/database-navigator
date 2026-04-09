package com.dbn.connection.config.io;

public final class OracleConnectionJsonExporter {
    private OracleConnectionJsonExporter() {}

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper()
                    .enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
    public static void exportConfig(OracleConnectionJsonConfig cfg, java.nio.file.Path file, String key) throws Exception {
        if (key == null || key.isBlank()) {
            MAPPER.writeValue(file.toFile(), cfg);
        } else {
            var root = MAPPER.createObjectNode();
            root.set(key.trim(), MAPPER.valueToTree(cfg));
            MAPPER.writeValue(file.toFile(), root);
        }
    }
}