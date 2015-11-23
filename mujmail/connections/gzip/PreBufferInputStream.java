//#condition MUJMAIL_COMPRESSED_CONNECTION
/*
MujMail - Simple mail client for J2ME
Copyright (C) 2003 Petr Spatka <petr.spatka@centrum.cz>
Copyright (C) 2005 Pavel Machek <pavel@ucw.cz>
Copyright (C) 2006 Nguyen Son Tung <n.sontung@gmail.com>
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

package mujmail.connections.gzip;

import java.io.IOException;
import java.io.InputStream;
import mujmail.connections.ConnectorInterface;
import mujmail.connections.InOutBuffer;

/** 
 * This class represent special input stream. 
 * 
 * At first uses data from buffer which gets in constructor. 
 * And then uses connector to read other data.
 * It buffers data.
 */
public class PreBufferInputStream extends InputStream {
    byte[] minibuff;
    int pos;
    int len;
    ConnectorInterface connector;
    
    /** 
     * Create InputStream from Connector interface
     * 
     * @param iface Connector to read data from
     * @param buff Prebuffered data (Can be null if no data are prebuffered)
     * <p>
     * Note: Data are copied from buffer buff. Buff is empty after this call.
     */
    public PreBufferInputStream( ConnectorInterface iface, InOutBuffer buff) {
        connector = iface;
        pos = 0;
        if (buff == null) {
            len = 512;
            minibuff = new byte[len];
            return;
        }

        len = buff.getBufferedInputBytesCount();
        if (len < 512) len = 512;
        minibuff = new byte[len];
        int tmp;
        len = 0;
        while (true) {
            tmp = buff.getByteNoFill();
            if ( tmp == -1) break;
            minibuff[len] = (byte)tmp;
            len++;
        }
    }
    
    /**
     * Gets byte from input.
     * @return Next byte from input stream.
     */
    public int read() throws IOException {
        // System.out.println("GZIPPrebuffer read call - datas in buffer: " + Integer.toString(len - pos));
        if ( pos >= len) { // No data available ... fill mini buffer
            pos = 0;
            len = 0;
            while (len == 0) 
                len = connector.read(minibuff, 0, minibuff.length);
        }
        return ((int)(minibuff[pos++])) & 0xFF; // from signed byte into unsigned byte returned as integer
    }

    /**
     * Reads data from input and fill given buffer.
     * @param buff Buffer where new incomming data to be stored.
     * @param start Position in buffer buff from where data will be stored.
     * @param length Maximal number of bytes to be read into buffer.
     * @return Number of new bytes stored buffer.
     */
    public int read( byte[] buff, int start, int length) throws IOException {
        // System.out.println("GZIPPrebuffer MiniBlock read call - datas in buffer: " + Integer.toString(len - pos));
        if ( pos >= len) { // No data available ... fill mini buffer
            pos = 0;
            len = 0;
            while (len == 0) 
                len = connector.read(minibuff, 0, minibuff.length);
        }
        // Calculate length
        int retval = len-pos;
        if ( retval > length) retval = length;
        // Copy data
        System.arraycopy(minibuff, pos, buff, start, retval);
        pos += retval;
        return retval;
    }

    /**
     * Gets how many bytes is available in input. Reading this number of bytes willn't block.
     * @return bytes availabe in buffer. 
     */
 
    public int available() throws IOException {
        if ( pos < len ) return len - pos;
        
        if ( connector.available_bug() == false ) return connector.available();
        
        return 0;
    }
}
