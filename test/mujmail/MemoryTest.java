package test.mujmail;

import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Graphics;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

public class MemoryTest extends MIDlet {

    protected void startApp() throws MIDletStateChangeException {
        InstanceCounter ic = new InstanceCounter();
        Display.getDisplay( this ).setCurrent( ic );
        Thread newThread = new MemoryOccupierWithGC( ic );
        newThread.setPriority( Thread.MIN_PRIORITY );
        newThread.start();
    }

    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }

    protected void pauseApp() {
        throw new RuntimeException( "Not implemented" );
    }

    private class MemoryOccupierWithGC extends Thread {

        private InstanceCounter ic = null;

        int size;
        int[] array;

        public MemoryOccupierWithGC( InstanceCounter ic ) {
            this.ic = ic;
            this.size = 1024;
            ic.setTestNumber(1);
        }

        public void run() {
            try {
                while ( true ) {
                    array = new int[ size ];
                    array[ size - 1 ] = Integer.MAX_VALUE; // :-) no reason for doing this
                    synchronized (this) {
                          System.out.println("waiting...");
                        try {
                            wait( 2000 );
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    size *= 2;
                    array = null;
                    System.out.println( "gc" );
                    System.gc();
                      System.out.println( "allocating..." );
                    ic.setCount( size );
                    ic.repaint();
                }
            } catch ( OutOfMemoryError oome ) {
                oome.printStackTrace();
                System.gc();
                Thread thread = new MemoryOccupierNoGC( this.ic );
                thread.setPriority( MIN_PRIORITY );
                thread.start();
            }
        }

    }

    private class MemoryOccupierNoGC extends Thread {

        private InstanceCounter ic = null;

        int size;
        int[] array;

        public MemoryOccupierNoGC( InstanceCounter ic ) {
            this.ic = ic;
            this.size = 1024;
            ic.setTestNumber(2);
        }

        public void run() {
            try {
                while ( true ) {
                    array = new int[ size ];
                    array[ size - 1 ] = Integer.MAX_VALUE; // :-) no reason for doing this
                    synchronized (this) {
                          System.out.println("waiting...");
                        try {
                            wait( 1000 );
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    size *= 2;
                    array = null;
                      System.out.println( "allocating..." );
                    ic.setCount( size );
                    ic.repaint();
                }
            } catch ( OutOfMemoryError oome ) {
                oome.printStackTrace();
                System.gc();
                Thread thread = new MemoryOccupierFourKBBlocksNoGC( this.ic );
                thread.setPriority( MIN_PRIORITY );
                thread.start();
            }
        }
    }

    private class MemoryOccupierFourKBBlocksNoGC extends Thread {

        private InstanceCounter ic = null;

        Vector vector;
        int[] array;

        public MemoryOccupierFourKBBlocksNoGC( InstanceCounter ic ) {
            this.ic = ic;
            vector = new Vector();
            ic.setTestNumber(3);
        }

        public void run() {
            try {
                while ( true ) {
                      System.out.println( "allocating..." );
                    array = new int[ 16*1024 ];
                    array[ 1024 - 1 ] = Integer.MAX_VALUE; // :-) no reason for doing this
                    vector.addElement( array );
                    ic.setCount( vector.size() );
                    synchronized (this) {
                          System.out.println("waiting...");
                        try {
                            wait( 1000 );
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    array = null;
                    ic.repaint();
                }
            } catch ( OutOfMemoryError oome ) {
                oome.printStackTrace();
            }
              System.out.println( "running GC" );
            System.gc();
            synchronized (this) {
                try {
                      System.out.println( "waiting before vector dereferencing and running GC again" );
                    wait( 5000 );
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            vector = null;
              System.out.println( "running GC again" );
            System.gc();
            Thread thread = new MemoryOccupierFourKBBlocksGC( this.ic );
            thread.setPriority( MIN_PRIORITY );
            thread.start();
        }
    }

    private class MemoryOccupierFourKBBlocksGC extends Thread {

        private InstanceCounter ic = null;

        Vector vector;
        int[] array;

        public MemoryOccupierFourKBBlocksGC( InstanceCounter ic ) {
            this.ic = ic;
            vector = new Vector();
            ic.setTestNumber(3);
        }

        public void run() {
            try {
                while ( true ) {
                      System.out.println( "allocating..." );
                    array = new int[ 16*1024 ];
                    array[ 1024 - 1 ] = Integer.MAX_VALUE; // :-) no reason for doing this
                    vector.addElement( array );
                    ic.setCount( vector.size() );
                    synchronized (this) {
                          System.out.println("waiting...");
                        try {
                            wait( 1000 );
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    array = null;
                    ic.repaint();
                }
            } catch ( OutOfMemoryError oome ) {
                oome.printStackTrace();
            }
            vector = null;
            System.gc();
        }
    }

    private class InstanceCounter extends Canvas {

        int testNumber;
        int count;

        public void setCount(int count) {
            this.count = count;
        }

        public void setTestNumber(int testNumber) {
            this.testNumber = testNumber;
        }

        protected void paint(Graphics g) {
              System.out.println( "repainting - start" );
            final int width = this.getWidth();
            final int height = this.getHeight();

            g.setColor( 0xFFFFFF );
            g.fillRect(0, 0, width, height);
            g.setColor( 0x000000 );
            //g.drawString( Integer.toString(testNumber) + ": " + Integer.toString( count ), width/2, height/2, Graphics.SOLID );
            final String s = Integer.toString(testNumber) + ": " + Integer.toString( count );
              System.out.println( "s: " + s );
            g.drawString( s, 1, 1, Graphics.TOP | Graphics.LEFT );
              System.out.println( "repainting - end" );
        }
    }
}
