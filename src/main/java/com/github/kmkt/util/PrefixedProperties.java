package com.github.kmkt.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * プロパティを階層的に扱い読み込むためのユーティリティラッパ
 *
 * userA.userinfo.name=foo
 * userA.userinfo.mail=foo@example.com
 * userA.config.appA.conf=...
 * userA.config.appB.conf=...
 * userB.userinfo.name=bar
 * userB.userinfo.mail=bar@example.com
 * userB.config.appA.conf=...
 * userB.config.appB.conf=...
 * このようなプロパティから userA.* や userB.userinfo.* での設定ペアの取り出しを行う。
 *
 * 書き込み側は未対応。
 *
 * License : MIT License
 */
public class PrefixedProperties extends Properties {
    private static final long serialVersionUID = -1322580429986053832L;
    @SuppressWarnings("rawtypes")
    private Map<String, Map> children = new HashMap<String, Map>();

    public PrefixedProperties() {
        super();
    }

    public PrefixedProperties(Properties initprop) {
        super(initprop);
        parseKeys();
    }

    /**
     * children にキーのツリー構造を構築する
     *
     *  userA
     *    userinfo
     *      name
     *      mail
     *    config
     *      appA
     *        conf
     *      appB
     *        conf
     *  userB
     *    userinfo...
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void parseKeys() {
        children.clear();
        for (Enumeration<?> o = super.propertyNames(); o.hasMoreElements();) {
            String keyname = (String) o.nextElement();
            String[] parts = keyname.split("\\.");
            Map<String, Map> focus = children;
            for (String part : parts) {
                if (!focus.containsKey(part)) {
                    focus.put(part, new HashMap<String, Map>());
                }
                focus = (Map<String, Map>) focus.get(part);
            }
        }
    }

    // 各種読み込み後、キーのツリー構造を構築する
    @Override
    public synchronized void load(Reader reader) throws IOException {
        super.load(reader);
        parseKeys();
    }

    @Override
    public synchronized void load(InputStream inStream) throws IOException {
        super.load(inStream);
        parseKeys();
    }

    @Override
    public synchronized void loadFromXML(InputStream in)
            throws IOException, InvalidPropertiesFormatException {
        super.loadFromXML(in);
        parseKeys();
    }

    /**
     * prefix の次階層のキー文字列を取得する。
     *
     * getNextRankKeys("userA") で ["userinfo", "config"] が得られる。
     * getNextRankKeys("userA.userinfo") で ["name", "mail"] が得られる。
     * getNextRankKeys(null) で ["userA", "userB"] が得られる。
     *
     * @param prefix キーのprefix
     * @return prefix の次階層のキーのSet
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Set<String> getNextRankKeys(String prefix) {
        if (prefix == null)
            return Collections.unmodifiableSet(children.keySet());

        String[] parts = prefix.split("\\.");
        Map<String, Map> focus = children;
        for (String part : parts) {
            if (!focus.containsKey(part))
                return Collections.emptySet();
            focus = (Map<String, Map>) focus.get(part);
        }
        return Collections.unmodifiableSet(focus.keySet());
    }

    /**
     * prefix が合致するキーを全て取得する。
     * @param prefix キーのprefix
     * @return prefix が合致するキーのSet
     */
    public Set<String> getKeys(String prefix) {
        Set<String> result = new HashSet<String>();
        for (Enumeration<?> o = super.propertyNames(); o.hasMoreElements();) {
            Object keyname = o.nextElement();
            if (keyname.toString().startsWith(prefix))
                result.add(keyname.toString());
        }

        return result;
    }

    /**
     * 指定されたキーに対応するプロパティを int 値として取得する。
     * 対応するプロパティが存在しない場合は値が未設定の DefaultOptionalInt インスタンスが返る。
     *
     * @param key プロパティキー notnull
     * @return DefaultOptionalInt インスタンス notnull
     * @throws NullPointerException key == null 時
     * @throws IllegalArgumentException key == "" 時
     * @throws NumberFormatException 指定されたキーの値が int にパースできなかった場合。詳細メッセージにはキー名が設定される。
     */
    public DefaultOptionalInt getPropertyAsInt(String key) {
        Objects.requireNonNull(key, "key should not be null");
        if (key.isEmpty())
            throw new IllegalArgumentException("key should not be empty");

        String val = getProperty(key);
        if (val == null)
            return DefaultOptionalInt.empty();

        try {
            int int_val = Integer.parseInt(val.trim());
            return DefaultOptionalInt.of(int_val);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(key);
        }
    }

    /**
     * 指定されたキーに対応するプロパティを int 値として取得する。
     * 対応するプロパティが存在しない場合は default_value をデフォルト値とする DefaultOptionalInt インスタンスが返る。
     *
     * @param key プロパティキー notnull
     * @param default_value デフォルト値
     * @return default_value をデフォルト値とする DefaultOptionalInt インスタンス notnull
     * @throws NullPointerException key == null 時
     * @throws IllegalArgumentException key == "" 時
     * @throws NumberFormatException 指定されたキーの値が int にパースできなかった場合。詳細メッセージにはキー名が設定される。
     */
    public DefaultOptionalInt getPropertyAsInt(String key, int default_value) {
        DefaultOptionalInt val = getPropertyAsInt(key);
        if (val.isPresent())
            return val;

        return DefaultOptionalInt.withDefault(default_value);
    }


    /**
     * 指定されたキーに対応するプロパティを long 値として取得する。
     * 対応するプロパティが存在しない場合は値が未設定の DefaultOptionalLong インスタンスが返る。
     *
     * @param key プロパティキー notnull
     * @return DefaultOptionalLong インスタンス notnull
     * @throws NullPointerException key == null 時
     * @throws IllegalArgumentException key == "" 時
     * @throws NumberFormatException 指定されたキーの値が long にパースできなかった場合。詳細メッセージにはキー名が設定される。
     */
    public DefaultOptionalLong getPropertyAsLong(String key) {
        Objects.requireNonNull(key, "key should not be null");
        if (key.isEmpty())
            throw new IllegalArgumentException("key should not be empty");

        String val = getProperty(key);
        if (val == null)
            return DefaultOptionalLong.empty();

        try {
            long long_val = Long.parseLong(val.trim());
            return DefaultOptionalLong.of(long_val);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(key);
        }
    }

    /**
     * 指定されたキーに対応するプロパティを long 値として取得する。
     * 対応するプロパティが存在しない場合は default_value をデフォルト値とする DefaultOptionalLong インスタンスが返る。
     *
     * @param key プロパティキー notnull
     * @param default_value デフォルト値
     * @return default_value をデフォルト値とする DefaultOptionalLong インスタンス notnull
     * @throws NullPointerException key == null 時
     * @throws IllegalArgumentException key == "" 時
     * @throws NumberFormatException 指定されたキーの値が long にパースできなかった場合。詳細メッセージにはキー名が設定される。
     */
    public DefaultOptionalLong getPropertyAsLong(String key, long default_value) {
        DefaultOptionalLong val = getPropertyAsLong(key);
        if (val.isPresent())
            return val;

        return DefaultOptionalLong.withDefault(default_value);
    }


    /**
     * 指定されたキーに対応するプロパティを double 値として取得する。
     * 対応するプロパティが存在しない場合は値が未設定の DefaultOptionalDouble インスタンスが返る。
     *
     * @param key プロパティキー notnull
     * @return DefaultOptionalDouble インスタンス notnull
     * @throws NullPointerException key == null 時
     * @throws IllegalArgumentException key == "" 時
     * @throws NumberFormatException 指定されたキーの値が double にパースできなかった場合。詳細メッセージにはキー名が設定される。
     */
    public DefaultOptionalDouble getPropertyAsDouble(String key) {
        Objects.requireNonNull(key, "key should not be null");
        if (key.isEmpty())
            throw new IllegalArgumentException("key should not be empty");

        String val = getProperty(key);
        if (val == null)
            return DefaultOptionalDouble.empty();

        try {
            double double_val = Double.parseDouble(val.trim());
            return DefaultOptionalDouble.of(double_val);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(key);
        }
    }

    /**
     * 指定されたキーに対応するプロパティを double 値として取得する。
     * 対応するプロパティが存在しない場合は default_value をデフォルト値とする DefaultOptionalDouble インスタンスが返る。
     *
     * @param key プロパティキー notnull
     * @param default_value デフォルト値
     * @return default_value をデフォルト値とする DefaultOptionalDouble インスタンス notnull
     * @throws NullPointerException key == null 時
     * @throws IllegalArgumentException key == "" 時
     * @throws NumberFormatException 指定されたキーの値が double にパースできなかった場合。詳細メッセージにはキー名が設定される。
     */
    public DefaultOptionalDouble getPropertyAsDouble(String key, double default_value) {
        DefaultOptionalDouble val = getPropertyAsDouble(key);
        if (val.isPresent())
            return val;

        return DefaultOptionalDouble.withDefault(default_value);
    }

    /**
     * 指定されたキーに対応するプロパティを boolean 値として取得する。
     * 対応するプロパティが存在しない場合は値が未設定の DefaultOptionalBoolean インスタンスが返る。
     *
     * @param key プロパティキー notnull
     * @return DefaultOptionalBoolean インスタンス notnull
     * @throws NullPointerException key == null 時
     * @throws IllegalArgumentException key == "" 時
     */
    public DefaultOptionalBoolean getPropertyAsBoolean(String key) {
        Objects.requireNonNull(key, "key should not be null");
        if (key.isEmpty())
            throw new IllegalArgumentException("key should not be empty");

        String val = getProperty(key);
        if (val == null)
            return DefaultOptionalBoolean.empty();

        boolean boolean_val = Boolean.parseBoolean(val.trim());
        return DefaultOptionalBoolean.of(boolean_val);
    }

    /**
     * 指定されたキーに対応するプロパティを boolean 値として取得する。
     * 対応するプロパティが存在しない場合は default_value をデフォルト値とする DefaultOptionalBoolean インスタンスが返る。
     *
     * @param key プロパティキー notnull
     * @param default_value デフォルト値
     * @return default_value をデフォルト値とする DefaultOptionalBoolean インスタンス notnull
     * @throws NullPointerException key == null 時
     * @throws IllegalArgumentException key == "" 時
     */
    public DefaultOptionalBoolean getPropertyAsBoolean(String key, boolean default_value) {
        DefaultOptionalBoolean val = getPropertyAsBoolean(key);
        if (val.isPresent())
            return val;

        return DefaultOptionalBoolean.withDefault(default_value);
    }

    /**
     * 指定されたキーに対応するプロパティを String 値として取得する。
     * 対応するプロパティが存在しない場合は値が未設定の DefaultOptional インスタンスが返る。
     * プロパティキーに対応するプロパティが空文字列の場合（プロパティの"="の後が空欄の場合）、
     * accept_emptyprop が true の場合では空文字列が設定された DefaultOptiona インスタンスが返り、
     * accept_emptyprop が false の場合では値が未設定の DefaultOptional インスタンスが返る。
     *
     * @param key プロパティキー notnull
     * @param accept_emptyprop 空のプロパティを認める場合は true
     * @return DefaultOptional インスタンス notnull
     * @throws NullPointerException key == null 時
     * @throws IllegalArgumentException key == "" 時
     */
    public DefaultOptional<String> getPropertyAsString(String key, boolean accept_emptyprop) {
        Objects.requireNonNull(key, "key should not be null");
        if (key.isEmpty())
            throw new IllegalArgumentException("key should not be empty");

        String val = getProperty(key);
        if (val == null)
            return DefaultOptional.empty();

        if (val.isEmpty() && !accept_emptyprop)
            return DefaultOptional.empty();

        return DefaultOptional.of(val.trim());
    }

    /**
     * 指定されたキーに対応するプロパティを String 値として取得する。
     * 対応するプロパティが存在しない場合は default_value をデフォルト値とする DefaultOptional インスタンスが返る。
     * プロパティキーに対応するプロパティが空文字列の場合（プロパティの"="の後が空欄の場合）、
     * accept_emptyprop が true の場合では空文字列が設定された DefaultOptional インスタンスが返り、
     * accept_emptyprop が false の場合ではデフォルト値が設定された DefaultOptional インスタンスが返る。
     *
     * @param key プロパティキー notnull
     * @param default_value デフォルト値 notnull
     * @return default_value をデフォルト値とする DefaultOptional インスタンス notnull
     * @throws NullPointerException key == null 時
     * @throws IllegalArgumentException key == "" 時
     */
    public DefaultOptional<String> getPropertyAsString(String key, boolean accept_emptyprop, String default_value) {
        DefaultOptional<String> val = getPropertyAsString(key, accept_emptyprop);
        if (val.isPresent())
            return val;

        return DefaultOptional.withDefault(default_value);
    }

    /**
     * 指定されたキーに対応するプロパティを Integer の List として取得する。
     * 対応するプロパティが存在しない場合は値が未設定の DefaultOptional インスタンスが返る。
     *
     * @param key プロパティキー notnull
     * @param delimiter_regex リストの要素を区切るデリミタを表す正規表現 notnull
     * @param ignore_empty_element 連続するデリミタによる空要素を無視する場合は true、 false 時に空要素があった場合は NumberFormatException がスローされる
     * @return DefaultOptional インスタンス notnull
     * @throws NullPointerException key == null 時、 delimiter_regex == null 時
     * @throws IllegalArgumentException key == "" 時
     * @throws NumberFormatException 指定されたキーの値が int にパースできなかった場合。詳細メッセージにはキー名が設定される。
     */
    public DefaultOptional<List<Integer>> getPropertyAsIntList(String key, String delimiter_regex, boolean ignore_empty_element) {
        Objects.requireNonNull(key, "key should not be null");
        if (key.isEmpty())
            throw new IllegalArgumentException("key should not be empty");
        Objects.requireNonNull(delimiter_regex, "delimiter_regex should not be null");

        String val = getProperty(key);
        if (val == null)
            return DefaultOptional.empty();

        val = val.trim();

        if (val.isEmpty()) {
            List<Integer> empty = new ArrayList<>();
            return DefaultOptional.of(empty);
        }

        String[] eles = val.split(delimiter_regex);
        List<Integer> result = new ArrayList<>(eles.length);
        try {
            for (String ele : eles) {
                String trimed_ele = ele.trim();
                if (trimed_ele.isEmpty() && ignore_empty_element)
                    continue;
                result.add(Integer.parseInt(trimed_ele));
            }
        } catch (NumberFormatException e) {
            throw new NumberFormatException(key);
        }
        return DefaultOptional.of(result);
    }

    /**
     * 指定されたキーに対応するプロパティを Long の List として取得する。
     * 対応するプロパティが存在しない場合は値が未設定の DefaultOptional インスタンスが返る。
     *
     * @param key プロパティキー notnull
     * @param delimiter_regex リストの要素を区切るデリミタを表す正規表現 notnull
     * @param ignore_empty_element 連続するデリミタによる空要素を無視する場合は true、 false 時に空要素があった場合は NumberFormatException がスローされる
     * @return DefaultOptional インスタンス notnull
     * @throws NullPointerException key == null 時、 delimiter_regex == null 時
     * @throws IllegalArgumentException key == "" 時
     * @throws NumberFormatException 指定されたキーの値が long にパースできなかった場合。詳細メッセージにはキー名が設定される。
     */
    public DefaultOptional<List<Long>> getPropertyAsLongList(String key, String delimiter_regex, boolean ignore_empty_element) {
        Objects.requireNonNull(key, "key should not be null");
        if (key.isEmpty())
            throw new IllegalArgumentException("key should not be empty");
        Objects.requireNonNull(delimiter_regex, "delimiter_regex should not be null");

        String val = getProperty(key);
        if (val == null)
            return DefaultOptional.empty();

        val = val.trim();

        if (val.isEmpty()) {
            List<Long> empty = new ArrayList<>();
            return DefaultOptional.of(empty);
        }

        String[] eles = val.split(delimiter_regex);
        List<Long> result = new ArrayList<>(eles.length);
        try {
            for (String ele : eles) {
                String trimed_ele = ele.trim();
                if (trimed_ele.isEmpty() && ignore_empty_element)
                    continue;
                result.add(Long.parseLong(trimed_ele));
            }
        } catch (NumberFormatException e) {
            throw new NumberFormatException(key);
        }
        return DefaultOptional.of(result);
    }

    /**
     * 指定されたキーに対応するプロパティを Double の List として取得する。
     * 対応するプロパティが存在しない場合は値が未設定の DefaultOptional インスタンスが返る。
     *
     * @param key プロパティキー notnull
     * @param delimiter_regex リストの要素を区切るデリミタを表す正規表現 notnull
     * @param ignore_empty_element 連続するデリミタによる空要素を無視する場合は true、 false 時に空要素があった場合は NumberFormatException がスローされる
     * @return DefaultOptional インスタンス notnull
     * @throws NullPointerException key == null 時、 delimiter_regex == null 時
     * @throws IllegalArgumentException key == "" 時
     * @throws NumberFormatException 指定されたキーの値が double にパースできなかった場合。詳細メッセージにはキー名が設定される。
     */
    public DefaultOptional<List<Double>> getPropertyAsDoubleList(String key, String delimiter_regex, boolean ignore_empty_element) {
        Objects.requireNonNull(key, "key should not be null");
        if (key.isEmpty())
            throw new IllegalArgumentException("key should not be empty");
        Objects.requireNonNull(delimiter_regex, "delimiter_regex should not be null");

        String val = getProperty(key);
        if (val == null)
            return DefaultOptional.empty();

        val = val.trim();

        if (val.isEmpty()) {
            List<Double> empty = new ArrayList<>();
            return DefaultOptional.of(empty);
        }

        String[] eles = val.split(delimiter_regex);
        List<Double> result = new ArrayList<>(eles.length);
        try {
            for (String ele : eles) {
                String trimed_ele = ele.trim();
                if (trimed_ele.isEmpty() && ignore_empty_element)
                    continue;
                result.add(Double.parseDouble(trimed_ele));
            }
        } catch (NumberFormatException e) {
            throw new NumberFormatException(key);
        }
        return DefaultOptional.of(result);
    }

    /**
     * 指定されたキーに対応するプロパティを Boolean の List として取得する。
     * 対応するプロパティが存在しない場合は値が未設定の DefaultOptional インスタンスが返る。
     *
     * @param key プロパティキー notnull
     * @param delimiter_regex リストの要素を区切るデリミタを表す正規表現 notnull
     * @param ignore_empty_element 連続するデリミタによる空要素を無視する場合は true、 false 時に空要素があった場合は NumberFormatException がスローされる
     * @return DefaultOptional インスタンス notnull
     * @throws NullPointerException key == null 時、 delimiter_regex == null 時
     * @throws IllegalArgumentException key == "" 時
     * @throws NumberFormatException ignore_empty_element == false 時に指定されたキーの値に空要素が含まれていた場合。詳細メッセージにはキー名が設定される。
     */
    public DefaultOptional<List<Boolean>> getPropertyAsBooleanList(String key, String delimiter_regex, boolean ignore_empty_element) {
        Objects.requireNonNull(key, "key should not be null");
        if (key.isEmpty())
            throw new IllegalArgumentException("key should not be empty");
        Objects.requireNonNull(delimiter_regex, "delimiter_regex should not be null");

        String val = getProperty(key);
        if (val == null)
            return DefaultOptional.empty();

        val = val.trim();

        if (val.isEmpty()) {
            List<Boolean> empty = new ArrayList<>();
            return DefaultOptional.of(empty);
        }

        String[] eles = val.split(delimiter_regex);
        List<Boolean> result = new ArrayList<>(eles.length);
        for (String ele : eles) {
            String trimed_ele = ele.trim();
            if (trimed_ele.isEmpty()) {
                if (ignore_empty_element)
                    continue;
                else
                    throw new NumberFormatException(key);
            }
            result.add(Boolean.parseBoolean(trimed_ele));
        }

        return DefaultOptional.of(result);
    }

    /**
     * 指定されたキーに対応するプロパティを String の List として取得する。
     * 対応するプロパティが存在しない場合は値が未設定の DefaultOptional インスタンスが返る。
     *
     * @param key プロパティキー notnull
     * @param delimiter_regex リストの要素を区切るデリミタを表す正規表現 notnull
     * @param ignore_empty_element 連続するデリミタによる空要素を無視する場合は true、 false 時は空文字列が List の要素として格納される。
     * @return DefaultOptional インスタンス notnull
     * @throws NullPointerException key == null 時、 delimiter_regex == null 時
     * @throws IllegalArgumentException key == "" 時
     */
    public DefaultOptional<List<String>> getPropertyAsStringList(String key, String delimiter_regex, boolean ignore_empty_element) {
        Objects.requireNonNull(key, "key should not be null");
        if (key.isEmpty())
            throw new IllegalArgumentException("key should not be empty");
        Objects.requireNonNull(delimiter_regex, "delimiter_regex should not be null");

        String val = getProperty(key);
        if (val == null)
            return DefaultOptional.empty();

        val = val.trim();

        if (val.isEmpty()) {
            List<String> empty = new ArrayList<>();
            return DefaultOptional.of(empty);
        }

        String[] eles = val.split(delimiter_regex);
        List<String> result = new ArrayList<>(eles.length);
        for (String ele : eles) {
            String trimed_ele = ele.trim();
            if (trimed_ele.isEmpty() && ignore_empty_element)
                continue;
            result.add(trimed_ele);
        }

        return DefaultOptional.of(result);
    }
}
