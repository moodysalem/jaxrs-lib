package com.moodysalem.jaxrs.lib.resources.util;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class SortInfoTest {
    @Test
    public void testSortInfo() {
        final List<String> sorts = new LinkedList<>();
        sorts.add("A|a.b.c");

        List<SortInfo> info = SortInfo.from(sorts, "|", ".");

        assert info.size() == 1;
        assert info.get(0).isAscending();
        assert Arrays.equals(info.get(0).getPath(), new String[]{"a", "b", "c"});

        // verify duplicate paths don't work
        sorts.add("D|a.b.c");

        info = SortInfo.from(sorts, "|", ".");
        assert info.size() == 1;
        assert info.get(0).isAscending();
        assert Arrays.equals(info.get(0).getPath(), new String[]{"a", "b", "c"});
    }
}
