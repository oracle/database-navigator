package com.dbn.object.diagram.model;

import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

@Getter
public enum DBDiagramType {
    DATA_MODEL("dbn_data_model_diagram", "Data Model Diagram"),
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
            case TABLE -> DATA_MODEL;
            case ROLE -> ROLE_MODEL;
            default -> null;
        };
    }
}
