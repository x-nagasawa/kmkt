package test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.github.kmkt.util.DefaultOptional;
import com.github.kmkt.util.DefaultOptionalBoolean;
import com.github.kmkt.util.DefaultOptionalDouble;
import com.github.kmkt.util.DefaultOptionalInt;
import com.github.kmkt.util.PrefixedProperties;

import org.junit.Test;


public class PrefixedPropertiesTest {

    @Test
    public void testPrefixedAccess() {
        Properties def = new Properties();
        def.put("a.b.c", "a.b.c");
        def.put("a.c.x", "a.x.x");
        def.put("b.a.c", "b.a.c");
        def.put("b.c.x", "b.x.x");
        PrefixedProperties p = new PrefixedProperties(def);
        System.out.println(p.getKeys("a"));
        System.out.println(p.getNextRankKeys("b"));
    }

    @Test
    public void testGetProperty(){
        PrefixedProperties pp = new PrefixedProperties();
        String testproperty =
                  "a= -121\n"
                + "b=1.2\n"
                + "c=true \n"
                + "d=1, 2, 3, 4\n";

        try (Reader sr = new StringReader(testproperty)) {
            pp.load(sr);

            DefaultOptionalInt a = pp.getPropertyAsInt("x");
            assertFalse(a.isPresent());

            a = pp.getPropertyAsInt("x", 1);
            assertTrue(a.isPresent());
            assertTrue(a.isDefault());
            assertEquals(1, a.getAsInt());

            a = pp.getPropertyAsInt("a", 1);
            assertTrue(a.isPresent());
            assertFalse(a.isDefault());
            assertEquals(-121, a.getAsInt());

            DefaultOptionalDouble b = pp.getPropertyAsDouble("x");
            assertFalse(b.isPresent());

            b = pp.getPropertyAsDouble("x", 2.2);
            assertTrue(b.isPresent());
            assertTrue(b.isDefault());
            assertEquals(2.2, b.getAsDouble(), 0.1);

            b = pp.getPropertyAsDouble("b", 2.2);
            assertTrue(b.isPresent());
            assertFalse(b.isDefault());
            assertEquals(1.2, b.getAsDouble(), 0.0001);

            DefaultOptionalBoolean c = pp.getPropertyAsBoolean("x");
            assertFalse(c.isPresent());

            c = pp.getPropertyAsBoolean("x", false);
            assertTrue(c.isPresent());
            assertTrue(c.isDefault());
            assertEquals(false, c.getAsBoolean());

            c = pp.getPropertyAsBoolean("c", false);
            assertTrue(c.isPresent());
            assertFalse(c.isDefault());
            assertEquals(true, c.getAsBoolean());

            DefaultOptional<List<Integer>> d = pp.getPropertyAsIntList("x", ",", true);
            assertFalse(d.isPresent());

            d = pp.getPropertyAsIntList("d", ",", true);
            assertTrue(d.isPresent());
            assertFalse(d.isDefault());
            List<Integer> except = new ArrayList<Integer>() {{ add(1); add (2); add(3); add(4); }};
            List<Integer> actual = d.get();
            assertEquals(except.size(), actual.size());
            for (int i = 0 ; i < except.size(); i++) {
                assertEquals(except.get(i), actual.get(i));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
