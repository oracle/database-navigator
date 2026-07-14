package com.dbn.common.ui.form.field;

/** Visibility and editability state of a form field. */
public enum FieldState {
    HIDDEN,
    VISIBLE,
    EDITABLE;

    public boolean isVisible() {
        return this != HIDDEN;
    }

    public boolean isEditable() {
        return this == EDITABLE;
    }
}
