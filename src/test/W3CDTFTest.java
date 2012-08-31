package test;

import java.util.Calendar;

import com.github.kmkt.util.W3CDTF;


public class W3CDTFTest {

    public static void main(String[] args) {
        Calendar c; //= parse("1994-11-05T08:15:30-05:00");
/*
        System.out.println(format(c, YEAR));
        System.out.println(format(c, YEAR_MONTH));
        System.out.println(format(c, FULL_DATE));
        System.out.println(format(c, DATE_HOURS_MINUTE));
        System.out.println(format(c, DATE_HOURS_MINUTE_SECOND));
        System.out.println(format(c, FULL));

        c = parse("1994-11-05T08:15:30.50Z");
        System.out.println(format(c, YEAR));
        System.out.println(format(c, YEAR_MONTH));
        System.out.println(format(c, FULL_DATE));
        System.out.println(format(c, DATE_HOURS_MINUTE));
        System.out.println(format(c, DATE_HOURS_MINUTE_SECOND));
        System.out.println(format(c, FULL));
*/
        c = W3CDTF.parse("1994");
        System.out.println(W3CDTF.format(c, W3CDTF.FULL));
        c = W3CDTF.parse("1994-12");
        System.out.println(W3CDTF.format(c, W3CDTF.FULL));
        c = W3CDTF.parse("1994-12-29");
        System.out.println(W3CDTF.format(c, W3CDTF.FULL));
        c = W3CDTF.parse("1994-12-29T12:25Z");
        System.out.println(W3CDTF.format(c, W3CDTF.FULL));
        c = W3CDTF.parse("1994-12-29T12:25+01:00");
        System.out.println(W3CDTF.format(c, W3CDTF.FULL));
        c = W3CDTF.parse("1994-12-29T12:25-01:00");
        System.out.println(W3CDTF.format(c, W3CDTF.FULL));
        c = W3CDTF.parse("1994-12-29T12:25:30Z");
        System.out.println(W3CDTF.format(c, W3CDTF.FULL));
        c = W3CDTF.parse("1994-12-29T12:25:30+01:00");
        System.out.println(W3CDTF.format(c, W3CDTF.FULL));
        c = W3CDTF.parse("1994-12-29T12:25:30-01:00");
        System.out.println(W3CDTF.format(c, W3CDTF.FULL));
        c = W3CDTF.parse("1994-12-29T12:25:30.1Z");
        System.out.println(W3CDTF.format(c, W3CDTF.FULL));
        c = W3CDTF.parse("1994-12-29T12:25:30.1+01:00");
        System.out.println(W3CDTF.format(c, W3CDTF.FULL));
        c = W3CDTF.parse("1994-12-29T12:25:30.1-01:00");
        System.out.println(W3CDTF.format(c, W3CDTF.FULL));
        c = W3CDTF.parse("1994-12-29T12:25:30.123Z");
        System.out.println(W3CDTF.format(c, W3CDTF.FULL));
        c = W3CDTF.parse("1994-12-29T12:25:30.123+01:00");
        System.out.println(W3CDTF.format(c, W3CDTF.FULL));
        c = W3CDTF.parse("1994-12-29T12:25:30.123-01:00");
        System.out.println(W3CDTF.format(c, W3CDTF.FULL));
        c = W3CDTF.parse("1994-12-29T12:25:30.12345Z");
        System.out.println(W3CDTF.format(c, W3CDTF.FULL));
        c = W3CDTF.parse("1994-12-29T12:25:30.12345+01:00");
        System.out.println(W3CDTF.format(c, W3CDTF.FULL));
        c = W3CDTF.parse("1994-12-29T12:25:30.12345-01:00");
        System.out.println(W3CDTF.format(c, W3CDTF.FULL));
    }


}
