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

package com.dbn.assistant.tool.spec;

import com.dbn.assistant.tool.AssistantTool;
import com.dbn.assistant.tool.AssistantToolFactoryBase;
import com.dbn.assistant.tool.AssistantToolInfo.ToolSpec;
import com.dbn.assistant.tool.AssistantToolInfo.UtilitySpec;
import com.dbn.assistant.tool.impl.SemanticSearchToolImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.util.List;

import static com.dbn.assistant.tool.AssistantToolCategory.DATA_PROVIDER;
import static com.dbn.assistant.tool.AssistantToolInfo.FactorySpec;

@ToolSpec(
        category = DATA_PROVIDER,
        type = "SEMANTIC_SEARCH", //AssistantToolType.SEMANTIC_SEARCH
        name = "Semantic search",
        description = "Provides semantic search utilities for retrieval‑augmented generation (RAG)")
public interface SemanticSearchTool extends AssistantTool {

    @FactorySpec(
            spec = SemanticSearchTool.class,
            impl = SemanticSearchToolImpl.class)
    class Factory extends AssistantToolFactoryBase<SemanticSearchTool> {}

    /*********************************************
     *                 TOOLS                     *
     *********************************************/

    @Tool(name = "PERFORM_SEMANTIC_SEARCH")
    @UtilitySpec(
            name = "Perform semantic search",
            description = "Performs a similarity search over a vector repository, " +
                    "returning the most relevant passages/documents related to the query, sorted by similarity score. " +
                    "Use this when you need domain-specific information that may not be in your training data")
    List<SemanticSearchResult> performSemanticSearch(
            @P("Search query") String query,
            @P("Number of results to retrieve (1–20)") int maxResults);

    @Data
    @Description("Semantic search result")
    class SemanticSearchResult {
        @Description("Unique identifier of the result")
        private String id;

        @Description("Text of the retrieved passage/document.")
        private String content;

        @Description("COSINE similarity measure of relevance to the query")
        private double score;

    }
}
