package com.dbn.common.util;

import com.dbn.common.filter.Filter;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.dbn.common.util.Commons.nvl;
import static java.lang.Math.max;
import static java.util.Collections.emptyList;

@UtilityClass
public class Lists {

    public static <T> boolean isLast(@NotNull List<T> list, @NotNull T element) {
        int size = list.size();
        if (size == 0) return false;
        return Objects.equals(element, list.get(size - 1));
    }

    public static <T> List<T> filtered(@NotNull List<T> list, Predicate<T> predicate) {
        List<T> result = new ArrayList<>();
        for (T element : list) {
            if (predicate.test(element)) {
                result.add(element);
            }
        }
        return result;
    }

    @NotNull
    public static <T> List<T> filter(@NotNull List<T> list, @Nullable Filter<T> filter) {
        if (filter == null) return list;
        if (list.isEmpty()) return list;
        if (filter.acceptsAll(list)) return list;

        List<T> result = null;
        for (T element : list) {
            if (filter.accepts(element)) {
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.add(element);
            }
        }
        return nvl(result, emptyList());
    }

    @NotNull
    public static <S, T> List<T> convert(@NotNull Collection<S> list, Function<S, T> mapper) {
        List<T> result = new ArrayList<>();
        for (S s : list) {
            T value = mapper.apply(s);
            result.add(value);
        }
        return result;
    }

    @Nullable
    public static <T> T first(@Nullable Collection<T> list, Predicate<? super T> predicate) {
        if (list == null) return null;
        if (list.isEmpty()) return null;

        for (T element : list) {
            if (predicate.test(element)) {
                return element;
            }
        }
        return null;
    }

    public static <T> boolean anyMatch(@Nullable Collection<T> list, Predicate<? super T> predicate) {
        return first(list, predicate) != null;
    }

    public static <T> boolean noneMatch(@Nullable Collection<T> list, Predicate<? super T> predicate) {
        return !anyMatch(list, predicate);
    }

    public static <T> boolean allMatch(@Nullable Collection<T> list, Predicate<? super T> predicate) {
        if (list == null) return true;
        if (list.isEmpty()) return true;

        for (T element : list) {
            if (!predicate.test(element)) {
                return false;
            }
        }
        return true;
    }

    public static <T> int count(@Nullable Collection<T> list, Predicate<? super T> predicate) {
        if (list == null) return 0;
        if (list.isEmpty()) return 0;

        int count = 0;
        for (T element : list) {
            if (!predicate.test(element)) {
                count++;
            }
        }
        return count;
    }

    public static <T> void forEach(@Nullable Collection<T> list, Consumer<? super T> consumer) {
        if (list == null) return;
        if (list.isEmpty()) return;

        for (T element : list) {
            consumer.accept(element);
        }
    }

    public static <T> int indexOf(@Nullable List<T> where, Predicate<T> what) {
        if (where == null) return -1;
        for (int i = 0; i < where.size(); i++) {
            T t = where.get(i);
            if (what.test(t)) return i;
        }
        return -1;
    }

    public static int indexOf(@NotNull List<String> where, @NotNull String what, boolean ignoreCase) {
        int index = where.indexOf(what);
        if (index == -1) {
            for (int i=0; i < where.size(); i++ ) {
                String string = where.get(i);
                if (ignoreCase && Strings.equalsIgnoreCase(string, what) ||
                        (!ignoreCase && Objects.equals(string, what))) {
                    return i;
                }
            }
        }
        return index;
    }

    @Nullable
    public static <T> T lastElement(List<T> list) {
        return list == null || list.isEmpty() ? null : list.get(list.size() - 1);
    }

    @Nullable
    public static <T> T firstElement(@Nullable List<T> list) {
        return list == null || list.isEmpty() ? null : list.get(0);
    }


    public static <T> Iterable<T> reversed(List<T> list) {
        return () -> {
            ListIterator<T> i = list.listIterator(list.size());
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return i.hasPrevious();
                }

                @Override
                public T next() {
                    return i.previous();
                }

                @Override
                public void remove() {
                    i.remove();
                }
            };
        };
    }

    public static boolean isInBounds(List<?> list, int index) {
        return index >= 0 && index < list.size();
    }

    public static boolean isOutOfBounds(List<?> list, int index) {
        return index < 0 || index >= list.size();
    }

    public static <T> String toCsv(List<T> elements, Function<T, String> toString) {
        return toCsv(elements, ", ", toString);
    }

    public static <T> String toCsv(List<T> elements, String separator, Function<T, String> toString) {
        StringBuilder buffer = new StringBuilder();
        for (T element : elements) {
            if (buffer.length() != 0) buffer.append(separator);
            buffer.append(toString.apply(element));
        }
        return buffer.toString();
    }

    public static <T> int greatest(List<T> elements, Function<T, Integer> size) {
        int greatest = 0;

        for (T element : elements) {
            Integer s = size.apply(element);
            greatest = max(greatest, s);
        }
        return greatest;
    }
}
