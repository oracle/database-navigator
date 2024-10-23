package com.dbn.connection;

import com.dbn.common.constant.Constant;
import com.dbn.common.ui.Presentable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.dbn.nls.NlsResources.txt;

@Getter
@AllArgsConstructor
public enum AuthenticationType implements Constant<AuthenticationType>, Presentable {
    NONE(txt("cfg.connection.const.AuthenticationType_NONE")),
    USER(txt("cfg.connection.const.AuthenticationType_USER")),
    USER_PASSWORD(txt("cfg.connection.const.AuthenticationType_USER_PASSWORD")),
    OS_CREDENTIALS(txt("cfg.connection.const.AuthenticationType_OS_CREDENTIALS")),
    TOKEN(txt("cfg.connection.const.AuthenticationType_TOKEN"));

    private final String name;
}
