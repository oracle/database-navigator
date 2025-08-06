package com.dbn.test.util;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(SOURCE)
@Target({ TYPE, METHOD })
public @interface RegressionTest {
    public enum BugSystem {
        JIRA, BUGDB, GITHUB, BITBUCKET;
    }
    BugSystem source();
    String component();
    int number();
}
