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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Dummy implementation of methods from MyFileConnection interface. Used
 * when JSR 75 is not available.
 * @author David Hauzar
 */
class MyFileConnectionDummy extends  MyFileConnection.MyFileConnectionAdapter {

    public void initialize(String fileURL) throws IOException {
    }

    public void initialize(String fileURL, int flag) throws IOException {
    }

    
    public void close() {
    }

    public long fileSize() {
        return -1;
    }

    public Enumeration list() {
        return new Vector().elements();
    }

    public InputStream openInputStream() {
        return new DummyInputStream();
    }

    public OutputStream openOutputStream() throws IOException {
        return new DummyOutputStream();
    }

    public DataOutputStream openDataOutputStream() throws IOException {
        return new DummyDataOutputStream(new DummyOutputStream());
    }

    public void create() throws IOException {
    }

    public boolean exist() {
        return false;
    }

    public void mkdir() throws IOException {
    }

	public DataInputStream openDataInputStream() throws IOException {
		return new DummyDataInputStream(openInputStream());
	}
}
