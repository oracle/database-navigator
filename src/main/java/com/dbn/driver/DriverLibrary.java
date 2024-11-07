package com.dbn.driver;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

@Getter
public class DriverLibrary {
    private final File jar;
    private final Set<String> classNames = new LinkedHashSet<>();

    @SneakyThrows
    public DriverLibrary(File jar) {
        this.jar = jar;
        try (JarFile jarFile = new JarFile(jar)) {
            List<String> classNames = Collections.list(jarFile.entries())
                    .stream()
                    .map(e -> e.getName())
                    .filter(n -> n.endsWith(".class"))
                    .map(n -> n.replaceAll("/", "."))
                    .map(n -> n.substring(0, n.length() - 6))
                    .map(n -> n.intern())
                    .collect(Collectors.toList());

            this.classNames.addAll(classNames);
        }
    }
}
