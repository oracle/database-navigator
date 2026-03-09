package com.dbn.connection.config.io;

public final class OracleConnectionJsonExporter {
    private OracleConnectionJsonExporter() {}

    public static void exportConfig(OracleConnectionJsonConfig cfg, java.nio.file.Path file, String key) throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                .enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);

        if (key == null || key.isBlank()) {
            mapper.writeValue(file.toFile(), cfg);
        } else {
            var root = mapper.createObjectNode();
            root.set(key.trim(), mapper.valueToTree(cfg));
            mapper.writeValue(file.toFile(), root);
        }
    }
}