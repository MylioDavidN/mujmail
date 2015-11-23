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

package mujmail.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Abstract class for representing output buffer. Output buffer should be used
 * when it is necessary to write data not immediately but later when specified
 * amount of data is collected in cache.
 * 
 * This abstract class offers template method write that can be used to implement
 * write methods for various data to be buffered.
 * Concrete implementation of abstract methods will probably need to access field
 * representing the data that user actually writes to the cache, a field
 * representing the cache and a field representing object to that the data will
 * be written when flush method is called.
 * 
 * @author David Hauzar
 */
public abstract class OutputBuffer {
    /** The amount of data in the cache when the cache should be flushed.
     Note that this is global limit for all buffers. */
    protected static final int MAX_CACHE_SIZE = 11000;
    // set MAX_CACHE_SIZE on devices with very small amount of memory to 0
    // TODO: solve better: special implementation of buffer that does not do any buffering
    //protected static final int MAX_CACHE_SIZE = 0;
    
    
    /**
     * Template method that can be used to implement write methods for various 
     * data to be buffered.
     * Flushes cache and writes data when it is necessary.
     */
    protected void write() throws Exception {
        try {
            appendDataToCache();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            System.out.println("Out of memory!!!");
            flush();
            try {
                makeNewCacheForWritedData();
                appendDataToCache();
            } catch (OutOfMemoryError e2) {
                e2.printStackTrace();
                writeActualData();
                return;
            }
        }
        
        if (cacheLenght() > MAX_CACHE_SIZE) {
            // write cache plus string
            flush();
        }
        
    }

    /**
     * Appends data that user actually writes to the buffer to cache.
     */
    protected abstract void appendDataToCache();

    /**
     * Gets actual length of the cache.
     * @return the actual length of the cache.
     */
    protected abstract int cacheLenght();

    /**
     * Makes new cache. It is recommended to make the cache minimally of the size
     * of data actually written to the buffer.
     * @throws OutOfMemoryError if it is not enough memory to create the cache
     *  for writed data.
     */
    protected abstract void makeNewCacheForWritedData();

    /**
     * Writes cached data to the destination object and clear the cache.
     * @throws java.lang.Exception
     */
    public abstract void flush() throws Exception;
    
    /**
     * Gets the size of data that is actually in the buffer.
     * 
     * @return the size of data that is actually in the buffer.
     */
    public abstract int bufferSize();
    
    /**
     * Discards all data from the cache.
     */
    public abstract void clearCache();

    /**
     * Write data that user actually writes to the buffer to the destination
     * object.
     * @throws java.lang.Exception
     */
    protected abstract void writeActualData() throws Exception;
    
    
    /**
     * Abstract class that serves for buffering byte data. 
     * 
     * Implementations of this class can vary in the way how the data are
     * written out from the buffer.
     * 
     * Criticism: The problem is that for caching the instance of 
     * {@link ByteArrayOutputStream} is used and it's method 
     * {@link ByteArrayOutputStream#toByteArray()} allocates new data. This
     * means that {@link OutOfMemoryError} can occur. To prevent 
     * {@link OutOfMemoryError} data of size {@link OutputBuffer#MAX_CACHE_SIZE}}
     * is allocated before writing any data to the buffer. This data
     * is released before calling method ByteArrayOutputStream#toByteArray()}
     * and than again allocated.
     * This means that preallocated memory is never used and that the amount
     * of this memory depends on maximum size of the buffer given by
     * constant {@link OutputBuffer#MAX_CACHE_SIZE}}. 
     * Note that preallocated memory is shared among all instances of this
     * class.  
     */
    public static abstract class ByteOutputBuffer extends OutputBuffer {
        private ByteArrayOutputStream cache = new ByteArrayOutputStream();
        private static byte[] preallocatedMemory = new byte[MAX_CACHE_SIZE];;
        private byte[] writedData;
        
        public ByteOutputBuffer() {
            super();
        }
        
        /**
         * Used to preallocate memory to be able to flush buffer without 
         * OutOfMemoryError.
         * @see #releaseMemory()
         */
        private static void preallocate() {
            preallocatedMemory = new byte[MAX_CACHE_SIZE];
        }
        
        /**
         * Releases memory to be able to flush the buffer.
         * @see #preallocate()
         */
        private static void releaseMemory() {
            preallocatedMemory = null;
            System.gc();
        }
        
        public void write(byte[] b) throws Exception {
            this.writedData = b;
            write();
        }

        protected void appendDataToCache() {
            try {
                cache.write(writedData);
            } catch (IOException iOException) {
                iOException.printStackTrace();
                  // OutOfMemoryError is thrown, because it is handled differently
                throw new OutOfMemoryError();
            }
        }

        public void clearCache() {
            //cache.reset();
            cache = new ByteArrayOutputStream();
            System.gc();
        }
        
        

        protected int cacheLenght() {
            return cache.size();
        }

        public int bufferSize() {
            return cache.size();
        }
        
        

        protected void makeNewCacheForWritedData() {
            clearCache();
        }

        public void flush() throws Exception {
            try {
                releaseMemory();
                cache.flush();
                writeDataFromBuffer(cache.toByteArray());
            } catch (IOException iOException) {
                iOException.printStackTrace();
                throw new Exception(iOException.getMessage());
            } catch (OutOfMemoryError err) {
                err.printStackTrace();
                throw err;
            } finally {
                clearCache();
                try {
                    preallocate();
                } catch (OutOfMemoryError err) {
                    err.printStackTrace();
                    throw err;
                }
            }
        }

        protected void writeActualData() throws Exception {
            try {
                writeDataFromBuffer(writedData);
            } catch (IOException iOException) {
                iOException.printStackTrace();
                throw new Exception(iOException.getMessage());
            }
        }
        
        /**
         * Writes the data out from this buffer to some output.
         * 
         * @param bufferData the data fro this buffer.
         * 
         * @throws java.lang.Exception
         */
        protected abstract void writeDataFromBuffer(byte[] bufferData) throws Exception;
    }
    
    
    /**
     * The implementation of byte output buffer that writes the data to the 
     * OutputStream.
     */
    public static class OutputStreamBuffer extends ByteOutputBuffer {
        
        private final OutputStream stream;

        public OutputStreamBuffer(OutputStream stream) {
            this.stream = stream;
        }

        protected void writeDataFromBuffer(byte[] bufferData) throws Exception {
            stream.write(bufferData);
        }
    }
    
    /**
     * Abstract class that serves for buffering string data. 
     * 
     * Implementations of this class can vary in the way how the data are
     * writed out from the buffer.
     */
    public abstract static class StringOutputBuffer extends OutputBuffer {
        private StringBuffer cache = new StringBuffer();
        private String writedData;
        
        public void write(String string) throws Exception {
            writedData = string;
            write();
        }
        
        protected void appendDataToCache() {
            // write cache plus string
            cache.ensureCapacity(cache.length() + writedData.length());
            cache.append(writedData);
        }

        public void clearCache() {
            cache = new StringBuffer();
        }

        public int bufferSize() {
            return cache.length();
        }
        
        protected int cacheLenght() {
            return cache.length();
        }

        protected void makeNewCacheForWritedData() {
            cache = null;
            System.gc();
            cache = new StringBuffer(writedData.length());
        }

        public void flush() throws Exception {
            try {
                writeDataFromBuffer(cache.toString());
            } catch (Exception e) {
                throw e;
            } finally {
                cache = new StringBuffer();
            }
        }

        protected void writeActualData() throws Exception {
            writeDataFromBuffer(writedData);
        }
        
        /**
         * Writes the data from this buffer to some output.
         * 
         * @param bufferData the data from this buffer.
         * 
         * @throws java.lang.Exception
         */
        protected abstract void writeDataFromBuffer(String bufferData) throws Exception;
    }
}
