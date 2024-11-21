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

package com.dbn.data.find;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.exception.OutdatedContentException;
import com.dbn.common.ui.util.Listeners;
import com.dbn.common.util.Lists;
import com.dbn.data.model.DataModelCell;
import com.intellij.openapi.Disposable;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Getter
@Setter
public class DataSearchResult implements Disposable {
    private final Listeners<DataSearchResultListener> listeners = Listeners.create(this);
    private List<DataSearchResultMatch> matches = Collections.emptyList();
    private DataSearchResultMatch selectedMatch;
    private int matchesLimit;
    private long updateTimestamp = 0;
    private boolean updating;

    public void clear() {
        selectedMatch = null;
        matches = Collections.emptyList();
    }

    public int size() {
        return matches.size();
    }

    public boolean isEmpty() {
        return matches.isEmpty();
    }

    public void addListener(DataSearchResultListener listener) {
        listeners.add(listener);
    }

    public void notifyListeners() {
        listeners.notify(l -> l.searchResultUpdated(this));
    }

    public void checkTimestamp(Long updateTimestamp) {
        if (this.updateTimestamp != updateTimestamp) {
            throw new OutdatedContentException(this);
        }
    }

    public Iterator<DataSearchResultMatch> getMatches(DataModelCell cell) {
        DataSearchResultMatch first = matches.isEmpty() ? null : findMatch(null, cell);
        if (first == null) return null;

        return new Iterator<>() {
            private DataSearchResultMatch next = first;

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public DataSearchResultMatch next() {
                DataSearchResultMatch current = next;
                next = findMatch(next, cell);
                return current;
            }

            @Override
            public void remove() {}
        };
    }

    private DataSearchResultMatch findMatch(DataSearchResultMatch previous, DataModelCell cell) {
        int index = previous == null ? 0 : matches.indexOf(previous) + 1;
        for (int i = index; i< matches.size(); i++) {
            DataSearchResultMatch match = matches.get(i);
            if (match != null && match.getCell() == cell) {
                return match;

            } else if (previous != null) {
                return null;
            }
        }
        return null;
    }

    public DataSearchResultMatch selectFirst(int fromRowIndex, int fromColumnIndex, DataSearchResultScrollPolicy scrollPolicy) {
        if (updating) return null;
        return next(fromRowIndex, fromColumnIndex, scrollPolicy);
    }
    
    public DataSearchResultMatch selectNext(DataSearchResultScrollPolicy scrollPolicy) {
        if (updating) return null;
        int fromRowIndex = 0;
        int fromColumnIndex = 0;
        
        if (selectedMatch != null) {
            fromRowIndex = selectedMatch.getRowIndex();
            fromColumnIndex = selectedMatch.getColumnIndex();
            switch (scrollPolicy) {
                case VERTICAL: fromRowIndex++; break;
                case HORIZONTAL: fromColumnIndex++; break;
            }
        }
        selectedMatch = next(fromRowIndex, fromColumnIndex, scrollPolicy);
        return selectedMatch;
    }

    public DataSearchResultMatch selectPrevious(DataSearchResultScrollPolicy scrollPolicy) {
        if (updating) return null;
        int fromRowIndex = 999999;
        int fromColumnIndex = 999999;

        if (selectedMatch != null) {
            fromRowIndex = selectedMatch.getRowIndex();
            fromColumnIndex = selectedMatch.getColumnIndex();
            switch (scrollPolicy) {
                case VERTICAL: fromRowIndex--; break;
                case HORIZONTAL: fromColumnIndex--; break;
            }
        }
        selectedMatch = previous(fromRowIndex, fromColumnIndex, scrollPolicy);
        return selectedMatch;
    }

    private DataSearchResultMatch next(int fromRow, int fromCol, DataSearchResultScrollPolicy scrollPolicy) {
        if (matches.isEmpty()) return null;

        for (DataSearchResultMatch match : matches) {
            int row = match.getRowIndex();
            int col = match.getColumnIndex();

            if (scrollPolicy == DataSearchResultScrollPolicy.HORIZONTAL) {
                if (row > fromRow || (row == fromRow && col >= fromCol)) return match;
            } else if (scrollPolicy == DataSearchResultScrollPolicy.VERTICAL) {
                if (col > fromCol || (col == fromCol && row >= fromRow)) return match;
            }
        }
        //reached end of the matches without resolving selection scroll to the beginning
        return Lists.firstElement(matches);
    }
    
    private DataSearchResultMatch previous(int fromRow, int fromCol, DataSearchResultScrollPolicy scrollPolicy) {
        if (matches.isEmpty()) return null;

        for (DataSearchResultMatch match : Lists.reversed(matches)) {
            int row = match.getRowIndex();
            int col = match.getColumnIndex();

            if (scrollPolicy == DataSearchResultScrollPolicy.HORIZONTAL) {
                if (row < fromRow || (row == fromRow && col <= fromCol)) return match;
            } else if (scrollPolicy == DataSearchResultScrollPolicy.VERTICAL) {
                if (col < fromCol || (col == fromCol && row <= fromRow)) return match;
            }
        }
        //reached beginning of the matches actions without resolving selection scroll to the end
        return Lists.lastElement(matches);
    }


    @Override
    public void dispose() {
        matches = Disposer.replace(matches, Collections.emptyList());
        selectedMatch = null;
    }

    public void startUpdating(long updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
        this.updating = true;
        clear();
    }

    public void stopUpdating() {
        this.updating = false;
    }
}
