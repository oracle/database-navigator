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

package com.dbn.common.options.setting;

import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.connection.SessionId;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jdom.CDATA;
import org.jdom.Content;
import org.jdom.Element;
import org.jdom.Text;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Strings.containsOneOf;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@NonNls
@UtilityClass
public final class Settings {

    public static char[] getChars(Element parent, @NonNls String childName, char[] defaultValue) {
        Element element = parent.getChild(childName);
        String stringValue = getStringValue(element);
        return stringValue == null ? defaultValue : stringValue.toCharArray();
    }

    public static String getString(Element parent, @NonNls String childName, @NonNls String defaultValue) {
        Element element = parent.getChild(childName);
        String stringValue = getStringValue(element);
        return stringValue == null ? defaultValue : stringValue;
    }

    public static int getInteger(Element parent, @NonNls String childName, int defaultValue) {
        try {
            Element element = parent.getChild(childName);
            String stringValue = getStringValue(element);
            return stringValue == null ? defaultValue : Integer.parseInt(stringValue);
        } catch (Exception e) {
            conditionallyLog(e);
            log.warn("Failed to read INTEGER config (" + childName + "): " + e.getMessage());
            return defaultValue;
        }
    }

    public static double getDouble(Element parent, @NonNls String childName, double defaultValue) {
        try {
            Element element = parent.getChild(childName);
            String stringValue = getStringValue(element);
            return stringValue == null ? defaultValue : Double.parseDouble(stringValue);
        } catch (Exception e){
            conditionallyLog(e);
            log.warn("Failed to read DOUBLE config ({}): {}", childName, e.getMessage());
            return defaultValue;
        }
    }

    public static boolean getBoolean(Element parent, @NonNls String childName, boolean defaultValue) {
        Element element = parent.getChild(childName);
        String stringValue = getStringValue(element);
        return stringValue == null ? defaultValue : Boolean.parseBoolean(stringValue);
    }

    public static <T extends Enum<T>> T getEnum(Element parent, @NonNls String childName, @NotNull T defaultValue) {
        try {
            Element element = parent.getChild(childName);
            String stringValue = getStringValue(element);
            return stringValue == null ? defaultValue : (T) T.valueOf(defaultValue.getClass(), stringValue);
        } catch (IllegalArgumentException e) {
            conditionallyLog(e);
            log.warn("Failed to read ENUM config ({}): {}", childName, e.getMessage());
            return defaultValue;
        }
    }

    public static <T extends Enum<T>> T getEnum(Element parent, @NonNls String childName, @NotNull Class<T> enumType) {
        try {
            Element element = parent.getChild(childName);
            String stringValue = getStringValue(element);
            return stringValue == null ? null : T.valueOf(enumType, stringValue);
        } catch (IllegalArgumentException e) {
            conditionallyLog(e);
            log.warn("Failed to read ENUM config ({}): {}", childName, e.getMessage());
            return null;
        }
    }

    public static String getStringValue(Element element) {
        if (element == null) return null;

        String value = stringAttribute(element, "value");
        if (Strings.isNotEmptyOrSpaces(value)) {
            return value;
        }
        return null;
    }

    @NonNls
    public static String stringAttribute(Element element, @NonNls String name) {
        String attributeValue = element == null ? null : element.getAttributeValue(name);
        return Strings.isEmptyOrSpaces(attributeValue) ? attributeValue : attributeValue.intern();
    }

    public static char[] charsAttribute(Element element, @NonNls String name) {
        String attributeValue = element == null ? null : element.getAttributeValue(name);
        attributeValue = Strings.isEmptyOrSpaces(attributeValue) ? attributeValue : attributeValue.intern();
        return attributeValue == null ? null : attributeValue.toCharArray();
    }

    public static boolean booleanAttribute(Element element, @NonNls String attributeName, boolean defaultValue) {
        String attributeValue = stringAttribute(element, attributeName);
        return Strings.isEmptyOrSpaces(attributeValue) ? defaultValue : Boolean.parseBoolean(attributeValue);
    }

    public static short shortAttribute(Element element, @NonNls String attributeName, short defaultValue) {
        try {
            String attributeValue = stringAttribute(element, attributeName);
            if (Strings.isEmpty(attributeValue)) {
                return defaultValue;
            }
            return Short.parseShort(attributeValue);
        } catch (Exception e) {
            conditionallyLog(e);
            log.warn("Failed to read SHORT config ({}): {}", attributeName, e.getMessage());
            return defaultValue;
        }
    }

    public static int integerAttribute(Element element, @NonNls String attributeName, int defaultValue) {
        try {
            String attributeValue = stringAttribute(element, attributeName);
            if (Strings.isEmpty(attributeValue)) {
                return defaultValue;
            }
            return Integer.parseInt(attributeValue);
        } catch (NumberFormatException e) {
            conditionallyLog(e);
            log.warn("Failed to read INTEGER config ({}): {}", attributeName, e.getMessage());
            return defaultValue;
        }
    }

    public static long longAttribute(Element element, @NonNls String attributeName, long defaultValue) {
        try {
            String attributeValue = stringAttribute(element, attributeName);
            if (Strings.isEmpty(attributeValue)) {
                return defaultValue;
            }
            return Long.parseLong(attributeValue);
        } catch (NumberFormatException e) {
            conditionallyLog(e);
            log.warn("Failed to read LONG config ({}): {}", attributeName, e.getMessage());
            return defaultValue;
        }
    }

    /*
        public static <T extends Enum<T>> T getEnumAttribute(Element element, String attributeName, T value) {
            String attributeValue = element.getAttributeValue(attributeName);
            Class<T> enumClass = (Class<T>) value.getClass();
            return StringUtil.isEmpty(attributeValue) ? value : T.valueOf(enumClass, attributeValue);
        }
    */

    public static <T extends Enum<T>> T enumAttribute(Element element, @NonNls String attributeName, Class<T> enumClass) {
        try {
            String attributeValue = stringAttribute(element, attributeName);
            return Strings.isEmpty(attributeValue) ? null : T.valueOf(enumClass, attributeValue);
        } catch (Exception e) {
            conditionallyLog(e);
            log.warn("Failed to read ENUM attribute ({}): {}", attributeName, e.getMessage());
            return null;
        }
    }

    public static <T extends Enum<T>> T enumAttribute(Element element, @NonNls String attributeName, @NotNull T defaultValue) {
        try {
            String attributeValue = stringAttribute(element, attributeName);
            return Strings.isEmpty(attributeValue) ? defaultValue : T.valueOf((Class<T>) defaultValue.getClass(), attributeValue);
        } catch (Exception e) {
            conditionallyLog(e);
            log.warn("Failed to read ENUM attribute ({}): {}", attributeName, e.getMessage());
            return defaultValue;
        }
    }

    public static ConnectionId connectionIdAttribute(Element element, @NonNls String name) {
        return ConnectionId.get(stringAttribute(element, name));
    }

    public static SessionId sessionIdAttribute(Element element, @NonNls String name, SessionId defaultSessionId) {
        SessionId sessionId = SessionId.get(stringAttribute(element, name));
        return Commons.nvl(sessionId, defaultSessionId);
    }

    public static SchemaId schemaIdAttribute(Element element, @NonNls String name) {
        return SchemaId.get(stringAttribute(element, name));
    }

    public static String readCdata(Element element) {
        StringBuilder builder = new StringBuilder();
        int contentSize = element.getContentSize();
        for (int i=0; i<contentSize; i++) {
            Content content = element.getContent(i);
            if (content instanceof Text) {
                Text text = (Text) content;
                builder.append(text.getText());
            }
        }
        return builder.toString();
    }

    public static void writeCdata(Element element, @NonNls String content) {
        element.setContent(new CDATA(content));
    }

    public static void writeCdata(Element element, @NonNls String content, boolean conditional) {
        if (needsCdataWrapping(content) || !conditional) {
            element.setContent(new CDATA(content));
        } else {
            element.setText(content);
        }
    }

    public static void setInteger(Element parent, @NonNls String childName, int value) {
        Element element = newElement(parent, childName);
        element.setAttribute("value", Integer.toString(value));
    }

    public static void setChars(Element parent, @NonNls String childName, char[] value) {
        Element element = newElement(parent, childName);
        element.setAttribute("value", value == null ? "" : new String(value));
    }

    public static void setString(Element parent, @NonNls String childName, @NonNls String value) {
        Element element = newElement(parent, childName);
        element.setAttribute("value", value == null ? "" : value);
    }

    public static void setDouble(Element parent, @NonNls String childName, double value) {
        Element element = newElement(parent, childName);
        element.setAttribute("value", Double.toString(value));
    }

    public static void setBoolean(Element parent, @NonNls String childName, boolean value) {
        Element element = newElement(parent, childName);
        element.setAttribute("value", Boolean.toString(value));
    }

    public static  <T extends Enum<T>> void setEnum(Element parent, @NonNls String childName, T value) {
        Element element = newElement(parent, childName);
        element.setAttribute("value",value == null ? "" : value.name());
    }

    public static void setBooleanAttribute(Element element, @NonNls String attributeName, boolean value) {
        element.setAttribute(attributeName, Boolean.toString(value));
    }

    public static void setIntegerAttribute(Element element, @NonNls String attributeName, int value) {
        element.setAttribute(attributeName, Integer.toString(value));
    }

    public static void setLongAttribute(Element element, @NonNls String attributeName, long value) {
        element.setAttribute(attributeName, Long.toString(value));
    }

    public static void setStringAttribute(Element element, @NonNls String attributeName, String value) {
        element.setAttribute(attributeName, value == null ? "" : value);
    }

    public static void setCharsAttribute(Element element, @NonNls String attributeName, char[] value) {
        element.setAttribute(attributeName, value == null ? "" : new String(value));
    }

    public static  <T extends Enum<T>> void setEnumAttribute(Element element, String attributeName, T value) {
        element.setAttribute(attributeName, value.name());
    }

    public static Element newStateElement() {
        return newElement(null, "state");
    }

    public static Element newElement(@NonNls String name) {
        return newElement(null, name);
    }

    public static Element newElement(@Nullable Element parent, @NonNls String name) {
        Element child = new Element(name);
        if (parent != null) parent.addContent(child);
        return child;
    }

    /**
     * Determines if the given string value needs to be wrapped in CDATA to ensure
     * proper handling within XML.
     *
     * @param value the string value to check for special characters that require CDATA wrapping
     * @return true if the string contains characters that need CDATA wrapping;
     *         false otherwise
     */
    public static boolean needsCdataWrapping(String value) {
        return
            isNotEmpty(value) &&
            containsOneOf(value, "<", ">", "&", "\"", "'", "\n", "\r");
    }

}
