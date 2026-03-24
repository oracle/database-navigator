package com.dbn.connection.config.configprovider;

import java.util.List;

public class ConfigProviderFormatRegistry {
    private static final ConfigProviderFormatRegistry INSTANCE = new ConfigProviderFormatRegistry();

    private final List<ConfigProviderFormatProcessor> processors =
            List.of(new JsonConfigProviderProcessor());

    private ConfigProviderFormatRegistry() {}

    public static ConfigProviderFormatRegistry getInstance() {
        return INSTANCE;
    }

    public List<ConfigProviderFormatProcessor> getAll() {
        return processors;
    }

    public ConfigProviderFormatProcessor getDefault() {
        return processors.get(0);
    }

    public ConfigProviderFormatProcessor get(String id) {
        if (id != null) {
            for (ConfigProviderFormatProcessor p : processors) {
                if (id.equalsIgnoreCase(p.getId())) return p;
            }
        }
        return getDefault();
    }
}
