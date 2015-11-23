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

/**
 * Represents date interval.
 * 
 * @author David Hauzar
 */
public class DateInterval {
    /** Represents not specified date interval. */
    public static final DateInterval NOT_SPECIFIED = new DateInterval(Date.MINIMUM_DATE, Date.MAXIMUM_DATE);
    
    private final Date from;
    private final Date to;

    /**
     * Creates the instance of DateInterval.
     * 
     * @param from the start of the interval
     * @param to the end of the interval
     */
    private DateInterval(Date from, Date to) {
        this.from = from;
        this.to = to;
    }
    
    public static DateInterval createDateInterval(Date from, Date to) {
        if (from.compareTo(to) > 1) {
            return NOT_SPECIFIED;
        }
        
        return new DateInterval(from, to);
    }
    
    /**
     * Parses date interval from strings.
     * @param dateFrom the start of the interval in format "ddmmyy"
     * @param dateTo the end of the interval in format "ddmmyy"
     * @return parsed date interval. If it is not possible to parse dateFrom,
     *  the beggining of the interval is Date.MINIMUM_DATE.
     *  If it is not possible to parse dateTo, the end of the interval is 
     *  Date.MAXIMUM_DATE.
     *  If it is not possible to parse dateFrom and dateTo, it returns
     *  DateInterval.NOT_SPECIFIED.
     */
    public static DateInterval parseDateInterval(String dateFrom, String dateTo) {
        Date from = Date.parseDate(dateFrom, Date.MINIMUM_DATE);
        Date to = Date.parseDate(dateTo, Date.MAXIMUM_DATE);
        
        if (from == Date.MINIMUM_DATE && to == Date.MAXIMUM_DATE) {
            return NOT_SPECIFIED;
        }
        
        return new DateInterval(from, to);
    }

    public String toString() {
        return "From: " + from + "; To: " + to;
    }
    
    /**
     * Determine whether this interval contains given date.
     * @param date the date
     * @return true if this interval contains given date
     */
    public boolean contains(Date date) {
        if (from.compareTo(date) <= 0 && to.compareTo(date) >= 0) return true;
        else return false;
    }
    
    /**
     * Determine whether this interval contains given date.
     * @param time the date given by unix timestamp
     * @return true if this interval contains given date
     */
    public boolean contains(long time) {
        if (from.compareTo(time) <= 0 && to.compareTo(time) >= 0) return true;
        else return false;
    }   
}
