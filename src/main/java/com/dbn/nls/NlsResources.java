package com.dbn.nls;

import com.dbn.common.util.Localization;
import com.dbn.common.util.Named;
import com.intellij.DynamicBundle;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.util.Commons.nvl;

public class NlsResources extends DynamicBundle{
    public static final @NonNls String BUNDLE = "messages.DBNResources";
    private static final NlsResources INSTANCE = new NlsResources();
    private static final Object[] EMPTY_PARAMS = new Object[0];
    private static final Map<String, Boolean> KEY_VALIDITY_CACHE = new ConcurrentHashMap<>();

    static { Localization.initDefaultLocale(); }

    public NlsResources() {
        super(BUNDLE);
    }

    public static @Nls String txt(@PropertyKey(resourceBundle = BUNDLE) String key) {
        return txt(key, EMPTY_PARAMS);
    }

    public static @Nls String txt(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        if (isValidKey(key)) {
            key = key.intern();
            adjustParams(params);
            return INSTANCE.getMessage(key, params);
        }
        return key;
    }

    /**
     * NLS Key validator
     * Workaround for partial implementation of nls resources
     * The resource bundle is returning the key surrounded with exclamation marks if the key is not available.
     * This usually happens when the nls engine is invoked with the text itself (again because not all texts are captured yet in NLS).
     * <p>
     * @param key the key to be verified
     * @return true if the key is valid, false otherwise
     */
    private static boolean isValidKey(String key) {
        if (key == null) return false;

        key = key.intern();
        Boolean valid = KEY_VALIDITY_CACHE.computeIfAbsent(key, k -> checkKeyValidity(k));
        return valid == Boolean.TRUE;
    }

    private static Boolean checkKeyValidity(String k) {
        // avoid boxing and unboxing
        return k.matches("^[a-zA-Z0-9._-]+$") ? Boolean.TRUE : Boolean.FALSE;
    }

    private static void adjustParams(Object ... params) {
        if (params == null || params.length == 0) return;
        for (int i = 0; i < params.length; i++) {
            if (params[i] instanceof Exception) {
                Exception exception = (Exception) params[i];
                params[i] = nvl(
                        exception.getLocalizedMessage(),
                        exception.getClass().getSimpleName());
            } else if (params[i] instanceof Named) {
                Named named = (Named) params[i];
                params[i] = named.getName();
            } else if (params[i] instanceof File) {
                File file = (File) params[i];
                params[i] = file.getPath();
            } else if (params[i] instanceof VirtualFile) {
                VirtualFile file = (VirtualFile) params[i];
                params[i] = file.getPath();
            }
        }
    }
}
