package com.dbn.connection.config.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class JsonConfigProviderProcessor extends ConfigProviderFormatProcessor{

    private static final ObjectMapper MAPPER =
            new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public String getId() {
        return "json";
    }

    @Override
    public String getDisplayName() {
        return "JSON";
    }

    @Override
    public String getDefaultExtension() {
        return "json";
    }

    @Override
    public void write(ConfigProviderPayload payload, Path file, String wrapperKey) throws Exception {
        if (file == null) throw new IllegalArgumentException("output file is null");
        if (payload == null) throw new IllegalArgumentException("payload is null");

        boolean hasWrapperKey = wrapperKey != null && !wrapperKey.isBlank();
        boolean hasExistingContent = Files.exists(file) && Files.size(file) > 0;
        if (!hasExistingContent) {
            Files.writeString(file, render(payload, wrapperKey), StandardCharsets.UTF_8);
            return;
        }
        ObjectNode root = readExistingRoot(file);
        if (!hasWrapperKey) {
            Files.writeString(file, render(payload, wrapperKey), StandardCharsets.UTF_8);
            return;
        }
        if (containsRootConfiguration(root)) {
            Files.writeString(file, render(payload, wrapperKey), StandardCharsets.UTF_8);
            return;
        }

        root.set(wrapperKey.trim(), toJson(payload));
        Files.writeString(file, MAPPER.writeValueAsString(root), StandardCharsets.UTF_8);
    }

    JsonExistingContentWriteMode getExistingContentWriteMode(Path file, String wrapperKey) throws Exception {
        if (file == null || !Files.exists(file) || Files.size(file) == 0) return JsonExistingContentWriteMode.NONE;

        ObjectNode root = readExistingRoot(file);
        if (wrapperKey == null || wrapperKey.isBlank() || containsRootConfiguration(root)) {
            return JsonExistingContentWriteMode.REPLACE_ROOT;
        }
        return root.has(wrapperKey.trim()) ? JsonExistingContentWriteMode.REPLACE_WRAPPER : JsonExistingContentWriteMode.NONE;
    }

    @Override
    public String render(ConfigProviderPayload payload, String wrapperKey) throws Exception {
        if (payload == null) throw new IllegalArgumentException("payload is null");

        ObjectNode payloadNode = toJson(payload);

        if (wrapperKey == null || wrapperKey.isBlank()) {
            return MAPPER.writeValueAsString(payloadNode);
        } else {
            ObjectNode root = MAPPER.createObjectNode();
            root.set(wrapperKey.trim(), payloadNode);
            return MAPPER.writeValueAsString(root);
        }
    }
    private static ObjectNode toJson(ConfigProviderPayload payload){
        ObjectNode node = MAPPER.createObjectNode();

        putIfNotBlank(node, "connect_descriptor", payload.getConnectDescriptor());
        putIfNotBlank(node, "user", payload.getUser());
        if (payload.getPassword() != null) {
            node.set("password", toSecretRefJson(payload.getPassword()));
        }

        if (payload.getWalletLocation() != null) {
            node.set("wallet_location", toSecretRefJson(payload.getWalletLocation()));
        }
        Map<String, Object> jdbc = payload.getJdbc();
        if (jdbc != null && !jdbc.isEmpty()) {
            node.set("jdbc", MAPPER.valueToTree(jdbc));
        }
        return node;

    }

    private static boolean containsRootConfiguration(ObjectNode root) {
        return root.has("connect_descriptor");
    }

    private static ObjectNode readExistingRoot(Path file) throws Exception {
        JsonNode existing = MAPPER.readTree(Files.readString(file, StandardCharsets.UTF_8));
        if (existing instanceof ObjectNode root) return root;

        throw new IllegalArgumentException("Existing export file must contain a JSON object.");
    }

    private static JsonNode toSecretRefJson(SecretRef ref) {
        ObjectNode node = MAPPER.createObjectNode();

        SecretProviderType type = ref.getType();
        if (type != null) {
            node.put("type", type.id());
        }

        putIfNotBlank(node, "value", ref.getValue());

        putIfNotBlank(node, "field_name", ref.getFieldName());

        SecretAuthentication auth = ref.getAuthentication();
        if (auth != null) {
            ObjectNode authNode = MAPPER.createObjectNode();
            putIfNotBlank(authNode, "method", auth.getMethod());

            Map<String, Object> params = auth.getParameters();
            if (params != null && !params.isEmpty()) {
                authNode.set("parameters", MAPPER.valueToTree(params));
            }

            if (authNode.size() > 0) node.set("authentication", authNode);
        }

        return node;
    }

    private static void putIfNotBlank(ObjectNode node, String key, String value) {
        if (value != null) {
            String v = value.trim();
            if (!v.isEmpty()) node.put(key, v);
        }
    }

}

enum JsonExistingContentWriteMode {
    NONE,
    REPLACE_WRAPPER,
    REPLACE_ROOT
}
