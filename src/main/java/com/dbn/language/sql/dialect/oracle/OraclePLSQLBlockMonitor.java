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

package com.dbn.language.sql.dialect.oracle;

import com.dbn.language.common.lexer.DBLanguageCompoundLexer;
import lombok.extern.slf4j.Slf4j;

import java.util.Deque;
import java.util.LinkedList;

@Slf4j
public final class OraclePLSQLBlockMonitor {
    public enum Marker {
        CASE,
        BEGIN,
        CREATE,
        DECLARE,
        PROGRAM}

    private final DBLanguageCompoundLexer lexer;
    private final Deque<Marker> stack = new LinkedList<>();
    private final int initialState;
    private final int psqlBlockState;
    private boolean debugMode = false;

    private int blockStart;

    public OraclePLSQLBlockMonitor(DBLanguageCompoundLexer lexer, int initialState, int psqlBlockState) {
        this.lexer = lexer;
        this.initialState = initialState;
        this.psqlBlockState = psqlBlockState;
    }

    public void ignore() {
        log("ignore:    ");
    }

    public void start(Marker marker) {
        log("start:     ");
        if (!stack.isEmpty()) {
            stack.clear();
        }
        stack.push(marker);

        // PLSQL block start
        lexer.yybegin(psqlBlockState);
        blockStart = lexer.getTokenStart();
    }

    public void mark(Marker marker) {
        log("mark:      ");
        stack.push(marker);
    }

    public boolean end(boolean force) {
        if (force) {
            log("end force: ");
            stack.clear();
        } else {
            log("end:       ");
            Marker marker = stack.poll();
            if (marker == Marker.BEGIN) {
                if (!stack.isEmpty()) {
                    Marker previousMarker = stack.peek();
                    if (previousMarker == Marker.DECLARE || previousMarker == Marker.CREATE) {
                        stack.poll();
                    }
                }
            } else {
                while (marker == Marker.PROGRAM) {
                    marker = stack.poll();
                }
            }
        }

        if (stack.isEmpty()) {
            // PLSQL block end
            lexer.yybegin(initialState);
            lexer.setTokenStart(blockStart);
            return true;
        }

        return false;
    }

    public void pushBack() {
        lexer.yypushback(lexer.yylength());
    }

    public boolean isBlockStarted() {
        return blockStart < lexer.getCurrentPosition();
    }

    public void reset() {
        stack.clear();
    }

    private void log(String step) {
        if (debugMode) log.info("[BLOCK-MONITOR] {}{}", step, lexer.getCurrentToken());
    }

}
