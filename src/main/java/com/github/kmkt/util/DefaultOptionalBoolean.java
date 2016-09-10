package com.github.kmkt.util;

import java.util.NoSuchElementException;

/**
 * 値が未設定の場合にデフォルト値を返す Optional。
 *
 * デフォルト値が設定された DefaultOptionalBoolean では､ {@link #getAsBoolean()} で得られる値が
 * デフォルト値か {@link #set(boolean)} された値か {@link #isDefault()} で判断できる
 */
public class DefaultOptionalBoolean {
    private boolean value = false;
    private boolean present = false;
    private boolean def = false;

    /**
     * 値が未設定の DefaultOptionalBoolean インスタンスを返す。
     * このメソッドで得られるインスタンスの {@link #isDefault()} は常に true を返し、
     * {@link #set(boolean)} で値が指定されるまで {@link #isPresent()} は false を返す。
     * @return 値が未設定の DefaultOptionalBoolean
     */
    public static DefaultOptionalBoolean empty() {
        return new DefaultOptionalBoolean();
    }

    /**
     * 指定された値を持つ DefaultOptionalBoolean インスタンスを返す。
     * このメソッドで得られるインスタンスの {@link #isPresent()} は常に true を返し、
     * {@link #isDefault()} は常に false を返す。
     * @param value 設定する値
     * @return 指定された値を持つ DefaultOptionalBoolean
     */
    public static DefaultOptionalBoolean of(boolean value) {
        DefaultOptionalBoolean result = DefaultOptionalBoolean.empty();
        result.set(value);
        return result;
    }

    /**
     * 指定されたデフォルト値を持つ DefaultOptionalBoolean インスタンスを返す。
     * このメソッドで得られるインスタンスの {@link #isPresent()} は常に true を返し、
     * {@link #set(boolean)} で値が指定されるまで {@link #isDefault()} は true を返す。
     * @param default_value 設定するデフォルト値
     * @return 指定され値を持つ DefaultOptionalBoolean
     */
    public static DefaultOptionalBoolean withDefault(boolean default_value) {
        return new DefaultOptionalBoolean(default_value);
    }

    /**
     * 値が未設定の DefaultOptionalBoolean インスタンスを生成する。
     * このコンストラクタで得られるインスタンスの {@link #isDefault()} は常に true を返し、
     * {@link #set(boolean)} で値が指定されるまで {@link #isPresent()} は false を返す。
     */
    private DefaultOptionalBoolean() {
        present = false;
        def = true;
    }

    /**
     * 指定されたデフォルト値を持つ DefaultOptionalBoolean インスタンスを生成する。
     * このコンストラクタで得られるインスタンスの {@link #isPresent()} は常に true を返し、
     * {@link #set(boolean)} で値が指定されるまで {@link #isDefault()} は true を返す。
     * @param default_value 設定するデフォルト値
     */
    private DefaultOptionalBoolean(boolean default_value) {
        present = true;
        def = true;
        value = default_value;
    }

    /**
     * 値を設定する。
     * {@link #isPresent()} は true となり、{@link #isDefault()} は false となる。
     * @param value 設定する値
     */
    public synchronized void set(boolean value) {
        present = true;
        def = false;
        this.value = value;
    }

    /**
     * この DefaultOptionalBoolean に値が設定されている場合はその値を返し、それ以外は NoSuchElementException をスローする。
     * @return この DefaultOptionalBoolean に設定されている値
     * @throws NoSuchElementException
     */
    public synchronized boolean getAsBoolean() {
        if (!isPresent())
            throw new NoSuchElementException();

        return value;
    }

    /**
     * この DefaultOptionalBoolean に値が設定されている場合は true を返し、それ以外では false を返す。
     * @return 値が設定されている場合は true 、それ以外では false
     */
    public synchronized boolean isPresent() {
        return present;
    }

    /**
     * この DefaultOptionalBoolean に値がデフォルト値のままの場合は true を返し、それ以外では false を返す。
     * @return デフォルト値のままの場合は true 、それ以外では false
     */
    public synchronized boolean isDefault() {
        return def;
    }

    /**
     * この DefaultOptionalBoolean に値が設定されている場合はその値を返し、それ以外の場合は other を返す。
     * @param other この DefaultOptionalBoolean に値が設定されていない場合に返す値。
     * @return この DefaultOptionalBoolean に値が設定されている場合はその値、それ以外の場合は other
     */
    public synchronized boolean orElse(boolean other) {
        return (isPresent() ? value : other);
    }
}