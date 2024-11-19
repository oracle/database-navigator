package com.dbn.common.action;

import com.dbn.common.compatibility.Workaround;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

@Workaround
public class DataProviders {

    public static void register(@NotNull JComponent component, @NotNull DataProviderDelegate delegate) {
        if (component instanceof DataProvider) return;

        DataProvider dataProvider = DataManager.getDataProvider(component);
        DataManager.removeDataProvider(component);

        dataProvider = createDataProvider(delegate, dataProvider);
        DataManager.registerDataProvider(component, dataProvider);
    }

    private static DataProvider createDataProvider(DataProviderDelegate delegate, DataProvider original) {
        return new CompositeDataProvider(delegate, original);
    }
}
