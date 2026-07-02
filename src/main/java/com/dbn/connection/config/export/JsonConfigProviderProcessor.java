package com.dbn.connection.config.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;


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
        if(payload == null) throw new IllegalArgumentException("payload is null");
        if (file == null)throw new IllegalArgumentException("output file is null");

        ObjectNode payloadNode = toJson(payload);

        if(wrapperKey == null || wrapperKey.isBlank()){
            MAPPER.writeValue(file.toFile(), payloadNode);
        }
        else {
            ObjectNode root = MAPPER.createObjectNode();
            root.set(wrapperKey.trim(), payloadNode);
            MAPPER.writeValue(file.toFile(), root);
        }

    }
    private static ObjectNode toJson(ConfigProviderPayload payload){
        ObjectNode node = MAPPER.createObjectNode();

        putIfNotBlank(node, "connect_descriptor", payload.getConnectDescriptor());
        putIfNotBlank(node, "user", payload.getUser());

        if (payload.getWalletLocation() != null) {
            node.set("wallet_location", toSecretRefJson(payload.getWalletLocation()));
        }
        Map<String, Object> jdbc = payload.getJdbc();
        if (jdbc != null && !jdbc.isEmpty()) {
            node.set("jdbc", MAPPER.valueToTree(jdbc));
        }
        return node;

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
