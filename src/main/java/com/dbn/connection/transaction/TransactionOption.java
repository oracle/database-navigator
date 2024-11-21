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

package com.dbn.connection.transaction;

import com.dbn.common.icon.Icons;
import com.dbn.common.option.InteractiveOption;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.swing.Icon;

import static com.dbn.nls.NlsResources.txt;

@Getter
@AllArgsConstructor
public enum TransactionOption implements InteractiveOption{
    ASK(txt("cfg.connection.const.TransactionOption_ASK"), null),
    COMMIT(txt("cfg.connection.const.TransactionOption_COMMIT"), Icons.CONNECTION_COMMIT),
    ROLLBACK(txt("cfg.connection.const.TransactionOption_ROLLBACK"), Icons.CONNECTION_ROLLBACK),
    REVIEW_CHANGES(txt("cfg.connection.const.TransactionOption_REVIEW_CHANGES"), null),
    CANCEL(txt("cfg.connection.const.TransactionOption_CANCEL"), null);

    private final String name;
    private final Icon icon;

    @Override
    public boolean isCancel() {
        return this == CANCEL;
    }

    @Override
    public boolean isAsk() {
        return this == ASK;
    }
}
