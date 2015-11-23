//#condition MUJMAIL_FS
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

package mujmail.jsr_75;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Dummy implementation of InputStream. Returned by dummy implementation of
 * MyFileConnection.
 * Does not write any data - method write() does not do anything..
 * @author David Hauzar
 */
class DummyOutputStream extends OutputStream {

    /**
     * Writes the specified byte to this output stream. This is dummy 
     * implementation of write method. Does not do anything.
     * @param arg0 the byte
     * @throws java.io.IOException this dummy implementation of the write method
     *  never throws any exception.
     */
    public void write(int arg0) throws IOException {
    }
    
}
