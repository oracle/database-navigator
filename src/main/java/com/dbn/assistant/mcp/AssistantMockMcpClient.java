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

package com.dbn.assistant.mcp;

public class AssistantMockMcpClient {
    public String call(String service, String argsJson) {
        switch (service) {
            case "listTables": return "{ \"tables\": [\"employees\", \"departments\"] }";
            case "getTableDetails": return " {\n" +
                    "   \"table\": \"employees\",\n" +
                    "   \"columns\": [\n" +
                    "           { \"name\": \"id\", \"type\": \"INT\" },\n" +
                    "           { \"name\": \"name\", \"type\": \"VARCHAR\" },\n" +
                    "           { \"name\": \"hire_date\", \"type\": \"DATE\" }\n" +
                    "           { \"name\": \"department_id\", \"type\": \"INT\" }\n" +
                    "   ]\n" +
                    "}";
            case "executeSql": return "{ \"rows\": [ { \"name\": \"Alice\" }, { \"name\": \"Bob\" } ] }";
            default: return "{ \"error\": \"Unknown service\" }";
        }
    }


}
