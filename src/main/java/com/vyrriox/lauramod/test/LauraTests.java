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
        WorkAreaTests.addTo(tests);
        MiscAreaTests.addTo(tests);
        CombatTests.addTo(tests);
        // One or several parts of test names, separated by commas.
        String only = System.getProperty("lauramod.selftest.only", "");
        if (!only.isBlank()) {
            List<String> parts = List.of(only.split(","));
            tests.removeIf(t -> parts.stream().map(String::trim).noneMatch(p -> !p.isEmpty() && t.name().contains(p)));
        }
        return tests;
    }
}
