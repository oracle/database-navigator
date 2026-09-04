package com.dbn.vector.pipeline;

import com.dbn.vector.model.VectorEmbeddingContext;
import org.jetbrains.annotations.NotNull;


public interface EmbeddingPipeline {

    /**
     * Execute the complete embedding pipeline.
     */
    void execute(@NotNull VectorEmbeddingContext context);
}
