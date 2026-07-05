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
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.codehaus.groovy.syntax.Types;

import java.util.List;

import static com.dbn.common.util.Lists.firstElement;
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

        // Disallow new instance creation and executable definitions
        secure.setIndirectImportCheckEnabled(true);
        secure.setClosuresAllowed(false);
        secure.setMethodDefinitionAllowed(false);

        // Groovy wraps evaluated snippets in a block; allow only plain expression statements inside it
        secure.setAllowedStatements(List.of(
                BlockStatement.class,
                ExpressionStatement.class));

        secure.addStatementCheckers(statement -> isSingleExpression(statement));

        // Allowed expression types
        secure.setAllowedExpressions(List.of(
                NotExpression.class,
                BinaryExpression.class,
                BooleanExpression.class,
                ConstantExpression.class,
                VariableExpression.class,
                ListExpression.class,
                ArgumentListExpression.class));

        // Allowed operators for SQL-filter style predicates
        secure.setAllowedTokens(List.of(
                Types.COMPARE_EQUAL,
                Types.COMPARE_NOT_EQUAL,
                Types.COMPARE_LESS_THAN,
                Types.COMPARE_LESS_THAN_EQUAL,
                Types.COMPARE_GREATER_THAN,
                Types.COMPARE_GREATER_THAN_EQUAL,
                Types.KEYWORD_IN,
                Types.LOGICAL_AND,
                Types.LOGICAL_OR,
                Types.MATCH_REGEX,
                Types.NOT,
                Types.PREFIX_MINUS,
                Types.PREFIX_PLUS));

        secure.setReceiversBlackList(List.of("java.lang.System"));

        // Prevent package declarations
        secure.setPackageAllowed(false);

        config.addCompilationCustomizers(secure);
        return new GroovyShell(binding, config);
    }

    private static boolean isSingleExpression(Statement statement) {
        if (statement instanceof BlockStatement blockStatement) {
            List<Statement> statements = blockStatement.getStatements();
            if (statements.size() != 1) return false;
            return firstElement(statements) instanceof ExpressionStatement;
        }
        return statement instanceof ExpressionStatement;
    }
}
