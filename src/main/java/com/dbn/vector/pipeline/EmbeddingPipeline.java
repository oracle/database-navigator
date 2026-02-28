package com.dbn.vector.pipeline;

import com.dbn.vector.model.VectorEmbeddingContext;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.VectorEmbeddingResult;
import org.jetbrains.annotations.NotNull;


public interface EmbeddingPipeline {

    /**
     * Execute the complete embedding pipeline.
     */
    void execute(
            @NotNull VectorEmbeddingContext context,
            @NotNull VectorEmbeddingRequest request,
            @NotNull VectorEmbeddingResult result);
}
