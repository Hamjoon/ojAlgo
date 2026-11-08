package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.concurrent.CountDownLatch;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ExpressionsBasedModel_getIntegerVariables_Test {

    private ExpressionsBasedModel model;

    private Variable intVar1;

    private Variable intVar2;

    private Variable nonIntVar;

    private Variable intVar3;

    private Variable fixedIntVar;

    @BeforeEach
    void setupBeforeEach() {
        // fresh model for each test
        model = new ExpressionsBasedModel();
        // free integer variables
        intVar1 = model.addVariable().integer(true);
        intVar2 = model.addVariable().integer(true);
        // a non‑integer variable
        nonIntVar = model.addVariable().integer(false);
        // a fixed integer variable – should be ignored by getIntegerVariables()
//         fixedIntVar = model.addVariable().integer(true).fix(5);
    }

    @Test
    void testGetIntegerVariablesReturnsOnlyIntegerVariables() {
        List<Variable> integerVariables = model.getIntegerVariables();
        assertEquals(2, integerVariables.size(), "Expected exactly two integer variables");
        assertTrue(integerVariables.contains(intVar1), "List should contain intVar1");
        assertTrue(integerVariables.contains(intVar2), "List should contain intVar2");
        assertFalse(integerVariables.contains(nonIntVar), "List should not contain non‑integer variable");
    }

    @Test
    void testReturnedListIsUnmodifiable() {
        List<Variable> integerVariables = model.getIntegerVariables();
        assertThrows(UnsupportedOperationException.class, () -> integerVariables.add(nonIntVar), "Returned list should be unmodifiable");
    }

    @Test
    void emptyModelReturnsEmptyUnmodifiableList() {
        ExpressionsBasedModel emptyModel = new ExpressionsBasedModel();
        List<Variable> result = emptyModel.getIntegerVariables();
        assertTrue(result.isEmpty(), "Expected empty list when no variables are present");
        assertThrows(UnsupportedOperationException.class, () -> result.add(intVar1), "Returned list must be unmodifiable");
    }

    @Test
    void mixedIntegerAndNonIntegerVariables() {
        List<Variable> result = model.getIntegerVariables();
        assertEquals(2, result.size(), "Only the two free integer variables should be returned");
        assertTrue(result.contains(intVar1));
        assertTrue(result.contains(intVar2));
        assertFalse(result.contains(nonIntVar));
        assertFalse(result.contains(fixedIntVar));
    }

    @Test
    void allFreeIntegerVariables() {
        ExpressionsBasedModel onlyInts = new ExpressionsBasedModel();
        Variable v1 = onlyInts.addVariable().integer(true);
        Variable v2 = onlyInts.addVariable().integer(true);
        Variable v3 = onlyInts.addVariable().integer(true);
        List<Variable> result = onlyInts.getIntegerVariables();
        assertEquals(3, result.size(), "All three integer variables should be returned");
        assertTrue(result.containsAll(List.of(v1, v2, v3)));
    }

    @Test
    void returnedListAddThrowsUnsupportedOperationException() {
        List<Variable> result = model.getIntegerVariables();
        assertThrows(UnsupportedOperationException.class, () -> result.add(nonIntVar));
    }

    @Test
    void returnedListClearThrowsUnsupportedOperationException() {
        List<Variable> result = model.getIntegerVariables();
        assertThrows(UnsupportedOperationException.class, result::clear);
    }

    @Test
    void returnedListRemoveThrowsUnsupportedOperationException() {
        List<Variable> result = model.getIntegerVariables();
        assertThrows(UnsupportedOperationException.class, () -> result.remove(0));
    }

    @Test
    void returnedListSetThrowsUnsupportedOperationException() {
        List<Variable> result = model.getIntegerVariables();
        assumeTrue(!result.isEmpty(), "List must contain at least one element for set test");
        assertThrows(UnsupportedOperationException.class, () -> result.set(0, nonIntVar));
    }

    @Test
    void nullEntryInInternalVariableListTriggersNullPointerException() throws Exception {
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> internalList = (ArrayList<Variable>) field.get(model);
        internalList.add(null);
        assertThrows(NullPointerException.class, model::getIntegerVariables, "A null entry in the internal variable list should cause NPE");
    }

    @Test
    void concurrentModificationDuringGetIntegerVariablesThrowsConcurrentModificationException() throws Exception {
        ExpressionsBasedModel raceModel = new ExpressionsBasedModel();
        // a
        raceModel.addVariable().integer(true);
        // b
        raceModel.addVariable().integer(true);
        // c
        raceModel.addVariable().integer(false);
        CountDownLatch startIteration = new CountDownLatch(1);
        Thread modifier = new Thread(() -> {
            try {
                // wait for the main thread
                startIteration.await();
                // give the main thread a chance to start iterating
                Thread.sleep(5);
                Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                ArrayList<Variable> list = (ArrayList<Variable>) f.get(raceModel);
                // modify during iteration
                list.add(raceModel.addVariable().integer(true));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        modifier.start();
        assertThrows(ConcurrentModificationException.class, () -> {
            // let the modifier thread run
            startIteration.countDown();
            // this should detect the concurrent modification
            raceModel.getIntegerVariables();
        }, "Concurrent modification of the variable list must result in CME");
        modifier.join();
    }
}