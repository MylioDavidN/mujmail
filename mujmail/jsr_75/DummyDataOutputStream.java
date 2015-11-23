/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mujmail.jsr_75;

import java.io.DataOutputStream;
import java.io.OutputStream;

/**
 * Dummy implementation of InputStream. Returned by dummy implementation of
 * MyFileConnection.
 * Does not write any data - method write() does not do anything..
 * @author David Hauzar
 */
public class DummyDataOutputStream extends DataOutputStream {
    public DummyDataOutputStream(OutputStream out) {
        super(out);
    }

}
