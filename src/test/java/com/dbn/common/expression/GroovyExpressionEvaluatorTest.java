/*
 * Copyright 2024 Oracle and/or its affiliates
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

import lombok.SneakyThrows;
import org.jetbrains.annotations.NonNls;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GroovyExpressionEvaluatorTest {
    GroovyExpressionEvaluator expressionEvaluator = new GroovyExpressionEvaluator();

    @Test
    public void evaluateBooleanExpression(){
        boolean result = expressionEvaluator.evaluateBooleanExpression("COLUMN_NAME = 'TEST1'", context("COLUMN_NAME", "TEST1"));
        Assert.assertTrue(result);

        result = expressionEvaluator.evaluateBooleanExpression("COLUMN_NAME != 'TEST1'", context("COLUMN_NAME", "TEST2"));
        Assert.assertTrue(result);

        result = expressionEvaluator.evaluateBooleanExpression("COLUMN_NAME = 'TEST1'", context("COLUMN_NAME", "TEST2"));
        Assert.assertFalse(result);

        result = expressionEvaluator.evaluateBooleanExpression("COLUMN_NAME IN ('TEST1',  'TEST2', 'TEST3')", context("COLUMN_NAME", "TEST1"));
        Assert.assertTrue(result);

        result = expressionEvaluator.evaluateBooleanExpression("COLUMN_NAME IN ('TEST1',  'TEST2', 'TEST3')", context("COLUMN_NAME", "TEST4"));
        Assert.assertFalse(result);


        result = expressionEvaluator.evaluateBooleanExpression("COLUMN_NAME    IN ('TEST1',  'TEST2', 'TEST3') AND COLUMN_SIZE IN (1, 2, 3)", context("COLUMN_NAME", "TEST3", "COLUMN_SIZE", 2));
        Assert.assertTrue(result);

        result = expressionEvaluator.evaluateBooleanExpression("COLUMN_NAME IN   ('TEST1',  'TEST2', 'TEST3') AND COLUMN_SIZE IN (1, 2, 3)", context("COLUMN_NAME", "TEST4", "COLUMN_SIZE", 4));
        Assert.assertFalse(result);

        result = expressionEvaluator.evaluateBooleanExpression("COLUMN_SIZE >= 10", context("COLUMN_SIZE", 10));
        Assert.assertTrue(result);

        result = expressionEvaluator.evaluateBooleanExpression("COLUMN_SIZE >= 10", context("COLUMN_SIZE", 11));
        Assert.assertTrue(result);

        result = expressionEvaluator.evaluateBooleanExpression("COLUMN_SIZE >= 10", context("COLUMN_SIZE", 9));
        Assert.assertFalse(result);


        result = expressionEvaluator.evaluateBooleanExpression("COLUMN_NAME LIKE 'TEST%'", context("COLUMN_NAME", "TEST1234"));
        Assert.assertTrue(result);

        result = expressionEvaluator.evaluateBooleanExpression("COLUMN_NAME LIKE 'TEST%'", context("COLUMN_NAME", "SOME_TEST_1234"));
        Assert.assertFalse(result);

        result = expressionEvaluator.evaluateBooleanExpression("COLUMN_NAME LIKE '%TEST%'", context("COLUMN_NAME", "SOME_TEST_1234"));
        Assert.assertTrue(result);

        result = expressionEvaluator.evaluateBooleanExpression("COLUMN_NAME LIKE '%TEST$%'", context("COLUMN_NAME", "SOME_TEST$_1234"));
        Assert.assertTrue(result);

        result = expressionEvaluator.evaluateBooleanExpression("COLUMN_NAME NOT LIKE '%TEST$%'", context("COLUMN_NAME", "SOME_TEST_1234"));
        Assert.assertTrue(result);

    }

    @Test
    public void evaluateExpression() {
        String result = expressionEvaluator.evaluateExpression("COLUMN_NAME", context("COLUMN_NAME", "TEST1"));
        Assert.assertEquals("TEST1", result);

        Integer numericResult = expressionEvaluator.evaluateExpression("COLUMN_SIZE", context("COLUMN_SIZE", 10));
        Assert.assertEquals(Integer.valueOf(10), numericResult);
    }

    @Test
    public void evaluateBooleanExpression_COMPARISON_OPERATORS() {
        assertBooleanExpression("COLUMN_SIZE < 10", true, "COLUMN_SIZE", 9);
        assertBooleanExpression("COLUMN_SIZE < 10", false, "COLUMN_SIZE", 10);
        assertBooleanExpression("COLUMN_SIZE <= 10", true, "COLUMN_SIZE", 10);
        assertBooleanExpression("COLUMN_SIZE > 10", true, "COLUMN_SIZE", 11);
        assertBooleanExpression("COLUMN_SIZE > 10", false, "COLUMN_SIZE", 10);
        assertBooleanExpression("COLUMN_SIZE >= 10", true, "COLUMN_SIZE", 10);
    }

    @Test
    public void evaluateBooleanExpression_NULLS() {
        assertBooleanExpression("COLUMN_NAME IS NULL", true, "COLUMN_NAME", null);
        assertBooleanExpression("COLUMN_NAME IS NULL", false, "COLUMN_NAME", "TEST1");
        assertBooleanExpression("COLUMN_NAME IS NOT NULL", true, "COLUMN_NAME", "TEST1");
        assertBooleanExpression("COLUMN_NAME IS NOT NULL", false, "COLUMN_NAME", null);
    }

    @Test
    public void evaluateBooleanExpression_SIGNED_NUMBERS() {
        assertBooleanExpression("COLUMN_SIZE >= -1", true, "COLUMN_SIZE", 0);
        assertBooleanExpression("COLUMN_SIZE IN (-1, +2)", true, "COLUMN_SIZE", -1);
        assertBooleanExpression("COLUMN_SIZE IN (-1, +2)", true, "COLUMN_SIZE", 2);
        assertBooleanExpression("COLUMN_SIZE IN (-1, +2)", false, "COLUMN_SIZE", 1);
    }

    @Test
    public void evaluateBooleanExpression_NOT() {
        assertBooleanExpression("NOT (COLUMN_NAME = 'TEST1')", false, "COLUMN_NAME", "TEST1");
        assertBooleanExpression("NOT (COLUMN_NAME = 'TEST1')", true, "COLUMN_NAME", "TEST2");
    }

    @Test
    public void evaluateBooleanExpression_FAILS_CLOSED() {
        assertBooleanExpression("System.exit(0)", false, "COLUMN_NAME", "TEST1");
        assertBooleanExpression("UNKNOWN_COLUMN = 'TEST1'", false, "COLUMN_NAME", "TEST1");
        assertBooleanExpression("COLUMN_NAME", false, "COLUMN_NAME", "TEST1");
    }

    @Test
    public void verifyExpression_EXPECTED_BOOLEAN() {
        ExpressionEvaluatorContext context = context("COLUMN_NAME", "TEST1");
        Assert.assertTrue(expressionEvaluator.verifyExpression("COLUMN_NAME = 'TEST1'", context, Boolean.class));
        Assert.assertNull(context.getError());

        context = context("COLUMN_NAME", "TEST1");
        Assert.assertFalse(expressionEvaluator.verifyExpression("COLUMN_NAME", context, Boolean.class));
        Assert.assertNotNull(context.getError());
    }

    @Test
    public void verifyExpression_SANDBOX_REJECTIONS() {
        assertInvalidExpression("System.exit(0)");
        assertInvalidExpression("java.lang.System.exit(0)");
        assertInvalidExpression("Runtime.runtime.exec('id')");
        assertInvalidExpression("new File('/tmp/dbn-sandbox-test').text");
        assertInvalidExpression("this.class.classLoader");
        assertInvalidExpression("COLUMN_NAME.getClass()");
        assertInvalidExpression("COLUMN_NAME.class");
        assertInvalidExpression("while(false){}");
        assertInvalidExpression("for (x in [1, 2]) {}");
        assertInvalidExpression("if (COLUMN_SIZE > 1) true else false");
        assertInvalidExpression("return COLUMN_SIZE > 1");
        assertInvalidExpression("assert COLUMN_SIZE > 1");
        assertInvalidExpression("COLUMN_SIZE += 1");
        assertInvalidExpression("[1] * 3");
        assertInvalidExpression("COLUMN_NAME + COLUMN_NAME");
        assertInvalidExpression("COLUMN_NAME = 'TEST1'; true");
        assertInvalidExpression("false; COLUMN_NAME = 'TEST1'");
        assertInvalidExpression("COLUMN_SIZE > 1; COLUMN_NAME = 'TEST1'");
    }

    @Test
    public void verifyExpression_REGEX_TIMEOUT_FAILS_CLOSED() {
        String value = "a".repeat(50_000) + "X";
        ExpressionEvaluatorContext context = context("COLUMN_NAME", value);

        long start = System.nanoTime();
        boolean valid = expressionEvaluator.verifyExpression("COLUMN_NAME ==~ /(a+)+$/", context, Boolean.class);
        long elapsedSeconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);

        Assert.assertFalse(valid);
        Assert.assertTrue(context.getError() instanceof TimeoutException);
        Assert.assertTrue(elapsedSeconds < 10);
    }

    @Test
    public void fromSql_AND_OR() {
        testSqlToGroovy(
                "(COLUMN_NAME = 'COLUMN_AND_OR_TRAP' or COLUMN_NAME = 'DATE_COLUMN') AND (COLUMN_TYPE = 'VARCHAR2' OR COLUMN_TYPE = 'DATE')",
                "(COLUMN_NAME == 'COLUMN_AND_OR_TRAP' || COLUMN_NAME == 'DATE_COLUMN') && (COLUMN_TYPE == 'VARCHAR2' || COLUMN_TYPE == 'DATE')");
    }

    @Test
    public void fromSql_OPERATORS() {
        testSqlToGroovy(
                "(COLUMN_NAME = 'COLUMN_AND_OR_TRAP' or COLUMN_NAME != 'DATE_COLUMN') AND (COLUMN_SIZE >= 10 OR COLUMN_SIZE != 3 OR COLUMN_SIZE<=0 OR COLUMN_SIZE==34)",
                "(COLUMN_NAME == 'COLUMN_AND_OR_TRAP' || COLUMN_NAME != 'DATE_COLUMN') && (COLUMN_SIZE >= 10 || COLUMN_SIZE != 3 || COLUMN_SIZE<=0 || COLUMN_SIZE==34)");
    }

    @Test
    public void fromSql_NULLS() {
        testSqlToGroovy(
                "COLUMN_NAME IS NULL OR COLUMN_TYPE IS NOT NULL",
                "COLUMN_NAME == null || COLUMN_TYPE != null");
    }

    @Test
    public void fromSql_SIGNED_NUMBERS() {
        testSqlToGroovy(
                "COLUMN_SIZE >= -1 AND COLUMN_SIZE IN (-1, +2)",
                "COLUMN_SIZE >= -1 && COLUMN_SIZE in [-1, +2]");
    }

    @Test
    public void fromSql_NOT() {
        testSqlToGroovy(
                "NOT (COLUMN_NAME = 'TEST1')",
                "! (COLUMN_NAME == 'TEST1')");
    }


    @Test
    public void fromSql_IN1() {
        testSqlToGroovy(
                "COLUMN_NAME IN ('TEST1', 'TEST2', 'TEST3')",
                "COLUMN_NAME in ['TEST1', 'TEST2', 'TEST3']");
    }

    @Test
    public void fromSql_IN2() {
        testSqlToGroovy(
                "COLUMN_SIZE IN (1, 4, 5)",
                "COLUMN_SIZE in [1, 4, 5]");
    }

    @Test
    public void fromSql_IN3() {
        testSqlToGroovy(
                "COLUMN_NAME IN ('TEST1', 'TEST2', 'TEST3') AND COLUMN_SIZE IN (1, 4, 5)",
                "COLUMN_NAME in ['TEST1', 'TEST2', 'TEST3'] && COLUMN_SIZE in [1, 4, 5]");
    }

    @Test
    public void fromSql_NOT_IN() {
        testSqlToGroovy(
                "COLUMN_NAME NOT IN ('TEST1', 'TEST2', 'TEST3')",
                "!(COLUMN_NAME in ['TEST1', 'TEST2', 'TEST3'])");
    }


    @Test
    public void fromSql_LIKE1() {
        testSqlToGroovy(
                "COLUMN_NAME LIKE 'TEST1%'",
                "COLUMN_NAME ==~ /(?i)TEST1.*/");
    }

    @Test
    public void fromSql_LIKE2() {
        testSqlToGroovy(
                "COLUMN_NAME LIKE '%TEST1%'",
                "COLUMN_NAME ==~ /(?i).*TEST1.*/");
    }

    @Test
    public void fromSql_LIKE3() {
        testSqlToGroovy(
                "COLUMN_SIZE >= 4 AND (COLUMN_NAME LIKE '%TEST1%' OR COLUMN_NAME LIKE    'TEST2%') AND COLUMN_TYPE = 'VARCHAR'",
                "COLUMN_SIZE >= 4 && (COLUMN_NAME ==~ /(?i).*TEST1.*/ || COLUMN_NAME ==~ /(?i)TEST2.*/) && COLUMN_TYPE == 'VARCHAR'");
    }

    @Test
    public void fromSql_NOT_LIKE() {
        testSqlToGroovy(
                "COLUMN_NAME NOT LIKE '%TEST1%'",
                "!(COLUMN_NAME ==~ /(?i).*TEST1.*/)");
    }

    @Test
    public void fromSql_LIKE_REGEX_META_CHARACTERS() {
        assertBooleanExpression("COLUMN_NAME LIKE 'TEST.$+[]?'", true, "COLUMN_NAME", "TEST.$+[]?");
        assertBooleanExpression("COLUMN_NAME LIKE 'TEST.$+[]?'", false, "COLUMN_NAME", "TESTX$+[]?");
        assertBooleanExpression("COLUMN_NAME LIKE 'A^B|C(D){E}'", true, "COLUMN_NAME", "A^B|C(D){E}");
        assertBooleanExpression("COLUMN_NAME LIKE 'A^B|C(D){E}'", false, "COLUMN_NAME", "AB|C(D){E}");
        assertBooleanExpression("COLUMN_NAME NOT LIKE 'A^B|C(D){E}'", false, "COLUMN_NAME", "A^B|C(D){E}");
        assertBooleanExpression("COLUMN_NAME NOT LIKE 'A^B|C(D){E}'", true, "COLUMN_NAME", "A-B|C(D){E}");

        testSqlToGroovy(
                "COLUMN_NAME LIKE 'TEST.$+[]?'",
                "COLUMN_NAME ==~ /(?i)TEST\\.[$]\\+\\[\\]\\?/");
        testSqlToGroovy(
                "COLUMN_NAME LIKE 'A^B|C(D){E}'",
                "COLUMN_NAME ==~ /(?i)A\\^B\\|C\\(D\\)\\{E\\}/");
        testSqlToGroovy(
                "COLUMN_NAME NOT LIKE 'A^B|C(D){E}'",
                "!(COLUMN_NAME ==~ /(?i)A\\^B\\|C\\(D\\)\\{E\\}/)");
    }

    @Test
    public void fromSql_LIKE_REGEX_DELIMITERS_AND_WILDCARDS() {
        assertBooleanExpression("COLUMN_NAME LIKE 'PATH/TO%'", true, "COLUMN_NAME", "PATH/TO_FILE");
        assertBooleanExpression("COLUMN_NAME LIKE 'PATH/TO%'", false, "COLUMN_NAME", "PATHXTO_FILE");
        assertBooleanExpression("COLUMN_NAME LIKE 'TEST*END'", true, "COLUMN_NAME", "TEST_MIDDLE_END");
        assertBooleanExpression("COLUMN_NAME LIKE 'TEST*END'", false, "COLUMN_NAME", "TEST_MIDDLE_STOP");
        assertBooleanExpression("COLUMN_NAME LIKE 'FILE_(1).sql'", true, "COLUMN_NAME", "FILE_(1).sql");
        assertBooleanExpression("COLUMN_NAME LIKE 'FILE_(1).sql'", false, "COLUMN_NAME", "FILE_X1Ysql");

        testSqlToGroovy(
                "COLUMN_NAME LIKE 'PATH/TO%'",
                "COLUMN_NAME ==~ /(?i)PATH\\/TO.*/");
        testSqlToGroovy(
                "COLUMN_NAME LIKE 'TEST*END'",
                "COLUMN_NAME ==~ /(?i)TEST.*END/");
        testSqlToGroovy(
                "COLUMN_NAME LIKE 'FILE_(1).sql'",
                "COLUMN_NAME ==~ /(?i)FILE_\\(1\\)\\.sql/");
    }


    @SneakyThrows
    private void testSqlToGroovy(@NonNls String in, @NonNls String out){
        String groovyExpression = SqlToGroovyExpressionConverter.sqlToGroovy(in);

        System.out.println();
        System.out.println(in);
        System.out.println(groovyExpression);
        expressionEvaluator.evaluateBooleanExpression(in, context("COLUMN_NAME", "TEST", "COLUMN_TYPE", "VARCHAR", "COLUMN_SIZE", 3));

        Assert.assertEquals(out, groovyExpression);
    }

    private void assertBooleanExpression(@NonNls String expression, boolean expected, @NonNls Object ... keyValues) {
        boolean result = expressionEvaluator.evaluateBooleanExpression(expression, context(keyValues));
        Assert.assertEquals(expected, result);
    }

    private void assertInvalidExpression(@NonNls String expression) {
        ExpressionEvaluatorContext context = context(
                "COLUMN_NAME", "TEST1",
                "COLUMN_SIZE", 2);

        Assert.assertFalse(expressionEvaluator.verifyExpression(expression, context, Boolean.class));
        Assert.assertNotNull(context.getError());
    }
    
    private ExpressionEvaluatorContext context(@NonNls Object ... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0, l = keyValues.length; i < l / 2; i++) {
            String key = (String) keyValues[i * 2];
            Object value = keyValues[i * 2 +1 ];
            map.put(key, value);
        }
        return new ExpressionEvaluatorContext(map);

    }
}
