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

package com.dbn.editor.session;

import com.dbn.common.filter.Filter;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Strings;
import com.dbn.editor.session.model.SessionBrowserModelRow;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@EqualsAndHashCode
public class SessionBrowserFilter implements Filter<SessionBrowserModelRow>, Cloneable<SessionBrowserFilter> {
    private String user;
    private String host;
    private String status;

    public SessionBrowserFilter() {
    }

    private SessionBrowserFilter(String user, String host, String status) {
        this.user = user;
        this.host = host;
        this.status = status;
    }

    @Override
    public boolean accepts(SessionBrowserModelRow row) {
        if (Strings.isNotEmpty(user) && !Objects.equals(user, row.getUser())) return false;
        if (Strings.isNotEmpty(host) && !Objects.equals(host, row.getHost())) return false;
        if (Strings.isNotEmpty(status) && !Objects.equals(status, row.getStatus())) return false;
        return true;
    }

    public void setFilterValue(SessionBrowserFilterType filterType, String value) {
        switch (filterType) {
            case USER: user = value; break;
            case HOST: host = value; break;
            case STATUS: status = value; break;
        }
    }

    public boolean isEmpty() {
        return Strings.isEmpty(getFilterValue(SessionBrowserFilterType.USER)) &&
                Strings.isEmpty(getFilterValue(SessionBrowserFilterType.HOST)) &&
                Strings.isEmpty(getFilterValue(SessionBrowserFilterType.STATUS));
    }

    public String getFilterValue(SessionBrowserFilterType filterType) {
        switch (filterType) {
            case USER: return user;
            case HOST: return host;
            case STATUS: return status;
            default: return null;
        }
    }

    public void clear() {
        user = null;
        host = null;
        status = null;
    }



    @Override
    public SessionBrowserFilter clone(){
        return new SessionBrowserFilter(user, host, status);
    }
}
