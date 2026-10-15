package com.trevlar.modhub.screen;

import com.trevlar.modhub.page.Value;

import net.minecraft.util.Util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * The open tab's undo and redo. Every control's value on the tab is {@link #watch watched}, so each
 * real change is recorded as a step (which setting, from what, to what), whatever library the
 * setting comes from. Reset to Defaults is recorded as one step ({@link #asOneStep}).
 *
 * <p><b>Gestures, not frames.</b> A slider sets its value at every step of a drag, and a text field
 * at every key. A change to the same setting within {@link #JOIN_MS} of its last change joins that
 * step; a step that ends where it began (ticked and unticked) is dropped.
 *
 * <p>Undo and Redo set values through the same {@link Value#set}, so they save at once and the
 * library reacts as to any change, but they aren't recorded themselves. A new change clears Redo.
 * Switching tabs or leaving the screen clears both ({@link #clear}).
 */
final class History {

    /** How soon a change to the same setting must follow its last to count as the same gesture. */
    static final long JOIN_MS = 1000;

    /** One setting's part in a step. Raw types: a step holds settings of any type. */
    @SuppressWarnings("rawtypes")
    private record Change(Value value, Object from, Object to) {}

    private record Step(List<Change> changes, long at) {}

    private final Deque<Step> undo = new ArrayDeque<>();
    private final Deque<Step> redo = new ArrayDeque<>();

    /** The time, in milliseconds; a test can stand its own clock in. */
    private final LongSupplier clock;

    History() { this(Util::getMillis); }

    History(LongSupplier clock) { this.clock = clock; }

    /** Set while Undo or Redo is applying a step, or Reset to Defaults is running, so it isn't recorded change by change. */
    private boolean applying;

    boolean canUndo() { return !undo.isEmpty(); }

    boolean canRedo() { return !redo.isEmpty(); }

    void clear() {
        undo.clear();
        redo.clear();
    }

    /**
     * {@code value}, recording each real change to it. The setting may refuse or adjust a value,
     * so the change recorded is what it held before and after, not what was asked for.
     */
    <T> Value<T> watch(Value<T> value) {
        return new Value<>() {
            @Override public T get() { return value.get(); }
            @Override public T defaultValue() { return value.defaultValue(); }
            @Override public boolean available() { return value.available(); }

            @Override
            public void set(T v) {
                T before = value.get();
                value.set(v);
                if (!applying) record(value, before, value.get());
            }
        };
    }

    /** Runs {@code action} (Reset to Defaults) and records whatever it changed among {@code values} as one step. */
    void asOneStep(List<Value<?>> values, Runnable action) {
        List<Object> before = new ArrayList<>();
        for (Value<?> v : values) before.add(v.get());
        applying = true;
        try {
            action.run();
        } finally {
            applying = false;
        }
        List<Change> changes = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            Object after = values.get(i).get();
            if (!Objects.equals(before.get(i), after)) changes.add(new Change(values.get(i), before.get(i), after));
        }
        push(changes, false);
    }

    void undo() {
        Step step = undo.poll();
        if (step == null) return;
        apply(step.changes().reversed(), true);
        redo.push(step);
    }

    void redo() {
        Step step = redo.poll();
        if (step == null) return;
        apply(step.changes(), false);
        undo.push(step);
    }

    private void record(Value<?> value, Object from, Object to) {
        if (Objects.equals(from, to)) return;   // refused, or set to what it was
        push(List.of(new Change(value, from, to)), true);
    }

    /**
     * Adds a step, or folds a one-setting change into the last step when it's the same setting
     * within the join window.
     */
    private void push(List<Change> changes, boolean mayJoin) {
        if (changes.isEmpty()) return;
        redo.clear();
        long now = clock.getAsLong();
        Step last = undo.peek();
        if (mayJoin && last != null && changes.size() == 1 && last.changes().size() == 1
                && last.changes().getFirst().value() == changes.getFirst().value() && now - last.at() <= JOIN_MS) {
            undo.poll();
            Change joined = new Change(changes.getFirst().value(), last.changes().getFirst().from(), changes.getFirst().to());
            // Back where the gesture began: nothing to undo.
            if (!Objects.equals(joined.from(), joined.to())) undo.push(new Step(List.of(joined), now));
            return;
        }
        undo.push(new Step(List.copyOf(changes), now));
    }

    /** Sets each change's "from" (undo) or "to" (redo) value, skipping settings that are greyed out now. */
    @SuppressWarnings("unchecked")
    private void apply(List<Change> changes, boolean back) {
        applying = true;
        try {
            for (Change c : changes) {
                if (c.value().available()) c.value().set(back ? c.from() : c.to());
            }
        } finally {
            applying = false;
        }
    }

    /** Every setting the page's controls edit, for Reset to Defaults' one step. */
    static List<Value<?>> values(Iterable<com.trevlar.modhub.page.ConfigPage.Row> rows) {
        List<Value<?>> out = new ArrayList<>();
        Map<Value<?>, Boolean> seen = new IdentityHashMap<>();
        for (var r : rows) {
            Value<?> v = PageView.valueOf(r.control());
            if (v != null && seen.put(v, true) == null) out.add(v);
        }
        return out;
    }
}
