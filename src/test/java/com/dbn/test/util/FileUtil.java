package com.dbn.test.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import static org.jsoup.helper.Validate.fail;
import static org.junit.Assert.assertTrue;

public class FileUtil {
    public static File getFileFromClasspath(Class<?> contextClass, String resourcePath) {
        return getFileFromClasspath(contextClass, resourcePath, true);
    }
    public static File getFileFromClasspath(Class<?> contextClass, String resourcePath, boolean checkExists) {
        return getFileFromClasspath(contextClass.getClassLoader(), resourcePath, checkExists);
    }
    /**
     *
     * @param classLoader -  the classloader to use to locate the resource
     * @param resourcePath a resource path that can be passed to cl.getResource() to find the resources.
     * @param checkExists if true, assert that the requested file exists
     * @return the file for the path or null if not and checkExists == false.
     * @throws AssertionError if the resource cannot be found.
     */
    public static File getFileFromClasspath(ClassLoader classLoader, String resourcePath, boolean checkExists) {
        URL resource = classLoader.getResource(resourcePath);
        File file = null;
        if (checkExists) {
            if (resource == null) {
                fail("Resource not found: " + resourcePath);
            }
        }
        if (resource != null) {
            file =  new File(resource.getPath());
            if (checkExists) {
                assertTrue(file.exists());
            }
        }
        return file;
    }

    public static Properties loadFromFile(File propFile) throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(propFile));
        return properties;
    }
}
