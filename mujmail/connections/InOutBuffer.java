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

package mujmail.connections;

import java.io.IOException;
import mujmail.util.Functions;
import mujmail.Settings;
import mujmail.MyException;

/** 
 * Special (autofilling) character oriented buffer.
 * <p>
 * Note: Used by ConnectionCompressed
 */
public class InOutBuffer {
    // Note no synchronisation done, synchronisation have to be ensured by higher level
    // No parallel access into Out* methods a no parallel access into In* methods
    
    /** Buffer that holds data to send */
    private byte[] outBuff = null;
    /** Position with first empty char in buffer */
    private int outPos = 0;

    /** Buffer that hold recieved data */
    private byte[] inBuff = null;
    /** Position in input buffer */
    private int inPos = 0;
    /** Number of recieved data in buffer 
     * Index just behind last valid byte in buffer */
    private int inRecieved = 0;
    
    /** Interface where flushes data from buffer */
    private ConnectorInterface connector;
    /** Counter of send data .. out direction */
    private int send = 0;
    /** Counder of recieved data ... in direction */
    private int recieved = 0;
    
    /** 
     * Constant that signal that at given position in buffer,
     *  there are no buffered data.
     * <p>
     * See {@link #InLookInBuffer}.
     */ 
    public static final int NO_DATA_IN_BUFFER = -1;
    
    /** 
     * Created buffer
     * @param bufferLen size of buffer for datas
     * @param reciever connector used for sending and retrieving data if buffer is full or empty
     */
    public InOutBuffer(int bufferLen, ConnectorInterface reciever) {
        outBuff = new byte[bufferLen];
        inBuff = new byte[bufferLen];
        connector = reciever;
    }
    
    /**
     * Append char into buffer. If buffer full, flushes buffer
     * @param data Byte to append
     */
    public void OutAddByte(byte data) throws IOException {
        send++;
        outBuff[outPos++] = data;
        if (outPos == outBuff.length) OutFlush();
    }

    /** 
     * Flushes buffer output data into connector (send them out)
     */
    public void OutFlush() throws IOException {
        connector.write(outBuff,0,outPos);
        outPos = 0;
    }

    /** 
     * Is used for getting data from incomming buffer 
     *  @return Get next byte from buffer, if no available it try to fill buffer automaticaly (can take long time)
     *          Shifts buffer position
     */
    public byte InGetByte() throws MyException {
        if (inPos < inRecieved) { // Byte available in buffer
            recieved++;
            return inBuff[inPos++];
        }
        // Try to fill buffer
        try { 
            InFillBuffer(true);
        } catch (IOException e) {
            e.printStackTrace();
            e.getMessage();
            e.toString();
            throw new MyException(MyException.COM_IN, e);
        }

        if (inPos < inRecieved) { // Byte available in buffer
            recieved++;
            return inBuff[inPos++];
        }
        // After reading no new data --> timeout
        throw new MyException(MyException.COM_TIMEOUT);
    }
    
    /** 
     * Read new fresh data into buffer
     * 
     * @param wait False signalize that if no data available than no wait and 
     *          end immedaitely with no new data in buffer.
     * @throws mujmail.MyException
     * @throws java.io.IOException
     */
    public void InFillBuffer(boolean wait) throws MyException, IOException {
        if (inPos >= inRecieved) { // No data available
            inPos = 0;
            inRecieved = 0;
        }
        // RLE Hack -- rle needs see at least two byte forward
        // if only one byte avail, copy them into begin of buffer and fill
        if (inPos + 1 == inRecieved) {
            for(int i = 0; i < 1; i++) {
                inBuff[i] = inBuff[inPos + i];
            }
            inPos = 0;
            inRecieved = 1;
        }
        // Check if place for dataReading
        if (inBuff.length <= inRecieved) 
            return; // No place for new data and some data are still in buffer
        
        // Filling begin
        int timeout = 0;
        int len = 0;
        if (connector.available_bug()) {
            if (wait == false) return; // No wait how to not block
            while ((len = connector.read(inBuff, inRecieved, inBuff.length - inRecieved)) <= 0) {
                if (len == -1) { // End of stream reached
                    throw new MyException(MyException.COM_HALTED);
                }
                if (timeout > Settings.timeout) {
                    throw new MyException(MyException.COM_TIMEOUT);
                }
                Functions.sleep(100);
                timeout += 100;
            }
        } else { // No avail bug
            int max;
            while ((max = connector.available()) == 0) { //wait until get some data from server
                if (max == -1) { // End of stream reached
                    throw new MyException(MyException.COM_HALTED);
                }
                if (timeout > Settings.timeout) {
                    throw new MyException(MyException.COM_TIMEOUT);
                }
                if (wait == false) return;
                Functions.sleep(100);
                timeout += 100;
            }
            if (max > inBuff.length - inRecieved) {
                max = inBuff.length - inRecieved;
            }
            len = connector.read(inBuff, inRecieved, max);
            if (len == -1) { // End of stream reached
                throw new MyException(MyException.COM_HALTED);
            }
        }
        // Data readed actualize buffer counters
        inRecieved += len;
    }
    
    /** 
     * Get's value from incomming buffer. Doesn't change buffer state or fill buffer.
     *  @param shift Position in buffer from current position you want access
     *  @return Byte from buffer or NO_DATA_IN_BUFFER if no such data in buffer 
     *              (reading behing end of buffer or before start)
     */
    public int InLookInBuffer(int shift) {
        int pos = inPos + shift;
        if (pos < 0 || pos >= inRecieved)
            return NO_DATA_IN_BUFFER;
        return inBuff[pos];
    }
    
    /** 
     * Clears buffer by removing all buffered characters 
     */
    public void InSkipBuffer() {
        inPos = inRecieved;
    }
   
    /** 
     * Change internal connector that is used for filling buffers 
     * and sending data.
     * @param newCi New connector to use.
     */
    public void changeConnector( ConnectorInterface newCi) {
        connector = newCi;
    }
    
    /** 
     * Gets number of bytes available in buffer.
     * 
     * @return Get count bytes currently available in buffer.
     */
    public int getBufferedInputBytesCount() {
        return inRecieved - inPos;
    }

    /** 
     *  Reads buffer data from buffer, but this call can make buffer empty. 
     *  No internal filling of buffer is done
     *  @return next byte in buffer. -1 of bufferEmpty
     *  Note: Is used by PreBufferInputStream
     */
    public int getByteNoFill() {
        if (inPos < inRecieved) { // Byte available in buffer
            recieved++;
            return inBuff[inPos++];
        }
        return -1;
    }
}
