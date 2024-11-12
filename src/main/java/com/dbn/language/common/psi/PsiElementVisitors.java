package com.dbn.language.common.psi;

import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PsiElementVisitors {
    private final List<String> supportedNames;
    private final Map<Class, Boolean> supported = new ConcurrentHashMap<>();

    private PsiElementVisitors(String ... supportedNames) {
        this.supportedNames = Arrays.asList(supportedNames);
    }

    public boolean isSupported(@NotNull PsiElementVisitor visitor) {
        Boolean supported = this.supported.computeIfAbsent(visitor.getClass(), c -> evaluateSupported(c));
        return supported == Boolean.TRUE;
    }

    private Boolean evaluateSupported(Class c) {
        return supportedNames.stream().anyMatch(n -> c.getName().contains(n)) ? Boolean.TRUE : Boolean.FALSE;
    }

    public static PsiElementVisitors create(String ... supportedNames) {
        return new PsiElementVisitors(supportedNames);
    }
}
