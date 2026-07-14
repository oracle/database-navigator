/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.connection.config.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class JsonConfigProviderProcessorTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final JsonConfigProviderProcessor processor = new JsonConfigProviderProcessor();

    @Test
    public void renderWritesOnlyConfiguredValuesAndPasswordTemplate() throws Exception {
        String json = processor.render(payload(), null);

        JsonNode node = MAPPER.readTree(json);
        assertEquals("(description=(connect_data=(service_name=prod)))", node.path("connect_descriptor").asText());
        assertEquals("scott", node.path("user").asText());
        assertEquals("FILL_THIS_TYPE", node.path("password").path("type").asText());
        assertEquals("FILL_THIS_VALUE", node.path("password").path("value").asText());
        assertEquals(1000, node.path("jdbc").path("timeout").asInt());
        assertFalse(node.has("wallet_location"));
    }

    @Test
    public void renderWrapsPayloadWhenWrapperKeyIsConfigured() throws Exception {
        JsonNode root = MAPPER.readTree(processor.render(payload(), " production "));

        assertEquals("scott", root.path("production").path("user").asText());
        assertFalse(root.has("user"));
    }

    @Test
    public void writeMergesWrappedPayloadIntoExistingConfigurationFile() throws Exception {
        Path file = temporaryFolder.newFile("connections.json").toPath();
        Files.writeString(file, "{\"development\":{\"user\":\"dev\"}}");

        processor.write(payload(), file, "production");

        JsonNode root = MAPPER.readTree(Files.readString(file));
        assertEquals("dev", root.path("development").path("user").asText());
        assertEquals("scott", root.path("production").path("user").asText());
    }

    @Test
    public void writeReplacesRootConfigurationWithWrappedPayload() throws Exception {
        Path file = temporaryFolder.newFile("connection.json").toPath();
        Files.writeString(file, "{\"connect_descriptor\":\"old\",\"user\":\"old-user\"}");

        processor.write(payload(), file, "production");

        JsonNode root = MAPPER.readTree(Files.readString(file));
        assertEquals("scott", root.path("production").path("user").asText());
        assertFalse(root.has("connect_descriptor"));
    }

    @Test
    public void writeRejectsExistingJsonArray() throws Exception {
        Path file = temporaryFolder.newFile("connection.json").toPath();
        Files.writeString(file, "[]");

        assertThrows(IllegalArgumentException.class, () -> processor.write(payload(), file, "production"));
    }

    @Test
    public void renderIncludesBase64WalletOnlyWhenPresent() throws Exception {
        ConfigProviderPayload payload = ConfigProviderPayload.builder()
                .walletLocation(SecretRef.builder().type(SecretProviderType.BASE64).value("d2FsbGV0").build())
                .build();

        JsonNode wallet = MAPPER.readTree(processor.render(payload, null)).path("wallet_location");
        assertEquals("base64", wallet.path("type").asText());
        assertEquals("d2FsbGV0", wallet.path("value").asText());
        assertTrue(wallet.isObject());
    }

    private static ConfigProviderPayload payload() {
        return ConfigProviderPayload.builder()
                .connectDescriptor("(description=(connect_data=(service_name=prod)))")
                .user("scott")
                .password(SecretRefFactory.emptyTemplate())
                .jdbc(Map.of("timeout", 1000))
                .build();
    }
}
