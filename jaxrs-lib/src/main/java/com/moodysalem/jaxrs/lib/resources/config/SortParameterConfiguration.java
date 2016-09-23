package com.moodysalem.jaxrs.lib.resources.config;

/**
 * Value type that contains information about how the sorting should be handled for a particular entity resource
 */
public class SortParameterConfiguration {
    public static SortParameterConfiguration DEFAULT = new SortParameterConfiguration("sort", "\\|", "\\.", (short) 3);

    private final String queryParameterName, sortInfoSeparator, sortPathSeparator;
    private final short maxSorts;

    public SortParameterConfiguration(String queryParameterName, String sortInfoSeparator, String sortPathSeparator, short maxSorts) {
        this.queryParameterName = queryParameterName;
        this.sortInfoSeparator = sortInfoSeparator;
        this.sortPathSeparator = sortPathSeparator;
        this.maxSorts = maxSorts;
    }

    public String getQueryParameterName() {
        return queryParameterName;
    }

    public String getSortInfoSeparator() {
        return sortInfoSeparator;
    }

    public String getSortPathSeparator() {
        return sortPathSeparator;
    }

    public short getMaxSorts() {
        return maxSorts;
    }
}
