package com.github.kmkt.util;

import java.util.NoSuchElementException;

/**
 * 値が未設定の場合にデフォルト値を返す Optional。
 *
 * デフォルト値が設定された DefaultOptionalInt では､ {@link #getAsInt()} で得られる値が
 * デフォルト値か {@link #set(int)} された値か {@link #isDefault()} で判断できる
 */
public class DefaultOptionalInt {
    private int value = 0;
    private boolean present = false;
    private boolean def = false;

    /**
     * 値が未設定の DefaultOptionalInt インスタンスを返す。
     * このメソッドで得られるインスタンスの {@link #isDefault()} は常に true を返し、
     * {@link #set(int)} で値が指定されるまで {@link #isPresent()} は false を返す。
     * @return 値が未設定の DefaultOptionalInt
     */
    public static DefaultOptionalInt empty() {
        return new DefaultOptionalInt();
    }

    /**
     * 指定された値を持つ DefaultOptionalInt インスタンスを返す。
     * このメソッドで得られるインスタンスの {@link #isPresent()} は常に true を返し、
     * {@link #isDefault()} は常に false を返す。
     * @param value 設定する値
     * @return 指定された値を持つ DefaultOptionalInt
     */
    public static DefaultOptionalInt of(int value) {
        DefaultOptionalInt result = DefaultOptionalInt.empty();
        result.set(value);
        return result;
    }

    /**
     * 指定されたデフォルト値を持つ DefaultOptionalInt インスタンスを返す。
     * このメソッドで得られるインスタンスの {@link #isPresent()} は常に true を返し、
     * {@link #set(int)} で値が指定されるまで {@link #isDefault()} は true を返す。
     * @param default_value 設定するデフォルト値
     * @return 指定され値を持つ DefaultOptionalInt
     */
    public static DefaultOptionalInt withDefault(int default_value) {
        return new DefaultOptionalInt(default_value);
    }

    /**
     * 値が未設定の DefaultOptionalInt インスタンスを生成する。
     * このコンストラクタで得られるインスタンスの {@link #isDefault()} は常に true を返し、
     * {@link #set(int)} で値が指定されるまで {@link #isPresent()} は false を返す。
     */
    private DefaultOptionalInt() {
        present = false;
        def = true;
    }

    /**
     * 指定されたデフォルト値を持つ DefaultOptionalInt インスタンスを生成する。
     * このコンストラクタで得られるインスタンスの {@link #isPresent()} は常に true を返し、
     * {@link #set(int)} で値が指定されるまで {@link #isDefault()} は true を返す。
     * @param default_value 設定するデフォルト値
     */
    private DefaultOptionalInt(int default_value) {
        present = true;
        def = true;
        value = default_value;
    }

    /**
     * 値を設定する。
     * {@link #isPresent()} は true となり、{@link #isDefault()} は false となる。
     * @param value 設定する値
     */
    public synchronized void set(int value) {
        present = true;
        def = false;
        this.value = value;
    }

    /**
     * この DefaultOptionalInt に値が設定されている場合はその値を返し、それ以外は NoSuchElementException をスローする。
     * @return この DefaultOptionalInt に設定されている値
     * @throws NoSuchElementException
     */
    public synchronized int getAsInt() {
        if (!isPresent())
            throw new NoSuchElementException();

        return value;
    }

    /**
     * この DefaultOptionalInt に値が設定されている場合は true を返し、それ以外では false を返す。
     * @return 値が設定されている場合は true 、それ以外では false
     */
    public synchronized boolean isPresent() {
        return present;
    }

    /**
     * この DefaultOptionalInt に値がデフォルト値のままの場合は true を返し、それ以外では false を返す。
     * @return デフォルト値のままの場合は true 、それ以外では false
     */
    public synchronized boolean isDefault() {
        return def;
    }

    /**
     * この DefaultOptionalInt に値が設定されている場合はその値を返し、それ以外の場合は other を返す。
     * @param other この DefaultOptionalInt に値が設定されていない場合に返す値。
     * @return この DefaultOptionalInt に値が設定されている場合はその値、それ以外の場合は other
     */
    public synchronized int orElse(int other) {
        return (isPresent() ? value : other);
    }
}
