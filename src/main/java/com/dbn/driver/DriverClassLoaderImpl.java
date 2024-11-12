package com.dbn.driver;

import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.util.Measured;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@Getter
class DriverClassLoaderImpl extends URLClassLoader implements DriverClassLoader {
    private final DriverBundleMetadata metadata;
    private final List<File> jars = new ArrayList<>();
    private final List<Class<Driver>> drivers = new ArrayList<>();
    private final Set<String> classNames = new HashSet<>();
    private final Map<String, Class> loadedClasses = new HashMap<>();

    public DriverClassLoaderImpl(DriverBundleMetadata metadata) {
        super(getUrls(metadata.getLibrary()), DriverClassLoader.class.getClassLoader());
        this.metadata = metadata;
        load();
    }


    @SneakyThrows
    private void load() {
        ProgressMonitor.setProgressText("Loading jdbc drivers from " + getLibrary());
        List<DriverLibrary> libraries = new ArrayList<>();
        for (URL url : getURLs()) {
            URI uri = url.toURI();
            File jarFile = new File(uri);
            DriverLibrary driverLibrary = new DriverLibrary(jarFile);
            classNames.addAll(driverLibrary.getClassNames());
            libraries.add(driverLibrary);
        }

        for (DriverLibrary library : libraries) {
            Measured.run("loading library " + library.getJar(), () -> load(library));
        }

        if (!metadata.isEmpty()) {
            DatabaseDriverManager driverManager = DatabaseDriverManager.getInstance();
            driverManager.setDriverMetadata(getLibrary(), metadata);
        }

    }

    private void load(DriverLibrary library) {
        File jar = library.getJar();
        jars.add(jar);

        try {
// TODO: I'm confused by this.  Can't we just do a lookup for the classnames
     //  that DriverBundleMetadata  knows about rather than going class-by-class?
            DriverBundleMetadata previousMetadata = getPreviousMetadata();

            for (String className : library.getClassNames()) {
                if (previousMetadata != null && !previousMetadata.isDriverClass(className)) continue;

                try {
                    Class<?> clazz = loadClass(className);
                    if (Driver.class.isAssignableFrom(clazz)) {
                        Class<Driver> driver = cast(clazz);
                        drivers.add(driver);
                        metadata.getDriverClassNames().add(driver.getName());
                    }
                } catch (Throwable e) {
                    conditionallyLog(e);
                    log.debug("Failed to load driver " + className + " from library " + jar, e);
                }
            }
        } catch (Throwable e) {
            conditionallyLog(e);
            log.debug("Failed to load drivers from library {}", jar, e);
        }
    }

    @Nullable
    private DriverBundleMetadata getPreviousMetadata() {
        DatabaseDriverManager driverManager = DatabaseDriverManager.getInstance();
        DriverBundleMetadata previousMetadata = driverManager.getDriverMetadata(getLibrary());
        if (previousMetadata == null) return null;
        if (previousMetadata.isEmpty()) return null;
        if (!previousMetadata.matchesSignature(metadata)) return null;

        return previousMetadata;
    }

    @Override
    public File getLibrary() {
        return this.metadata.getLibrary();
    }


    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException{
        // TODO check if class level synchronization is needed - the access of the entire class-loader is synchronized while loading
        //synchronized (getClassLoadingLock(name)) {

        Class<?> clazz = loadedClasses.get(name);
        if (clazz != null) return clazz;
        if (classNames.contains(name)) {
            try {
                clazz = findClass(name);
                if (clazz != null && resolve) resolveClass(clazz);
            } catch (Throwable e) {
                conditionallyLog(e);
            }
        }
        if (clazz == null) return super.loadClass(name, resolve);

        loadedClasses.put(clazz.getName().intern(), clazz);
        return clazz;


        //}
    }


    @SneakyThrows
    private static URL[] getUrls(File library) {
        if (library.isDirectory()) {
            File[] files = library.listFiles();
            if (files == null || files.length == 0) throw new IOException("No files found at location");
            return Arrays.
                    stream(files).
                    filter(file -> file.getName().endsWith(".jar")).
                    map(file -> getFileUrl(file)).
                    toArray(URL[]::new);
        } else {
            return new URL[]{getFileUrl(library)};
        }
    }

    @SneakyThrows
    private static URL getFileUrl(File file) {
        return file.toURI().toURL();
    }

}
