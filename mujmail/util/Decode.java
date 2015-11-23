package mujmail.util;
/*
MujMail - Simple mail client for J2ME
Copyright (C) 2003-2005 Petr Spatka <petr.spatka@centrum.cz>
Copyright (C) 2005 Pavel Machek <pavel@ucw.cz>
Copyright (C) 2006 Nguyen Son Tung <n.sontung@gmail.com>
Copyright (C) 2006 Martin Stefan <martin.stefan@centrum.cz>
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

import java.io.*;

import mujmail.BodyPart;
import mujmail.MyException;

/**
 * Class responsible for converting different character 
 * sets and encodings (Base64, QuotedPrintable).
 * 
 */
public class Decode {
    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false;

    //iso 8859-2 from 128 to 255
    // Unicode char codes get from http://encyklopedie.seznam.cz/heslo/454274-iso-8859-2
    static final char latin2[] = {
        '\u0080', '\u0081', '\u0082', '\u0083', '\u0084', '\u0085', '\u0086', '\u0087', '\u0088', '\u0089', '\u008A', '\u008B', '\u008C', '\u008D', '\u008E', '\u008F',
     // ' ',      ' ',      ' ',      ' ',      ' ',      ' ',      ' ',      ' ',      ' ',      ' ',      ' ',      ' ',      ' ',      ' ',      ' ',      ' ', // 144 - 159
        '\u0090', '\u0091', '\u0092', '\u0093', '\u0094', '\u0095', '\u0096', '\u0097', '\u0098', '\u0099', '\u009A', '\u009B', '\u009C', '\u009D', '\u009E', '\u009F',
     // ' ',      'Ą',      '˘',      'Ł',      '¤',      'Ľ',      'Ś',      '§',      '¨',      'Š',      'Ş',      'Ť',      'Ź',      '-',      'Ž',      'Ż',
        '\u00A0', '\u0104', '\u02D8', '\u0141', '\u00A4', '\u013D', '\u015A', '\u00A7', '\u00A8', '\u0160', '\u015E', '\u0164', '\u0179', '-',      '\u017D', '\u017B',
     // '°',      'ą',      '˛',      'ł',      '´',      'ľ',      'ś',      'ˇ',      '¸',      'š',      'ş',      'ť',      'ź',      '˝',      'ž',      'ż', 
        '\u00B0', '\u0105', '\u02DB', '\u0142', '\u00B4', '\u013E', '\u015B', '\u02C7', '\u00B8', '\u0161', '\u015F', '\u0165', '\u017A', '\u02DD', '\u017E', '\u017C',
     // 'Ŕ',      'Á',      'Â',      'Ă',      'Ä',      'Ĺ',      'Ć',      'Ç',      'Č',      'É',      'Ę',      'Ë',      'Ě',      'Í',      'Î',      'Ď', 
        '\u0154', '\u00C1', '\u00C2', '\u0102', '\u00C4', '\u0139', '\u0106', '\u00C7', '\u010C', '\u00C9', '\u0118', '\u00CB', '\u011A', '\u00CD', '\u00CE', '\u010E',
     //  'Đ',      'Ń',      'Ň',      'Ó',      'Ô',      'Ő',      'Ö',      '×',      'Ř',      'Ů',      'Ú',      'Ű',      'Ü',      'Ý',      'Ţ',      'ß', 
        '\u0110', '\u0143', '\u0147', '\u00D3', '\u00D4', '\u0150', '\u00D6', '\u00D7', '\u0158', '\u016E', '\u00DA', '\u0170', '\u00DC', '\u00DD', '\u0162', '\u00DF', 
     // 'ŕ',      'á',      'â',      'ă',      'ä',      'ĺ',      'ć',      'ç',      'č',      'é',      'ę',      'ë',      'ě',      'í',      'î',      'ď', 
        '\u0155', '\u00E1', '\u00E2', '\u0103', '\u00E4', '\u013A', '\u0107', '\u00E7', '\u010D', '\u00E9', '\u0119', '\u00EB', '\u011B', '\u00ED', '\u00EE', '\u010F', 
     // 'đ',      'ń',      'ň',      'ó',      'ô',      'ő',      'ö',      '÷',      'ř',      'ů',      'ú',      'ű',      'ü',      'ý',      'ţ',      '˙', 
        '\u0111', '\u0144', '\u0148', '\u00F3', '\u00F4', '\u0151', '\u00F6', '\u00F7', '\u0159', '\u016F', '\u00FA', '\u0171', '\u00FC', '\u00FD', '\u0163', '\u02D9'
    };

    //from 128 to 255
    // Unicode char codes get from http://encyklopedie.seznam.cz/heslo/464614-windows-1250
    static final char windows1250[] = {
     // '€',      '?',      '‚',      '?',      '„',      '…',      '†',      '‡',      '?',      '‰',      'Š',      '‹',      'Ś',      'Ť',      'Ž',      'Ź',
        '\u20AC', '\u0020', '\u201A', '\u0020', '\u201E', '\u2026', '\u2020', '\u2021', '\u0020', '\u2030', '\u0160', '\u2039', '\u015A', '\u0164', '\u017D', '\u0179', 
     // '?',      '‘',      '’',      '“',      '”',      '•',      '–',      '—',      '?',      '™',      'š',      '›',      'ś',      'ť',      'ž',      'ź',
        '\u0020', '\u2018', '\u2019', '\u201C', '\u201D', '\u2022', '\u2013', '\u2014', '\u0020', '\u2122', '\u0161', '\u203A', '\u015B', '\u0165', '\u017E', '\u017A',
     // ' ',      'ˇ',      '˘',      'Ł',      '¤',      'Ą',      '¦',      '§',      '¨',      '©',      'Ş',      '«',      '¬',      '?',      '®',      'Ż',
        '\u00A0', '\u02C7', '\u02D8', '\u0141', '\u00A4', '\u0104', '\u00A6', '\u00A7', '\u00A8', '\u00A9', '\u015E', '\u00AB', '\u00AC', '\u00AD', '\u00AE', '\u017B', 
     // '°',      '±',      '˛',      'ł',      '´',      'µ',      '¶',      '·',      '¸',      'ą',      'ş',      '»',      'Ľ',      '˝',      'ľ',      'ż',
        '\u00B0', '\u00B1', '\u02DB', '\u0142', '\u00B4', '\u00B5', '\u00B6', '\u00B7', '\u00B8', '\u0105', '\u015F', '\u00BB', '\u013D', '\u02DD', '\u013E', '\u017C', 
     // 'Ŕ',      'Á',      'Â',      'Ă',      'Ä',      'Ĺ',      'Ć',      'Ç',      'Č',      'É',      'Ę',      'Ë',      'Ě',      'Í',      'Î',      'Ď', 
        '\u0154', '\u00C1', '\u00C2', '\u0102', '\u00C4', '\u0139', '\u0106', '\u00C7', '\u010C', '\u00C9', '\u0118', '\u00CB', '\u011A', '\u00CD', '\u00CE', '\u010E',
     //  'Đ',      'Ń',      'Ň',      'Ó',      'Ô',      'Ő',      'Ö',      '×',      'Ř',      'Ů',      'Ú',      'Ű',      'Ü',      'Ý',      'Ţ',      'ß', 
        '\u0110', '\u0143', '\u0147', '\u00D3', '\u00D4', '\u0150', '\u00D6', '\u00D7', '\u0158', '\u016E', '\u00DA', '\u0170', '\u00DC', '\u00DD', '\u0162', '\u00DF', 
     // 'ŕ',      'á',      'â',      'ă',      'ä',      'ĺ',      'ć',      'ç',      'č',      'é',      'ę',      'ë',      'ě',      'í',      'î',      'ď', 
        '\u0155', '\u00E1', '\u00E2', '\u0103', '\u00E4', '\u013A', '\u0107', '\u00E7', '\u010D', '\u00E9', '\u0119', '\u00EB', '\u011B', '\u00ED', '\u00EE', '\u010F', 
     // 'đ',      'ń',      'ň',      'ó',      'ô',      'ő',      'ö',      '÷',      'ř',      'ů',      'ú',      'ű',      'ü',      'ý',      'ţ',      '˙', 
        '\u0111', '\u0144', '\u0148', '\u00F3', '\u00F4', '\u0151', '\u00F6', '\u00F7', '\u0159', '\u016F', '\u00FA', '\u0171', '\u00FC', '\u00FD', '\u0163', '\u02D9'
    };

    /** 
     * Changes character set to unicode ... internal Java encoding
     * 
     * @param s Regular string. Have to be unQuoted, and unbase Base64.
     * @param charset Character set (encoding) used in string s
     * @return converted string s
     */
    private static StringBuffer convertCharSet( StringBuffer s, byte charset) {
        StringBuffer output = new StringBuffer();
        int len = s.length();

        switch (charset) {
            case BodyPart.CH_ISO88591 : {
                // One to one mapping
                char c;
                for(int i = 0; i < len; ++i) {
                    c = (char)(s.charAt(i) & 0x00FF);
                    output.append(c);
                }
                break;
            }
            case BodyPart.CH_ISO88592 : {
                for(int i = 0; i < len; ++i) {
                    char c = s.charAt(i);
                    if (c > 128) 
                        c = latin2[c & 0x007f];
                    output.append(c);
                }
                break;
            }
            case BodyPart.CH_WIN1250 : {
                for(int i = 0; i < len; ++i) {
                    char c = s.charAt(i);
                    if (c > 128) c = windows1250[c & 0x007f];
                    output.append(c);
                }
                break;
            }
            case BodyPart.CH_UTF8 : {
            // see http://en.wikipedia.org/wiki/UTF-8
            // see http://java.sun.com/javase/6/docs/api/java/io/DataInput.html#modified-utf-8
                for(int i = 0; i < len; ++i) {
                    char c1 = s.charAt(i);
                    // one byte encoded entity 1:1
                    if (c1<128) output.append(c1);
                    // two byte endoced entity
                    else if ( (c1 & 0x00E0) == 0x00C0) {
                        i++;
                        if (i >= len) return output;
                        char c2 = s.charAt(i);
                        int uniVal = (( c1 & 0x001F) << 6) | (c2 & 0x003F);
                        char c3 = (char)uniVal;
                        output.append( c3);
                    }
                    // three byte endoced entity
                    else if ( (c1 & 0x00F0) == 0x00E0) {
                        if (i+2 >= len) return output;
                        char c2 = s.charAt(i+1);
                        char c3 = s.charAt(i+2);
                        i +=2;
                        int uniVal = (( c1 & 0x000F) << 12) | ((c2 & 0x003F) << 6) | (c3 & 0x003F);
                        char c4 = (char)uniVal;
                        output.append( c4);
                    }
                    // four byte endoced entity
                    else if ( (c1 & 0x00F8) == 0x00F0) {
                        if (i+3 >= len) return output;
                        char c2 = s.charAt(i+1);
                        char c3 = s.charAt(i+2);
                        char c4 = s.charAt(i+3);
                        i +=3;
                        int uniVal = (( c1 & 0x0007) << 18) | ((c2 & 0x003F) << 12) | ((c3 & 0x003F) << 6) | (c4 & 0x003F);
                        char c5 = (char)uniVal;
                        output.append( c5);
                    }
                }
                break;
            }
            default : {
                // No change done
                output = s;
            }
                
        }
        return output;
        
    }
    /**
     * Decodes 8-bit character set. 
     * Characters in interval 0..127 are the same as 7-bit ASCII for all character sets.
     * 
     *  On the other hand they differs in character 
     *   with codes in interval 128..255, so it must be worked out separately.
     * 
     * @param s String that will be converted
     * @param charset Character coding mapping used in string s
     * @return converted string s
     * <p>
     * @see #convertCharSet
     */
    public static String decode8bitCharset(String s, byte charset) {
        return convertCharSet(new StringBuffer(s), charset).toString();
    }


    /**
     * Decodes a quoted-printable encoded string. This is used when an 8-bit character set (256 characters) is
     * written by a 7-bit ASCII (128 characters). Characters, that code is in interval 128..255, are displayed
     * by three chars: "=" and their hexadecimal character code (i.e 'A9').

     * @param s String that will be converted
     * @param charset Character coding mapping used in string s
     * @return decoded string s
     */
    public static String decodeQuotedPrintable(String s, byte charset) {
        StringBuffer output = new StringBuffer();
        int n = 0, strLength = s.length();
        char c;
        while (n < strLength) {
            // decode quoted character
            if (s.charAt(n) == '=') {
                if ((n + 2) < strLength) {
                    if (s.substring(n + 1, n + 3).equals("\r\n")) {
                        n += 3;
                        continue;
                    }

                    // thorws exception if input is uncorrectly encoded
                    try {
                        c = (char) Integer.parseInt(s.substring(n + 1, n + 3), 16);
                    } catch (NumberFormatException ex) {
                        output.append(s.charAt(n));
                        n++;
                        continue;
                    }
                    output.append(c);
                }
                n += 3;
            } // not quoted character
            else {
		c = s.charAt(n);
                c &= 0x00FF;
                output.append(c);
		n++;
            }
 	}
        String result = convertCharSet( output, charset).toString();
        return result;
    }

    /**
     * Decodes header fields Subject and From encoded in any 8-bit charset.
     * 
     * @param s string from header entry where is encoding type and value.
     * @return decoded value string  (without endding type header)
     * <p>
     * Note: Is intended for mail headers decoding.
     */
    private static String decodeQuotedOrBinary(String s) throws MyException {
        String lower = s.toLowerCase();

        // quoted printable encoding
        if (lower.startsWith("=?us-ascii?q?")) {
            return decodeQuotedPrintable(s.substring(13, s.length() - 2), BodyPart.CH_USASCII);
        } else if (lower.startsWith("=?windows-1250?q?")) {
            return decodeQuotedPrintable(s.substring(17, s.length() - 2), BodyPart.CH_WIN1250);
        } else if(lower.startsWith("=?windows-1252?q?")) {
            return decodeQuotedPrintable(s.substring(17, s.length() - 2), BodyPart.CH_NORMAL);
        } else if (lower.startsWith("=?iso-8859-1?q?")) {
            return decodeQuotedPrintable(s.substring(15, s.length() - 2), BodyPart.CH_ISO88591);
        } else if (lower.startsWith("=?iso-8859-2?q?")) {
            return decodeQuotedPrintable(s.substring(15, s.length() - 2), BodyPart.CH_ISO88592);
        } else if (lower.startsWith("=?utf-8?q?")) {
            return decodeQuotedPrintable(s.substring(10, s.length() - 2), BodyPart.CH_UTF8);
        } // base64 encoding
        else if (lower.startsWith("=?windows-1250?b?")) {
            return decodeBase64(s.substring(17, s.length() - 2), BodyPart.CH_WIN1250);
        } else if (lower.startsWith("=?iso-8859-1?b?")) {
            return decodeBase64(s.substring(15, s.length() - 2), BodyPart.CH_ISO88591);
        } else if (lower.startsWith("=?iso-8859-2?b?")) {
            return decodeBase64(s.substring(15, s.length() - 2), BodyPart.CH_ISO88592);
        } else if (lower.startsWith("=?utf-8?b?") ) {
            //return  decodeBase64(s.substring(10,s.length()-2), MessageHeader.CH_UTF8);
            return decodeBase64(s.substring(10, s.length() - 2), BodyPart.CH_UTF8);
        }
        return s;
    }

    /**
     * Decodes quoted header field (mostly a SUBJECT nad RECIPIENTS).
     * @param s Message header entry. line from message header. ( Subject:=?windows-1250?q?Alf?=)
     * @return original header entry where vakue sting was decoded (Subject:alf)
     */
    public static String decodeHeaderField(String s) throws MyException {
        int beginQuoted;
        int endQuoted;
        String begin, middle, end;
        beginQuoted = s.indexOf("=?");

        if (beginQuoted == -1) {
            return s;
        } else {
            endQuoted = s.indexOf("?", beginQuoted + 2);
            endQuoted = s.indexOf("?", endQuoted + 1);
            endQuoted = s.indexOf("?=", endQuoted + 1) + 2;
            begin = s.substring(0, beginQuoted);
            try {
                middle = decodeQuotedOrBinary(s.substring(beginQuoted, endQuoted));
            } catch (Exception ex) {
                throw new MyException(MyException.VARIOUS_DECODE_ILLEGAL_MIME);
            }
            end = decodeHeaderField(s.substring(endQuoted));
            return begin + middle + end;
        }
    }

    /** 
     * Encodes header value to into standartized mail format.
     * @param input Plain text value representation
     * @return string in utf-8 encoding stored in base64 with header
     */
    public static String encodeHeaderField(String input) {
        if (input.length() == 0) {
            return input;
        }
        return "=?UTF-8?B?" + toBase64(input, false) + "?=";
    }

    /**
     * Encodes an input string into base64 format. It means, that only 64 characters are needed to encode the text string.
     * @author <a href="http://izhuk.com">Igor Zhukovsky</a>
     * @param input string to convert to base64
     * @param isFile set to true if input is raw file data, false if input is text to be convert to utf-8
     */
    public static String toBase64(String input, boolean isFile) {
          if (DEBUG) { System.out.println("DEBUG Decode.toBase64(input=\"" + input + "\", isFile=" + isFile + ")"); }
        byte inData[] = null;
        if (isFile) {
            inData = new byte[ input.length()];
                  for (int l = 0; l < input.length(); l++) {
                inData[l] = (byte)input.charAt(l);
            }
        } else {
            try {
                // Not file, standard stirng
                inData = input.getBytes("utf-8");
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
		// if exception is created converting to utf-8 (meaning it is likely
		// not available, use naive text conversion before converting to base64
                for (int l = 0; l < input.length(); l++) {
                    inData[l] = (byte)input.charAt(l);
                }
            }
        }
        
        String base64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        char output[] = new char[4];
        int state = 1;
        int restbits = 0;
        int chunks = 0;

        StringBuffer encoded = new StringBuffer();

        for (int i = 0; i < inData.length; i++) {
            int ic = inData[i] & 0xFF;
            switch (state) {
                case 1:
                    output[0] = base64.charAt(ic >>> 2);
                    restbits = ic & 0x03;
                    break;
                case 2:
                    output[1] = base64.charAt((restbits << 4) | (ic >>> 4));
                    restbits = ic & 0x0F;
                    break;
                case 3:
                    output[2] = base64.charAt((restbits << 2) | (ic >>> 6));
                    output[3] = base64.charAt(ic & 0x3F);
                    encoded.append(output);

                    // keep no more the 76 character per line
                    chunks++;
                    if ((chunks % 19) == 0) {
                        encoded.append("\r\n");
                    }
                    break;
            }
            state = (state < 3 ? state + 1 : 1);
        } // for

        /* final */
        switch (state) {
            case 2:
                output[1] = base64.charAt((restbits << 4));
                output[2] = output[3] = '=';
                encoded.append(output);
                break;
            case 3:
                output[2] = base64.charAt((restbits << 2));
                output[3] = '=';
                encoded.append(output);
                break;
        }

          if (DEBUG) { System.out.println("DEBUG Decode.toBase64 result=\"" + encoded + "\""); }
        return encoded.toString();
    }

    /**
     * Does a base64 decoding for a single character.
     * @return 6bit number reprezented by this char if base64 encoded
     * <p>
     * Contains code from Stefan Haustein's KObjects library (www.kobjects.org)
     * used by permission.
     */
    private static int decode(char c) throws MyException {
        //System.out.println("char= " + c);
        if (c >= 'A' && c <= 'Z') {
            return ((int) c) - 65;
        } else if (c >= 'a' && c <= 'z') {
            return ((int) c) - 97 + 26;
        } else if (c >= '0' && c <= '9') {
            return ((int) c) - 48 + 26 + 26;
        } else {
            switch (c) {
                case '+':
                    return 62;
                case '/':
                    return 63;
                case '=':
                    return 0;
                default:
                    throw new MyException(MyException.VARIOUS_DECODE_ILLEGAL_MIME);
            }
        }
    }

    /**
     * Decodes a <code>String</code> that is encoded in the Base64 encoding. If there occures an illegel
     * Base64 character, then a <code>MyException</code> with an appropriate message is thrown. This method
     * serves to encode a header fields.
     * @param s an encoded input header field.
     * @param charset a charset of the header field.
     */
    public static String decodeBase64(String s, byte charset) throws MyException {
        StringBuffer buff = new StringBuffer();
        int i = 0;
        int len = s.length(), b;

        while (true) {
            while (i < len && s.charAt(i) <= ' ') {
                i++;
            }

            if (i + 3 >= len) {
                break;
            }
            int tri = (decode(s.charAt(i)) << 18) + (decode(s.charAt(i + 1)) << 12) + (decode(s.charAt(i + 2)) << 6) + (decode(s.charAt(i + 3)));

            for (int j = 16; j >= 0; j -= 8) {
                if (j == 8 && s.charAt(i + 2) == '=') {
                    break;
                }
                if (j == 0 && s.charAt(i + 3) == '=') {
                    break;
                }
                b = (tri >> j) & 255;
                buff.append((char) b);
            }
            i += 4;
        }

        return convertCharSet(buff, charset).toString();
    }
    
    /**
     * Decodes a <code>String</code> that is encoded in the Base64 encoding. If there occures an illegel
     * Base64 character, then a <code>MyException</code> with a propriete message is thrown.
     */
    public static ByteArrayOutputStream decodeBase64(String s) throws MyException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int i = 0;
        int len = s.length(), b;

        while (true) {
            while (i < len && s.charAt(i) <= ' ') {
                i++;
            }

            if (i + 3 >= len) {
                break;
            }
            int tri = (decode(s.charAt(i)) << 18) + (decode(s.charAt(i + 1)) << 12) + (decode(s.charAt(i + 2)) << 6) + (decode(s.charAt(i + 3)));

            for (int j = 16; j >= 0; j -= 8) {
                if (j == 8 && s.charAt(i + 2) == '=') {
                    break;
                }
                if (j == 0 && s.charAt(i + 3) == '=') {
                    break;
                }
                b = (tri >> j) & 255;
                bos.write((char) b);
            }
            i += 4;
        }
	return bos;
    }
}
