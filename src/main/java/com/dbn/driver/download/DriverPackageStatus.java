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

package com.dbn.driver.download;

import com.dbn.common.state.PersistentStateElement;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.longAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setLongAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
public class DriverPackageStatus implements PersistentStateElement {
    private final Map<String, LibraryStatus> libraryStatuses = new ConcurrentHashMap<>();
    private final String packageId;
    private final int packageCount;

    DriverPackageStatus(String packageId, int packageCount){
        this.packageId = packageId;
        this.packageCount = packageCount;
    }

    public LibraryStatus getLibraryStatus(String libraryId){
        return libraryStatuses.computeIfAbsent(libraryId, n-> new LibraryStatus(libraryId));
    }

    public boolean isComplete(){
        return libraryStatuses.size()==packageCount && libraryStatuses.values().stream().allMatch(s->s.downloadStatus.equals(DownloadStatus.DONE));
    }

    public void addLibraryStatus(Element element){

        LibraryStatus libraryStatus = getLibraryStatus(stringAttribute(element, "library-id"));
        libraryStatus.readState(element);
    }

    public Collection<LibraryStatus> getLibraryStatuses(){
        return libraryStatuses.values();
    }

    @Override
    public void readState(Element element) {
        for (Element jarElement : element.getChildren("jar")) {
            this.addLibraryStatus(jarElement);
        }}

    @Override
    public void writeState(Element element) {
        element.setAttribute("id", packageId);

        for (DriverPackageStatus.LibraryStatus jarEntry : this.getLibraryStatuses()) {
            Element jarElement = newElement(element, "jar");
            jarEntry.writeState(jarElement);
        }
    }

    @Getter
    @Setter
    public static class LibraryStatus implements PersistentStateElement{
        private final String libraryId;
        private DownloadStatus downloadStatus = DownloadStatus.NEW;
        private long downloadTimestamp;

        LibraryStatus(String libraryId){
            this.libraryId = libraryId;
        }

        @Override
        public void readState(Element element) {
            this.setDownloadStatus(enumAttribute(element, "download-status", DownloadStatus.class));
            this.setDownloadTimestamp(longAttribute(element, "download-timestamp", 0));
        }

        @Override
        public void writeState(Element element) {
            setStringAttribute(element, "library-id", this.libraryId);
            setEnumAttribute(element, "download-status", this.downloadStatus);
            setLongAttribute(element, "download-timestamp", this.downloadTimestamp);
        }
    }
}