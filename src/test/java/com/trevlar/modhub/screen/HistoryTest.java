package com.trevlar.modhub.screen;

import com.trevlar.modhub.page.Value;

import net.minecraft.network.chat.Component;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The undo history's rules: gestures join, round trips vanish, Reset is one step, a new change clears Redo. */
class HistoryTest {

    private long now;
    private final History h = new History(() -> now);

    private static final class Box { int v; }

    private Value<Integer> value(Box box) {
        return Value.of(() -> box.v, x -> box.v = x, 0);
    }

    @Test
    void aDragIsOneStep() {
        Box b = new Box();
        Value<Integer> v = h.watch(value(b), Component.literal("B"));
        for (int i = 1; i <= 10; i++) { now += 50; v.set(i); }
        h.undo();
        assertEquals(0, b.v);
        assertFalse(h.canUndo());
        h.redo();
        assertEquals(10, b.v);
    }

    @Test
    void changesApartAreSeparateSteps() {
        Box b = new Box();
        Value<Integer> v = h.watch(value(b), Component.literal("B"));
        v.set(1);
        now += History.JOIN_MS + 1;
        v.set(2);
        h.undo();
        assertEquals(1, b.v);
        assertTrue(h.canUndo());
    }

    @Test
    void tickedAndUntickedLeavesNothing() {
        Box b = new Box();
        Value<Integer> v = h.watch(value(b), Component.literal("B"));
        v.set(1);
        now += 100;
        v.set(0);
        assertFalse(h.canUndo());
    }

    @Test
    void resetIsOneStepAndANewChangeClearsRedo() {
        Box a = new Box(), b = new Box();
        a.v = 5; b.v = 7;
        Value<Integer> va = value(a), vb = value(b);
        h.asOneStep(List.of(va, vb), Component.literal("Reset to Defaults"), () -> { a.v = 0; b.v = 0; });
        assertEquals("Reset to Defaults", h.undoLabel().getString());
        h.undo();
        assertEquals(5, a.v);
        assertEquals(7, b.v);
        assertTrue(h.canRedo());
        now += 5000;
        h.watch(va, Component.literal("A")).set(9);
        assertFalse(h.canRedo());
    }
}
