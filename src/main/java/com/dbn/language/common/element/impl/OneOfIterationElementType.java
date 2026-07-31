/* Copyright 2024 Oracle and/or its affiliates */
package com.dbn.language.common.element.impl;

import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.util.ElementTypeDefinitionException;
import org.jdom.Element;

import java.util.List;

import static com.dbn.common.options.setting.Settings.stringAttribute;

/** An iteration whose alternatives are declared as flat children. */
public class OneOfIterationElementType extends IterationElementType {
    public boolean unique;

    public OneOfIterationElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id, Element def)
            throws ElementTypeDefinitionException {
        super(bundle, parent, id, def);

        unique = getBooleanAttribute(def, "unique");
    }

    @Override
    protected void loadDefinition(Element def) throws ElementTypeDefinitionException {
        super.loadDefinition(def);

        // TODO

/*        OneOfElementType oneOfElementType = new OneOfElementType(bundle, this, this.id + ".o", def);
        String separator = stringAttribute(def, "separator");
        oneOfElementType.initSeparatorTokens(separator);
        oneOfElementType.iteratedElement = this.wrappedElement;

        this.wrappedElement = oneOfElementType;
        this.wrappedElementOptional = false;     */

    }

    private static Element toIterationDefinition(Element def) throws ElementTypeDefinitionException {
        List<Element> children = def.getChildren();
        if (children.isEmpty()) {
            throw new ElementTypeDefinitionException("Invalid one-of-iteration definition. Element should contain at least one child.");
        }

        Element iteration = new Element("iteration");
        String separator = stringAttribute(def, "separator");
        if (separator != null) iteration.setAttribute("separator", separator);
        String optional = stringAttribute(def, "optional");
        if (optional != null) iteration.setAttribute("optional", optional);

        Element oneOf = new Element("one-of");
        for (Element child : children) {
            oneOf.addContent(child.clone());
        }
        iteration.addContent(oneOf);
        return iteration;
    }
}
