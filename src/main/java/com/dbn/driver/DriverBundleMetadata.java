package com.dbn.driver;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Checksum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;


@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class DriverBundleMetadata implements PersistentStateElement {
    private File library;
    private String checksum;
    private Set<String> driverClassNames = new HashSet<>();

    public DriverBundleMetadata(File library) {
        this.library = library;
        this.checksum = Checksum.soft(library);
    }

    public boolean matchesSignature(DriverBundleMetadata metadata) {
        return Objects.equals(this.checksum, metadata.checksum);
    }

    public boolean isValid() {
        return library.exists();
    }

    public boolean isEmpty() {
        return driverClassNames.isEmpty();
    }

    public boolean isDriverClass(String className) {
        return driverClassNames.contains(className);
    }

    @Override
    public void readState(Element element) {
        this.library = new File(stringAttribute(element, "path"));
        this.checksum = stringAttribute(element, "checksum");
        String[] classNames = stringAttribute(element, "driver-classes").split(",");
        this.driverClassNames.addAll(Arrays.asList(classNames));
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "checksum", checksum);
        setStringAttribute(element, "path", library.getAbsolutePath());
        setStringAttribute(element, "driver-classes", String.join(",", driverClassNames));

    }
}
