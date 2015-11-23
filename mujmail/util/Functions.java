package mujmail.util;

/*
MujMail - Simple mail client for J2ME
Copyright (C) 2005 Pavel Machek <pavel@ucw.cz>
Copyright (C) 2006 Nguyen Son Tung <n.sontung@gmail.com>
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

import mujmail.*;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Random;
import java.util.TimeZone;
import java.util.Vector;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.rms.RecordStore;

import mujmail.ordering.Comparator;

/**
 * Static class that provides basic common functions used in the application.
 * @author Pavel Machek
 */
public class Functions {

    private static final boolean DEBUG = false;

    //criterias for sort()
    //for headers
    public static final byte SRT_HDR_TIME = 0;
    public static final byte SRT_HDR_SUBJECT = 1;
    public static final byte SRT_HDR_TO = 2;
    public static final byte SRT_HDR_FROM = 3;
    public static final byte SRT_HDR_SIZE = 4;
    public static final byte SRT_HDR_RECORD_ID = 5;
    public static final byte SRT_HDR_MSGID = 6;
    //for address book contact
    public static final byte SRT_CNT_NAME = 16;
    //sort orders
    public static final byte SRT_ORDER_INC = 0;
    public static final byte SRT_ORDER_DEC = 1;

    /**
     * Creates vector of strings. Each string will correspond to one recipient.
     *
     * @param rcp string terminated by " *" is a string containing
     * emails separated by commas, '*' or space, there might be To: Bcc: Cc
     * name of a recipient might be enclosed in "" or not, ie:
     * To: mail@ads.com *Bcc:"someone here" <mail@ads.com>,   [manySpaces]     nextperson@here.com, *    Cc: crazy@nuts.it, Tung <Tung@there.cz>
     * @return vector of strings of recipients.
     */
    public static Vector parseRcp(String rcp) {
        Vector rcps = new Vector();
        String buff = new String(rcp);
        int sb, sc; //sb is beginning index of email address, sc is of ending		

        if (!buff.endsWith("*")) {
            buff += "*";
        }
        while (buff.length() >= 6) {
            if (buff.indexOf("<") != -1 && buff.indexOf("<") < buff.indexOf("@")) //did we skipped some email? its when buff.indexOf(">") > buff.indexOf("@")
            {
                sb = buff.indexOf("<") + 1;
            } //email begins with <
            else if (buff.indexOf(":") != -1 && buff.indexOf(":") < buff.indexOf("@")) {
                sb = buff.indexOf(":") + 1;
            } //another email after Cc: or Bcc: etc
            else {
                if (buff.charAt(0) == ',') //next email
                {
                    buff = buff.substring(1).trim();
                }
                sb = 0;      //we skipped some simple address like thisperson@here.com, *Cc: crazy@nuts.it
            }

            if (sb > 0 && buff.charAt(sb - 1) == '<') //email's terminated by >
            {
                sc = buff.indexOf(">", sb + 1);
            } else { //email's terminated by , or space*
                sc = buff.indexOf(",", sb + 1) < buff.indexOf(" *", sb + 1) ? buff.indexOf(",", sb + 1) : buff.indexOf(" *", sb + 1);
            }
            if (sc == -1) //email's terminated by space or *
            {
                sc = buff.indexOf(" ", sb + 1) == -1 ? buff.indexOf("*", sb + 1) : buff.indexOf(" ", sb + 1);
            }

            rcps.addElement(buff.substring(sb, sc).trim());
            buff = buff.substring(sc + 1).trim();
        }
        return rcps;
    }

    public static String encodeRcpNames(String rcp) {
        StringBuffer output = new StringBuffer();
        int length = rcp.length();
        char c;
        int j, i = 0;
        while (i < length) {
            c = rcp.charAt(i);
            output.append(c);
            if (c == '"') {
                j = rcp.indexOf(c, i + 1);
                if (j != -1) {
                    output.append(Decode.encodeHeaderField(rcp.substring(i + 1, j)) + '"');
                    i = j;
                }
            }
            ++i;
        }
        return output.toString();
    }

    /**
     * From given string containing email address parses just email address.
     * @param s string containing email address enclosed in brackets.
     * @return email address.
     */
    public static String emailOnly(String s) {
        if (s.indexOf("<") == -1) {
            return s;
        }
        return s.substring(s.indexOf("<") + 1, s.indexOf(">"));
    }

    public static String genID() {
        return Long.toString(System.currentTimeMillis(), 36) + "." +
                Integer.toString(Math.abs(new Random().nextInt()), 36);
    }

    /**
     * Returns unicode string size in bytes
     * 
     * @param s string to get size for
     * @return size in bytes
     */
    public static int getStringByteSize(String s) {
        return 1 + s.length() * 2;
    }

    /**
     * Gets unix time from date encoded in given string.
     * @param str a string in format Tue, 28 Nov 2006 17:00:05 [-+]hhmm
     * @return unix time
     */
    public static long getStrToLongTime(String str) {
        if (str == null) {
            return 0;
        }

        try {
            String s = str.trim().toLowerCase();
            Calendar time = Calendar.getInstance();
            if (!Character.isDigit(s.charAt(0))) {//its format is Tue, 28 Nov 2006 17:00:05 [-+]hhmm. skip the day of week part				
                s = s.substring(s.indexOf(" ")).trim();

            }

            time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(s.substring(0, 2).trim()));
            s = s.substring(s.indexOf(" ") + 1).trim();

            if (s.startsWith("jan")) {
                time.set(Calendar.MONTH, Calendar.JANUARY);
            } else if (s.startsWith("feb")) {
                time.set(Calendar.MONTH, Calendar.FEBRUARY);
            } else if (s.startsWith("mar")) {
                time.set(Calendar.MONTH, Calendar.MARCH);
            } else if (s.startsWith("apr")) {
                time.set(Calendar.MONTH, Calendar.APRIL);
            } else if (s.startsWith("may")) {
                time.set(Calendar.MONTH, Calendar.MAY);
            } else if (s.startsWith("jun")) {
                time.set(Calendar.MONTH, Calendar.JUNE);
            } else if (s.startsWith("jul")) {
                time.set(Calendar.MONTH, Calendar.JULY);
            } else if (s.startsWith("aug")) {
                time.set(Calendar.MONTH, Calendar.AUGUST);
            } else if (s.startsWith("sep")) {
                time.set(Calendar.MONTH, Calendar.SEPTEMBER);
            } else if (s.startsWith("oct")) {
                time.set(Calendar.MONTH, Calendar.OCTOBER);
            } else if (s.startsWith("nov")) {
                time.set(Calendar.MONTH, Calendar.NOVEMBER);
            } else {
                time.set(Calendar.MONTH, Calendar.DECEMBER);
            }

            s = s.substring(s.indexOf(" ") + 1).trim();
            time.set(Calendar.YEAR, Integer.parseInt(s.substring(0, 4)));

            s = s.substring(s.indexOf(" ") + 1).trim();
            time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(s.substring(0, 2)));
            time.set(Calendar.MINUTE, Integer.parseInt(s.substring(3, 5)));
            time.set(Calendar.SECOND, Integer.parseInt(s.substring(6, 8)));

            s = s.substring(8).trim();
            int offset = 0;
            if (s.indexOf("-") != -1 || s.indexOf("+") != -1) {
                int x = Math.max(s.indexOf("-"), s.indexOf("+")) + 1;
                offset = Integer.parseInt(s.substring(x, x + 2)) * 60 + Integer.parseInt(s.substring(x + 2, x + 4));
                offset *= 60 * 1000;
                if (s.charAt(x) == '-') {
                    offset = -offset;
                }
                TimeZone timezone = TimeZone.getDefault();
                offset = timezone.useDaylightTime() ? timezone.getRawOffset() - offset + 3600000 : timezone.getRawOffset() - offset;
            }
            return time.getTime().getTime() + offset;

        } catch (Exception ex) {
            return 0;
        }

    }

    /**
     * Removes html tags from given string.
     * @param text string where html tags will be removed.
     * @return text without html tags.
     */
    public static String removeTags(String text) {
        if (text == null) {
            return null;
        }
        StringBuffer newText = new StringBuffer("");
        boolean inquote = false, intag = false;
        char c;
        for (int i = 0; i < text.length(); ++i) {
            c = text.charAt(i);
            if (intag) {
                if (c == '"') {
                    inquote = !inquote;
                } else if (text.charAt(i) == '>' && !inquote) {
                    intag = false;
                }
            } else {
                if (c == '<') {
                    intag = true;
                    inquote = false;
                } else {
                    newText.append(c);
                }
            }
        }
        return newText.toString();
    }

    /**
     * Converts an integer to a string. If the result has less digits than the
     * specified length, it is left-padded with zeroes. the number must not
     * exceed 9999 and nonnegative.
     */
    public static String intToStr(int value, int length) {
        String result = Integer.toString(value);
        if (length > result.length()) {
            result = "0000".substring(4 - (length - result.length())) + result;
        }
        return result;
    }

    /**
     * Gets string representation of local time zone.
     * @return
     */
    public static String getLocalTimeZone() {
        TimeZone timezone = TimeZone.getDefault();
        int offset = timezone.useDaylightTime() ? timezone.getRawOffset() / 1000 + 3600 : timezone.getRawOffset() / 1000;
        char sign = offset >= 0 ? '+' : '-';
        offset = Math.abs(offset);
        return sign + intToStr(offset / 3600, 2) + intToStr(offset % 3600, 2);
    }

    /**
     * Causes the currently executing thread to sleep (temporarily cease execution)
     * for the specified number of milliseconds. The thread does not lose
     * ownership of any monitors.
     * @param msec the length of time to sleep in milliseconds.
     */
    public static void sleep(int msec) {
        try {
            Thread.sleep(msec);
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    /**
     * Loads icon of given name.
     * @param name the name of the ico.
     * @return the image representing the icon.
     */
    public static Image getIcon(String name) {
        String imgRoot = "/icons/";
        Image img = null;
        try {
            img = Image.createImage(imgRoot + name);
        } catch (java.io.IOException x) {
            x.printStackTrace();
        }
        return img;
    }
    
    /**
     * Make a copy of given vector.
     * @param vector the vector to be copyed
     * @return the copy of given vector.
     */
    public static Vector copyVector(Vector vector) {
        Vector newVector = new Vector(vector.size());
        for (int i = 0; i < vector.size(); i++) {
            newVector.addElement(vector.elementAt(i));
        }
        
        return newVector;
    }
    
    /**
     * Creates new vector from given enumeration.
     * @param enumeration the enumeration to be copyed.
     * @return new vector with elements from given enumeration.
     */
    public static Vector copyEnumerationToVector(Enumeration enumeration) {
        Vector newVector = new Vector();
        while (enumeration.hasMoreElements()) {
            newVector.addElement(enumeration.nextElement());
        }
        
        return newVector;
    }

    /**
     * Scales given image.
     * @param src the image to be scaled.
     * @param width the width of scaled image.
     * @param height the height of scaled image.
     * @return scaled image.
     */
    public static Image scaleImage(Image src, int width, int height) {
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        Image tmp = Image.createImage(width, srcHeight);
        Graphics g = tmp.getGraphics();

        int delta = (srcWidth << 16) / width;
        int pos = delta / 2;

        for (int x = 0; x < width; x++) {
            g.setClip(x, 0, 1, srcHeight);
            g.drawImage(src, x - (pos >> 16), 0, Graphics.LEFT | Graphics.TOP);
            pos += delta;
        }

        Image dst = Image.createImage(width, height);
        g = dst.getGraphics();

        delta = (srcHeight << 16) / height;
        pos = delta / 2;

        for (int y = 0; y < height; y++) {
            g.setClip(0, y, width, 1);
            g.drawImage(tmp, 0, y - (pos >> 16), Graphics.LEFT | Graphics.TOP);
            pos += delta;
        }

        return dst;
    }

    /**
     * Converts given string to CRLF format.
     * @param text the string that is not CRLF to be converted.
     * @return string in CRLF
     */
    public static String toCRLF(String text) {
        StringBuffer buffer = new StringBuffer(text);
        int j = text.length(), i = 0;
        while (i < j) {
            if (buffer.charAt(i) == '\r' && i + 1 < j && buffer.charAt(i + 1) != '\n') {
                buffer.insert(i + 1, '\n');
                ++i;
                ++j;
            } else if (buffer.charAt(i) == '\n') {
                buffer.insert(i, '\r');
                ++i;
                ++j;
            }
            ++i;
        }
        return buffer.toString();
    }

    public static void Ticker(String text, int begin, int xOffset, int yOffset, int maxWidth, Graphics g, int anchor) {
        int strlen = text.length();
        char c;
        if (begin >= strlen) {
            begin = 0;
        }

        for (int i = 0;; i = (i + 1) % strlen) {
            c = text.charAt((begin + i) % strlen);//imagine the text as a circle, take a char at the position	(begin+i)					
            if (xOffset + g.getFont().charWidth(c) > maxWidth) //try if it fits in
            {
                break;
            }
            g.drawChar(c, xOffset, yOffset, anchor);
            xOffset += g.getFont().charWidth(c);
        }
    }

    /**
     * Gets substring of given string that has desired width on the screen.
     * @param string the string to be cuted.
     * @param begin index in the string where is the first character of returned
     *  substring.
     * @param width desiredd width of returned substring.
     * @param g represents the screen.
     * @return the substring of given string with desired width.
     */
    static public String cutString(String string, int begin, int width, Graphics g) {
        int strWidth = 0;
        int strLength = string.length();
        char c;
        int i = begin;
        while (i < strLength) {
            c = string.charAt(i);
            strWidth += g.getFont().charWidth(c);
            if (strWidth <= width) {
                i++;
            } else {
                break;
            }
        }
        return string.substring(begin, i);
    }

    /**
     * Returns true if left object is less than right object.
     * @param left left object
     * @param right right object
     *  SRT_HDR_TIME left and right objects are headers, compare their times
     *  SRT_HDR_TO left and right objects are headers, compare their "to" fields
     *  SRT_HDR_FROM left and right objects are headers, compare their from fields
     *  SRT_HDR_SUBJECT left and right objects are headers, compare their subject fields
     *  SRT_HDR_SIZE left and right objects are headers, compare their size fields
     *  SRT_HDR_RECORD_ID left and right objects are headers, compare their record id fields
     *  SRT_HDR_MSGID left and right objects are headers, compare their message id fields
     *  SRT_CNT_NAME left and right objects are contacts in address book, compare their names.
     * @param mode describes the type of left and right object
     *
     * @return true if left object is less than right object.
     */
    static private boolean less(Object left, Object right, byte mode) {
        String l, r;
        switch (mode) {
            case SRT_HDR_TIME:
                return ((MessageHeader) left).getTime() < ((MessageHeader) right).getTime();

            case SRT_HDR_TO:
                l = Functions.emailOnly(((MessageHeader) left).getRecipients()).toLowerCase();
                r = Functions.emailOnly(((MessageHeader) right).getRecipients()).toLowerCase();
                l = l.substring(0, l.indexOf("@"));
                r = r.substring(0, r.indexOf("@"));

                if (l.charAt(0) == r.charAt(0)) {
                    return l.compareTo(r) < 0;
                } else {
                    return l.charAt(0) < r.charAt(0);
                }

            case SRT_HDR_FROM:
                l = Functions.emailOnly(((MessageHeader) left).getFrom()).toLowerCase();
                r = Functions.emailOnly(((MessageHeader) right).getFrom()).toLowerCase();
                l = l.substring(0, l.indexOf("@"));
                r = r.substring(0, r.indexOf("@"));

                if (l.charAt(0) == r.charAt(0)) {
                    return l.compareTo(r) < 0;
                } else {
                    return l.charAt(0) < r.charAt(0);
                }

            case SRT_HDR_SUBJECT:
                l = ((MessageHeader) left).getSubject().toLowerCase();
                r = ((MessageHeader) right).getSubject().toLowerCase();
                if (l.length() == 0 || r.length() == 0) {
                    return l.length() < r.length();
                }
                if (l.charAt(0) == r.charAt(0)) {
                    return l.compareTo(r) < 0;
                } else {
                    return l.charAt(0) < r.charAt(0);
                }

            case SRT_HDR_SIZE:
                return ((MessageHeader) left).getSize() < ((MessageHeader) right).getSize();

            case SRT_HDR_RECORD_ID:
                return ((MessageHeader) left).getRecordID() < ((MessageHeader) right).getRecordID();

            case SRT_HDR_MSGID:
                l = ((MessageHeader) left).getMessageID().toLowerCase();
                r = ((MessageHeader) right).getMessageID().toLowerCase();
                if (l.length() == 0 || r.length() == 0) {
                    return l.length() < r.length();
                }
                if (l.charAt(0) == r.charAt(0)) {
                    return l.compareTo(r) < 0;
                } else {
                    return l.charAt(0) < r.charAt(0);
                }

            case SRT_CNT_NAME:
                l = ((AddressBook.Contact) left).getName().toLowerCase();
                r = ((AddressBook.Contact) right).getName().toLowerCase();
                if (l.charAt(0) == r.charAt(0)) {
                    return l.compareTo(r) < 0;
                } else {
                    return l.charAt(0) < r.charAt(0);
                }
        }
        return false;
    }

    static private boolean order(boolean val, byte order) {
        return order == SRT_ORDER_INC ? val : !val;
    }

    // exchange storage[i] and storage[j]
    private static void exch(Vector storage, int i, int j) {
        Object swap = storage.elementAt(i);
        storage.setElementAt(storage.elementAt(j), i);
        storage.setElementAt(swap, j);
    }

    // shuffle the storage
    private static void shuffle(Vector vector) {
        int N = vector.size(), r;
        for (int i = 0; i < N; i++) {
            r = i + Math.abs(new Random().nextInt()) % (N - i);   // between i and N-1		    
            exch(vector, i, r);
        }
    }

    private static int partition(Vector storage, int left, int right, byte order, byte mode) {
        int i = left - 1;
        int j = right;
        Object pivot = storage.elementAt(right);
        while (true) {
            // find item on left to swap. don't go out-of-bounds
            while (order(less(storage.elementAt(++i), pivot, mode), order) && i < right) {
                ;
            }
            // find item on right to swap. 
            while (order(less(pivot, storage.elementAt(--j), mode), order) && j > left) {
                ;
            }
            // check if pointers cross
            if (i >= j) {
                break;
            }
            exch(storage, i, j); // swap two elements into place
        }
        exch(storage, i, right); // swap with partition element
        return i;
    }

    /**
     * Sorts elements in vector between positions left and right.
     * 
     * @param vector
     * @param left
     * @param right
     * @param order
     * @param mode
     */
    static public void sort(Vector vector, int left, int right, byte order, byte mode) {
        if (right <= left) {
            return;
        }
        int i = partition(vector, left, right, order, mode);
        sort(vector, left, i - 1, order, mode);
        sort(vector, i + 1, right, order, mode);
    }

    /**
     * Sorts vector.
     * 
     * @param vector
     * @param order
     * @param mode
     */
    //This is sort function that sorts a vector by given order (Funtioncs.SRT_ORDER_INC, Funtioncs.SRT_ORDER_DEC) and sort mode Functions.SRT_HDR_TIME...
    //The code is modified version of Princeton University code at http://www.cs.princeton.edu/introcs/42sort/QuickSort.java.html
    static public void sort(Vector vector, byte order, byte mode) {
        shuffle(vector);
        sort(vector, 0, vector.size() - 1, order, mode);
    }

    /**
     * Opens given record store.
     * @param file the string that describes the record store file.
     * @param createIfNeccessary true if record store should be created if it
     *  does not exists. yet.
     * @return opened recordstor.
     * @throws mujmail.MyException if the record store cannot be opened.
     */
    static public RecordStore openRecordStore(String file, boolean createIfNeccessary) throws MyException {
        try {
              if (DEBUG) System.out.println("DEBUG Functions.openRecordStore(String, boolean) - file: " + file);
            return RecordStore.openRecordStore(file, createIfNeccessary);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new MyException(MyException.DB_CANNOT_OPEN_DB);
        }
    }

    /**
     * Closes given record store.
     * @param recordStore the record store to be closed.
     */
    static public void closeRecordStore(RecordStore recordStore) {
        try {
            if (recordStore != null) {
                recordStore.closeRecordStore();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns how many bytes are left in given database represented by given file
     * name.
     * @param file the file name of the database.
     * @return number of bytes that are left in database.
     */
    public static int spaceLeft(String file) {
        RecordStore store = null;
        int size = -1;
        try {
            //must be create a new one to give the correct space left.
            store = RecordStore.openRecordStore(file, true);
            size = store.getSizeAvailable();
        } catch (Exception ex) {
        }
        closeRecordStore(store);
        return size;
    }
    
    /**
     * Gets the size of given database - that means occupied space.
     * @return database size in bytes.
     */
    public static int spaceOccupied(String file) {
        RecordStore store = null;
        int size = 0;
        try {
            store = RecordStore.openRecordStore(file, true);
            size = store.getSize();
        } catch (Exception ex) {
        	ex.printStackTrace();
        }
        closeRecordStore(store);
        return size;
    }
    
    /**
	 * Formats size to be Bytes if <= 1024 or KBytes to one decimal point
	 * if >1024
	 * @param size size to format in Bytes
	 * @return	formatted size as String
	 */
	public static String formatNumberByteorKByte(long size) {
		if (size <= 1024) {
			return String.valueOf(size) + "B";
		} else {
			// l is file size in kB
			long l = size / 1024; // / 1024 ~ >> 10
			double d = l * 10;
		
			// round kB number to one decimal point
			d = (d - Math.floor(d) >= 0.5 ?
				Math.ceil(d) / 10: Math.floor(d) / 10);
			
            return String.valueOf(d) + "kB";
		}
	}

	// === NEW SORTING CONCEPT ===

	/**
	 * Sorts elements in {@link Vector} using comparator passed as parameter.
	 * 
	 * @param vector vector to be sorted
	 * @param comparator used for elements comparison
	 */
	public static void sort(Vector vector, Comparator comparator) {
	      if (DEBUG) { System.out.println("DEBUG Functions.sort(Vector, Comparator) - comparator: " + comparator.getClass().getName() ); }
        shuffle(vector);
        sort(vector, 0, vector.size() - 1, comparator);
	}

	/**
	 * Sorts {@link Vector} part bounded with leftBound and rightBound parameters.
	 * 
     * @param vector vector to be sorted
	 * @param leftBound left bound of the vector part in which elements have to be sorted
	 * @param rightBound right bound of the vector part in which elements have to be sorted
	 * @param comparator used for elements comparison
	 */
    static public void sort(Vector vector, int leftBound, int rightBound, Comparator comparator) {
        if (rightBound <= leftBound) {
            return;
        }
        int i = partition(vector, leftBound, rightBound, comparator);
        sort(vector, leftBound, i - 1, comparator);
        sort(vector, i + 1, rightBound, comparator);
    }

    /**
     * Classic part of the QuickSort algorithm.
     * Pivot is chosen and elements are split on elements lower than pivot
     * and elements greater than pivot.
     * 
     * @param vector vector we are sorting
     * @param leftBound left bound defining vector part we are interested in actually
     * @param rightBound right bound defining vector part we are interested in actually
     * @param comparator used for comparing elements in vector
     * @return index of the pivot in vector (the partitions with lower elements
     *         and partitions with greater elements have to be sorted).
     * @see Comparator
     */
    private static int partition(Vector vector, int leftBound, int rightBound, Comparator comparator) {
        int i = leftBound - 1;
        int j = rightBound;
        Object pivot = vector.elementAt(rightBound);
        while (true) {
              // find item on left to swap. don't go out-of-bounds
            while ( comparator.compare( vector.elementAt(++i), pivot) < 0 && i < rightBound ) {
                ;
            }
              // find item on right to swap. 
            while (comparator.compare( pivot, vector.elementAt(--j) ) < 0 && j > leftBound ) {
                ;
            }
              // check if pointers cross
            if (i >= j) {
                break;
            }
            exch(vector, i, j); // swap two elements into place
        }
        exch(vector, i, rightBound); // swap with partition element
        return i;
    }

    /**
     * Adds mails that are in given storage to given vector.
     * @param storage the storage with mails to be added.
     * @param v the vector to that add the mails from given storage.
     */
    public static void addMailsInStorageToVector( final IStorage storage, final Vector v) {
        final Enumeration emails = storage.getEnumeration();
        while ( emails.hasMoreElements() ) {
            v.addElement( emails.nextElement() );
        }
    }
}
