/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.language.common.element.lookup;

import com.dbn.language.common.element.cache.ElementLookupContext;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.LeafElementType;
import com.intellij.lang.ASTNode;

import java.util.Set;

/**
 * Resolves successors within one grammar construct using its direct AST child as the current position.
 * Implementations append candidates to the shared result; {@link NextLeafResolver} owns AST ascent
 * and removes branch markers when leaving named scopes.
 */
interface ElementNextLeafResolver<E extends ElementTypeBase> {
    /**
     * Adds locally reachable leaves and determines whether lookup may leave this construct.
     *
     * @param element the grammar type of the source's parent
     * @param source the direct child reached while walking upward from the original leaf
     * @param context the current branch, version, and boundary settings
     * @param leafs the mutable candidate set shared by the traversal
     * @return the next traversal action; abort requires the coordinator to discard the entire result
     */
    Step resolve(E element, ASTNode source, ElementLookupContext context, Set<LeafElementType> leafs);

    enum Step {
        /**
         * Keep the collected candidates and finish lookup at this boundary.
         */
        STOP,
        /**
         * Allow the coordinator to apply wrapping rules and continue into the parent scope.
         */
        ASCEND,
        /**
         * The position cannot be resolved; discard candidates from all visited scopes.
         */
        ABORT
    }
}
