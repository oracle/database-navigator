package com.dbn.common.environment;

import com.dbn.common.color.Colors;
import com.dbn.common.options.PersistentConfiguration;
import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.util.LookAndFeel;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.ColorIcon;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

import static com.dbn.common.options.setting.Settings.*;
import static com.dbn.common.util.Strings.cachedLowerCase;

@Getter
@Setter
@EqualsAndHashCode
public class EnvironmentType implements Cloneable<EnvironmentType>, PersistentConfiguration, Presentable {

    private static final Color DEFAULT_REGULAR_COLOR = Color.LIGHT_GRAY;
    private static final Color DEFAULT_DARK_COLOR = Color.DARK_GRAY;

    public interface EnvironmentColor {
/*        JBColor DEVELOPMENT = new JBColor(new Color(-2430209), new Color(0x445F80));
        JBColor INTEGRATION = new JBColor(new Color(-2621494), new Color(0x466646));
        JBColor PRODUCTION = new JBColor(new Color(-11574), new Color(0x634544));
        JBColor OTHER = new JBColor(new Color(-1576), new Color(0x5C5B41));*/
        JBColor NONE = new JBColor(new Color(0xffffff), Color.DARK_GRAY);
    }

    public static final EnvironmentType DEFAULT     = new EnvironmentType(EnvironmentTypeId.DEFAULT, "", "", new Color(214,214,214), new Color(72,74,74), false, false);
    public static final EnvironmentType DEVELOPMENT = new EnvironmentType(EnvironmentTypeId.DEVELOPMENT, "Development", "Development environment", new Color(-2430209), new Color(0x445F80), false, false);
    public static final EnvironmentType INTEGRATION = new EnvironmentType(EnvironmentTypeId.INTEGRATION, "Integration", "Integration environment", new Color(-2621494), new Color(0x466646), true, false);
    public static final EnvironmentType PRODUCTION  = new EnvironmentType(EnvironmentTypeId.PRODUCTION, "Production", "Productive environment", new Color(-11574), new Color(0x634544), true, true);
    public static final EnvironmentType OTHER       = new EnvironmentType(EnvironmentTypeId.OTHER, "Other", "", new Color(-1576), new Color(0x5C5B41), false, false);
    private static final EnvironmentType[] DEFAULT_ENVIRONMENT_TYPES = new EnvironmentType[] {
            DEVELOPMENT,
            INTEGRATION,
            PRODUCTION,
            OTHER};

    private EnvironmentTypeId id;
    private String name;
    private String description;
    private Color regularColor;
    private Color darkColor;
    private JBColor color;
    private boolean readonlyCode = false;
    private boolean readonlyData = false;

    public static EnvironmentType forName(String name) {
        for (EnvironmentType environmentType : DEFAULT_ENVIRONMENT_TYPES){
            if (Objects.equals(environmentType.name, name)) {
                return environmentType;
            }
        }
        return null;
    }

    public EnvironmentType() {
        this(EnvironmentTypeId.create());
    }

    public EnvironmentType(EnvironmentTypeId id) {
        this.id = id;
    }

    public EnvironmentType(EnvironmentTypeId id, String name, String description, Color regularColor, Color darkColor, boolean readonlyCode, boolean dataEditable) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.regularColor = regularColor;
        this.darkColor = darkColor;
        this.readonlyCode = readonlyCode;
        this.readonlyData = dataEditable;
    }

    @Override
    @NotNull
    public String getName() {
        return Commons.nvl(name, "");
    }

    @Nullable
    @Override
    public Icon getIcon() {
        Color color = getColor();
        return color == null ? null : new ColorIcon(12, color);
    }

    @Nullable
    public Color getColor() {
        if (color == null) {
            boolean darkMode = com.dbn.common.ui.util.LookAndFeel.isDarkMode();
            if (darkMode && darkColor != null) {
                Color regularColor = Commons.nvl(this.regularColor, DEFAULT_REGULAR_COLOR);
                color = new JBColor(regularColor, darkColor);
            } else if (!darkMode && regularColor != null) {
                Color darkColor = Commons.nvl(this.darkColor, DEFAULT_DARK_COLOR);
                this.color = new JBColor(regularColor, darkColor);
            }
        }

        if (color == null) {
            return Colors.lafDarker(Colors.getPanelBackground(), 3);
        }

        return color;
    }

    public void setColor(Color color) {
        if (LookAndFeel.isDarkMode())
            darkColor = color; else
            regularColor = color;
        this.color = null;
    }

    @Override
    public EnvironmentType clone() {
        return new EnvironmentType(id, name, description, regularColor, darkColor, readonlyCode, readonlyData);
    }
    
    @Override
    public String toString() {
        return name;
    }


    @Override
    public void readConfiguration(Element element) {
        id = EnvironmentTypeId.get(stringAttribute(element, "id"));
        name = stringAttribute(element, "name");
        description = stringAttribute(element, "description");

        String value = stringAttribute(element, "color");
        if (Strings.isNotEmpty(value)) {
            int index = value.indexOf('/');
            if (index > -1) {
                String regularRgb = value.substring(0, index);
                String darkRgb = value.substring(index + 1);
                regularColor = Strings.isEmpty(regularRgb) ? null : new Color(Integer.parseInt(regularRgb));
                darkColor = Strings.isEmpty(darkRgb) ? null : new Color(Integer.parseInt(darkRgb));
            }
        }

        EnvironmentType defaultEnvironmentType = forName(name);
        if (id == null && defaultEnvironmentType != null) {
            id = defaultEnvironmentType.id;
        }
        if (id == null) id = EnvironmentTypeId.get(cachedLowerCase(name));
        readonlyCode = booleanAttribute(element, "readonly-code", readonlyCode);
        readonlyData = booleanAttribute(element, "readonly-data", readonlyData);

    }

    @Override
    public void writeConfiguration(Element element) {
        element.setAttribute("id", id.id());
        element.setAttribute("name", name);
        element.setAttribute("description", Commons.nvl(description, ""));
        element.setAttribute("color",
                (regularColor != null ? regularColor.getRGB() : "") + "/" +
                (darkColor != null ? darkColor.getRGB() : ""));
        setBooleanAttribute(element, "readonly-code", readonlyCode);
        setBooleanAttribute(element, "readonly-data", readonlyData);
    }
}
