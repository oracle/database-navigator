package com.dbn.common.ui;

import com.dbn.common.ui.form.DBNForm;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

@UtilityClass
public class CardLayouts {

    private static final String BLANK_CARD_ID = "DBN_BLANK_CARD";

    public static void addBlankCard(JPanel container) {
        addBlankCard(container, -1, -1);
    }

    public static void addBlankCard(JPanel container, int width, int height) {
        JPanel blankPanel = new JPanel();
        blankPanel.setPreferredSize(new Dimension(width, height));
        addCard(container, blankPanel, BLANK_CARD_ID);
    }

    public static void addCard(JPanel container, JComponent component, Object identifier) {
        String name = Objects.toString(identifier);
        component.setName(name);
        container.add(component, name);
    }

    public static void addCard(JPanel container, DBNForm component, Object identifier) {
        addCard(container, component.getComponent(), identifier);
    }

    public static void showCard(JPanel container, @Nullable Object identifier) {
        CardLayout cardLayout = (CardLayout) container.getLayout();
        cardLayout.show(container, Objects.toString(identifier));
    }

    public static void showBlankCard(JPanel container) {
        showCard(container, BLANK_CARD_ID);
    }

    public static JPanel createCardPanel() {
        return createCardPanel(false);
    }

    public static JPanel createCardPanel(boolean withBlankCard) {
        JPanel panel = new JPanel(new CardLayout());
        if (withBlankCard) addBlankCard(panel);
        return panel;
    }

    public static String visibleCardId(JPanel container) {
        for (Component component : container.getComponents()) {
            if (component.isVisible()) return component.getName();
        }
        return null;
    }

    public static boolean isBlankCard(String identifier) {
        return BLANK_CARD_ID.equals(identifier);
    }
}
