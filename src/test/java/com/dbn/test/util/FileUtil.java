package com.dbn.test.util;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FileUtil {
    public static File getFileFromClasspath(Class<?> contextClass, String resourcePath) {
        return getFileFromClasspath(contextClass.getClassLoader(), resourcePath);
    }
    /**
     *
     * @param classLoader -  the classloader to use to locate the resource
     * @param resourcePath a resource path that can be passed to cl.getResource() to find the resources.
     * @return the file for the path.
     * @throws AssertionError if the resource cannot be found.
     */
    public static File getFileFromClasspath(ClassLoader classLoader, String resourcePath) {
        URL resource = classLoader.getResource(resourcePath);
        assertNotNull(resource);
        File file =  new File(resource.getPath());
        assertTrue(file.exists());
        return file;
    }
}
