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

package com.dbn.common.util;

import com.intellij.openapi.util.JDOMUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NonNls;

import java.io.InputStream;

@Slf4j
@UtilityClass
public final class XmlContents {

    public static Element fileToElement(Class clazz, @NonNls String fileName) throws Exception {
        try (InputStream inputStream = clazz.getResourceAsStream(fileName)){
            return streamToElement(inputStream);
        }
    }

    public static Element streamToElement(InputStream inputStream) throws Exception{
        return JDOMUtil.load(inputStream);
    }

    public static Document fileToDocument(Class clazz, @NonNls String fileName) throws Exception {
        try (InputStream inputStream = clazz.getResourceAsStream(fileName)){
            return streamToDocument(inputStream);
        }
    }

    public static Document streamToDocument(InputStream inputStream) throws Exception{
        SAXBuilder builder = new SAXBuilder();
        return builder.build(inputStream);
    }

}
