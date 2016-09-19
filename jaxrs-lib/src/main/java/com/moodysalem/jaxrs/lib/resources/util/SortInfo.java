package com.moodysalem.jaxrs.lib.resources.util;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SortInfo {
    /**
     * Parse the sort information out of some set of sort strings
     *
     * @param sortParams     strings indicating sorts
     * @param orderSeparator the separator between the Ascending/Descending indicator and the path of the attribute
     * @param pathSeparator  the separator between levels of the path attribute to sort on
     * @return sorting information
     */
    public static List<SortInfo> from(final List<String> sortParams, final String orderSeparator, final String pathSeparator) {
        if (sortParams == null || sortParams.isEmpty()) {
            return Collections.emptyList();
        }

        final List<SortInfo> sorts = new LinkedList<>();
        final String orderSep = Pattern.quote(orderSeparator), pathSep = Pattern.quote(pathSeparator);

        for (final String sort : sortParams) {
            if (sort == null || sort.trim().isEmpty()) {
                continue;
            }

            final String[] pieces = sort.split(orderSep);
            if (pieces.length != 2) {
                continue;
            }

            final boolean asc = "A".equalsIgnoreCase(pieces[0].trim());
            final String[] pathPieces = Stream.of(pieces[1].split(pathSep))
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .map(String::trim)
                    .toArray(String[]::new);

            if (pathPieces.length == 0) {
                continue;
            }

            sorts.add(new SortInfo(pathPieces, asc));
        }

        return removeDuplicates(sorts);
    }

    /**
     * Remove the sort attributes that appear more than once because you cannot sort on the same attribute twice
     *
     * @param sorts list of sort information
     * @return sort information without duplicate paths
     */
    private static List<SortInfo> removeDuplicates(final List<SortInfo> sorts) {
        if (sorts == null || sorts.isEmpty()) {
            return Collections.emptyList();
        }

        final Set<String> paths = new HashSet<>();

        return sorts.stream()
                .filter(si -> paths.add(Stream.of(si.getPath()).collect(Collectors.joining("."))))
                .collect(Collectors.toList());
    }

    private final String[] path;
    private final boolean ascending;

    public SortInfo(final String[] path, final boolean ascending) {
        if (path == null || path.length == 0) {
            throw new IllegalArgumentException();
        }
        this.path = path;
        this.ascending = ascending;
    }

    public String[] getPath() {
        return path;
    }

    public boolean isAscending() {
        return ascending;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SortInfo sortInfo = (SortInfo) o;
        return isAscending() == sortInfo.isAscending() &&
                Arrays.equals(getPath(), sortInfo.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPath(), isAscending());
    }
}
