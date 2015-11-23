//#condition MUJMAIL_SEARCH
/*
MujMail - Simple mail client for J2ME
Copyright (C) 2008 David Hauzar <david.hauzar.mujmail@gmail.com>

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package mujmail.search;

import java.util.Calendar;

/**
 * Represents date in precision of days.
 *
 * @author David Hauzar
 */
public class Date {
    private static final boolean DEBUG = false;

    private static final long MILISECONDS_A_DAY = 86400000;
    
    /** The minimum date. */
    public static final Date MINIMUM_DATE = new MinimumDate();
    /** The maximum date. */
    public static final Date MAXIMUM_DATE = new MaximumDate();
    
    private final int year;
    private final int month;
    private final int day;
    private long time = -1;

    /**
     * Creates new instance of Date object.
     * @param year the year of this date.
     * @param month the mont of this date.
     * @param day the day of this date.
     */
    public Date(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }
    
    /**
     * Creates new instance of Date object.
     * @param time the number of milliseconds since the standard base time known 
     *  as "the epoch", namely January 1, 1970, 00:00:00 GMT.
     */
    public Date(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new java.util.Date(time));
        
        day = calendar.get(Calendar.DAY_OF_MONTH);
        // TODO: verify
        month = calendar.get(Calendar.MONTH) + 1;
        year = calendar.get(Calendar.YEAR);
//        
//        
//        // converts the time to format "Tue, 28 Nov 17:00:05 2006"
//        String dateStr = new java.util.Date(time).toString();
//        
//        // parses the information from dateStr
//        String dayNumber = dateStr.substring(8, 10);
//        String monthName = dateStr.substring(3, 7);
//        
//        day = Integer.parseInt(dayNumber);
//        month = getMonthNumber(monthName);
//        if (dateStr.length() >= 33) { //has timezone info and time shift (nokia 7500 has this)
//            String yearNumber = dateStr.substring(29); 
//            year = Integer.parseInt(yearNumber);
//        } else if (dateStr.length() >= 27) { //has timezone info
//            String yearNumber = dateStr.substring(23);
//            year = Integer.parseInt(yearNumber);
//        } else {
//            String yearNumber = dateStr.substring(20);
//            year = Integer.parseInt(yearNumber);
//        }
    }
    
    /**
     * Verify whether the date represented by paremeters exists.
     *
     * @param day the day in given month of given year
     * @param month the mont in given year
     * @param year given year
     * @return true if the date represented by parameters exists.
     */
    private static boolean dateExists(int day, int month, int year) {
        try {
            Calendar calendar = Calendar.getInstance();
            if (DEBUG) System.out.println("Year" + year);
            calendar.set(Calendar.YEAR, year);
            if (DEBUG) System.out.println("Month" + month);
            calendar.set(Calendar.MONTH, month - 1);
            if (DEBUG) System.out.println("Day" + day);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.getTime();
        } catch (Exception e) {
            if (DEBUG) System.out.println("Exception " + e);
            return false;
        }
        
        return true;
    }
    
    /**
     * Creates the instance of this class repersenting the same date given 
     * 
     * @param date date with format "ddmmyy"
     * @param dateIfCannotParse date to which set the date if it is not possible 
     *  to parse string date
     * @return new date according to the string date or object dateIfCannotParse
     *  if it was not possible to parse the date.
     */
    public static Date parseDate(String date, Date dateIfCannotParse) {
        if (date.length() != 6) return dateIfCannotParse;
        
        int day = Integer.parseInt(date.substring(0, 2));
        int month = Integer.parseInt(date.substring(2, 4));
        int year = Integer.parseInt(date.substring(4, 6)) + 2000;
        
        if (!dateExists(day, month, year)) {
            return dateIfCannotParse;
        }
        
        return new Date(year, month, day);
    }
    
    
    /**
     * Compares this object with with the specified date object for order. 
     * Returns a negative integer, zero, or a positive integer as this object is 
     * less than, equal to, or greater than the specified object.
     * 
     * @param date the date to be compared
     * @return a negative integer, zero, or a positive integer as this object is 
     *  less than, equal to, or greater than the parameter.
     */
    public int compareTo(Date date) {
       if (year < date.year) return -1;
       if (year > date.year) return 1;
       
       if (month < date.month) return -1;
       if (month > date.month) return 1;
       
       if (day < date.day) return -1;
       if (day > date.day) return 1;
       
       return 0;
    }
    
    /**
     * Compares this object with with the date specified with unix timestamp for 
     * order. 
     * Returns a negative integer, zero, or a positive integer as this object is 
     * less than, equal to, or greater than the specified object.
     * 
     * @param time the unix timestamp representing the date to be compared
     * @return a negative integer, zero, or a positive integer as this object is 
     *  less than, equal to, or greater than the parameter.
     */
    public int compareTo(long time) {
        long thisTimeFrom = getTime();
        // TODO: +- 1 error?
        long thisTimeTo = thisTimeFrom + MILISECONDS_A_DAY-1;
        if (time < thisTimeFrom) return 1;
        else if (time > thisTimeTo) return -1;
        
        return 0;
    }
    
    /**
     * Gets the UNIX timestamtp - the time in miliseconds from the beginning 
     * of epoch.
     * Note that the time is measured in the beginning of the day represented by this 
     * date.
     * @return the unix timestamp.
     */
    public long getTime() {
        if (time == -1) {
            toString();
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.MONTH, month - 1);
            calendar.set(Calendar.YEAR, year);

            time = calendar.getTime().getTime();
        }
        
        return time;
    }

    public String toString() {
        return new Integer(day).toString() + 
                new Integer(month).toString() + 
                new Integer(year).toString();
    }

    /**
     * Represents maximum date.
     */
    private static class MaximumDate extends Date {

        public MaximumDate() {
            super(Integer.MAX_VALUE, 12, 31);
        }

        public String toString() {
            return "maximum date";
        }

        public int compareTo(Date date) {
            return 1;
        }

        public int compareTo(long time) {
            return 1;
        }
    }

    /**
     * Representes minimum date.
     */
    private static class MinimumDate extends Date {

        public MinimumDate() {
            super(Integer.MIN_VALUE, 1, 1);
        }
        
        public String toString() {
            return "minimum date";
        }
        
        public int compareTo(Date date) {
            return -1;
        }

        public int compareTo(long time) {
            return -1;
        }
        
    }
}
