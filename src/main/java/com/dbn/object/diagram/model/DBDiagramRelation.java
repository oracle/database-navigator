package com.dbn.object.diagram.model;

import com.dbn.object.common.DBObject;
import org.jetbrains.annotations.NotNull;

public record DBDiagramRelation<T extends DBObject>(
        @NotNull T source,
        @NotNull T target,
        @NotNull String name) {
}
