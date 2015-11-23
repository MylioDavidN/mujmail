package mujmail.protocols;

/*
MujMail - Simple mail client for J2ME
Copyright (C) 2006 Nguyen Son Tung <n.sontung@gmail.com>
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

import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import mujmail.BodyPart;
import mujmail.InBox;
import mujmail.Lang;
import mujmail.MessageHeader;
import mujmail.MujMail;
import mujmail.MyException;
import mujmail.Settings;
import mujmail.TheBox;
import mujmail.account.MailAccount;
import mujmail.connections.ConnectionCompressed;
import mujmail.connections.ConnectionInterface;
import mujmail.tasks.BackgroundTask;
import mujmail.tasks.Progress;
import mujmail.tasks.StoppableBackgroundTask;
import mujmail.tasks.StoppableProgress;
import mujmail.ui.AudioAlert;
import mujmail.util.Decode;
import mujmail.util.Functions;

/**
 * The interface for manipulating and downloading mails.
 * For communication with servers uses the object of class BasicConnection
 */
public abstract class InProtocol {
    /** The name of this source file */
    private static final String SOURCE_FILE = "InProtocol";

    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false;

    private static final Object notifier = new Object(); /// Object on which wait fo beeing notify

    protected InProtocolTask inProtocolTask;
    
    /** The body part that is actually parsed. */
    private BodyPart actuallyParsedBodyPart;
    
    // TODO (Betlista): describe these constants, why have REDOWNLOAD_BODY (3) and GET_URL (5) same description?
    //thread run modes
    public static final byte GET_NEW_MAILS = 1;
    public static final byte RETRIEVE_BODY = 2; //retrieving mail bodies
    public static final byte REDOWNLOAD_BODY = 3; //reretreving mail bodies - redownload incompleted mails
    public static final byte REMOVE_MAILS = 4;
    public static final byte GET_URL = 5; //reretreving mail bodies - redownload incompleted mails
    public static final byte SAVE_MAIL_SENT = 6;
    public static final byte POLL = 8;
    public static final byte SET_FLAGS = 9;
    public static final byte REMOVE_FLAGS = 10;
    public static final byte CONVERT_BODY = 11;
    public static final byte CLOSE = 16; 
    
    byte runMode = -1;
    byte reDownloadMode = -1; //-1 for redownloading the whole mail, values >= 0 for redownloading particular bodypart only
    static short instanceRunning = 0; //counts how many threads ALL subclasses of Inprotocol are running
    short threadCount = 0; //counts how many threads a single subclass of Inprotocol are running			
    boolean locked; //for thread synchronizing
    boolean forcedDisc; //if it should disconnect from the server unconditionally		
    MailAccount account; //each server has an account
    private TheBox reportBox = null;
    /** The box to that the action of this object is associated. That means the box to that mails are actually downloded etc. */
    public InBox targetBox = null;
    MessageHeader actHeader; //a header which should be fetched	
    protected ConnectionInterface connection;

    //mailsOnServer store message unique identifier(UID) and message number of mails on the server
    //its used for faster accessing to the message number 
    //and detecting if a concrete mail is on the server - used for inbox-server sync
    //keys are UID(String) objects are message numbers(String) in the case of POP3, 
    //in case the case of IMAP4 objects are just random number, because we don't need to operate with message numbers in IMAP implementation	
    Hashtable mailsOnServer;

    String END_OF_MAIL; //a string that indicates the end of transmitting of a mail body
    String saveMailData;  // data to save into server mailbox if saving on server set on
    String flagsToSet = "()";
    
    public InProtocol(MailAccount account) {
        //super("In protocol task");
        this.account = account;
        connection = new ConnectionCompressed();
        mailsOnServer = new Hashtable();
    }

    //return if the object is busy
    public boolean isBusy() {
        return (threadCount > 0) ? true : false;
    }

    /**
     * Handles input flags - set processed message header flags according to 
     * these flags.
     * @param flags the part of protocol line with flags
     */
    protected void handleFlags(MessageHeader msgHeader, String flags) {
        if (DEBUG) { System.out.println("DEBUG InProtocol.handleFlags " + actHeader); }
        if (flags.indexOf("\\Seen") != -1) {
            msgHeader.markAsRead();
        }
        if (flags.indexOf("\\Answered") != -1) {
            msgHeader.markAsReplied();
        }
        if (flags.indexOf("\\Deleted") != -1) {
            msgHeader.markAsDeleted();
        }
        if (flags.indexOf("\\Flagged") != -1) {
            msgHeader.markAsFlagged();
        }
    }
    
    //zvysi counter pocet bezicich threadu
    protected synchronized void inThread() {
        ++threadCount;
    }

    protected synchronized void decThread() {
        --threadCount;
    }
    //returns if some of all instances (POP, IMAP, SPOP..) of inProtocol are running, resp. if the class Inprotocol is busy.
    public static boolean isBusyGlobal() {
        return instanceRunning > 0 ? true : false;
    }

    protected synchronized void incThreadGlobal() {
        ++instanceRunning;
    }

    protected synchronized void decThreadGlobal() {
        --instanceRunning;
        synchronized(notifier) {
            notifier.notifyAll();
        }
    }

    public void stop() {
        if (isBusy()) {
            connection.quit();
        }
    }

    /*
     * TODO (Betlista): add JavaDoc comment
     */
    public abstract int countNew() throws MyException;

    protected synchronized void lock() {
        try {
            while (locked) {
                wait(100);
            }
        } catch (InterruptedException e) {
        }
        locked = true;
    }

    protected void unlock() {
        locked = false;
    }

    //add a mail to a queue of mails that are going to be deleted from the server
    abstract public void addDeleted(MessageHeader header);

    public boolean containsMail(MessageHeader header) {
        return mailsOnServer.containsKey(header.getMessageID());
    }

    //synchronized to ensure no other threads are changing runMode
    //all these methods must be run in a thread
    /**
     * Retrieve mails from server
     * @param box Target inbox where add mails
     */
    public synchronized void getNewMails(InBox box) {
          if (DEBUG) System.out.println("DEBUG InProtocol.getNewMails(InBox) - getting new mails");
        incThreadGlobal();
        runMode = InProtocol.GET_NEW_MAILS;
        this.targetBox = box;
        this.reportBox = box;
        inProtocolTask = new InProtocolTask(this, "Getting new mails");
        inProtocolTask.start(targetBox, MujMail.mujmail.getMenu());
    }

    /**
     * This method should be called if polling discovers mail with given ID.
     * @param ID id of new mail that was discovered by polling.
     * @return true if this is new email: that means that it  was not yet
     *  downloaded.
     */
    protected boolean handleMailDiscoveredByPolling(String ID) {
            if (!getTargetBox().wasOnceDownloaded(account.getEmail(), ID)) {
                if (Settings.pollDownloadsMails) {
                    getNewMails( getTargetBox());
                }
                if (Settings.pollPlaysSound) {
                    new AudioAlert();
                }
                return true;
            }

        return false;
    }

    /**
     * Blocks calling thread until no InProtocol action of any InProtocol
     * instance is running.
     */
    public static void waitForNotBusyGlobal() {
        try {
            synchronized(notifier) {
                if (!isBusyGlobal()) return;
                notifier.wait();
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Finds first new mail while polling. The connection is already open.
     * @throws mujmail.MyException
     */
    protected abstract void findFirstNewMailWhilePolling() throws MyException;

    /**
     * <p>Opens the connection.</p>
     * <p>Note: task can be null (polling).</p>
     * 
     * @param task
     * @return
     * @throws mujmail.MyException
     */
    protected abstract boolean open(BackgroundTask task) throws MyException;

    private void resolveMyExceptionWhileRunning(MyException ex) {
        if (ex.getErrorCode() == MyException.COM_HALTED) {
            connection.unQuit();
        }
        resolveExceptions(ex.getDetails() + "/ " + account.getEmail(), SOURCE_FILE);
    }

    protected abstract void getNewMails();
    protected abstract void downloadBody();
    protected abstract void removeMails();
    protected abstract void setFlags();
    protected abstract void removeFlags();
    protected abstract void getURL() throws MyException;

    public void doWork() {
        //polling is an extra thread, is not counted by inThread() or inThreadGlobal()
        if (doPolling()) return;

        //if its a forced disconnect, then we will not wait for someone by calling lock()
        if (runMode == InProtocol.CLOSE && forcedDisc) {
            closeForceDist();
            return;
        }

        try {
            //we use lock() instead of making run() synchronized in order to allow forced disconnecting from servers
            //even some jobs are still running
            lock();
            inThread();
            getReportBox().report(Lang.get(Lang.ALRT_INITIATING) + Lang.get(Lang.ALRT_WAIT), SOURCE_FILE); // Not in background task no setTitle
            connection.unQuit();
            connection.clearInput();

            switch (runMode) {
                case InProtocol.CLOSE:
                    _close(inProtocolTask);
                    break;
                case InProtocol.GET_NEW_MAILS:
                    getNewMails();
                    break;

                case InProtocol.REDOWNLOAD_BODY:
                case InProtocol.RETRIEVE_BODY:
                case InProtocol.CONVERT_BODY:
                    downloadBody();
                    break;

                case InProtocol.REMOVE_MAILS:
                    removeMails();
                    break;
                case InProtocol.SET_FLAGS:
                	setFlags();

                	break;
                case InProtocol.REMOVE_FLAGS:
                    removeFlags();
                	break;
                case IMAP4.GET_URL:
                    getURL();
                    break;
            }

            
        } catch (MyException ex) {
            ex.printStackTrace();
            resolveMyExceptionWhileRunning( ex);
        } catch (Error e) {
            e.printStackTrace();
            resolveExceptions("100: " + e + "/ " + account.getEmail(), SOURCE_FILE);
        } catch (Exception ex) {
            ex.printStackTrace();
            resolveExceptions("100: " + ex + "/ " + account.getEmail(), SOURCE_FILE);
        }

        actionAfterTasksCompleted();


        
    }

    public synchronized void getBody(MessageHeader header, InBox box) {
          if (DEBUG) System.out.println("DEBUG InProtocol.getBody(MessageHeader) - before execution");
        
        if (actHeader != null) //already fetching
        {
            if (DEBUG) { System.out.println("DEBUG InProtocol.getBody already fetching"); }
            return;
        }
        //ok, its not nice thing to put inThreadGlobal() immediately here, to make InProtocol be busy
        //but it triggers inBox to draw the progress bar right after we press an action button
        //otherwise it can make an impatient user pressing the buttons more times thinking that the operation didnt undergo.
        //and mess our parsing
        incThreadGlobal();
        actHeader = header;
        reDownloadMode = -1;
        runMode = InProtocol.RETRIEVE_BODY;
        this.targetBox = box;
        this.reportBox = box;

        if (DEBUG) { System.out.println("DEBUG InProtocol.getBody starting retrieving body"); }
        inProtocolTask = new InProtocolTask(this, "Retrieving mail body");
        inProtocolTask.start();
    }

    /**
     * Sets the given message flags at server side. 
     * @param header			Message to set flags for
     * @param flags				Flags to set, e.g. "(\Seen \Answered)"
     */
    public synchronized void setFlags(MessageHeader header, String flags, int ACTION, TheBox reportBox) {
        //ok, its not nice thing to put inThreadGlobal() immediately here, to make InProtocol be busy
        //but it triggers inBox to draw the progress bar right after we press an action button
        //otherwise it can make an impatient user pressing the buttons more times thinking that the operation didnt undergo.
        //and mess our parsing
        incThreadGlobal();
        
        actHeader = header;
        flagsToSet = flags;
        this.reportBox = reportBox;
        this.targetBox = (InBox)reportBox;			//TODO Ask David if it is correct
        if (ACTION == SET_FLAGS)
        	runMode = InProtocol.SET_FLAGS;
        else
        	runMode = InProtocol.REMOVE_FLAGS;
        inProtocolTask = new InProtocolTask(this, "Setting flags at IMAP server");
        inProtocolTask.start();
    }
    
    //called to redownload a partial mail (some parts of a mail is missing)
    public synchronized void getConvertedBody(MessageHeader header, byte mode, InBox box) {
        if (DEBUG) { System.out.println("DEBUG InProtocol.getConvertedBody before execution"); }
        
        if (actHeader != null) //already fetching
        {
            return;
        }
        incThreadGlobal();
        actHeader = header;
        reDownloadMode = mode;
        runMode = InProtocol.CONVERT_BODY;
        this.targetBox = box;
        this.reportBox = box;
        //Thread t = new Thread(this);
		//t.start();
        inProtocolTask = new InProtocolTask(this, "Converting bodypart");
        inProtocolTask.start();
        //t.setPriority(Thread.MAX_PRIORITY);
    }

    //called to redownload a partial mail (some parts of a mail is missing)
    public synchronized void regetBody(MessageHeader header, byte mode, InBox box) {
        if (DEBUG) {  System.out.println("DEBUG InProtocol.regetBody - before execution"); }
        
        if (actHeader != null) //already fetching
        {
            return;
        }
        incThreadGlobal();
        actHeader = header;
        reDownloadMode = mode;
        runMode = InProtocol.REDOWNLOAD_BODY;
        this.targetBox = box;
        this.reportBox = box;
        inProtocolTask = new InProtocolTask(this, "Redownloading mail body");
        inProtocolTask.start();
    }

    // deletes all the mails that are in the queue deleted
    public synchronized void removeMsgs(InBox box) {
        if (DEBUG) { System.out.println("DEBUG InProtocol.removeMsgs - before execution"); } 
        
        incThreadGlobal();
        runMode = InProtocol.REMOVE_MAILS;
        this.targetBox = box;
        this.reportBox = box;
        inProtocolTask = new InProtocolTask(this, "Removing mails");
        inProtocolTask.start();
    }

    public synchronized void close(boolean forcedDisc, TheBox reportBox) { //closes and must close streams for correct reopen/relogin
        if (this.forcedDisc) //some thread allready wants really to disconnect
        {
            return;
        }
        incThreadGlobal();
        this.forcedDisc = forcedDisc;
        runMode = InProtocol.CLOSE;
        this.reportBox = reportBox;
        inProtocolTask = new InProtocolTask(this, "Closing connection");
        inProtocolTask.start();
    }

    public synchronized void poll(InBox box) {
        runMode = InProtocol.POLL;
        this.targetBox = box;
        this.reportBox = box;
        inProtocolTask = new InProtocolTask(this, "Polling");
        inProtocolTask.disableDisplayingProgress();
        inProtocolTask.start();
    }

    public synchronized void saveMailToSent(String inMailText, InBox box) {
        if (DEBUG) { System.out.println("DEBUG InProtocol.saveMailToSent " + inMailText); }
        saveMailData = inMailText;
        incThreadGlobal();
        runMode = InProtocol.SAVE_MAIL_SENT;
        this.targetBox = box;
        this.reportBox = box;
        inProtocolTask = new InProtocolTask(this, "Saving mails to server's Sent folder");
        inProtocolTask.start();
    }

    private boolean doPolling() {
        if (runMode == InProtocol.POLL) {
            lock();
            try {
                connection.unQuit();
                connection.clearInput();
                if (open(null)) {
                    findFirstNewMailWhilePolling();
                }
            } catch (MyException ex) {
                ex.printStackTrace();
                //do nothing, be silent bitch
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            unlock();
            return true;
        }
        return false;
    }

    /**
     * Handles input line with flags - set processed message header flags 
     * according to these flags.
     * @param line protocol line with flags
     */
    private void handleLineWithFlags(String line) {
        if (!line.startsWith(" FLAGS (")) return; 
        
        String flags = line.substring(8); //length of " FLAGS ("
        //length of " FLAGS ("
        flags = flags.substring(0, flags.indexOf(")"));
        handleFlags(actHeader, flags);
    }

    /**
     * Parses body of the multipart e-mail.
     * 
     * @param header
     * @param progress
     * @throws MyException
     */
    private void parseBodyOfMultipartMessage(MessageHeader header, StoppableProgress progress) throws MyException {
        //boundary of the actual email body, 
        //it may be changed if the email has many encapsulated emails within its body
        
        //multipart mail: fetch all body parts that we want to fetch

    	//StringBuffer bf = new StringBuffer();
        int linesCount = 0;

        final String endBoundary = header.getBoundary() + "--"; //this is where the email body ends
          if (DEBUG) System.out.println("DEBUG InProtocol.parseBodyOfMultipartMessage - endBoundary=" + endBoundary);
        byte nextIncomplete = 0;
        byte actualBPNumber = -1; //counter of actual body part
        //boundary of the actual email body,
        //it may be changed if the email has many encapsulated emails within its body
        String bodyBoundary = header.getBoundary();
        Stack/*<String>*/ boundaries = new Stack();
          if (DEBUG) System.out.println("DEBUG InProtocol.parseBodyOfMultipartMessage - bodyBoundary=" + bodyBoundary);

        String line = connection.getLine();
          if (DEBUG) System.out.print("DEBUG InProtocol.parseBodyOfMultipartMessage - line='" + line + "'");
          // process the body until end of mail (boundary end is found or END_OF_MAIL)

        boolean wasStopped = false; // true if fetching of bodypart was stopped
        boolean redownloaded = false;
        while ( !line.startsWith(END_OF_MAIL) && !line.startsWith(endBoundary) ) {
            ++actualBPNumber;
            
            ////if it's not IMAP and redownloading a particular bodypart
            //if ( ! (account.isIMAP() && reDownloadMode != -1) ) {
            if ( !account.isIMAP() || reDownloadMode == -1 ) { // Betlista: De Morgan, hope it's the same as line above
                actuallyParsedBodyPart = new BodyPart(header);
                
                //skip everything to the boundary
                while (!line.startsWith(bodyBoundary)) {
                    line = connection.getLine();
                      if (DEBUG) System.out.print("DEBUG InProtocol.parseBodyOfMultipartMessage - line='" + line + "'");
                }
                //but if its also endBoundary then its the end of the mail
                if (line.startsWith(endBoundary)) {
                    break;
                }

                if (runMode == REDOWNLOAD_BODY) {
                    //find an incomplete body part
                    if (reDownloadMode == -1) {
                        nextIncomplete = actualBPNumber;
                    } else {
                        //redownloading a particular part
                        nextIncomplete = reDownloadMode;
                    }

                    //if we want to redownload a body part but it's not the actual one
                    if (actualBPNumber < nextIncomplete) {
                        //skip the line to the body part
                        continue;
                    }
                    //if all incomplete parts have been downloaded, stop the downloading operation
                    if (actualBPNumber > nextIncomplete) {
                        //close(false); //let's save the connection and save the bandwidth
                        break;
                    }
                }

                //parse body parts headers
                //it it has different bodypart's boundary than the actual boundary then its encapsulated email type
                String tmpBoundary = parseBodyPartHeader(actuallyParsedBodyPart.getHeader());
                  if (DEBUG) System.out.println("DEBUG InProtocol.parseBodyOfMultipartMessage - tmpBoundary=" + tmpBoundary);
                if (tmpBoundary != null && !tmpBoundary.equals(bodyBoundary)) {
                	  // we have new boundary
                	  // 1.) add old boundary to "stack"
                	  if (DEBUG) System.out.println( "DEBUG InProtocol.parseBodyOfMultipartMessage - adding boundary: " + bodyBoundary );
                	boundaries.push( bodyBoundary );
                    bodyBoundary = tmpBoundary;
                }

            } else {
                if (redownloaded) {
                    break;
                }
                redownloaded = true;
                //if its IMAP and redownloading a particular bodypart
                //then by executing BODY"+"["+reDownloadMode+"] we reach to the right bodypart
                actualBPNumber = reDownloadMode;
                //make a copy, don't call parseBodyPartHeader(bp) as IMAP protocol reach to data directly already
                if (runMode == CONVERT_BODY) {
                	actuallyParsedBodyPart = header.getBodyPart(actualBPNumber);
                	actuallyParsedBodyPart.createConvertedContentStorage();
                	actuallyParsedBodyPart.switchToConvertedContent();
                        if (DEBUG) { 
                            System.out.print("DEBUG InProtocol.parseBodyOfMultipartMessage - CONTENTSTORAGE==null: ");
                            System.out.println(actuallyParsedBodyPart.getStorage() == null);
                        }
                }
                else {
                   	actuallyParsedBodyPart = new BodyPart(header, header.getBodyPart(actualBPNumber).getHeader());
                }
                connection.unGetLine(); //return back the first bodypart's data line
            }

            //now its bodyparts content, fetch to the boundary
            line = connection.getLine();
              if (DEBUG) {
            	  System.out.print("DEBUG InProtocol.parseBodyOfMultipartMessage - line='" + line + "'");
            	  System.out.println(", bodyBoundary " + bodyBoundary);
              }

            wasStopped = false;
            long bodyPartSize = 0;
            while ( ! line.startsWith(bodyBoundary) && !line.startsWith(END_OF_MAIL) &&  (line.indexOf("OK Success") == -1)) {
            	++linesCount;
                bodyPartSize += line.length();
                if (stopDownloadingBodyPart(progress, bodyPartSize, linesCount)) {
                    handleNotSuccesfullyDownloaded(actuallyParsedBodyPart);
                    wasStopped = true;
                    break;
                }

                try {
                	if (runMode != CONVERT_BODY) {
                		actuallyParsedBodyPart.getStorage().addToContentBuffered(line);
                	}
                	else {
                	    System.out.print("DEBUG InProtocol.parseBodyOfMultipartMessage - adding to converted storage");
						// set temporary encoding for converted bodypart
						//byte enc = actuallyParsedBodyPart.getHeader().getEncoding();
						// TODO: maybe when it is pdf bodypart, it should be not saved as raw
						//actuallyParsedBodyPart.getHeader().setEncoding(BodyPart.ENC_NORMAL);
						// save converted bodypart content
                		actuallyParsedBodyPart.getStorage().addToContentBuffered(line);
                		// set original encoding for bodypart
                		//actuallyParsedBodyPart.getHeader().setEncoding(enc);
                	}
                } catch (Throwable t) {
                    t.printStackTrace();
                    if (DEBUG) { System.out.println("DEBUG InProtocol.parseBodyOfMultipartMessage - Error 1"); }
                    //something's gone wrong during the saving process
                    displayAlertIfFetchingBodyFailed(actuallyParsedBodyPart, progress);
                    handleNotSuccesfullyDownloaded(actuallyParsedBodyPart);
                    break;
                }

                if (runMode != GET_NEW_MAILS) {
                    progress.incActual(line.length());
                }
                line = connection.getLine();
                  if (DEBUG) {
                	  System.out.print("DEBUG InProtocol.parseBodyOfMultipartMessage - line='" + line + "'");
                	  System.out.println(", bodyBoundary " + bodyBoundary);
                  }
            }
            
            if ( line.startsWith( bodyBoundary + "--" ) && !boundaries.isEmpty() ) {
                  if (DEBUG) System.out.println( "DEBUG InProtocol.parseBodyOfMultipartMessage - removing boundary: " + bodyBoundary );
            	bodyBoundary = (String)boundaries.pop();
          	      if (DEBUG) System.out.println( "DEBUG InProtocol.parseBodyOfMultipartMessage - new body boundary: " + bodyBoundary );
            }
            try {
                if (!wasStopped) {
                    if (DEBUG) { System.out.println("DEBUG InProtocol.parseBodyOfMultipartMessage - FLUSHING BUFFER"); }
                    actuallyParsedBodyPart.getStorage().flushBuffer();
                }
            	if (runMode == CONVERT_BODY ) {
                    if (DEBUG) { System.out.println("DEBUG InProtocol.parseBodyOfMultipartMessage - hash="+actuallyParsedBodyPart.hashCode()); }
                    synchronized (actuallyParsedBodyPart) {
                        if (DEBUG) { System.out.println("DEBUG InProtocol.parseBodyOfMultipartMessage - NOTIFYING"); }
                        actuallyParsedBodyPart.notify();
                    }
            	}
            } catch (Throwable t) {
                t.printStackTrace();
                if (DEBUG) { System.out.println("DEBUG InProtocol.parseBodyOfMultipartMessage - Error 2"); }
                displayAlertIfFetchingBodyFailed(actuallyParsedBodyPart, progress);
                handleNotSuccesfullyDownloaded(actuallyParsedBodyPart);
            }

            //now lets save the bodypart\\
            actuallyParsedBodyPart.setOrder(actualBPNumber);

            //redownloading and new bodypart that was successfully saved to the DB and replacing the old bodypart
            if (runMode == REDOWNLOAD_BODY && actuallyParsedBodyPart.atLeastPartial()  && isBigger(header, actuallyParsedBodyPart)) {
                byte old = 0;
                //markAsDeleted the old bodypart from DB
                for (old = (byte) (header.getBodyPartCount() - 1); old >= 0; --old) {
                    if (header.getBpOriginalOrder(old) == actuallyParsedBodyPart.getOrder()) {
                        header.getBodyPart(old).getStorage().deleteContent();
                        break;
                    }
                }
                header.replaceBodyPart(actuallyParsedBodyPart, old); //replace the old one in vector bodyParts
            } else if ((runMode != CONVERT_BODY) && (runMode != REDOWNLOAD_BODY || !isInMail(header, actuallyParsedBodyPart))) {
                header.insertBodyPartAt(actuallyParsedBodyPart, actualBPNumber); //accept any bodypart - partial, empty, complete
            } else if (runMode == REDOWNLOAD_BODY){
                actuallyParsedBodyPart.getStorage().deleteContent();
            }

            if (line.startsWith(endBoundary)) {
                //In some bizarre cases END_OF_MAIL can be at the 
            	//end of the boundary string and not in the next 
            	//line. That's why we need the following
            	//if-statement.
                if (line.indexOf(END_OF_MAIL) == -1) {
	            	line = connection.getLine();
	                  if (DEBUG) System.out.print("DEBUG InProtocol.parseBodyOfMultipartMessage - line='" + line + "'");
	                while ( ! line.startsWith(END_OF_MAIL) ) {
	                	line = connection.getLine();
	                      if (DEBUG) System.out.print("DEBUG InProtocol.parseBodyOfMultipartMessage - line='" + line + "'");
	                }
                }
                break;
            }
            //if it wasn't end boundary but just separating boundary, 
            //return the boundary line back the buffer
            //so it could be detected in the next iteration
            connection.unGetLine();

            if (stopDownloadingBodyPart(progress, 0, linesCount)) {
                wasStopped = true;
                break;
            }
              // get next line
            line = connection.getLine();
              if (DEBUG) System.out.print("DEBUG InProtocol.parseBodyOfMultipartMessage - line='" + line + "'");
              
          if (runMode == CONVERT_BODY) {
              actuallyParsedBodyPart.switchToNotConvertedContent();
          }
        }
        if (runMode == CONVERT_BODY) {
            actuallyParsedBodyPart.switchToNotConvertedContent();
        }
        if (!wasStopped && " FLAGS (".equals(END_OF_MAIL)) //parse FLAGS parameter
        {
        	connection.unGetLine();
        	line = connection.getLine();
              if (DEBUG) System.out.print("DEBUG InProtocol.parseBodyOfMultipartMessage - line='" + line + "'");

            handleLineWithFlags(line);
        }
    }

    private void parseBodyOfPlainMessage(MessageHeader header, StoppableProgress progress) throws MyException, MyException {
        String line;
        int linesCount = 0;

        BodyPart.Header bpHeader;
        //body part header was created already by parseHeader() - when retrieving header and body at once
        if (header.getBodyPartCount() != 0 && runMode != REDOWNLOAD_BODY) {
            actuallyParsedBodyPart = header.getBodyPart((byte) 0);
        } else if (account.getType() == MailAccount.IMAP && reDownloadMode != -1) {
            actuallyParsedBodyPart = new BodyPart(header, header.getBodyPart(0).getHeader());
        } else {
            //downloading or redownloading the whole mail
            bpHeader = new BodyPart.Header();
            actuallyParsedBodyPart = new BodyPart(header, bpHeader);
            parseBodyPartHeader(bpHeader);
            if (DEBUG) { System.out.println("DEBUG InProtocol.parseBodyOfPlainMessage - body part header parsed 1"); }
        }

        //take all the rest
        long bodyPartSize = 0;
        boolean fetchingStopped = false; // true if fetching of body was stopped
        while (!(line = connection.getLine()).startsWith(END_OF_MAIL) && (line.indexOf("OK Success") == -1)) {
            ++linesCount;
            bodyPartSize += line.length();
            if (stopDownloadingBodyPart(progress, bodyPartSize, linesCount)) {
                handleNotSuccesfullyDownloaded(actuallyParsedBodyPart);
                fetchingStopped = true;
                break;
            }

            if (DEBUG) System.out.println("DEBUG InProtocol.parseBodyOfPlainMessage - this bodypart should be saved.");
            
            try {
                actuallyParsedBodyPart.getStorage().addToContentBuffered(line);
            } catch (Exception e) {
                if (DEBUG) { System.out.println("DEBUG InProtocol.parseBodyOfPlainMessage - body part parsed 4"); }

                //something's gone wrong during the saving process
                displayAlertIfFetchingBodyFailed(actuallyParsedBodyPart, progress);
                handleNotSuccesfullyDownloaded(actuallyParsedBodyPart);
            }
         
            //increase the counter of downloaded mails only when we're downloading mail body
            if (runMode != GET_NEW_MAILS) {
                progress.incActual(line.length());
            }
        }

        if (!fetchingStopped && " FLAGS (".equals(END_OF_MAIL)) //parse FLAGS parameter
        {
            handleLineWithFlags(line);
        }
        try {
                actuallyParsedBodyPart.getStorage().flushBuffer();
            } catch (Exception ex) {
                ex.printStackTrace();
                displayAlertIfFetchingBodyFailed(actuallyParsedBodyPart, progress);
                handleNotSuccesfullyDownloaded(actuallyParsedBodyPart);
            }
        
        //redownloading the mail and its body part is saved to the DB
        if (runMode == REDOWNLOAD_BODY && actuallyParsedBodyPart.atLeastPartial() && isBigger(header, actuallyParsedBodyPart)) {
            //markAsDeleted the old bodypart from DB
            header.getBodyPart((byte) 0).getStorage().deleteContent();
            header.replaceBody(actuallyParsedBodyPart); // replace the old one
        } else if (header.getBodyPartCount() == 0) {
            header.addBodyPart(actuallyParsedBodyPart);
        } else {
            actuallyParsedBodyPart.getStorage().deleteContent();
        }

        if (DEBUG) { System.out.println("DEBUG InProtocol.parseBodyOfPlainMessage - body part parsed 4"); }
    }

    /**
     * <p>Returns value for the "boundary" parameter in passed line staring
     * at the position <code>index</code> in the line.</p>
     * 
     * <p>Handles cases like:
     * <ul>
     * <li>boundary=value (returns value)</li>
     * <li>boundary="value" (returns value)</li>
     * <li>boundary="value"; (returns value)</li>
     * </ul>
     * </p>
     * 
     * @param index position in line where the boundary parameter name starts
     * @param line where to find the boundary parameter value in
     * @return boundary parameter value
     */
    private String getBoundaryParamValue( final int index, final String line ) {
		String boundaryValue = line.substring( index + 9 ).trim(); // 9="boundary=".length()
          // if boundary value is quoted: "value" -> value
		if (boundaryValue.charAt(0) == '"') {
			boundaryValue = boundaryValue.substring(1, boundaryValue.indexOf('"', 2));
		}
          // if there is semicolon at the end, remove it
		int semicolonIndex = boundaryValue.indexOf(';');
		if (semicolonIndex != -1) {
			boundaryValue = boundaryValue.substring(0, semicolonIndex);
		}
		return boundaryValue;
	}

    /**
     * <p>Parses the lines in the header of email which begins with the word
     * content.</p>
     * <p>
     * If the email is plain (not multipart) in such lines are information about
     * the header of email body. Thats why it is needed to pass body part header
     * and fill it with information from these lines.
     * If the email is multipart, we need to extract only body part boundary.
     * </p>
     * 
     * @param bpHeader if the email is plain, in content lines are information
     *        about the body of the mail, fill it
     * @return body part boundary if the email is multipart,
     *         null if the message is plain text
     * @throws MyException when there is error in getting line from connection
     *         or in decoding
     */
    private String parseContentLines(BodyPart.Header bpHeader) throws MyException {
        StringBuffer buff = new StringBuffer();
        String bodyPartBoundary = null;

        String line = connection.getLine();
          if (DEBUG) System.out.print( "DEBUG InProtocol.parseContentLines(BodyPart.Header) - line: " + line);
        String lower = line.toLowerCase();
        int i;
        while ( line.charAt(0) == ' ' || line.charAt(0) == '\t' || line.toLowerCase().startsWith("content-") ) {
        	i = lower.indexOf( "boundary=" );
        	if ( i >= 0 ) {
        		bodyPartBoundary = "--" + getBoundaryParamValue( i, line );
        	}
              if (DEBUG) System.out.println("DEBUG InProtocol.parseContentLines(BodyPart.Header) - bodyPartBoundary: " + bodyPartBoundary);

            i = lower.indexOf("name=");
            if (i != -1) {
                bpHeader.setName(Decode.decodeHeaderField(line.substring(i + 5)).trim());
                if (bpHeader.getName().charAt(0) == '"') {
                    bpHeader.setName( bpHeader.getName().substring(1, bpHeader.getName().indexOf('"', 2)));
                } else if (bpHeader.getName().indexOf(';', 1) != -1) {
                    bpHeader.setName( bpHeader.getName().substring(0, bpHeader.getName().indexOf(';', 1)) );
                }
                //let's remove the filename from the string preventing detecting bad charset, 
                //in case the filename itself is named as some charset like windows-1250.txt
                lower = lower.substring(0, i) + lower.substring(i + 5 + bpHeader.getName().length());

            }
            buff.append(lower);
            
            line = connection.getLine();
              if (DEBUG) System.out.print( "DEBUG InProtocol.parseContentLines(BodyPart.Header) - line: " + line);
            lower = line.toLowerCase();
        }
        connection.unGetLine();

        //we may be searching in the big String lower, but only once, making it a little faster
        lower = buff.toString();
        if (lower.indexOf("windows-1250") != -1) {
            bpHeader.setCharSet (BodyPart.CH_WIN1250);
        } else if (lower.indexOf("iso-8859-1") != -1) {
            bpHeader.setCharSet( BodyPart.CH_ISO88591 );
        } else if (lower.indexOf("iso-8859-2") != -1) {
            bpHeader.setCharSet( BodyPart.CH_ISO88592 );
        } else if (lower.indexOf("utf-8") != -1) {
            bpHeader.setCharSet( BodyPart.CH_UTF8 );
        }
        
        if (lower.indexOf("image/") != -1 || lower.indexOf("video/") != -1 || lower.indexOf("audio/") != -1) {
            bpHeader.setBodyPartContentType( BodyPart.TYPE_MULTIMEDIA );
        } else if (lower.indexOf("application/") != -1) {
            bpHeader.setBodyPartContentType( BodyPart.TYPE_APPLICATION );
        } else if (lower.indexOf("text/html") != -1) {
            bpHeader.setBodyPartContentType( BodyPart.TYPE_HTML );
        } else if (lower.indexOf("text/") == -1 && lower.indexOf("content-type") != -1) {
            bpHeader.setBodyPartContentType( BodyPart.TYPE_OTHER );
        }
        
        int j = lower.indexOf("content-transfer-encoding:");
        if (j != -1) {
            j += 26;
            if (lower.indexOf("base64", j) != -1) {
                bpHeader.setEncoding( BodyPart.ENC_BASE64 );
            } else if (lower.indexOf("quoted-printable", j) != -1) {
                bpHeader.setEncoding( BodyPart.ENC_QUOTEDPRINTABLE );
            } else if (lower.indexOf("8bit", j) != -1) {
                bpHeader.setEncoding( BodyPart.ENC_8BIT );
            }
        }
        
          if (DEBUG) System.out.println("DEBUG InProtocol.parseContentLines(BodyPart.Header) - returning bodyPartBoundary: " + bodyPartBoundary);
        return bodyPartBoundary;
    }

    /**
     * Parses the header of body part in multiparted messages.
     * If the message is plain, the information about the header of body are
     * parsed from the lines begining with word content in the header of the
     * message. See parseContentLines.
     * @param bpHeader
     * @return the boundary of the body part header
     * @throws MyException
     * @see parseContentLines
     */
    private String parseBodyPartHeader(BodyPart.Header bpHeader) throws MyException {
          if (DEBUG) { System.out.println("DEBUG InProtocol.parseBodyPartHeader - before execution"); }
        String line = connection.getLine();
        String lower;
        String bodyPartBoundary = null;
        while ( ! "\r\n".equals(line) ) {
            lower = line.toLowerCase();
            if (lower.startsWith("content-")) {
            	  // return line back, because parseContentLines() needs it
                connection.unGetLine();
                bodyPartBoundary = parseContentLines(bpHeader);
            }
            line = connection.getLine();
        }
        return bodyPartBoundary;
    }

    /**
     * Parses and sets charset, types... of header.
     * 
     * According to MIME, if the message is plain, the information about the body 
     * of such message are in the lines of the message header beginning with
     * the word content. So such information are parsed during parsing of the
     * message header. 
     * Thats why when the message is plain, also bodypart is created and added to
     * the header. See method parseContentLines() for more details.
     * 
     * @param header the header to be parsed
     * @throws MyException
     */
    protected void parseHeaders(MessageHeader header) throws MyException {
        try {
            String line, lower;
            //if downloading the whole mail is required then we'll create a BodyPart
            //because of for a plain (non-mulitpart) mail some information like Charset, MIME encoding are stored in headers lines
            BodyPart.Header bpHeader = (Settings.downWholeMail && !Settings.safeMode) ? new BodyPart.Header() : null;
    
            while (!(line = connection.getLine()).equals("\r\n")) {
                lower = line.toLowerCase();
    
                if (lower.startsWith("from:")) {
                    header.setFrom(Decode.decodeHeaderField(line.substring(5)).trim());
                } else if (lower.startsWith("to:") || lower.startsWith("cc:") || lower.startsWith("bcc:")) {
                    // parse a multiline recipients
                    StringBuffer line2 = new StringBuffer();
                    do {
                        line2.append(line);
                        line = connection.getLine();
                    } while (line.startsWith("\t") || line.startsWith(" "));
                    connection.unGetLine();
                    String rcps = line2.toString().trim();
                    if (rcps.length() > 0) {
                        header.addRecipient(Decode.decodeHeaderField(line2.toString()).trim());
                    }
    
                } else if (lower.startsWith("subject:")) {
                    // parse a multiline subject
                    StringBuffer line2 = new StringBuffer();
                    do {
                        line2.append(line);
                        line = connection.getLine();
                    } while (line.startsWith("\t") || line.startsWith(" "));
                    header.setSubject(Decode.decodeHeaderField(line2.toString().substring(8)).trim());
                    connection.unGetLine();
                } else if (lower.startsWith("date:")) {
                    header.setTime(Functions.getStrToLongTime(lower.substring(5)));
                } else if (lower.startsWith("status:") && line.indexOf("R", 7) != -1) { //this is not defined by rfc, doesnt work on all servers
                    header.markAsRead();
                } else if (lower.startsWith("x-status:") && line.indexOf("A", 9) != -1) { //this is not defined by rfc, doesnt work on all servers
                    header.markAsReplied();
                } else if (lower.startsWith("content-")) {
                    do {
                        if (lower.indexOf("multipart") != -1) {
                            header.markAsMultipart();
                        }
                        final int index = lower.indexOf("boundary=");
                        if ( index != -1) {
                            setBoundary( header, index, line, lower );
                        }
                        
                        // if we for the present don't know whether the mail is multipart
                        // and we want to download whole mail (bp != null -- safer than checking Settings.downloadWholeMail).
                        else if (bpHeader != null) {
                            // if the message is plain, the information about the body are
                            // stored in the header of the message so we need to parse
                            // it and store it to the bpHeader
                            // if the message is multipart, we need only,the boundary 
                            // of body parts (and we don't need bpHeader)
                            connection.unGetLine();
                            String tmpBoundary = parseContentLines(bpHeader);
                              if (DEBUG) System.out.println( "DEBUG parseHeaders(MessageHeader) - " + tmpBoundary );
                            if (tmpBoundary != null) { //if it has a boundary 
                                header.markAsMultipart();// then it must be multipart mail
                                header.setBoundary(tmpBoundary);
                            }//else we have parsed usefull info for nonmutli mail like content transfer, charset..						
                        }
    
                        line = connection.getLine();
                        lower = line.toLowerCase();
                    } while (lower.startsWith(" ") || lower.startsWith("\t"));
                    connection.unGetLine();
                } else if ( lower.startsWith( "message-id" ) ) {
                    int startIndex = lower.indexOf("<");
                    int endIndex = lower.indexOf(">", startIndex);
                    if ( startIndex > -1 ) {
                        final String threadingMessageID = line.substring( startIndex + 1, endIndex );
                        header.setThreadingMessageID( threadingMessageID );
                          if (DEBUG) { System.out.println("DEBUG InProtocol.parseHeaders(MessageHeader)[message-id] - " + threadingMessageID); }
                    }
                } else if ( lower.startsWith( "in-reply-to" ) ) {
                    int startIndex = lower.indexOf("<");
                    int endIndex = lower.indexOf(">", startIndex);
                    if ( startIndex > -1 ) {
                        final Vector parentIDs = header.getParentIDs();
                          if (DEBUG) { System.out.println("DEBUG InProtocol.parseHeaders(MessageHeader)[in-reply-to] - parentIDs: " + parentIDs); }
                        final String parentID = line.substring( startIndex + 1, endIndex);
                          if (DEBUG) { System.out.println("DEBUG InProtocol.parseHeaders(MessageHeader)[in-reply-to] - parentID: " + parentID); }
                        header.setParentID( parentID );
                          if (DEBUG) { System.out.println("DEBUG InProtocol.parseHeaders(MessageHeader)[in-reply-to] - parentID set"); }
                        if (parentIDs == null) {
                            if (DEBUG) { System.out.println("DEBUG InProtocol.parseHeaders(MessageHeader)[in-reply-to] - parentIDs vector is null !"); }
                        }
                    }
                } else if (lower.startsWith("references") ) {
                    int startIndex = lower.indexOf("<");
                    int endIndex = lower.indexOf(">", startIndex);
                    final Vector/*<String>*/ parentIDs = header.getParentIDs();
                      if (DEBUG) { System.out.println("DEBUG InProtocol.parseHeaders(MessageHeader)[references] - parentIDs: " + parentIDs); }
                    String parentID;
                    while ( startIndex > -1) {
                        parentID = line.substring(startIndex + 1, endIndex);
                          if (DEBUG) { System.out.println("DEBUG InProtocol.parseHeaders(MessageHeader)[references] - parentID: " + parentID); }
                        if ( !parentIDs.contains( parentID ) ) {
                            parentIDs.addElement( parentID );
                        }
                        startIndex = lower.indexOf("<", endIndex);
                          // when startIndex is -1 end index points to first
                          //   occurrence of '>', but it doesn't matter, because
                          //   loop ends when startIndex = -1
                        endIndex = lower.indexOf(">", startIndex);
                    } // while
                } // if
            }
            
            header.ensureRecipient(account.getEmail());
            
            //if we want to download whole mail (bp != null) and the message is plain
            // we have already parsed information about body part header from the 
            // message header. So insert the prepared bodypart bp for latter parseBody() call
            if (bpHeader != null && header.isPlain()) {
                BodyPart bp = new BodyPart(header, bpHeader);
                header.addBodyPart(bp);
            }
            header.setAccountID(account.getEmail());
            header.ensureRecipient(account.getEmail());
            
            //if we want to download whole mail (bp != null) and the message is plain
            // we have already parsed information about body part header from the 
            // message header. So insert the prepared bodypart bp for latter parseBody() call
            if (bpHeader != null && header.isPlain()) {
                BodyPart bp = new BodyPart(header, bpHeader);
                header.addBodyPart(bp);
            }
            header.setAccountID(account.getEmail());

              // references field sometimes doesn't contain parentID
              //   (In-Reply-To) field, so we add this one to the references
            final Vector parentIDs = header.getParentIDs();
            final String parentID = header.getParentID();
            if ( ! "".equals( parentID ) && ! parentIDs.contains( parentID ) ) {
                parentIDs.addElement( parentID );
                  if (DEBUG) { System.out.println("DEBUG InProtocol.parseHeaders(MessageHeader)[in-reply-to] - parentID added to parentIDs vector"); }
            }
              // "Message-ID" filed from e-mail header is optional
              //   when it's missing, it's replaced with messageID
            String threadingMessageID = header.getThreadingMessageID();
            if ( threadingMessageID == null
                    || "".equals( threadingMessageID ) ) {
                header.setThreadingMessageID( header.getMessageID() );
            }
        } catch (RuntimeException re) {
            if (DEBUG) { System.err.println("ERROR InProtocol.parseHeader() - uncautched exception"); }
            re.printStackTrace();
            throw re;
        }
        
    }

    /**
     * Parse the line to get boundary parameter value and it is set to message
     * header.
     * 
     * @param header the header that boundary is set for
     * @param line the input line with boundary
     * @param lower the lower case version of line
     * @see #getBoundaryParamValue(int, String)
     */
    private void setBoundary( final MessageHeader header, final int index, final String line, final String lower ) {
    	  if (DEBUG) System.out.println("setBoundary(line='" + line + "', lower='" + lower + "')");
    	final String boundaryValue = getBoundaryParamValue( index, line );
        header.setBoundary( "--" + boundaryValue );
    }
    
    /**
     * Determines whether given mail should be redownloaded.
     * 
     * @param bp the body part which has been yet downloaded
     * @param header the header of original message
     * @return
     */
    private boolean shouldBeRedownloaded(BodyPart bp, MessageHeader header) {
        return true;
        // TODO: (David) before redownloading we don't know size of bodypart that will
        // be downloaded so bp.getSize() is always zero so it makes no sence to
        // compare it with old one.
        // That means want to always redownload the message.
        // Get rid of this method?


//        if (header.isPlain()) {
//
//            // redownload the e-mail, if newly downloaded body part is better, that
//        	//   means it's bigger than the old one or if the e-mail is not
//        	//   downloaded yet (header.getBodyPartCount() == 0)
//            if (  (header.getBodyPartCount() == 0 || bp.getSize() > header.getBpSize((byte) 0)) ) {
//                return true;
//            }
//
//            return false;
//        } else {
//        	// if body part is not in e-mail yet or body part is better
//        	//   (that means it's bigger than the old one)
//            if ( (!isInMail(header, bp) || isBigger(header, bp) ) ) {
//                return true;
//            }
//
//            return false;
//        }
    }

    /**
     * Parses data in the body of the mail: this means data after the header
     * of the mail.
     * @param header
     * @param progress used to display progress and stop the action
     * @throws MyException
     */
    protected void parseBody(MessageHeader header, StoppableProgress progress) throws MyException {
        // TODO: there can occur OutOFMemoryError when reading line from the
        // connection. Catch such error, flush buffers, unGetLine and read
        // the line again.
        
        if (DEBUG) { System.out.println("DEBUG InProtocol.parseBody - before execution"); }

        try {
            //if plain mail, then its just text or HTML: fetch it
            if (header.isPlain()) {
                parseBodyOfPlainMessage(header, progress);
            } else { //multipart mail: fetch all body parts that we want to fetch
                parseBodyOfMultipartMessage(header, progress);
            }
        } catch (Throwable err) {
            // delete the content of last downloaded bodypart
            err.printStackTrace();
            handleNotSuccesfullyDownloaded(actuallyParsedBodyPart);
            if (runMode == CONVERT_BODY) {
                actuallyParsedBodyPart.switchToNotConvertedContent();
            }
            
            throw new MyException(0, err);
        }
        
        System.out.println("Header = " + header);

    }

    //this function clears partially downloaded mails
    //if something went wrong  and other previous bodyparts were stored to the DB
    //but those bodyparts will never be accessible, because we could not update headers info about them into DB. 
    //thus those unreacheable bodyparts will cause a DB leak. 
    protected void clear(MessageHeader header) {
        for (byte i = (byte) (header.getBodyPartCount() - 1); i >= 0; --i) {
            try {
                header.getBodyPart(i).getStorage().deleteContent();
            } catch (Exception exp) {
            	exp.printStackTrace();
            }
        }
        //also mark the mail as not downloaded(no bodyparts) so next time it could be downloaded
        header.deleteAllBodyParts();
    }

    //clears and wakes up MailForm (or anything having actheader as semaphore)
    protected synchronized void fixAndNotify() {
        if (DEBUG) { System.out.println("DEBUG InProtocol.fixAndNotify - before execution"); }
        
        if (actHeader == null) {
            return;
        }
        try {
            actHeader.saveHeader();
            //box.getMailDB().saveHeader(actHeader);
        } catch (Exception any) {
            clear(actHeader);
        }
        
        if (DEBUG) { System.out.println("DEBUG InProtocol.fixAndNotify - before actHeader"); }
        
        synchronized (actHeader) {
            actHeader.notify();
            actHeader = null;
        }
        
        if (DEBUG) { System.out.println("DEBUG InProtocol.fixAndNotify - after actHeader"); }
    }

    //solves an error raised during common function
    protected void resolveExceptions(String errorDetails, String sourceFile) {
        if (getTargetBox() != null && getTargetBox().isSyncRunning())
        {
            ((InBox)getTargetBox()).synchronizationError();
        } //something's gone wrong, now we have to stop sync
        if (runMode == InProtocol.RETRIEVE_BODY || runMode == InProtocol.REDOWNLOAD_BODY) {
            fixAndNotify();
        }
        getReportBox().report(errorDetails, sourceFile + " -> " + SOURCE_FILE);
    }

    /**
     * Displays the alert if fetching body failed.
     */
    private void displayAlertIfFetchingBodyFailed(BodyPart bp, Progress progress) {
        progress.setTitle(Lang.get(Lang.ALRT_SAVING) +
                bp.getMessageHeader().getSubject() + "/" +
                bp.getHeader().getName() + " " +
                Lang.get(Lang.FAILED) + " " +
                Lang.get(Lang.ALRT_SYS_LOW_MEMORY) + " / " +
                Lang.get(Lang.ALRT_SYS_NO_DBSPACE));
    }
    
    private void handleNotSuccesfullyDownloaded(BodyPart bp) {
        if (MessageHeader.canBePartiallySaved(bp)) {
            try {
                bp.getStorage().flushBuffer();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            bp.setBodyState(BodyPart.BS_PARTIAL);
        } else {
            System.out.println("Cannot be partially saved");
            if (bp.getBodyState() != BodyPart.BS_EMPTY) bp.getStorage().deleteContent();
            bp.setBodyState(BodyPart.BS_EMPTY);
        }
        System.out.println("After handle not succesfully");
    }

    //check whether a bodypart is in the header
    private boolean isInMail(MessageHeader header, BodyPart bp) {
        for (byte i = (byte) (header.getBodyPartCount() - 1); i >= 0; --i) {
            if (header.getBpOriginalOrder(i) == bp.getOrder()) {
                return true;
            }
        }
        return false;
    }

    //checks whether the bodypart bp is bigger than the same bodypart stored in header
    private boolean isBigger(MessageHeader header, BodyPart bp) {
        for (byte i = (byte) (header.getBodyPartCount() - 1); i >= 0; --i) {
            if (header.getBpOriginalOrder(i) == bp.getOrder() && header.getBpSize(i) < bp.getSize() ) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isImap() {
        return false;
    }
    
    /** @return box used for reporting errors */
    protected TheBox getReportBox() {
        return reportBox;
    }

    /** @return InBox where mail changes (adding, removing) take place */
    protected InBox getTargetBox() {
        return targetBox;
    }

    /**
     * Do the work of closing the connection.
     * @param task TODO: add description
     * @param waitForReply true if it should be waited for reply from the server
     * 	      after sending closing command.
     */
    protected abstract void _close(BackgroundTask task, boolean waitForReply);

    /**
     * TODO (Betlista): add description
     * @param task TODO: add description
     */
    protected void _close(BackgroundTask task) {
    	_close(task, true);
    }
 
    /**
     * closing connection when forcedDisc is true.
     */
    protected void closeForceDist() {
        //setAlert(Lang.get(Lang.ALRT_PL_CLOSING) + account.getEmail());
    	inProtocolTask.updateProgress(2, 1);
    	inProtocolTask.setTitle(Lang.get(Lang.ALRT_PL_CLOSING) + account.getEmail());
        _close(inProtocolTask);
        //setAlert(Lang.get(Lang.ALRT_PL_CLOSING) + account.getEmail() + Lang.get(Lang.SUCCESS));
        inProtocolTask.updateProgress(2, 2);
        inProtocolTask.setTitle(Lang.get(Lang.ALRT_PL_CLOSING) + account.getEmail() + Lang.get(Lang.SUCCESS));
        forcedDisc = false;
        decThreadGlobal();
        //getReportBox().repaint();
    }

    private void actionAfterTasksCompleted() {
            decThread();
            decThreadGlobal();
            unlock();
            synchronized (getTargetBox()) {
                //if im the last headerRetrieving thread, i should resort the box
                    if (getTargetBox().isNeedResort() && !InProtocol.isBusyGlobal()) {
                        incThreadGlobal(); //this will make thebox trigger the progress bar
                        getTargetBox().resort();
                        getTargetBox().setNeedResort(false);
                        getTargetBox().setCurFirstUnread();
                        decThreadGlobal();
                    }
                    //if its servers sync notify the box to completed the sync process
                    if (getTargetBox().isSyncRunning()) {
                        getTargetBox().serversSync(inProtocolTask);
                    }
            }
            //getReportBox().repaint();
        }
    
    protected static class InProtocolTask extends StoppableBackgroundTask {
    	private final InProtocol inProtocol;
    	
    	public InProtocolTask(InProtocol inProtocol, String label) {
    		super(label);
    		this.inProtocol = inProtocol;
    	}
    	
    	public void doWork() {
    		inProtocol.doWork();
    	}
    	
    	
    }

    /**
     * Returns true if getting body part should be stopped.
     * @param progress the progress that indicates whether getting should be
     *  stopped
     * @param bodyPartSize the size of already downloaded bodypart.
     * @param linesCount the number of lines that were already downloaded.
     * @return true if downladng body part should be stopped.
     */
    private boolean stopDownloadingBodyPart(StoppableProgress progress, long bodyPartSize, int linesCount) {
        return progress.stopped() || bodyPartSize > Settings.getMaxSizeOfBodypart() || (Settings.maxLinesRetrieve != -1 && linesCount > Settings.maxLinesRetrieve);
    }

}
