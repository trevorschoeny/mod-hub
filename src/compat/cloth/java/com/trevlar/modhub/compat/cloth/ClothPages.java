package com.trevlar.modhub.compat.cloth;

import com.trevlar.modhub.page.ConfigPage;
import com.trevlar.modhub.page.Control;
import com.trevlar.modhub.page.Value;

import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.AbstractListListEntry;
import me.shedaniel.clothconfig2.gui.entries.AbstractNumberListEntry;
import me.shedaniel.clothconfig2.gui.entries.BaseListCell;
import me.shedaniel.clothconfig2.gui.entries.BaseListEntry;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.ColorEntry;
import me.shedaniel.clothconfig2.gui.entries.DoubleListEntry;
import me.shedaniel.clothconfig2.gui.entries.DoubleListListEntry;
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry;
import me.shedaniel.clothconfig2.gui.entries.EmptyEntry;
import me.shedaniel.clothconfig2.gui.entries.FloatListEntry;
import me.shedaniel.clothconfig2.gui.entries.FloatListListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerListListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.gui.entries.KeyCodeEntry;
import me.shedaniel.clothconfig2.gui.entries.LongListEntry;
import me.shedaniel.clothconfig2.gui.entries.LongListListEntry;
import me.shedaniel.clothconfig2.gui.entries.LongSliderEntry;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import me.shedaniel.clothconfig2.gui.entries.SelectionListEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListListEntry;
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;
import me.shedaniel.clothconfig2.gui.entries.TextListEntry;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Reads a Cloth Config screen's entries ({@code getCategorizedEntries}: category name to entries,
 * in order) into a {@link ConfigPage}. The original screen is never shown.
 *
 * <p><b>Each entry is its own widget</b> and holds its own pending value. A change is made the way
 * Cloth's widget would make it (the entry's setter, or its private state where it has none), then,
 * if the entry reports no error, the entry's save pushes it into the mod and the screen's save runs
 * the mod's saving action. Cloth's Save button does the same for every entry at once.
 *
 * <p>Sub-categories become groups, nested ones named by their path. Colours use Mod Hub's colour
 * picker; lists of text and numbers are a row per element. Key bindings, dropdowns, nested lists
 * and multi-element entries send the mod to Cloth's screen, as does any entry class Mod Hub
 * doesn't know.
 */
final class ClothPages {

    private static final Component RESTART = Component.literal("Needs a restart to take effect.");

    private ClothPages() {}

    static @Nullable ConfigPage translate(Screen screen) {
        if (!(screen instanceof AbstractConfigScreen cloth)) return null;
        List<ConfigPage.Category> categories = new ArrayList<>();
        List<Runnable> resets = new ArrayList<>();
        for (Map.Entry<Component, List<AbstractConfigEntry<?>>> c : cloth.getCategorizedEntries().entrySet()) {
            List<ConfigPage.Block> blocks = new ArrayList<>();
            List<ConfigPage.Block> groups = new ArrayList<>();
            for (AbstractConfigEntry<?> e : c.getValue()) visit(cloth, e, null, blocks, groups, resets);
            blocks.addAll(groups);
            categories.add(new ConfigPage.Category(c.getKey(), blocks));
        }
        return new ConfigPage(categories, () -> resets.forEach(Runnable::run));
    }

    /**
     * One entry: a row into {@code rows}, or for a sub-category, a group into {@code groups} (its
     * own sub-categories after it, named by their path, since groups don't nest).
     */
    private static void visit(AbstractConfigScreen screen, AbstractConfigEntry<?> e, @Nullable String path,
                              List<ConfigPage.Block> rows, List<ConfigPage.Block> groups, List<Runnable> resets) {
        // Children of a sub-category only learn their screen while drawn, which this one never is.
        e.setScreen(screen);
        if (e instanceof EmptyEntry) return;
        if (e instanceof SubCategoryListEntry sub) {
            String name = path == null ? sub.getCategoryName().getString() : path + " / " + sub.getCategoryName().getString();
            List<ConfigPage.Block> own = new ArrayList<>();
            List<ConfigPage.Block> inner = new ArrayList<>();
            for (AbstractConfigListEntry<?> child : sub.getValue()) visit(screen, child, name, own, inner, resets);
            List<ConfigPage.Row> ownRows = own.stream().map(b -> (ConfigPage.Row) b).toList();
            if (!ownRows.isEmpty()) groups.add(new ConfigPage.Group(Component.literal(name), null, sub.isExpanded(), ownRows));
            groups.addAll(inner);
            return;
        }
        if (e instanceof TextListEntry t) {
            // A line of text: its text is its own, not its field name.
            Component text = field(t, TextListEntry.class, "text");
            rows.add(new ConfigPage.Row(text != null ? text : t.getFieldName(), null, null, new Control.Label()));
            return;
        }
        Component note = e.isRequiresRestart() ? RESTART : null;
        rows.add(new ConfigPage.Row(e.getFieldName(), tooltip(e), note, control(screen, e, resets)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Control control(AbstractConfigScreen screen, AbstractConfigEntry<?> e, List<Runnable> resets) {
        return switch (e) {
            case BooleanListEntry b -> {
                AtomicBoolean state = field(b, BooleanListEntry.class, "bool");
                if (state == null) yield new Control.Unsupported("toggle");
                yield new Control.Toggle(value(screen, b, state::set, resets));
            }
            case IntegerSliderEntry s -> {
                Integer min = field(s, IntegerSliderEntry.class, "minimum"), max = field(s, IntegerSliderEntry.class, "maximum");
                if (min == null || max == null) yield new Control.Unsupported("slider");
                yield new Control.IntSlider(value(screen, s, s::setValue, resets), min, max, 1, s.getTextGetter());
            }
            case LongSliderEntry s -> {
                Long min = field(s, LongSliderEntry.class, "minimum"), max = field(s, LongSliderEntry.class, "maximum");
                if (min == null || max == null) yield new Control.Unsupported("slider");
                Value<Long> v = value(screen, s, s::setValue, resets);
                Value<Double> d = Value.of(() -> (double) v.get(), x -> v.set(Math.round(x)), (double) v.defaultValue(), v::available);
                Function<Long, Component> text = s.getTextGetter();
                yield new Control.DecimalSlider(d, min, max, 1, x -> text.apply(Math.round(x)));
            }
            case IntegerListEntry n -> number(screen, n, Integer::parseInt, resets);
            case LongListEntry n -> number(screen, n, Long::parseLong, resets);
            case FloatListEntry n -> number(screen, n, Float::parseFloat, resets);
            case DoubleListEntry n -> number(screen, n, Double::parseDouble, resets);
            case StringListEntry s -> new Control.Text(value(screen, s, s::setValue, resets));
            // EnumListEntry is a SelectionListEntry over the enum's constants.
            case SelectionListEntry<?> s -> selection(screen, (SelectionListEntry) s, resets);
            case ColorEntry c -> {
                Boolean alpha = field(c, ColorEntry.class, "alpha");
                boolean a = alpha != null && alpha;
                Value<Integer> v = value(screen, c, c::setValue, resets);
                yield new Control.Color(Value.of(() -> a ? v.get() : v.get() & 0xFFFFFF, v::set,
                        a ? v.defaultValue() : v.defaultValue() & 0xFFFFFF, v::available), a);
            }
            case KeyCodeEntry k -> new Control.Unsupported("key binding");
            case DropdownBoxEntry<?> d -> new Control.Unsupported("dropdown");
            case StringListListEntry l -> list(screen, l, "", v -> new Control.Text(v), resets);
            case IntegerListListEntry l -> numbers(screen, l, 0, Integer::parseInt, resets);
            case LongListListEntry l -> numbers(screen, l, 0L, Long::parseLong, resets);
            case FloatListListEntry l -> numbers(screen, l, 0f, Float::parseFloat, resets);
            case DoubleListListEntry l -> numbers(screen, l, 0d, Double::parseDouble, resets);
            case AbstractListListEntry<?, ?, ?> l -> new Control.Unsupported("list");
            case MultiElementListEntry<?> m -> new Control.Unsupported("multi-element entry");
            default -> new Control.Unsupported("custom entry " + e.getClass().getSimpleName());
        };
    }

    /**
     * An entry as a Mod Hub value. Read the entry's own value; on set, put the new value into the
     * entry with {@code into}, and if the entry is happy with it, save the entry into the mod and
     * run the mod's saving action. Available while Cloth would let the player edit it.
     */
    private static <T> Value<T> value(AbstractConfigScreen screen, AbstractConfigEntry<T> e, Consumer<T> into, List<Runnable> resets) {
        T fallback = e.getValue();
        Value<T> v = Value.of(e::getValue, x -> {
            into.accept(x);
            if (e.getConfigError().isPresent()) return;   // Cloth's own Save would refuse it too
            e.save();
            screen.save();
        }, e.getDefaultValue().orElse(fallback), () -> editable(e));
        e.getDefaultValue().ifPresent(def -> resets.add(() -> v.set(def)));
        return v;
    }

    /** A number field: the text goes into the entry, which parses it and checks its range itself. */
    private static <N extends Number> Control number(AbstractConfigScreen screen, AbstractNumberListEntry<N> n,
                                                    Function<String, N> parse, List<Runnable> resets) {
        Value<N> v = value(screen, n, x -> n.setValue(String.valueOf(x)), resets);
        N min = field(n, AbstractNumberListEntry.class, "minimum");
        N max = field(n, AbstractNumberListEntry.class, "maximum");
        return new Control.NumberField<>(v, t -> {
            try {
                N x = parse.apply(t);
                if (min != null && x.doubleValue() < min.doubleValue()) return null;
                if (max != null && x.doubleValue() > max.doubleValue()) return null;
                return x;
            } catch (NumberFormatException ex) {
                return null;
            }
        }, String::valueOf);
    }

    /**
     * A list entry: a row per element. A change puts the new list into the entry's cells, as
     * Cloth's own reset button does (its private cells, each made by the entry from a value), and
     * saves as any other entry does. A Cloth update that renames those sends the mod to Cloth's screen.
     */
    private static <T> Control list(AbstractConfigScreen screen, AbstractListListEntry<T, ?, ?> l, T fresh,
                                    Function<Value<T>, Control> element, List<Runnable> resets) {
        List<Object> cells = field(l, BaseListEntry.class, "cells");
        List<Object> widgets = field(l, BaseListEntry.class, "widgets");
        List<Object> narratables = field(l, BaseListEntry.class, "narratables");
        Method make = method(BaseListEntry.class, "getFromValue", Object.class);
        if (cells == null || widgets == null || narratables == null || make == null) return new Control.Unsupported("list");
        Value<List<T>> v = value(screen, l, values -> {
            widgets.removeAll(cells);
            narratables.removeAll(cells);
            cells.forEach(c -> ((BaseListCell) c).onDelete());
            cells.clear();
            for (T x : values) {
                try {
                    cells.add(make.invoke(l, x));
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException(ex);
                }
            }
            cells.forEach(c -> ((BaseListCell) c).onAdd());
            widgets.addAll(cells);
            narratables.addAll(cells);
        }, resets);
        return new Control.ListOf<>(v, element, () -> fresh, 0, Integer.MAX_VALUE);
    }

    /** A list of numbers, each typed in, inside the entry's own limits. */
    private static <N extends Number> Control numbers(AbstractConfigScreen screen, AbstractListListEntry<N, ?, ?> l, N zero,
                                                      Function<String, N> parse, List<Runnable> resets) {
        Number min = field(l, l.getClass(), "minimum"), max = field(l, l.getClass(), "maximum");
        N fresh = min != null && zero.doubleValue() < min.doubleValue() ? parse.apply(String.valueOf(min)) : zero;
        return list(screen, l, fresh, v -> new Control.NumberField<>(v, t -> {
            try {
                N x = parse.apply(t);
                if (min != null && x.doubleValue() < min.doubleValue()) return null;
                if (max != null && x.doubleValue() > max.doubleValue()) return null;
                return x;
            } catch (NumberFormatException ex) {
                return null;
            }
        }, String::valueOf), resets);
    }

    /** A private method of a Cloth class, or null if it isn't there. */
    private static @Nullable Method method(Class<?> declaring, String name, Class<?>... params) {
        try {
            Method m = declaring.getDeclaredMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    /** A cycling choice: its values and their names, and set by moving its index. */
    private static <T> Control selection(AbstractConfigScreen screen, SelectionListEntry<T> s, List<Runnable> resets) {
        List<T> values = field(s, SelectionListEntry.class, "values");
        AtomicInteger index = field(s, SelectionListEntry.class, "index");
        Function<T, Component> names = field(s, SelectionListEntry.class, "nameProvider");
        if (values == null || index == null || names == null) return new Control.Unsupported("selection");
        Value<T> v = value(screen, s, x -> {
            int i = values.indexOf(x);
            if (i >= 0) index.set(i);
        }, resets);
        return new Control.Choice<>(v, List.copyOf(values), names);
    }

    /**
     * Whether Cloth would let the player edit the entry now. Its requirements are only checked on
     * a tick, and the original screen never ticks, so the entry is ticked here first.
     */
    private static boolean editable(AbstractConfigEntry<?> e) {
        e.tick();
        return !(e instanceof AbstractConfigListEntry<?> l) || l.isEditable();
    }

    /** The entry's tooltip lines as one text, or null. */
    private static @Nullable Component tooltip(AbstractConfigEntry<?> e) {
        if (!(e instanceof TooltipListEntry<?> t) || t.getTooltipSupplier() == null) return null;
        Component[] lines = t.getTooltipSupplier().get().orElse(null);
        if (lines == null || lines.length == 0) return null;
        net.minecraft.network.chat.MutableComponent out = Component.empty();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) out.append("\n");
            out.append(lines[i]);
        }
        return out;
    }

    /** A private field of a Cloth class, or null if it isn't there (renamed in an update). */
    @SuppressWarnings("unchecked")
    private static <T> @Nullable T field(Object of, Class<?> declaring, String name) {
        try {
            Field f = declaring.getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(of);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }
}
