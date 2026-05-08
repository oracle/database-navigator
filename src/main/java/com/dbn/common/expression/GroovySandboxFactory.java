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

package com.dbn.common.expression;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.experimental.UtilityClass;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.NotExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.SynchronizedStatement;
import org.codehaus.groovy.ast.stmt.ThrowStatement;
import org.codehaus.groovy.ast.stmt.TryCatchStatement;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;

import java.util.List;

import static java.util.Collections.emptyList;

@UtilityClass
public final class GroovySandboxFactory {

    public static GroovyShell createSandbox(Binding binding) {
        CompilerConfiguration config = new CompilerConfiguration();

        SecureASTCustomizer secure = new SecureASTCustomizer();

        // Completely disable imports
        secure.setAllowedImports(emptyList());
        secure.setAllowedStarImports(emptyList());
        secure.setStaticImportsWhitelist(emptyList());
        secure.setStaticStarImportsWhitelist(emptyList());

        // Disallow new instance creation
        secure.setIndirectImportCheckEnabled(true);
        secure.setClosuresAllowed(false);
        secure.setMethodDefinitionAllowed(false); // no user‑defined methods

        // Allowed expression types (whitelist);
        secure.setAllowedExpressions(List.of(
                NotExpression.class,
                BinaryExpression.class,
                BooleanExpression.class,
                ConstantExpression.class,
                VariableExpression.class,
                ListExpression.class,
                ArgumentListExpression.class));

        // Disallow things that are obviously dangerous
        secure.setReceiversBlackList(List.of("java.lang.System"));
        secure.setStatementsBlacklist(List.of(
                ThrowStatement.class,
                TryCatchStatement.class,
                SynchronizedStatement.class));

        // Prevent package declarations
        secure.setPackageAllowed(false);

        // using an additional customizer (not shown here for brevity).
        config.addCompilationCustomizers(secure);

        return new GroovyShell(binding, config);
    }
}