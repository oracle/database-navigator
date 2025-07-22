package com.dbn.common.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link Commons}
 *
 * TODO C.B: Could add a bit more coverage here.
 */
public class CommonsTest {
    @Test
    public void testBoxArray() {
        char[] charArray = {'a', 'b', 'c'};
        Object[] objects = Commons.boxArray(charArray);
        Assert.assertEquals(charArray.length, objects.length);
        for (Object object : objects) {
            Assert.assertTrue(object instanceof Character);
        }
    }

    @Test
    public void testMatch() {
        char[] charArray = {'a', 'b', 'c'};
        char[] charArray2 = {'a', 'b', 'c'};
        Assert.assertTrue(Commons.matchArrays(charArray, charArray2));
        char[] charArray3 = {'e', 'f', 'g'};
        Assert.assertFalse(Commons.matchArrays(charArray, charArray3));
        Character[] boxedCharArray = {'a', 'b','c'};
        Assert.assertTrue(Commons.matchArrays(charArray, boxedCharArray));
        Character[] boxedCharArray2 = {'e', 'f', 'g'};
        Assert.assertFalse(Commons.matchArrays(charArray, boxedCharArray2));
    }

}
