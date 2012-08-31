package test;

import java.util.Properties;

import com.github.kmkt.util.PrefixedProperties;

public class PrefixedPropertiesTest {
    public static void main(String[] args) {
        Properties def = new Properties();
        def.put("a.b.c", "a.b.c");
        def.put("a.c.x", "a.x.x");
        def.put("b.a.c", "b.a.c");
        def.put("b.c.x", "b.x.x");
        PrefixedProperties p = new PrefixedProperties(def);
        System.out.println(p.getKeys("a"));
    }
}
