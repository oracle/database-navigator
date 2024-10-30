package com.dbn.language.common.element.util;

import com.dbn.common.property.Property;
import lombok.Getter;

@Getter
public enum ElementTypeAttribute implements Property.LongBase {
    
    ROOT("ROOT", "Executable statement"),
    EXECUTABLE("EXECUTABLE", "Executable statement"),
    DEBUGGABLE("DEBUGGABLE", "Debuggable"),
    TRANSACTIONAL("TRANSACTIONAL", "Transactional statement"),
    TRANSACTIONAL_CANDIDATE("TRANSACTIONAL_CANDIDATE", "Transactional statement"),
    QUERY("QUERY", "Query statement", true),
    DATA_DEFINITION("DATA_DEFINITION", "Data definition statement", true),
    DATA_MANIPULATION("DATA_MANIPULATION", "Data manipulation statement", true),
    COMPILABLE_BLOCK("COMPILABLE_BLOCK", "Compilable block", false),
    TRANSACTION_CONTROL("TRANSACTION_CONTROL", "Transaction control statement", true),
    OBJECT_SPECIFICATION("OBJECT_SPECIFICATION", "Object specification", true),
    OBJECT_DECLARATION("OBJECT_DECLARATION", "Object declaration", true),
    OBJECT_DEFINITION("OBJECT_DEFINITION", "Object definition", true),
    SUBJECT("SUBJECT", "Statement subject"),
    STATEMENT("STATEMENT", "Statement"),
    CLAUSE("CLAUSE", "Statement clause"),
    CONDITION("CONDITION", "Condition expression"),
    STRUCTURE("STRUCTURE", "Structure view element"),
    SCOPE_ISOLATION("SCOPE_ISOLATION", "Scope isolation"),
    SCOPE_DEMARCATION("SCOPE_DEMARCATION", "Scope demarcation"),
    FOLDABLE_BLOCK("FOLDABLE_BLOCK", "Foldable block"),
    EXECUTABLE_CODE("EXECUTABLE_CODE", "Executable code"),
    BREAKPOINT_POSITION("BREAKPOINT_POSITION", "Default breakpoint position"),
    ACTION("ACTION", "Action"),
    GENERIC("GENERIC", "Generic element"),
    SPECIFIC("SPECIFIC", "Specific element"),
    SPECIFIC_OVERRIDE("SPECIFIC_OVERRIDE", "Specific override element"),
    DATABASE_LOG_PRODUCER("DATABASE_LOG_PRODUCER", "Database logging"),
    METHOD_PARAMETER_HANDLER("METHOD_PARAMETER_HANDLER", "Method parameter handler"),
    COLUMN_PARAMETER_HANDLER("COLUMN_PARAMETER_HANDLER", "Column parameter handler"),
    COLUMN_PARAMETER_PROVIDER("COLUMN_PARAMETER_PROVIDER", "Column parameter provider"),
    SCHEMA_CHANGE("SCHEMA_CHANGE", "Schema change clause"),
    DB_ASSISTANT("DB_ASSISTANT", "Database assistant statement"),
    DB_ASSISTANT_PROMPT("DB_ASSISTANT_PROMPT", "Database assistant prompt"),
    ;

    public static final ElementTypeAttribute[] VALUES = values();

    private final LongMasks masks = new LongMasks(this);
    private final String name;
    private final String description;
    private final boolean specific;

    ElementTypeAttribute(String name, String description) {
        this(name, description, false);
    }

    ElementTypeAttribute(String name, String description, boolean specific) {
        this.name = name;
        this.description = description;
        this.specific = specific;
    }

    @Override
    public LongMasks masks() {
        return masks;
    }

    @Override
    public String toString() {
        return name;
    }

}
