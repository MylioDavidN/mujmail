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
import java.io.InputStream;

/**
 * Dummy implementation of InputStream. Returned by dummy implementation of
 * MyFileConnection.
 * Does not contain any data - method read() always returns -1.
 * @author David Hauzar
 */
class DummyInputStream extends InputStream {

    /**
     * Reads the next byte of data from the input stream. 
     * This is dummy implementation of read metdhod - always returns -1 which
     * means the end of the stream. Never blocks. Never throws any exception.
     * @return always -1 - this means the end of the stream
     * @throws java.io.IOException this implementation of read method never
     *  throws an exception
     */
    public int read() throws IOException {
        return -1;
    }
}
