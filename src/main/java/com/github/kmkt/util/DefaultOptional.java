package com.github.kmkt.util;

import java.util.NoSuchElementException;
import java.util.Objects;


/**
 * 値が未設定の場合にデフォルト値を返す Optional。
 *
 * デフォルト値が設定された DefaultOptional では､ {@link #get()} で得られる値が
 * デフォルト値か {@link #set(Object)} された値か {@link #isDefault()} で判断できる
 */
public class DefaultOptional<T> {
    private T value = null;
    private boolean present = false;
    private boolean def = false;

    /**
     * 値が未設定の DefaultOptional インスタンスを返す。
     * このメソッドで得られるインスタンスの {@link #isDefault()} は常に true を返し、
     * {@link #set(Object)} で値が指定されるまで {@link #isPresent()} は false を返す。
     * @return 値が未設定の DefaultOptional
     */
    public static <T> DefaultOptional<T> empty() {
        return new DefaultOptional<T>();
    }

    /**
     * 指定された非 null 値を持つ DefaultOptional インスタンスを返す。
     * このメソッドで得られるインスタンスの {@link #isPresent()} は常に true を返し、
     * {@link #isDefault()} は常に false を返す。
     * @param value 設定する値 notnull
     * @return 指定された非 null 値を持つ DefaultOptional
     * @throws NullPointerException value == null 時
     */
    public static <T> DefaultOptional<T> of(T value) {
        DefaultOptional<T> result = DefaultOptional.empty();
        result.set(value);
        return result;
    }

    /**
     * 指定された非 null のデフォルト値を持つ DefaultOptional インスタンスを返す。
     * このメソッドで得られるインスタンスの {@link #isPresent()} は常に true を返し、
     * {@link #set(Object)} で値が指定されるまで {@link #isDefault()} は true を返す。
     * @param default_value 設定するデフォルト値 notnull
     * @return 指定された非 null 値を持つ DefaultOptional
     * @throws NullPointerException default_value == null 時
     */
    public static <T> DefaultOptional<T> withDefault(T default_value) {
        return new DefaultOptional<T>(default_value);
    }

    /**
     * 値が未設定の DefaultOptional インスタンスを生成する。
     * このコンストラクタで得られるインスタンスの {@link #isDefault()} は常に true を返し、
     * {@link #set(Object)} で値が指定されるまで {@link #isPresent()} は false を返す。
     */
    private DefaultOptional() {
        present = false;
        def = true;
    }

    /**
     * 指定された非 null のデフォルト値を持つ DefaultOptional インスタンスを生成する。
     * このコンストラクタで得られるインスタンスの {@link #isPresent()} は常に true を返し、
     * {@link #set(Object)} で値が指定されるまで {@link #isDefault()} は true を返す。
     * @param default_value 設定するデフォルト値 notnull
     * @throws NullPointerException default_value == null 時
     */
    private DefaultOptional(T default_value) {
        Objects.requireNonNull(default_value, "default_value should not be null");
        present = true;
        def = true;
        value = default_value;
    }

    /**
     * 非 null 値を設定する。
     * {@link #isPresent()} は true となり、{@link #isDefault()} は false となる。
     * @param value 設定する値 notnull
     * @throws NullPointerException value == null 時
     */
    public synchronized void set(T value) {
        Objects.requireNonNull(value, "value should not be null");

        present = true;
        def = false;
        this.value = value;
    }

    /**
     * この DefaultOptional に値が設定されている場合はその値を返し、それ以外は NoSuchElementException をスローする。
     * @return この DefaultOptional に設定されている非 null 値
     * @throws NoSuchElementException
     */
    public synchronized T get() {
        if (!isPresent())
            throw new NoSuchElementException();

        return value;
    }

    /**
     * この DefaultOptional に値が設定されている場合は true を返し、それ以外では false を返す。
     * @return 値が設定されている場合は true 、それ以外では false
     */
    public synchronized boolean isPresent() {
        return present;
    }

    /**
     * この DefaultOptional に値がデフォルト値のままの場合は true を返し、それ以外では false を返す。
     * @return デフォルト値のままの場合は true 、それ以外では false
     */
    public synchronized boolean isDefault() {
        return def;
    }

    /**
     * この DefaultOptional に値が設定されている場合はその値を返し、それ以外の場合は other を返す。
     * @param other この DefaultOptional に値が設定されていない場合に返す値。 nullable
     * @return この DefaultOptional に値が設定されている場合はその値、それ以外の場合は other
     */
    public synchronized T orElse(T other) {
        return (isPresent() ? value : other);
    }
}
