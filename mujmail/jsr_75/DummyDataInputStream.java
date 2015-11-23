//#condition MUJMAIL_FS
package mujmail.jsr_75;

import java.io.DataInputStream;
import java.io.InputStream;

/**
 * Dummy implementation of DataInputStream. Returned by dummy implementation of
 * MyFileConnection.
 * Does not contain any data - method read() always returns -1.
 * @author David Hauzar
 */
public class DummyDataInputStream extends DataInputStream {

	public DummyDataInputStream(InputStream in) {
		super(in);
	}


}
