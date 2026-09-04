package com.dbn.connection.config.export;

import java.nio.file.Path;

public abstract class ConfigProviderFormatProcessor {

    public abstract String getId();
    public abstract String getDisplayName();
    public abstract String getDefaultExtension();

    public abstract String render(ConfigProviderPayload payload, String wrapperKey) throws Exception;

    public abstract void write (ConfigProviderPayload payload, Path file, String wrapperKey) throws Exception;

    @Override
    public String toString(){
        return  getDisplayName();
    }
}
