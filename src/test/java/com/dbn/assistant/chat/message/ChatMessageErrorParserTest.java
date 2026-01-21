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

package com.dbn.assistant.chat.message;

import org.junit.Test;

public class ChatMessageErrorParserTest {

    @Test
    public void convertJsonToHtml() {
        String html = ChatMessageErrorParser.convertJsonToHtml("{\n" +
                "  \"error\": {\n" +
                "    \"code\": 429,\n" +
                "    \"message\": \"You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits.\\n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_input_token_count, limit: 0\\n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 0\\n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 0\\nPlease retry in 42.031100388s.\",\n" +
                "    \"status\": \"RESOURCE_EXHAUSTED\",\n" +
                "    \"details\": [\n" +
                "      {\n" +
                "        \"@type\": \"type.googleapis.com/google.rpc.QuotaFailure\",\n" +
                "        \"violations\": [\n" +
                "          {\n" +
                "            \"quotaMetric\": \"generativelanguage.googleapis.com/generate_content_free_tier_input_token_count\",\n" +
                "            \"quotaId\": \"GenerateContentInputTokensPerModelPerMinute-FreeTier\",\n" +
                "            \"quotaDimensions\": {\n" +
                "              \"location\": \"global\",\n" +
                "              \"model\": \"gemini-1.5-pro\"\n" +
                "            }\n" +
                "          },\n" +
                "          {\n" +
                "            \"quotaMetric\": \"generativelanguage.googleapis.com/generate_content_free_tier_requests\",\n" +
                "            \"quotaId\": \"GenerateRequestsPerMinutePerProjectPerModel-FreeTier\",\n" +
                "            \"quotaDimensions\": {\n" +
                "              \"location\": \"global\",\n" +
                "              \"model\": \"gemini-1.5-pro\"\n" +
                "            }\n" +
                "          },\n" +
                "          {\n" +
                "            \"quotaMetric\": \"generativelanguage.googleapis.com/generate_content_free_tier_requests\",\n" +
                "            \"quotaId\": \"GenerateRequestsPerDayPerProjectPerModel-FreeTier\",\n" +
                "            \"quotaDimensions\": {\n" +
                "              \"location\": \"global\",\n" +
                "              \"model\": \"gemini-1.5-pro\"\n" +
                "            }\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"@type\": \"type.googleapis.com/google.rpc.Help\",\n" +
                "        \"links\": [\n" +
                "          {\n" +
                "            \"description\": \"Learn more about Gemini API quotas\",\n" +
                "            \"url\": \"https://ai.google.dev/gemini-api/docs/rate-limits\"\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"@type\": \"type.googleapis.com/google.rpc.RetryInfo\",\n" +
                "        \"retryDelay\": \"42s\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}");

        System.out.println(html);
    }
}