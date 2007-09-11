/*
 * Copyright (C) 2007 by Scott C. Gray
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, write to the Free Software Foundation, 675 Mass Ave,
 * Cambridge, MA 02139, USA.
 */
package org.sqsh;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Helper class that is capable of formatting the value of a column into
 * a string according to sqsh configuration variables.
 */
public class DataFormatter {
    
    private static final double DOUBLE_NEG =
        -12345678901239.123456D;
    private static final double DOUBLE_POS =
        -(DOUBLE_NEG);
    
    private String nullFormat = "[NULL]";
    private String intFormat = "#";
    private String doubleFormat = "###.000000";
    private int maxDoubleWidth = -1;
    private int localizedDecimal = -1;
    private String dateFormat = "dd-MMM-yyyy";
    private int maxDateWidth = -1;
    private String datetimeFormat = "MMM dd yyyy HH:mm:ss";
    private int maxDatetimeWidth = -1;
    private String timeFormat = "HH:mm";
    private int maxTimeWidth = -1;;
    
    private int maxShortWidth = -1;
    private int maxIntWidth = -1;
    private int maxLongWidth = -1;
    
    
    /**
     * Sets the string that will be used to represent a null value.
     * 
     * @param str the string that will be used to represent a null value.
     */
    public void setNull(String str) {
        
        this.nullFormat = str;
    }
    
    /**
     * Returns the representation of null.
     * 
     * @return the representation of null.
     */
    public String getNull() {
        
        return nullFormat;
    }
    
    /**
     * Returns the number of characters required to display a NULL.
     * 
     * @return the number of characters required to display a NULL.
     */
    public int getNullWidth() {
        
        return nullFormat.length();
    }
    
    /**
     * @return the dateFormat
     */
    public String getDateFormat () {
    
        return dateFormat;
    }
    
    /**
     * @param dateFormat the dateFormat to set
     */
    public void setDateFormat (String dateFormat) {
    
        this.dateFormat = dateFormat;
        maxDateWidth = -1;
    }
    
    /**
     * @return the timeFormat
     */
    public String getTimeFormat () {
    
        return timeFormat;
    }
    
    /**
     * @param dateFormat the dateFormat to set
     */
    public void setTimeFormat (String timeFormat) {
    
        this.timeFormat = timeFormat;
        maxTimeWidth = -1;
    }
    
    /**
     * @return the dateFormat
     */
    public String getDatetimeFormat () {
    
        return datetimeFormat;
    }
    
    /**
     * @param dateFormat the dateFormat to set
     */
    public void setDatetimeFormat (String datetimeFormat) {
    
        this.datetimeFormat = datetimeFormat;
        maxDatetimeWidth = -1;
    }
    
    public void setDoubleFormat (String doubleFormat) {
    
        this.doubleFormat = doubleFormat;
        maxDoubleWidth = -1;
        localizedDecimal = -1;
    }
    
    /**
     * Returns the double format.
     * @return the double format
     */
    public String getDoubleFormat() {
        
        return doubleFormat;
    }
    
    /**
     * @return the intFormat
     */
    public String getIntFormat () {
    
        return intFormat;
    }
    
    /**
     * @param intFormat the intFormat to set
     */
    public void setIntFormat (String intFormat) {
    
        maxShortWidth = -1;
        maxIntWidth = -1;
        maxLongWidth = -1;
        
        this.intFormat = intFormat;
    }
    
    /**
     * Formats a string for display.
     * 
     * @param value The value to be formatted.
     * @return The formatted value.
     */
    public String formatString (String value) {
        
        return value;
    }
    
    /**
     * Formats a boolean.
     * 
     * @param value The value to format
     * @return The formatted value.
     */
    public String formatBoolean (boolean value) {
        
        return Boolean.toString(value);
    }
    
    /**
     * Returns the number of characters required to display the
     * widest possible boolean.
     * 
     * @return Number of characters required to display the widest
     *  possible boolean.
     */
    public int getBooleanWidth() {
        
        int trueWidth = Boolean.toString(true).length();
        int falseWidth = Boolean.toString(false).length();
        
        return (trueWidth > falseWidth ? trueWidth : falseWidth);
    }
    
    /**
     * Formats a byte.
     * 
     * @param value The value to format
     * @return The formatted value.
     */
    public String formatByte (byte value) {
        
        byte []bytes = new byte[1];
        bytes[0] = value;
        
        return formatBytes(bytes);
    }
    
    /**
     * Returns the number of characters required to display the
     * widest possible byte.
     * 
     * @return Number of characters required to display the widest
     *  possible byte.
     */
    public int getByteWidth() {
        
        return 4; /* 0xff */
    }
    
    /**
     * Formats a sequence of bytes as a big hex string.
     * 
     * @param session The session that drives the formatting.
     * @param value The value to be formatted.
     * @return The formatted value.
     */
    public String formatBytes (byte []bytes) {
        
        StringBuilder sb = new StringBuilder(2 + (bytes.length * 2));
        byte ch;
        
        sb.append("0x");
        
        String hexDigits[] = {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", 
            "a", "b", "c", "d", "e", "f"
        };
        
        for (int i = 0; i < bytes.length; i++) {

            ch = (byte) (bytes[i] & 0xF0);
            ch = (byte) (ch >>> 4);
            ch = (byte) (ch & 0x0F);
            
            sb.append(hexDigits[(int) ch]);
            ch = (byte) (bytes[i] & 0x0F);

            sb.append(hexDigits[(int) ch]);
        }
        
        return sb.toString();
    }
    
    /**
     * Returns the number of characters required to display a sequence
     * of bytes nBytes long.
     * 
     * @param nBytes The number of bytes that need to be displayed.
     * @return Number of characters.
     */
    public int getBytesWidth(int nBytes) {
        
        return 2 + (nBytes * 2);
    }
    
    
    /**
     * Formats a short.
     * 
     * @param value The value to format.
     * @return 
     */
    public String formatShort (short value) {
        
        return formatLong((long) value);
    }
    
    /**
     * Returns the maximum number of characters required to display 
     * a short value.
     * 
     * @return The maximum number of characters required to display 
     * a short value.
     */
    public int getShortWidth() {
        
        if (maxShortWidth != -1) {
            
            return maxShortWidth;
        }
        
        int negWidth = formatShort(Short.MIN_VALUE).length();
        int posWidth = formatShort(Short.MAX_VALUE).length();
        
        maxShortWidth = (negWidth > posWidth) ? negWidth : posWidth;
        return maxShortWidth;
    }
    
    /**
     * Formats an integer.
     * 
     * @param value The value to format.
     * @return 
     */
    public String formatInt (int value) {
        
        return formatLong((long) value);
    }
    
    /**
     * Returns the maximum number of characters required to display 
     * a short value.
     * 
     * @return The maximum number of characters required to display 
     * a short value.
     */
    public int getIntWidth() {
        
        if (maxIntWidth != -1) {
            
            return maxIntWidth;
        }
        
        int negWidth = formatInt(Integer.MAX_VALUE).length();
        int posWidth = formatInt(Integer.MIN_VALUE).length();
        
        maxIntWidth = (negWidth > posWidth) ? negWidth : posWidth;
        return maxIntWidth;
    }
    
    /**
     * Formats a long for display.
     * 
     * @param session The session that drives the formatting.
     * @param value The value to be formatted.
     * @return The formatted value.
     */
    public String formatLong (long value) {
        
        NumberFormat fmt;
        if (intFormat == null || intFormat.length() == 0) {
            
            fmt = NumberFormat.getInstance();
        }
        else {
            
            fmt = new DecimalFormat(intFormat);
        }
        
        return fmt.format(value);
    }
    
    /**
     * Formats a BigInteger for display. This uses the format defined
     * for the intFormat.
     * 
     * @param value  The value to format.
     * @return The formatted number.
     */
    public String formatBigInteger(BigInteger value) {
        
        NumberFormat fmt;
        if (intFormat == null || intFormat.length() == 0) {
            
            fmt = NumberFormat.getInstance();
        }
        else {
            
            fmt = new DecimalFormat(intFormat);
        }
        
        return fmt.format(value);
    }
    
    /**
     * Returns the size of a formatted BigInteger.
     * 
     * @param precision The precision of the integer.
     * @return The resulting value.
     */
    public int getBigIntegerWidth(int precision) {
        
        StringBuilder val = new StringBuilder(precision+1);
        
        val.append("-");
        for (int i = 0; i < precision; i++) {
            
            val.append("9");
        }
        
        return formatBigInteger(new BigInteger(val.toString())).length();
    }
    
    /**
     * This method is used as part of the BigDecimal formatting process
     * to determine what the localized decimal point is.
     * @return The localized decimal point.
     */
    private char getLocalizedDecimal() {
        
        if (localizedDecimal != -1) {
            
            return (char) localizedDecimal;
        }
        
        double val = 3.2;
        String valString = formatDouble(val);
        int idx = valString.indexOf("3");
        if (idx < 0 || idx == valString.length()) {
            
            localizedDecimal = '.';
        }
        
        char ch = valString.charAt(idx + 1);
        if (Character.isDigit(ch)) {
            
            localizedDecimal = '.';
        }
        
        return (char) localizedDecimal;
    }
    
    /**
     * Formats a BigDecimal for display. This uses the format defined
     * for the intFormat.
     * 
     * @param value  The value to format.
     * @return The formatted number.
     */
    public String formatBigDecimal(BigDecimal value) {
        
        NumberFormat fmt;
        if (intFormat == null || doubleFormat.length() == 0) {
            
            fmt = NumberFormat.getInstance();
        }
        else {
            
            fmt = new DecimalFormat(doubleFormat);
        }
        
        return fmt.format(value);
    }
    
    /**
     * Returns the size of a formatted BigInteger.
     * 
     * @param precision The precision of the integer.
     * @return The resulting value.
     */
    public int getBigDecimalWidth(int precision, int scale) {
        
        StringBuilder val = new StringBuilder(precision+2);
        
        val.append("-");
        for (int i = 0; i < precision; i++) {
            
            if (i == (precision - scale)) {
                
                val.append('.');
            }
            
            val.append("9");
        }
        
        return formatBigDecimal(new BigDecimal(val.toString())).length();
    }
    
    /**
     * Returns the maximum number of characters required to display 
     * a long value.
     * 
     * @return The maximum number of characters required to display 
     * a long value.
     */
    public int getLongWidth() {
        
        if (maxLongWidth != -1) {
            
            return maxLongWidth;
        }
        
        int negWidth = formatLong(Long.MAX_VALUE).length();
        int posWidth = formatLong(Long.MIN_VALUE).length();
        
        maxLongWidth = (negWidth > posWidth) ? negWidth : posWidth;
        return maxLongWidth;
    }
    
    /**
     * Formats a double for display.
     * 
     * @param value The value to be formatted.
     * @return The formatted value.
     */
    public String formatDouble (double value) {
        
        NumberFormat fmt;
        if (doubleFormat == null || doubleFormat.length() == 0) {
            
            fmt = NumberFormat.getInstance();
        }
        else {
            
            fmt = new DecimalFormat(doubleFormat);
        }
        
        return fmt.format(value);
    }
    
    /**
     * Returns the maximum number of characters required to display 
     * a double value.
     * 
     * @return The maximum number of characters required to display 
     * a long value.
     */
    public int getDoubleWidth() {
        
        if (maxDoubleWidth != -1) {
            
            return maxDoubleWidth;
        }
        
        int negWidth = formatDouble(DOUBLE_NEG).length();
        int posWidth = formatDouble(DOUBLE_POS).length();
        
        maxDoubleWidth = (negWidth > posWidth) ? negWidth : posWidth;
        return maxDoubleWidth;
    }
    
    /**
     * Formats a date for display.
     * 
     * @param value The value to be formatted.
     * @return The formatted value.
     */
    public String formatDate (Date value) {
        
        return formatDate(dateFormat, value);
    }
    
    /**
     * Returns the maximum number of characters that would be required to
     * display a date string.
     * 
     * @return the maximum number of characters that would be required to
     *  to display a date.
     */
    public int getDateWidth() {
        
        if (maxDateWidth == -1) {
            
            maxDateWidth = getMaxDateWidth(dateFormat);
        }
        
        return maxDateWidth;
    }
    
    /**
     * Formats a date for display.
     * 
     * @param value The value to be formatted.
     * @return The formatted value.
     */
    public String formatDatetime (Date value) {
        
        return formatDate(datetimeFormat, value);
    }
    
    /**
     * Returns the maximum number of characters that would be required to
     * display a date string.
     * 
     * @return the maximum number of characters that would be required to
     *  to display a date.
     */
    public int getDatetimeWidth() {
        
        if (maxDatetimeWidth == -1) {
            
            maxDatetimeWidth = getMaxDateWidth(datetimeFormat);
        }
        
        return maxDatetimeWidth;
    }
    
    /**
     * Formats a time for display.
     * 
     * @param value The value to be formatted.
     * @return The formatted value.
     */
    public String formatTime (Date value) {
        
        return formatDate(timeFormat, value);
    }
    
    /**
     * Returns the maximum number of characters that would be required to
     * display a time string.
     * 
     * @return the maximum number of characters that would be required to
     *  to display a time.
     */
    public int getTimeWidth() {
        
        if (maxTimeWidth == -1) {
            
            maxTimeWidth = getMaxDateWidth(timeFormat);
        }
        
        return maxTimeWidth;
    }
    
    /**
     * Used internally to determine the maximum amount of space required by
     * a specific date/time/datetime format.
     * @param format The format to test.
     * @return The maximum width.
     */
    private int getMaxDateWidth(String format) {
        
        Calendar cal = Calendar.getInstance();
        int maxWidth = 0;
        
        
        /*
         * The following will attempt to cover all of the months in the year
         * (on the assumption the current formatter includes the month name)
         * as well as all of the days of the week and double-digit day of month
         * numbers.
         */
        for (int month = 1; month <= 12; month++) {
            
            for (int dayOfMonth = 20; dayOfMonth < 28; dayOfMonth++) {
                
                cal.set(2007, month, dayOfMonth, 23, 59, 59);
                
                int width = formatDate(format, cal.getTime()).length();
                if (width > maxWidth) {
                    
                    maxWidth = width;
                }
            }
        }
        
        return maxWidth;
    }
    
    /**
     * Used by all date/time/datetime formatters.
     * @param format The format string.
     * @param date The date.
     * @return The formatted date.
     */
    private String formatDate(String format, Date date) {
        
        DateFormat fmt;
        if (format == null || format.length() == 0) {
            
            fmt = DateFormat.getInstance();
        }
        else {
            
            fmt = new SimpleDateFormat(format);
        }
        
        return fmt.format(date);
    }
    
    private static int toHex (int v) {
        
        if (v < 16) {
            
            return ('0' +  v);
        }
        
        return 'a' + (v - 16);
    }
}
