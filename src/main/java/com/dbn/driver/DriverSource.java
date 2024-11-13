package com.dbn.driver;

import com.dbn.common.ui.Presentable;
import com.dbn.nls.NlsResources;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.dbn.nls.NlsResources.txt;

@Getter
@AllArgsConstructor
public enum DriverSource implements Presentable{
    @Deprecated // replaced by BUNDLED
    BUILTIN(txt("cfg.connection.const.DriverSource_BUILTIN")),

    BUNDLED(txt("cfg.connection.const.DriverSource_BUNDLED")),
    EXTERNAL(txt("cfg.connection.const.DriverSource_EXTERNAL")),
    DOWNLOAD(txt("cfg.connection.const.DriverSource_DOWNLOAD"));

    private final String name;
}
