package com.github.kmkt.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
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
}
