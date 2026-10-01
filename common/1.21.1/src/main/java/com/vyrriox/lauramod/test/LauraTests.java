package com.vyrriox.lauramod.test;

import java.util.ArrayList;
import java.util.List;

/**
 * The list of self tests. Filled in {@link LauraTestCases}.
 *
 * @author vyrriox
 */
public final class LauraTests {
    private LauraTests() {
    }

    public static List<TestRunner.TestCase> all() {
        List<TestRunner.TestCase> tests = new ArrayList<>();
        LauraTestCases.register(tests);
        MiscAreaTests.addTo(tests);
        String only = System.getProperty("lauramod.selftest.only", "");
        if (!only.isBlank()) {
            tests.removeIf(t -> !t.name().contains(only));
        }
        return tests;
    }
}
