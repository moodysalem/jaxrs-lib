package com.moodysalem.jaxrs.lib.resources.config;

import java.util.Objects;

public class PaginationParameterConfiguration {
    public static PaginationParameterConfiguration DEFAULT =
            new PaginationParameterConfiguration("start", "count", "X-Start", "X-Count", "X-Total-Count", null);

    private final String startQueryParameterName, countQueryParameterName, startHeader, countHeader, totalCountHeader;
    private final Integer maxPerPage;

    public PaginationParameterConfiguration(String startQueryParameterName, String countQueryParameterName,
                                            String startHeader, String countHeader, String totalCountHeader,
                                            Integer maxPerPage) {
        this.startQueryParameterName = startQueryParameterName;
        this.countQueryParameterName = countQueryParameterName;
        this.startHeader = startHeader;
        this.countHeader = countHeader;
        this.totalCountHeader = totalCountHeader;
        this.maxPerPage = maxPerPage;
    }

    public String getStartQueryParameterName() {
        return startQueryParameterName;
    }

    public String getCountQueryParameterName() {
        return countQueryParameterName;
    }

    public String getStartHeader() {
        return startHeader;
    }

    public String getCountHeader() {
        return countHeader;
    }

    public String getTotalCountHeader() {
        return totalCountHeader;
    }

    public Integer getMaxPerPage() {
        return maxPerPage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaginationParameterConfiguration that = (PaginationParameterConfiguration) o;
        return Objects.equals(getStartQueryParameterName(), that.getStartQueryParameterName()) &&
                Objects.equals(getCountQueryParameterName(), that.getCountQueryParameterName()) &&
                Objects.equals(getStartHeader(), that.getStartHeader()) &&
                Objects.equals(getCountHeader(), that.getCountHeader()) &&
                Objects.equals(getTotalCountHeader(), that.getTotalCountHeader()) &&
                Objects.equals(getMaxPerPage(), that.getMaxPerPage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStartQueryParameterName(), getCountQueryParameterName(), getStartHeader(), getCountHeader(), getTotalCountHeader(), getMaxPerPage());
    }
}
