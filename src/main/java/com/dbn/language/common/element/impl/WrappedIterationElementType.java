/* Copyright 2024 Oracle and/or its affiliates */
package com.dbn.language.common.element.impl;

import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.util.ElementTypeDefinitionException;
import org.jdom.Element;

import java.util.List;

import static com.dbn.common.options.setting.Settings.stringAttribute;

/** A wrapper whose single child is implicitly iterated. */
public class WrappedIterationElementType extends WrapperElementType {
    public WrappedIterationElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id, Element def)
            throws ElementTypeDefinitionException {
        super(bundle, parent, id, def);
        List<Element> children = def.getChildren();
        if (children.size() != 1) {
            throw new ElementTypeDefinitionException("Invalid wrapped-iteration definition. Element should contain exactly one child.");
        }
    }

    @Override
    protected void loadDefinition(Element def) throws ElementTypeDefinitionException {
        super.loadDefinition(def);

        IterationElementType iterationElement = new SurrogateIterationElementType();
        String separator = stringAttribute(def, "separator");
        iterationElement.initSeparatorTokens(separator);
        iterationElement.iteratedElement = this.wrappedElement;

        this.wrappedElement = iterationElement;
        this.wrappedElementOptional = false;
    }

    private class SurrogateIterationElementType extends IterationElementType {
        public SurrogateIterationElementType() throws ElementTypeDefinitionException {
            super(WrappedIterationElementType.this.bundle,
                    WrappedIterationElementType.this,
                    WrappedIterationElementType.this.id + ".i", new Element("iteration"));
        }

        @Override
        protected void loadDefinition(Element def){}
    }
}
