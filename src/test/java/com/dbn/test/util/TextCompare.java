package com.dbn.test.util;

/**
 * TODO C.B.: we should replace this with something better either apache commons text or Files.mismatch when we move
 * to Java 12 or beyond
 */
public class TextCompare {
    public static class Diff {
        private final String expected;
        private final String actual;
        // the character offset into expected;
        private final int startOfMismatch;
        private boolean isDiff = true;  // default

        public Diff() {
            this(null, null, -2);
            this.isDiff = false;
        }

        public Diff(String expected, String actual, int startOfMismatch) {
            this.expected = expected;
            this.actual = actual;
            this.startOfMismatch = startOfMismatch;
        }

        public String getExpected() {
            return expected;
        }

        public String getActual() {
            return actual;
        }

        public int getStartOfMismatch() {
            return startOfMismatch;
        }

        public boolean isDiff() {
            return isDiff;
        }
    }

    public final static  Diff NO_DIFF = new Diff();
    public static Diff diff(String expected, String actual) {
        if (expected == null || actual == null) {
            throw new AssertionError("Arguments can't be null");
        }
        if (expected.isEmpty()) {
            if (actual.isEmpty()) {
                return NO_DIFF;
            }
            else {
                // expected is empty and actual isn't so -1 is the first offset
                return new Diff(expected, actual, 0);
            }
        }
        int charOffsetExpected;
        for (charOffsetExpected = 0; charOffsetExpected < expected.length(); charOffsetExpected++) {
            char expectedChar = expected.charAt(charOffsetExpected);
            if (charOffsetExpected >= actual.length()) {
                return new Diff(expected, actual, charOffsetExpected);
            }
            else if (expectedChar != actual.charAt(charOffsetExpected)) {
                return new Diff(expected, actual, charOffsetExpected);
            }
        }
        if (charOffsetExpected != actual.length()) {
            return new Diff(expected, actual, charOffsetExpected);
        }
        return NO_DIFF;
    }

}
