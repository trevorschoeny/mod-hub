package com.trevlar.modhub.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * One setting: a key in its mod's file, a type, a default and a side. Declared in common code
 * through {@link Settings}, so a dedicated server can read it; read in game code with
 * {@link #get()}.
 *
 * <pre>{@code
 * public static final Settings S = ModHub.settings("hivesight");
 * public static final Setting<Boolean> HUD   = S.bool("hud", true);
 * public static final Setting<Boolean> EJECT = S.bool("eject", true).server();
 * public static final Setting<Integer> RANGE = S.intRange("range", 8, 1, 32);
 * }</pre>
 *
 * <p><b>Mod Hub owns the value; the mod holds the handle.</b> The value lives in this object, so
 * {@code get()} is a field read and always has an answer (the default, before the file loads).
 * Every value passes its type's check on the way in, from the file and from {@link #set}: a number
 * clamps to its range, an unknown choice falls back to the default. A hand-edited file can't make
 * a setting hold something its declaration doesn't allow.
 *
 * <p>The types are the nested classes {@link Bool}, {@link Int} and {@link Choice}. The menu draws
 * each its own way: a checkbox, a slider, a dropdown.
 */
public abstract sealed class Setting<T> permits Setting.Bool, Setting.Int, Setting.Choice {

    private final Settings owner;
    private final String key;
    private final T defaultValue;
    private volatile T value;
    private boolean serverSide;
    private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();

    Setting(Settings owner, String key, T defaultValue) {
        this.owner = owner;
        this.key = key;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    /** The key in the mod's file, and the last part of its lang keys. */
    public String key() { return key; }

    /** The mod's settings this belongs to. */
    public Settings owner() { return owner; }

    public T defaultValue() { return defaultValue; }

    /** The current value. Loads the mod's file on first use. */
    public T get() {
        owner.ensureLoaded();
        return value;
    }

    /**
     * Sets the value, after its type's check, then saves the file and tells the listeners. A value
     * equal to the current one does nothing, so a control that writes every frame costs nothing.
     */
    public void set(T newValue) {
        owner.ensureLoaded();
        T checked = check(newValue);
        if (checked.equals(value)) return;
        value = checked;
        owner.save();
        for (Consumer<T> l : listeners) l.accept(checked);
    }

    /** Back to the default. */
    public void reset() { set(defaultValue); }

    /**
     * Marks this as a server setting: on a server, the server's own file decides. The client's
     * menu will show the server's value greyed when the server has Mod Hub (not built yet).
     */
    public Setting<T> server() {
        this.serverSide = true;
        return this;
    }

    public boolean isServerSide() { return serverSide; }

    /** Runs {@code listener} with the new value after every change, for settings that apply live. */
    public Setting<T> onChange(Consumer<T> listener) {
        listeners.add(listener);
        return this;
    }

    // The file side. Package-private: only Settings reads and writes the file.

    /** Takes this setting's value from the file, or the default when it's missing or the wrong type. */
    void load(@Nullable JsonElement json) {
        T read = json == null ? null : read(json);
        if (json != null && read == null) {
            Settings.LOG.warn("Mod Hub: {} in {} has a value it can't use ({}); using the default.",
                    key, owner.modId(), json);
        }
        value = read == null ? defaultValue : check(read);
    }

    JsonElement toJson() { return write(value); }

    /** The value the type allows nearest to {@code v} (clamped, or the default). */
    protected abstract T check(T v);

    /** {@code json} as this type, or null when it isn't one. */
    protected abstract @Nullable T read(JsonElement json);

    protected abstract JsonElement write(T v);

    // The types

    /** On or off. Drawn as a checkbox. */
    public static final class Bool extends Setting<Boolean> {
        Bool(Settings owner, String key, boolean defaultValue) { super(owner, key, defaultValue); }

        @Override protected Boolean check(Boolean v) { return v != null ? v : defaultValue(); }

        @Override protected @Nullable Boolean read(JsonElement json) {
            return json.isJsonPrimitive() && json.getAsJsonPrimitive().isBoolean() ? json.getAsBoolean() : null;
        }

        @Override protected JsonElement write(Boolean v) { return new JsonPrimitive(v); }

        @Override public Bool server() { super.server(); return this; }
    }

    /** A whole number between {@link #min} and {@link #max}. Drawn as a slider. */
    public static final class Int extends Setting<Integer> {
        public final int min, max;

        Int(Settings owner, String key, int defaultValue, int min, int max) {
            super(owner, key, defaultValue);
            if (min > max || defaultValue < min || defaultValue > max) {
                throw new IllegalArgumentException("Mod Hub: " + key + " default " + defaultValue
                        + " is outside " + min + ".." + max);
            }
            this.min = min;
            this.max = max;
        }

        @Override protected Integer check(Integer v) {
            return v == null ? defaultValue() : Math.max(min, Math.min(max, v));
        }

        @Override protected @Nullable Integer read(JsonElement json) {
            return json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber() ? json.getAsInt() : null;
        }

        @Override protected JsonElement write(Integer v) { return new JsonPrimitive(v); }

        @Override public Int server() { super.server(); return this; }
    }

    /**
     * One of an enum's values. Written to the file as the constant's name in lower case, read back
     * case-insensitively, so {@code "AUTO"} and {@code "auto"} both load. Drawn as a dropdown.
     */
    public static final class Choice<E extends Enum<E>> extends Setting<E> {
        private final Class<E> type;

        Choice(Settings owner, String key, E defaultValue) {
            super(owner, key, defaultValue);
            this.type = defaultValue.getDeclaringClass();
        }

        /** Every value, in declaration order. */
        public List<E> values() { return List.of(type.getEnumConstants()); }

        @Override protected E check(E v) { return v != null ? v : defaultValue(); }

        @Override protected @Nullable E read(JsonElement json) {
            if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString()) return null;
            String name = json.getAsString();
            for (E e : type.getEnumConstants()) if (e.name().equalsIgnoreCase(name)) return e;
            return null;
        }

        @Override protected JsonElement write(E v) { return new JsonPrimitive(v.name().toLowerCase(Locale.ROOT)); }

        @Override public Choice<E> server() { super.server(); return this; }
    }
}
