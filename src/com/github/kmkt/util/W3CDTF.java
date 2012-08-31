package com.github.kmkt.util;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * W3CDTF 形式の日時フォーマットをパース・出力するクラス。
 * 
 * http://www.kanzaki.com/docs/html/dtf.html#w3cdtf より
 * (1) 年のみ
 * YYYY（例：2001）
 * (2) 年月
 * YYYY-MM（例：2001-08）
 * (3) 年月日
 * YYYY-MM-DD（例：2001-08-02）
 * (4) 年月日および時分
 * YYYY-MM-DDThh:mmTZD（例：2001-08-02T10:45+09:00）
 * (5) 年月日および時分秒
 * YYYY-MM-DDThh:mm:ssTZD（例：2001-08-02T10:45:23+09:00）
 * (6) 年月日および時分秒および小数部分
 * YYYY-MM-DDThh:mm:ss.sTZD（例：2001-08-02T10:45:23.5+09:00）
 */
public class W3CDTF {
    private static final Pattern W3CDTF_PATTERN = Pattern.compile("(\\d{4})(?:-(\\d{2})(?:-(\\d{2})(?:T(\\d{2}):(\\d{2})(?::(\\d{2})(?:\\.(\\d+))?)?(?:(?:([\\+-])(\\d{2})\\:(\\d{2}))|(Z)))?)?)?");

    // 文字列形式
    public static final int YEAR = 1;   // (1) 年のみ
    public static final int YEAR_MONTH = 2; // (2) 年月
    public static final int FULL_DATE = 3;  // (3) 年月日
    public static final int DATE_HOURS_MINUTE = 4;  // (4) 年月日および時分
    public static final int DATE_HOURS_MINUTE_SECOND = 5;   // (5) 年月日および時分秒
    public static final int FULL = 6;   // (6) 年月日および時分秒および小数部分

    /**
     * W3CDTF 形式の日時フォーマットをパースする。
     * 
     * @param w3cdtf W3CDTF 形式の文字列
     * @return パースされた Calendar
     */
    public static Calendar parse(String w3cdtf) {
        if (w3cdtf == null || "".equals(w3cdtf))
            throw new IllegalArgumentException("w3cdtf should not be null or empty.");

        Matcher m = W3CDTF_PATTERN.matcher(w3cdtf);
        if (!m.matches())
            return null;

        Calendar result = Calendar.getInstance();
        result.clear();
        result.set(Calendar.YEAR, Integer.parseInt(m.group(1)));
        result.set(Calendar.MONTH, m.group(2) != null ? Integer.parseInt(m.group(2)) - 1: 0);
        result.set(Calendar.DAY_OF_MONTH, m.group(3) != null ? Integer.parseInt(m.group(3)) : 1);
        if ((m.group(8) != null && m.group(9) != null && m.group(10) != null) || "Z".equals(m.group(11))) {
            result.set(Calendar.HOUR_OF_DAY, m.group(4) != null ? Integer.parseInt(m.group(4)) : 0);
            result.set(Calendar.MINUTE, m.group(5) != null ? Integer.parseInt(m.group(5)) : 0);
            result.set(Calendar.SECOND, m.group(6) != null ? Integer.parseInt(m.group(6)) : 0);
            if (m.group(7) != null) {
                result.set(Calendar.MILLISECOND, (int) (Double.parseDouble("0." + m.group(7)) * 1000.0));
            } else {
                result.set(Calendar.MILLISECOND, 0);
            }
            if ("Z".equals(m.group(11))) {
                TimeZone tz = TimeZone.getTimeZone("Europ/London");
                tz.setRawOffset(0);
                result.setTimeZone(tz);
            } else {
                TimeZone tz = TimeZone.getTimeZone("Europ/London");
                int offset_hour = Integer.parseInt(m.group(9));
                int offset_min = Integer.parseInt(m.group(10));
                int offset = (offset_hour*60+offset_min) * 60 * 1000;
                if ("-".equals(m.group(8)))
                    offset = -offset;
                tz.setRawOffset(offset);
                result.setTimeZone(tz);
            }
        }

        return result;
    }

    /**
     * Date から W3CDTF形式文字列を得る。
     * @param date 変換元 Date オブジェクト
     * @param format 出力形式
     * @return W3CDTF形式文字列
     */
    public static String format(Date date, int format) {
        return format(date, TimeZone.getDefault(), format);
    }

    /**
     * Date から W3CDTF形式文字列を得る。
     * @param date 変換元 Date オブジェクト
     * @param tz date のタイムゾーン
     * @param format 出力形式
     * @return W3CDTF形式文字列
     */
    public static String format(Date date, TimeZone tz, int format) {
        if (date == null)
            throw new IllegalArgumentException("date should not be null.");
        if (tz == null)
            throw new IllegalArgumentException("tz should not be null.");
        if (format < YEAR || FULL < format)
            throw new IllegalArgumentException("invalid format value.");

        Calendar c = Calendar.getInstance(tz);
        c.setTime(date);
        return format(c, format);
    }

    /**
     * Calendar から W3CDTF形式文字列を得る。
     * @param calendar 変換元 Calendar オブジェクト
     * @param format 出力形式
     * @return W3CDTF形式文字列
     */
    public static String format(Calendar calendar, int format) {
        if (calendar == null)
            throw new IllegalArgumentException("calendar should not be null.");
        if (format < YEAR || FULL < format)
            throw new IllegalArgumentException("invalid format value.");

        StringBuilder buf = new StringBuilder();
        if (YEAR <= format) {
            buf.append(String.format("%04d", calendar.get(Calendar.YEAR)));
        }
        if (YEAR_MONTH <= format) {
            buf.append(String.format("-%02d", calendar.get(Calendar.MONTH) + 1));
        }
        if (FULL_DATE <= format) {
            buf.append(String.format("-%02d", calendar.get(Calendar.DAY_OF_MONTH)));
        }
        if (DATE_HOURS_MINUTE <= format) {
            buf.append(String.format("T%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE)));
        }
        if (DATE_HOURS_MINUTE_SECOND <= format) {
            buf.append(String.format(":%02d", calendar.get(Calendar.SECOND)));
        }
        if (FULL == format) {
            buf.append(String.format(".%03d", calendar.get(Calendar.MILLISECOND)));
        }
        if (DATE_HOURS_MINUTE <= format) {
            TimeZone tz = calendar.getTimeZone();
            if (tz.getRawOffset() == 0) {
                buf.append("Z");
            } else {
                int offset = tz.getRawOffset() / (60*1000);
                if (0 < offset) {
                    buf.append(String.format("+%02d:%02d", offset/60, offset%60));
                } else {
                    offset = -offset;
                    buf.append(String.format("-%02d:%02d", offset/60, offset%60));
                }
            }
        }
        return buf.toString();
    }
}
