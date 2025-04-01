/*
 * Copyright 2025 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.common.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonTest {

    @Test
    public void createJsonPreview1() {
        String json = "{\"_id\":101,\"name\":\"Max Verstappen\",\"points\":0,\"retired\":false,\"team\":\"Red Bull\",\"race\":[],\"_metadata\":{\"etag\":\"F9D9815DFF27879F61386CFD1622B065\",\"asof\":\"0000287FD844FC9A\"}}";
        String preview = Json.createJsonPreview(json, 3);
        assertEquals("{\"name\": \"Max Verstappen\", \"points\": 0, \"retired\": false...}", preview);
    }

    @Test
    public void createJsonPreview2() {
        String json = "{\"_id\":105,\"name\":\"George Russell\",\"points\":12,\"retired\":true,\"team\":\"Ferrari\",\"race\":[],\"_metadata\":{\"etag\":\"C98F2BC901F43FFB046A137321573C12\",\"asof\":\"0000287FD844FC9A\"}}";
        String preview = Json.createJsonPreview(json, 3);
        assertEquals("{\"name\": \"George Russell\", \"points\": 12, \"retired\": true...}", preview);
    }

    @Test
    public void createJsonPreview3() {
        String json = "{\"_id\":2,\"name\":\"Mercedes\",\"points\":40,\"description\":null,\"driver\":[{\"driverId\":103,\"name\":\"Charles Leclerc\",\"points\":25},{\"driverId\":106,\"name\":\"Lewis Hamilton\",\"points\":15}],\"_metadata\":{\"etag\":\"9E266CD7554A89663B73B9977B1F967C\",\"asof\":\"0000287FDE03C539\"}}";
        String preview = Json.createJsonPreview(json, 3);
        assertEquals("{\"name\": \"Mercedes\", \"points\": 40, \"description\": null...}", preview);
    }

    @Test
    public void createJsonPreview4() {
        String json = "";
        String preview = Json.createJsonPreview(json, 3);
        assertEquals("", preview);
    }

    @Test
    public void formatJsonContentProperlyFormatted() {
        String json = "{\n  \"name\": \"Max Verstappen\",\n  \"points\": 25\n}";
        String formatted = Json.formatJsonContent(json);
        assertEquals("{\n  \"name\" : \"Max Verstappen\",\n  \"points\" : 25\n}", formatted);
    }

    @Test
    public void formatJsonContentIncorrectlyFormatted() {
        String json = "{\"name\":\"Max Verstappen\",\"points\":25}";
        String formatted = Json.formatJsonContent(json);
        assertEquals("{\n  \"name\" : \"Max Verstappen\",\n  \"points\" : 25\n}", formatted);
    }

    @Test
    public void formatJsonContentEmptyString() {
        String json = "";
        String formatted = Json.formatJsonContent(json);
        assertEquals("", formatted);
    }

    @Test
    public void formatJsonContentInvalidJson() {
        String json = "{name:Max Verstappen}";
        String formatted = Json.formatJsonContent(json);
        assertEquals(json, formatted);
    }
}