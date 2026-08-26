/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.dbn.common.exception;

/** Signals that execution must be resumed after an external approval step. */
public class ExecutionPausedException extends RuntimeException {
    public ExecutionPausedException() {
    }
}
