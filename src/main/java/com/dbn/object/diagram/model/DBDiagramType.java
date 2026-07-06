package com.dbn.object.diagram.model;

import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

@Getter
public enum DBDiagramType {
    TABLE_MODEL("dbn_table_model_diagram", "Table Model Diagram"),
    ROLE_MODEL("dbn_role_model_diagram", "Role Model Diagram");

    private final String providerId;
    private final String presentableName;

    DBDiagramType(@NonNls String providerId, String presentableName) {
        this.providerId = providerId;
        this.presentableName = presentableName;
    }

    @Nullable
    public static DBDiagramType forObjectType(DBObjectType objectType) {
        return switch (objectType) {
            case TABLE -> TABLE_MODEL;
            case ROLE -> ROLE_MODEL;
            default -> null;
        };
    }
}
