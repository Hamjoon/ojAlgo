package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.stream.Collectors;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExpressionsBasedModel_variables_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
    }

    /**
     * Returns the names of the variables in the supplied stream, materialised as a list.
     */
    private static List<String> namesOf(Stream<Variable> stream) {
        return stream.map(Variable::getName).collect(Collectors.toList());
    }

    /**
     * When the model contains a mixture of ordinary variables and equality‑constraint
     * variables, {@code variables()} must return only the ordinary ones.
     */
    @Test
    void testVariablesExcludesEqualityConstraint() {
        // ordinary variable (bounds are different)
        Variable ordinary = model.newVariable("ordinary").lower(0).upper(10).weight(1);
        // equality‑constraint variable (lower == upper via level())
        Variable equality = model.newVariable("equality").level(// sets both lower and upper to 5
        5).weight(1);
        List<Variable> vars = model.variables().collect(Collectors.toList());
        assertTrue(vars.contains(ordinary), "Ordinary variable should be present");
        assertFalse(vars.contains(equality), "Equality‑constraint variable should be filtered out");
    }

    /**
     * If the model has no equality‑constraint variables, all variables must be returned.
     */
    @Test
    void testVariablesReturnsAllWhenNoEquality() {
        Variable v1 = model.newVariable("v1").lower(0).upper(5).weight(1);
        // no bounds → not an equality constraint
        Variable v2 = model.newVariable("v2").weight(1);
        List<Variable> vars = model.variables().collect(Collectors.toList());
        assertEquals(2, vars.size(), "Both variables should be returned");
        assertTrue(vars.containsAll(List.of(v1, v2)));
    }

    /**
     * If every variable is an equality‑constraint, the returned stream should be empty.
     */
    @Test
    void testVariablesEmptyWhenAllEquality() {
        model.newVariable("e1").level(1).weight(1);
        model.newVariable("e2").level(2).weight(1);
        List<Variable> vars = model.variables().collect(Collectors.toList());
        assertTrue(vars.isEmpty(), "Stream should be empty when all variables are equality constraints");
    }

    /**
     * Accesses the private {@code myVariables} field via reflection.
     */
    @SuppressWarnings("unchecked")
    private ArrayList<Variable> getInternalVariableList() throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        return (ArrayList<Variable>) f.get(model);
    }

    /**
     * Empty model – no variables.
     */
    @Test
    void testEmptyModel() {
        List<Variable> vars = model.variables().collect(Collectors.toList());
        assertTrue(vars.isEmpty(), "When the model contains no variables, the stream should be empty.");
    }

    /**
     * All variables are non‑equality constraints.
     */
    @Test
    void testAllNonEqualityVariables() {
        Variable varA = model.newVariable("varA").lower(0).upper(10).weight(1);
        Variable varB = model.newVariable("varB").lower(-5).upper(5).weight(1);
        // no bounds → not equality
        Variable varC = model.newVariable("varC").weight(1);
        List<String> names = namesOf(model.variables());
        assertEquals(List.of("varA", "varB", "varC"), names, "All non‑equality variables must be present in insertion order.");
    }

    /**
     * Mixed equality and non‑equality variables.
     */
    @Test
    void testMixedEqualityVariables() {
        Variable varX = model.newVariable("varX").lower(0).upper(10).weight(1);
        // equality constraint
        Variable varY = model.newVariable("varY").level(5).weight(1);
        Variable varZ = model.newVariable("varZ").lower(-2).upper(2).weight(1);
        List<String> names = namesOf(model.variables());
        assertEquals(List.of("varX", "varZ"), names, "Equality‑constraint variable varY must be filtered out.");
    }

    /**
     * All variables are equality constraints.
     */
    @Test
    void testAllEqualityVariables() {
        model.newVariable("e1").level(1).weight(1);
        model.newVariable("e2").level(2).weight(1);
        model.newVariable("e3").level(3).weight(1);
        List<Variable> vars = model.variables().collect(Collectors.toList());
        assertTrue(vars.isEmpty(), "All variables are equality constraints – stream must be empty.");
    }

    /**
     * Stream laziness – underlying list mutation after stream creation.
     */
    @Test
    void testStreamLazinessAfterCreation() {
        // Create initial stream but do not consume yet
        Stream<Variable> stream = model.variables();
        // Add a new non‑equality variable after the stream has been obtained
        Variable var1 = model.newVariable("var1").lower(0).upper(10).weight(1);
        List<String> names = namesOf(stream);
        assertEquals(List.of("var1"), names, "The newly added variable must appear because the stream is evaluated lazily.");
    }

    /**
     * Variable equality flag changed after stream creation.
     */
    @Test
    void testEqualityFlagChangedAfterStreamCreation() {
        Variable var1 = model.newVariable("var1").lower(0).upper(10).weight(1);
        Variable var2 = model.newVariable("var2").lower(0).upper(10).weight(1);
        Variable var3 = model.newVariable("var3").lower(0).upper(10).weight(1);
        Stream<Variable> stream = model.variables();
        // Turn var2 into an equality constraint before the terminal operation
        // now lower == upper
        var2.level(5);
        List<String> names = namesOf(stream);
        assertEquals(List.of("var1", "var3"), names, "Filter must be evaluated at terminal‑operation time, excluding var2 after it became equality.");
    }

    /**
     * Variable removed after stream creation.
     */
    @Test
    void testVariableRemovedAfterStreamCreation() throws Exception {
        Variable varA = model.newVariable("varA").lower(0).upper(10).weight(1);
        Variable varB = model.newVariable("varB").lower(0).upper(10).weight(1);
        Variable varC = model.newVariable("varC").lower(0).upper(10).weight(1);
        Stream<Variable> stream = model.variables();
        // Remove varB directly from the internal list
        ArrayList<Variable> internal = getInternalVariableList();
        internal.remove(varB);
        List<String> names = namesOf(stream);
        assertEquals(List.of("varA", "varC"), names, "Removed variable must not appear in the consumed stream.");
    }

    /**
     * Multiple stream instances reflect list changes independently.
     */
    @Test
    void testMultipleIndependentStreams() {
        Variable v1 = model.newVariable("v1").lower(0).upper(10).weight(1);
        Variable v2 = model.newVariable("v2").lower(0).upper(10).weight(1);
        // captures v1, v2
        Stream<Variable> firstStream = model.variables();
        // Add third variable after first stream creation
        Variable v3 = model.newVariable("v3").lower(0).upper(10).weight(1);
        // captures v1, v2, v3
        Stream<Variable> secondStream = model.variables();
        List<String> first = namesOf(firstStream);
        List<String> second = namesOf(secondStream);
        assertEquals(List.of("v1", "v2"), first, "First stream should contain only the variables present at its creation.");
        assertEquals(List.of("v1", "v2", "v3"), second, "Second stream should reflect the later addition.");
    }

    /**
     * Parallel stream consumption respects filter.
     */
    @Test
    void testParallelStreamFiltersEquality() {
        Variable p1 = model.newVariable("p1").lower(0).upper(10).weight(1);
        // equality
        Variable p2 = model.newVariable("p2").level(5).weight(1);
        Variable p3 = model.newVariable("p3").lower(0).upper(10).weight(1);
        Variable p4 = model.newVariable("p4").lower(0).upper(10).weight(1);
        List<String> names = model.variables().parallel().map(Variable::getName).collect(Collectors.toList());
        // Order is not guaranteed in parallel, so just check content
        assertTrue(names.containsAll(List.of("p1", "p3", "p4")), "Parallel stream must contain all non‑equality variables.");
        assertFalse(names.contains("p2"), "Equality‑constraint variable must be filtered out even in parallel.");
    }

    /**
     * Count terminal operation correctness.
     */
    @Test
    void testCountTerminalOperation() {
        // non‑equality
        model.newVariable("a").lower(0).upper(10).weight(1);
        // equality
        model.newVariable("b").level(5).weight(1);
        // non‑equality
        model.newVariable("c").lower(0).upper(10).weight(1);
        // equality
        model.newVariable("d").level(7).weight(1);
        // equality
        model.newVariable("e").level(9).weight(1);
        long count = model.variables().count();
        assertEquals(2L, count, "Only the two non‑equality variables should be counted.");
    }

    /**
     * Null variable in model should cause NPE.
     */
    @Test
    void testNullVariableCausesNPE() throws Exception {
        // Insert a null entry into the internal list via reflection
        ArrayList<Variable> internal = getInternalVariableList();
        internal.add(null);
        // The stream will attempt to call isEqualityConstraint() on the null entry
        assertThrows(NullPointerException.class, () -> model.variables().count(), "A null variable should trigger NullPointerException during stream processing.");
    }

    /**
     * Concurrent modification during stream consumption should throw CME.
     */
    @Test
    void testConcurrentModificationDuringConsumption() throws Exception {
        // Populate model with a few variables
        model.newVariable("c1").lower(0).upper(10).weight(1);
        model.newVariable("c2").lower(0).upper(10).weight(1);
        model.newVariable("c3").lower(0).upper(10).weight(1);
        ExecutorService exec = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch modifyLatch = new CountDownLatch(1);
        Future<Long> counting = exec.submit(() -> {
            try {
                // Wait until the other thread signals that it is about to modify
                startLatch.await();
                // Trigger terminal operation that will iterate over the list
                return model.variables().mapToLong(v -> 1L).sum();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return 0L;
            }
        });
        // Thread that performs modification
        exec.submit(() -> {
            try {
                // Signal that modification is about to happen
                startLatch.countDown();
                // Small pause to increase chance of overlap
                Thread.sleep(10);
                // Structural modification while the other thread may be iterating
                model.newVariable("c4").lower(0).upper(10).weight(1);
                modifyLatch.countDown();
            } catch (InterruptedException ignored) {
            }
        });
        // Wait for modification to happen
        modifyLatch.await();
        // The counting future should complete with a CME
        ExecutionException ee = assertThrows(ExecutionException.class, counting::get, "Concurrent modification should result in ExecutionException wrapping CME.");
        assertTrue(ee.getCause() instanceof ConcurrentModificationException, "Root cause must be ConcurrentModificationException.");
        exec.shutdownNow();
    }
}
