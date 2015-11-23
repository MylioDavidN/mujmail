
package mujmail.protocols;

/*
MujMail - Simple mail client for J2ME
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

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import mujmail.Lang;
import mujmail.MessageHeader;
import mujmail.MujMail;
import mujmail.MyException;
import mujmail.Settings;
import mujmail.account.MailAccount;
import mujmail.connections.ConnectionCompressed;
import mujmail.tasks.BackgroundTask;
import mujmail.tasks.StoppableBackgroundTask;
import mujmail.threading.Algorithm;
import mujmail.ui.AudioAlert;
import mujmail.util.Functions;

/**
 * Implements InProtocol using IMAP4.
 * 
 * The advantages of implementation using IMAP4 compared to POP3:
 * - it can use the command UID_SEARCH_UNSEEN for access to identifiers of 
 * unread mails
 * - easier deleting of mails (without disconnecting from the server)
 * - getting of required bodypart
 * 
 * IMAP supports more parallel connections to one account so identification of
 * the connection must be added before each command
 * 
 */
public class IMAP4 extends InProtocol {
    
    /** The name of this source file */
    private static final String SOURCE_FILE = "IMAP4";

    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false;

    int commandCounter = 0;
    //name of a selected IMAP4 folder followed with it's UIDVALIDITY encapsulated within two slashes //	
    String sld_mailBox_uidval; //so looks like: INBOX/12313/
    String flags;
    int indexOfFlags = -1;
    Vector deleted;
    Timer connectionKeeper;
    boolean pushing = false;

    protected boolean isMujMailServer = false; /// Flag signaling that you are connected to mujMail server
    static final byte GET_URL         = 5;     //re-retrieving mail bodies - redownload uncompleted mails

    /**
     * Resolves myException while running some task.
     * @param ex
     */
    private void resolveMyExceptionRun(MyException ex) {
        ex.printStackTrace();
        if (ex.getErrorCode() == MyException.COM_HALTED) {
            connection.unQuit();
        }
        resolveExceptions(ex.getDetails() + "/ " + account.getEmail(), SOURCE_FILE);
    }
    private class Keeper extends TimerTask {

        public void run() { //sends a command to check and keeps the connection alive to avoid long reconnecting
              if (DEBUG) System.out.println(account.getEmail() + ": keeping connection alive...");
            if (isBusy()) {
                return;
            }
            lock();
            if (!isConnected()) {
                _close(null);
            }
            unlock();
        }
    }

    public IMAP4(MailAccount account) {
        super(account);
        END_OF_MAIL = ")\r\n";
        deleted = new Vector();
    }

    public void addDeleted(MessageHeader header) {
        deleted.addElement(header);
    }

    /**
     * Sends a command to the server.
     * If resultOnly is <code>true</code> it flushes all response lines
     * to the last response line, that begins with the tag and returns the rest
     * of the line (without the tag).
     * 
     * @return tag, when resultOnly is <code>true</code>, rest of the last
     *         response line otherwise
     */ 
    protected String execute(String command, boolean resultOnly) throws MyException {
          if (DEBUG) System.out.println("DEBUG IMAP4.execute(command='" + command + "', resultOnly=" + resultOnly + ")");
        String tag = "A" + commandCounter++ + " ";
          if (DEBUG) System.out.println("DEBUG IMAP4.execute() tag: " + tag);
          if (DEBUG) System.out.println("DEBUG IMAP4.execute() sending CRLF");
        connection.sendCRLF(tag + command);
          if (DEBUG) System.out.println("DEBUG IMAP4.execute() CRLF sent");
        if (resultOnly) {
              if (DEBUG) System.out.println("DEBUG IMAP4.execute() result only, getting line");
            String reply = connection.getLine();
              if (DEBUG) System.out.println("DEBUG IMAP4.execute() line getted");
            while ( !reply.startsWith(tag) ) {
                  if (DEBUG) System.out.println("DEBUG IMAP4.execute() reply: " + reply);
                reply = connection.getLine();
            }

            if (DEBUG) System.out.println("DEBUG IMAP4.execute() reply: " + reply.substring(tag.length()));

            return reply.substring(tag.length());
        }
        return tag;
    }

    /**
     * Sends the command to server and returns the result that is 
     * mostly closed in brackets.
     */ 	
    protected String execute(String command, String arguments) throws MyException {
        String tag = "A" + commandCounter++ + " ";
        connection.sendCRLF(tag + command + (arguments == null ? "" : " " + arguments));       
        String result = "";

        String temp = connection.getLine();
        while (!temp.startsWith(tag)) { //multiline response
            if (temp.indexOf(" " + command + " ") != -1) { //is it the response for the executed command?
                int p = temp.indexOf('(');
                int q = temp.indexOf(')', p + 1);

                if (p != -1 && q > p) {	//a response in the form of "* command (result)"			
                    result = temp.substring(p + 1, q);
                } else //a response in the form of "* command result"
                {
                    result += temp.substring(2 + command.length() + 1);
                } //2 - "* ", 1 - the space after the command
            }
            temp = connection.getLine();
        }

        temp = temp.substring(tag.length());
        if (temp.startsWith("BAD ") || temp.startsWith("NO ")) {
            throw new MyException(MyException.PROTOCOL_COMMAND_NOT_EXECUTED);
        }

        return result;
    }

    public boolean isConnected() {
        try {
              if (DEBUG) System.out.println("DEBUG IMAP4.isConnected() calling connection.isConnected");
            if (connection.isConnected()) {
                  if (DEBUG) System.out.println("DEBUG IMAP4.isConnected() is connected, clearing input");
                connection.clearInput();
                  if (DEBUG) System.out.println("DEBUG IMAP4.isConnected() executing noop");
                if (execute("NOOP", true).startsWith("OK")) {
                      if (DEBUG) System.out.println("DEBUG IMAP4.isConnected() OK");
                    return true;
                }
            }
        } catch (Exception ex) {
            connection.close();
            ex.printStackTrace();
        }
          if (DEBUG) System.out.println("DEBUG IMAP4.isConnected() is not");
        return false;
    }

    public int countNew() throws MyException {
        return searchMailsMatching("UNSEEN").size();
    }

    /**
     * Returns Vector having UID of mails matching the concrete criteria.
     */
    private Vector searchMailsMatching(String criteria) throws MyException {
        Vector mails = new Vector();
        String tag;
        execute("UID SEARCH " + criteria, false);
        String line = connection.getLine();
        line = line.substring(line.indexOf("SEARCH") + 7).trim() + " ";
        int i = 0, j = 0, mailsCount = 0;
        try {
            while (line.length() - 1 > i) {
            	// Break only in case Synchronization
            	// is not running
            	if (!targetBox.isSyncRunning() &&
            		Settings.maxMailsRetrieve > 0 && mailsCount >= Settings.maxMailsRetrieve) //limit exceeded
                {
                    break;
                }
                j = line.indexOf(" ", i);
                tag = line.substring(i, j);

                // In case of synchronization we 
                // want all mails 
                if (targetBox.isSyncRunning()) {
                	mails.addElement(tag);
            	}
                else if (!targetBox.wasOnceDownloaded(account.getEmail(), sld_mailBox_uidval + tag)) {
                    // If the mail with given message ID was 
                    // already downloaded, remove it from 
                    // the list of new mails
                	mails.addElement(tag);
                	mailsCount++;
                }
                i = j + 1;
            }
        } catch (Exception ex) {
              // process chybicky ... proc tu odchytavat
        	  // protoze line.substring() vyhodi Exception
        	  // na konci: tohle by asi melo byt silent
        	  // exception
        	  // TODO (Betlista): tohle je hodně debilní :-/
            ex.printStackTrace();
        }
        return mails;
    }

    private String parseUID(String s) {
        return s.substring(s.lastIndexOf(MessageHeader.MSG_ID_SEPARATOR) + 1);
    }

    // Note task can be null (polling)
    protected boolean open(BackgroundTask task) throws MyException {
        if (DEBUG) System.out.println("DEBUG IMAP4.open() calling isConnected");
        if (isConnected()) {
            if (DEBUG) System.out.println("DEBUG IMAP4.open() is connected");
            return true;
        }
        if (DEBUG) System.out.println("DEBUG IMAP4.open() is not connected");

        _close(task); //we'd better close inactive connections
        try {
            commandCounter = 0;//reset the counter for each connection			
            String reply;

            if (task != null) { task.setTitle(Lang.get(Lang.ALRT_PL_CONNECTING) + " " + account.getEmail()); }
            connection.open(account.getServer() + ":" + account.getPort(),account.isSSL(), account.getSSLType());
            reply = connection.getLine(); // Welcome message from server

            isMujMailServer = reply.indexOf("mujMail") > 0;
            if (reply.length() == 0) {
                if (task != null) { task.setTitle(Lang.get(Lang.ALRT_PL_CONNECTING) + account.getEmail() + Lang.get(Lang.FAILED)); }
                return false;
            }
            reply = execute("LOGIN " + '"' + account.getUserName() + '"' + " " + '"' + account.getPassword() + '"', true);
            if (!reply.startsWith("OK")) {
                getReportBox().report("100: " + Lang.get(Lang.PL_NOTAUTHORIZED), SOURCE_FILE);
                return false;
            }

            if (task != null) { task.setTitle(Lang.get(Lang.ALRT_PL_CONNECTING) + account.getEmail() + Lang.get(Lang.SUCCESS)); }
            if (getTargetBox() != null && !getTargetBox().isPushActive()){
                connectionKeeper = new Timer();
                connectionKeeper.scheduleAtFixedRate(new Keeper(), Settings.noopIMAPPeriod, Settings.noopIMAPPeriod);
            }

            //#ifdef MUJMAIL_COMPRESSED_CONNECTION
            //if (isMujMailServer)
                //((ConnectionCompressed)connection).changeCompression( ConnectionCompressed.COMPRESSION_TYPE_GZIP);
            //#endif
            return true;

        } catch (MyException e) {
            e.printStackTrace();
            if (task != null) { task.setTitle(Lang.get(Lang.ALRT_PL_CONNECTING) + account.getEmail() + Lang.get(Lang.FAILED)); }
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            if (task != null) { task.setTitle(Lang.get(Lang.ALRT_PL_CONNECTING) + account.getEmail() + Lang.get(Lang.FAILED)); }
            e.printStackTrace();
            throw new MyException(MyException.COM_UNKNOWN, "100: " + e);
        }
    }

    private boolean selectMailBox(String box) throws MyException {
        String tag = execute("SELECT \"" + box + "\"", false);
        sld_mailBox_uidval = null; //SELECT automatically deselects currently selected mailbox
        String reply, uidvalidity = "";
        while (!(reply = connection.getLine()).startsWith(tag)) {
            if (reply.indexOf("[UIDVALIDITY") != -1) {
                uidvalidity = reply.substring(reply.indexOf("[UIDVALIDITY") + 13).trim();
                uidvalidity = uidvalidity.substring(0, uidvalidity.indexOf(']')).trim();
            }
        }
        if (reply.startsWith(tag + "OK")) {
            sld_mailBox_uidval = box + MessageHeader.MSG_ID_SEPARATOR_STR + uidvalidity + MessageHeader.MSG_ID_SEPARATOR_STR;
            return true;
        }
        return false;
    }

        // Note task can be null (background Keeper task)
    protected synchronized void _close(BackgroundTask task, boolean waitForReply) {
        if (!connection.isConnected()) {
            return;
        }
        if (task != null) { task.setTitle(Lang.get(Lang.ALRT_PL_CLOSING) + account.getEmail()); }
        if (connectionKeeper != null) {
            connectionKeeper.cancel();
        }
        connectionKeeper = null;
        forcedDisc = false;
        sld_mailBox_uidval = null;
        try {
            execute("CLOSE", waitForReply);
            execute("LOGOUT", waitForReply);
        } catch (MyException e) {
            if (task != null) { task.setTitle(Lang.get(Lang.ALRT_PL_CLOSING) + account.getEmail() + ": " + e.getDetails()); }
        }
        try {
            connection.close();
        } catch (Exception e) {
            if (task != null) { task.setTitle(Lang.get(Lang.ALRT_PL_CLOSING) + account.getEmail() + ": " + e); }
        }
        isMujMailServer = false;
        if (task != null) { task.setTitle(Lang.get(Lang.ALRT_PL_CLOSING) + account.getEmail() + Lang.get(Lang.SUCCESS)); }
    }

    protected void findFirstNewMailWhilePolling() throws MyException {
        Vector newMails = searchMailsMatching("UNSEEN");
        for (int i = newMails.size(); i > 0; --i) {
            String ID = (String) newMails.elementAt(i - 1);
            if (handleMailDiscoveredByPolling(ID)) break;
        }
    }

    protected void getNewMails() {
        // TODO: data overheads when we want to receive only X new mails?
        // check out countNew(), while (actual < max) {...} and
        //  while (actual > 0) {..}
        // the same is in POP3

        try {
            long startTime = System.currentTimeMillis();
              if (DEBUG) { System.out.println("STARTING GET_NEW_MAILS"); }
            if (!open(inProtocolTask)) { //in case of server->inbox sync we need to notify about this error
                //otherwise the synchronization will think that no mails are on the server
                //throw new MyException(MyException.PROTOCOL_CANNOT_CONNECT);
            	return;
            }
              if (DEBUG) { System.out.println("OPEN FINISHED"); }
            if (getTargetBox().isSyncRunning()) {
                mailsOnServer.clear();
            }//we need to recreate a new mailsOnServer lists
            //if its server->inbox sync is called then we want all mails
            //otherwise we just want to check new mails
            String criterium = getTargetBox().isSyncRunning() ? "ALL" : "UNSEEN";
            inProtocolTask.setTitle(Lang.get(Lang.ALRT_INPL_CHECK_MAILS) + account.getEmail());
            MessageHeader header;
            String mailBoxes = account.getIMAPPprimaryBox();
            if (!mailBoxes.endsWith(",")) {
                mailBoxes += ",";
            }

            Vector tempStorage = new Vector();
            while (mailBoxes.length() != 0) {
                //choose next mailbox
                String sld_box = mailBoxes.substring(0, mailBoxes.indexOf(','));
                //define what's the next mailbox
                mailBoxes = mailBoxes.substring(sld_box.length() + 1);
                //if selecting a mailbox is unsuccessful skip to the next one
                if (!selectMailBox(sld_box)) {
                	inProtocolTask.setTitle(Lang.get(Lang.ALRT_PL_CONNECTING) + account.getEmail() + ": " + sld_box + Lang.get(Lang.FAILED));
                    continue;
                }
                Vector newMails = new Vector();
                Vector fetchTags = new Vector();
                String fetchTag;
                //let's find UIDs of mails
                Vector tmp = searchMailsMatching(criterium);
                for (int i = 0; i < tmp.size(); ++i) {

                	if (!targetBox.isSyncRunning() &&
                		Settings.maxMailsRetrieve > 0 && i >= Settings.maxMailsRetrieve) //limit exceeded
                    {
                        break;
                    }

                    header = new MessageHeader(getTargetBox());
                    header.setMessageID(sld_mailBox_uidval + tmp.elementAt(i));
                    header.setIMAPFolder(sld_box);
                    newMails.addElement(header); //mark it to potentially new mails

                    if (inProtocolTask.stopped()) {
                    	_close(inProtocolTask, false);
                        return;
                    }
                }
                /* Memory management */
                tmp = null;
                System.gc();
                
                int n = 0;
                inProtocolTask.updateProgress(newMails.size(), 0);
                //for all potentially new mails, the newest first
                for (int actual = newMails.size() - 1; actual >= 0; --actual) {
                    header = (MessageHeader) newMails.elementAt(actual);
                    //let's remember that this mail is stored on the server
                    mailsOnServer.put(header.getMessageID(), String.valueOf(actual));
                    getTargetBox().newMailOnServer(); //increase synchronization counter

                    if (!targetBox.wasOnceDownloaded(account.getEmail(), header.getMessageID())) {
                        fetchTag = execute("UID FETCH " + parseUID(header.getMessageID()) + " (RFC822.SIZE FLAGS)", false);
                        if (!Settings.downWholeMail || Settings.safeMode) {
                            execute("UID FETCH " + parseUID(header.getMessageID()) + " (RFC822.HEADER)", false);
                        } else {
                            execute("UID FETCH " + parseUID(header.getMessageID()) + " (RFC822)", false);
                        }
                        fetchTags.addElement(fetchTag);
                        ++n;
                        if (Settings.maxMailsRetrieve > 0 && n >= Settings.maxMailsRetrieve) //limit exceeded
                        {
                            break;
                        }
                    }
                    else {
                    	newMails.removeElementAt(actual);
                        inProtocolTask.incActual(1);
                    }

                    if (inProtocolTask.stopped()) {
                        break;
                    }

                }
                int j = fetchTags.size() - 1;
                n = newMails.size() - n; //we will parse only mails whose size and header were required
                for (int actual = newMails.size() - 1; actual >= n; --actual) { //now lets parse new mails' headers
                    header = (MessageHeader) newMails.elementAt(actual);
                    fetchTag = (String) fetchTags.elementAt(j--);
                    String line = "";
                    do {
                        //skip useless lines of previous iteration or of fetch response
                        //until we get tagged BAD or NO response or good UID response
                        line = connection.getLine();
                          if (DEBUG) System.out.println( "DEBUG IMAP4.getNewMails() - line: " + line ); 
                    } while (!(line.startsWith(fetchTag) || line.indexOf("UID " + parseUID(header.getMessageID())) != -1));
                    if (line.startsWith(fetchTag)) { //bad response
                    	inProtocolTask.setTitle(account.getEmail() + ": " + line);
                        continue;
                    }

                    //Check for '\Seen', '\Answered', '\Flagged' and '\Deleted' flags
                    flags = line.substring(line.indexOf("FLAGS (") + 7);
                    flags = flags.substring(0, flags.indexOf(")"));
                    handleFlags(header, flags);

                    int i = line.indexOf("RFC822.SIZE") + 12;
                    line = line.substring(i);
                    for (i = 0; i < line.length(); ++i) {
                        if ( !('0' <= line.charAt(i) && line.charAt(i) <= '9')) {
                            break;
                        }
                    }
                    final String headerSize = line.substring(0, i);
                    if (DEBUG) System.out.println( "DEBUG IMAP4.getNewMails() - header size: " + headerSize);
                    header.setSize( Integer.parseInt( headerSize ) );
                    parseHeaders(header);

                    if (Settings.downWholeMail && !Settings.safeMode) {
                    	inProtocolTask.setTitle(Lang.get(Lang.ALRT_INPL_DOWN_MAIL) + header.getSubject());
                        try {
                            parseBody(header, inProtocolTask);
                            inProtocolTask.setTitle(Lang.get(Lang.ALRT_INPL_DOWN_MAIL) + header.getSubject() + Lang.get(Lang.SUCCESS));
                        } catch (MyException me) {
                        	inProtocolTask.setTitle(Lang.get(Lang.ALRT_INPL_DOWN_MAIL) + header.getSubject() + " " + Lang.get(Lang.FAILED) + " " + me.getDetails());
                        }
                    }
                    try {
                        header.saveHeader();
                        //getBox().getMailDB().saveHeader(header);
                        //cache the mail so next time we can quickly recognize it as already downloaded
                        getTargetBox().addToOnceDownloaded(header);
                        //also mark this mail as checked
                        getTargetBox().addToMsgIDs(header);
                        tempStorage.addElement( header ); // store the mail to temporary storage
                        if (!header.wasRead()) {
                            getTargetBox().changeUnreadMails(1);
                        }
                    } catch (MyException exp) {
                        clear(header); //markAsDeleted partially downloaded bodies
                        inProtocolTask.setTitle(Lang.get(Lang.ALRT_SAVING) + header.getSubject() + " " + Lang.get(Lang.FAILED) + " " + exp.getDetails());
                        if (getTargetBox().isSyncRunning()) //something's gone wrong, now we have to stop sync
                        {
                            throw exp;
                        }
                    }

                    inProtocolTask.incActual(1);
                    if (inProtocolTask.stopped()) {
                        break;
                    }
                }

                if (inProtocolTask.stopped()) {
                	break;
                }
            }
            
            if (inProtocolTask.stopped()) {
            	_close(inProtocolTask, false);
            }

              if (DEBUG) System.out.println( "DEBUG GetMailsTask.doRunWork() - storing mails to the box: " + getTargetBox().getName() );

            // In case this method is called from serversSync() method
            // addMailsInStorageToVector() and setStorage() methods
            // have to be atomic, otherwise TheBox.storage can be
            // overwritten by the last thread running getNewMails() method
            synchronized (getTargetBox().getStorage()) {
            	Functions.addMailsInStorageToVector( getTargetBox().getStorage(), tempStorage );
            	getTargetBox().setStorage( Algorithm.getAlgorithm().invoke( tempStorage ) );
            	System.gc();
            }
              if (DEBUG) System.out.println("DEBUG GetMailsTask.doRunWork() - Mails stored to the box.");
            long wholeTime = System.currentTimeMillis() - startTime;
            String waitTime = wholeTime > 1000 ? wholeTime / 1000 + "sec" : wholeTime + "msec";
            inProtocolTask.setTitle(Lang.get(Lang.ALRT_INPL_CHECK_MAILS) + account.getEmail() + "" + Lang.get(Lang.IN) + waitTime);
        } catch (MyException myException) {
            myException.printStackTrace();
            resolveMyExceptionRun(myException);
        } catch (Exception ex) {
            ex.printStackTrace();
            resolveExceptions("100: " + ex + "/ " + account.getEmail(), SOURCE_FILE);
        }

        if (DEBUG) System.out.println("DEBUG GetMailsTask.doRunWork() - Mails retrieved");
    }

    protected void downloadBody() {
        if (DEBUG) System.out.println("DEBUG IMAP4.downloadBody starting downloading body.");
        try {
            if (DEBUG) System.out.println("DEBUG IMAP4.DownloadBodyTask.doRunWork starting");
            long startTime = System.currentTimeMillis();
            if (!open(inProtocolTask)) {
                if (DEBUG) System.out.println("DEBUG IMAP4.DownloadBodyTask.doRunWork cannot open connection");
                synchronized (actHeader) {
                    actHeader.notify();
                    actHeader = null;
                }
                if (DEBUG) System.out.println("DEBUG IMAP4.DownloadBodyTask.doRunWork returning");
                return;
            }

            if (DEBUG) System.out.println("DEBUG IMAP4.DownloadBodyTask.doRunWork connection opened");

            inProtocolTask.setTitle(Lang.get(runMode == RETRIEVE_BODY ? Lang.ALRT_INPL_DOWN_MAIL : Lang.ALRT_INPL_REDOWN_MAIL) + actHeader.getSubject());
            inProtocolTask.updateProgress(actHeader.getSize(), 0);
            String sld_box = actHeader.getMessageID().substring(0, actHeader.getMessageID().lastIndexOf(MessageHeader.MSG_ID_SEPARATOR) + 1);
             //if the actually selected mailbox differs from header's mailBox
            if (sld_mailBox_uidval == null || !sld_mailBox_uidval.equals(sld_box)) {
                //select header's mailBox
                if (!selectMailBox(actHeader.getIMAPFolder())) {
                    throw new MyException(MyException.PROTOCOL_CANNOT_RETRIEVE_BODY, Lang.get(Lang.ALRT_INPL_IMAP_CANNOT_SELECT_MAILBOX) + sld_box.substring(sld_box.indexOf(0, MessageHeader.MSG_ID_SEPARATOR)));
                }
                if (!sld_mailBox_uidval.equals(sld_box)) {
                    throw new MyException(MyException.PROTOCOL_CANNOT_RETRIEVE_BODY, Lang.get(Lang.ALRT_INPL_IMAP_UIDVALIDITY_DIFFERS));
                }
            }
            String fetchTag;
            if (runMode == RETRIEVE_BODY || (runMode == REDOWNLOAD_BODY && reDownloadMode == -1)) {
                fetchTag = execute("UID FETCH " + parseUID(actHeader.getMessageID()) + " (FLAGS RFC822)", false);
            } else if (runMode == CONVERT_BODY) {
            	String extension = actHeader.getBodyPart(reDownloadMode).getHeader().getExtension();
            	if ("pdf".equalsIgnoreCase(extension)) {
            	    fetchTag = execute("xmujmail-convert pdf " + parseUID(actHeader.getMessageID()) + " (FLAGS BINARY" + "[" + (reDownloadMode + 1) + "])", false);
            	}
            	else { //JPEG conversion
            		int size = MujMail.mujmail.getMenu().getHeight();
            		fetchTag = execute("xmujmail-convert jpg " + Integer.toString(size) + " " + parseUID(actHeader.getMessageID()) + " (FLAGS BINARY" + "[" + (reDownloadMode + 1) + "])", false);            		
            	}
            } else {
                fetchTag = execute("UID FETCH " + parseUID(actHeader.getMessageID()) + " (FLAGS BODY" + "[" + (reDownloadMode + 1) + "])", false);
            }

            String line = "";
            do { //skip useless lines fetch response
                line = connection.getLine();
            } while (!(line.startsWith(fetchTag) || line.indexOf("UID " + parseUID(actHeader.getMessageID())) != -1));
            if (line.startsWith(fetchTag)) {
                throw new MyException(MyException.PROTOCOL_CANNOT_RETRIEVE_BODY, "200: " + Lang.get(Lang.ALRT_INPL_NO_LONGER_ON_SERVER));
            }

            //Check for '\Seen', '\Answered', '\Flagged' and '\Deleted' flags
            indexOfFlags = line.indexOf("FLAGS (");
            if (indexOfFlags != -1) //FLAGS parameter is given first
            {
                flags = line.substring(indexOfFlags + 7);
                flags = flags.substring(0, flags.indexOf(")"));
                handleFlags(actHeader, flags);
            } else //FLAGS parameter is given last
            {
                END_OF_MAIL = " FLAGS (";
            }

            if (DEBUG) System.out.println("DEBUG - IMAP4.downloadBody - before parsing body");
            parseBody(actHeader, inProtocolTask);
            if (DEBUG) System.out.println("DEBUG - IMAP4.downloadBody - after parsing body");

            actHeader.saveHeader();

            if (Settings.safeMode) {
                getTargetBox().setLastSafeMail(actHeader);
            }

            long wholeTime = System.currentTimeMillis() - startTime;
            String waitTime = wholeTime > 1000 ? wholeTime / 1000 + "sec" : wholeTime + "msec";
            inProtocolTask.setTitle("*" + Lang.get(runMode == RETRIEVE_BODY ? Lang.ALRT_INPL_DOWN_MAIL : Lang.ALRT_INPL_REDOWN_MAIL) + actHeader.getSubject() + " " + Lang.get(Lang.IN) + waitTime);
            inProtocolTask.updateProgress(actHeader.getSize(), actHeader.getSize());

            synchronized (actHeader) { //everything is ok now
                actHeader.notify();
                actHeader = null;
            }
        } catch (MyException myException) {
            resolveMyExceptionRun(myException);
        }
    }

    protected void removeMails() {
        try {
            long startTime = System.currentTimeMillis();
            if (deleted.isEmpty() || !open(inProtocolTask)) {
                return;
            }
            inProtocolTask.setTitle(Lang.get(Lang.ALRT_INPL_DEL_MAILS) + account.getEmail());

            inProtocolTask.updateProgress(deleted.size(), 0);
            //let's sort the marked mails by their mailboxes
            //so we don't have to reselect mailboxes for every mail having the same mailbox
            Functions.sort(deleted, Functions.SRT_ORDER_INC, Functions.SRT_HDR_MSGID);
            String msgID;
            int actual = 0;
            int i = 0,
                    j = 0;

            for (actual = deleted.size() -1 ; actual >= 0; --actual) {
                msgID = ((MessageHeader) deleted.elementAt(actual)).getMessageID();
                String sld_box = msgID.substring(0, msgID.lastIndexOf(MessageHeader.MSG_ID_SEPARATOR) + 1); //this mail's mail box
                j = sld_box.indexOf(MessageHeader.MSG_ID_SEPARATOR);
                //if we're gonna select another mailbox, expunge the currently selected mailbox
                if (i != 0 && (i != j || !sld_mailBox_uidval.regionMatches(false, 0, sld_box, 0, j))) {
                    execute("EXPUNGE", true);
                }

                //if no mailbox was set or the actually selected mailbox differs from header's mailBox
                if (i != j || !sld_mailBox_uidval.regionMatches(false, 0, sld_box, 0, j)) {
                    i = 0;
                    //select header's mailBox
                    if (!selectMailBox(sld_box.substring(0, j))) {
                    	inProtocolTask.setTitle(Lang.get(Lang.ALRT_INPL_IMAP_CANNOT_SELECT_MAILBOX) + sld_box.substring(sld_box.indexOf(0, MessageHeader.MSG_ID_SEPARATOR)));
                        //don't consider this as deleted from the server
                        //and don't remove it from the onceDownloaded cache to prevent redownloading it again
                        deleted.removeElementAt(actual);
                        continue;
                    }
                    i = sld_mailBox_uidval.indexOf(MessageHeader.MSG_ID_SEPARATOR);
                }

                //if the mailbox has changed its UIDVALIDITY from the previous session
                if (!sld_mailBox_uidval.equals(sld_box)) {
                	inProtocolTask.setTitle(Lang.get(Lang.ALRT_INPL_IMAP_UIDVALIDITY_DIFFERS));
                    deleted.removeElementAt(actual);
                } else {
                    execute("UID STORE " + parseUID(msgID) + " +FLAGS (\\Deleted)", false);
                }

                removeMessageFromBox(actual);

                if (inProtocolTask.stopped()) {
                    break;
                }

            }
            if (actual != 0) {
                execute("EXPUNGE", true);
            }

            deleted.removeAllElements();

            long wholeTime = System.currentTimeMillis() - startTime;
            String waitTime = wholeTime > 1000 ? wholeTime / 1000 + "sec" : wholeTime + "msec";
            inProtocolTask.setTitle(Lang.get(Lang.ALRT_INPL_DEL_MAILS) + account.getEmail() + Lang.get(Lang.IN) + waitTime);
        } catch (MyException myException) {
            myException.printStackTrace();
            resolveMyExceptionRun(myException);
        }
    }

    protected void setFlags() {
        try {
            //try to open connection
            if (!open(inProtocolTask)) {
                throw new MyException(MyException.PROTOCOL_CANNOT_CONNECT);

            }
            String messageID = actHeader.getMessageID();

            String sld_box = messageID.substring(0, messageID.lastIndexOf(MessageHeader.MSG_ID_SEPARATOR) + 1); //this mail's mail box

            selectMailBox(actHeader.getIMAPFolder());

            //if the mailbox has changed its UIDVALIDITY from the previous session
            if (!sld_mailBox_uidval.equals(sld_box)) {
            	inProtocolTask.setTitle(Lang.get(Lang.ALRT_INPL_IMAP_UIDVALIDITY_DIFFERS));
            } else {
                execute("UID STORE " + parseUID(messageID) + " +FLAGS " + flagsToSet, false);
            }

            synchronized (actHeader) { //everything is ok now
                actHeader.notify();
                actHeader = null;
            }
        } catch (MyException myException) {
            resolveMyExceptionRun(myException);
        }
    }

    protected void removeFlags() {
        try {
            //try to open connection
            if (!open(inProtocolTask)) {
                throw new MyException(MyException.PROTOCOL_CANNOT_CONNECT);

            }
            String mesgID = actHeader.getMessageID();

            String sld_box = mesgID.substring(0, mesgID.lastIndexOf(MessageHeader.MSG_ID_SEPARATOR) + 1); //this mail's mail box

            selectMailBox(actHeader.getIMAPFolder());

            //if the mailbox has changed its UIDVALIDITY from the previous session
            if (!sld_mailBox_uidval.equals(sld_box)) {
            	inProtocolTask.setTitle(Lang.get(Lang.ALRT_INPL_IMAP_UIDVALIDITY_DIFFERS));
            } else {
                execute("UID STORE " + parseUID(mesgID) + " -FLAGS " + flagsToSet, false);
            }
            
            synchronized (actHeader) { //everything is ok now
                actHeader.notify();
                actHeader = null;
            }
        } catch (MyException myException) {
            resolveMyExceptionRun(myException);
        }
    }
    
    /* MUJMAIL IMAP4 PROTOCOL EXTENSIONS */
    
    String URL = null;
    public synchronized String getURL( String url) {
        runMode = IMAP4.GET_URL;
        URL = url;
        inProtocolTask = new InProtocolTask(this, "Retrieving URL page");
        inProtocolTask.start();
        return URL;
    }
    protected void getURL() throws MyException {
        if ( URL == null ) return;
        if ( isConnected() == false ) {
            open(null);
        }
        String tag = execute( "XmujMail-url processed noncompacted \"" + URL + "\"", false);
        
        String reply = connection.getLine();
        // parse reply
        if ( reply.startsWith(tag) ) {
            URL = null;

            String errorTag = tag + " BAD ";
            if ( reply.startsWith(errorTag) )                 
                throw new MyException( MyException.PROTOCOL_BASE, "200: " + "Internal error - bad syntax" + reply.substring( errorTag.length()));
            
            String problemTag = tag + " NO ";
            if ( reply.startsWith( problemTag) ) {
                System.out.println("Error report IMAP4.getURLCommand:" + reply);
                throw new MyException(MyException.PROTOCOL_CANNOT_GET_URL, "200: " + Lang.get(Lang.ALRT_INPL_IMAP_GETURL_NO_PAGE) + "Debug server reply:" + reply);
            }
            return;
        }
        if ( reply.startsWith("* xMujmail-url") == false ) 
                throw new MyException( MyException.PROTOCOL_BASE, "200: " + "Internal error - unknown server reply 1- " + reply);
        // Parse length and read HTML
        int len = 0; // HTML length
        String num = reply.substring(reply.indexOf("{")+1, reply.indexOf("}"));
        len = Integer.parseInt(num);
        System.out.print("getURLCommand - len"); System.out.println(len);
        URL = "";
        while ( URL.length() <= len ) URL = URL + connection.getLine();
        
        // Parse final tag
        reply = connection.getLine();
        if ( reply.startsWith(tag + "OK") == false )
                throw new MyException( MyException.PROTOCOL_BASE, "200: " + "Internal error - unknown server reply 2 - " + reply);
       
    }
    //For push mail testing
    public boolean isImap(){
        return true;
    }
    
    // IMAP IDLE
    /* TODO: Prevent connection time out by sending NOOP every 15 min?
     * 
    */
    public synchronized void push(){
        PushTask pushTask = new PushTask();
        pushTask.disableDisplayingProgress();
        pushTask.start();
        
    }

    private void removeMessageFromBox(int actual) {
        String msgID = ((MessageHeader) deleted.elementAt(actual)).getMessageID();
        mailsOnServer.remove(msgID);
        // Remove from global seen mail database
        MujMail.mujmail.getMailDBSeen().deleteSeen(account.getEmail(), msgID);
        inProtocolTask.incActual(1);
    }

    private class PushTask extends StoppableBackgroundTask {

        public PushTask() {
            super(Lang.get(Lang.AC_PUSH) + " " + account.getEmail() + " PushTask");
        }

        public void doWork() {
            Vector newMails = null;
	        String ID = null;
	        //lock();
	        System.out.println("In IMAP IDLE run 1");

            try {
                connection.unQuit();
                connection.clearInput();
                    if (open(this)) {
                        Vector tempVector = new Vector();
                        while (getTargetBox().isPushActive()) {
                            selectMailBox("INBOX");
                            execute("IDLE", false);
                            String reply = connection.getLine();
                            System.out.println("Entering IDLE mode");
                            System.out.println("Reply IDLE: " + reply);
                            while(connection.available()==false){
                                if(!getTargetBox().isPushActive()){
                                	System.out.println("PUSH IS NOT ACTIVE");
                                    connection.sendCRLF("DONE");
                                    System.out.println("End IDLE execution");
                                    return;
                                }
                                Thread.sleep(20 * 1000); //Sleep for 20 seconds
                            }
                            System.out.println("Mail(s) arrived");
                            System.out.println("Reply2: " + connection.getLine());
                            connection.sendCRLF("DONE");
                            System.out.println("Reply3: " + connection.getLine());
                            newMails = searchMailsMatching("UNSEEN");
                            for (int i = newMails.size(); i > 0; --i) {
                                ID = (String) newMails.elementAt(i - 1);
                                if (!getTargetBox().wasOnceDownloaded(account.getEmail(), ID)) {
                                    //Actually if activate in settings
                                    new AudioAlert();

                                    //getNewMails(); --> Problem with GUI because of busy thread (TODO)

                                    //Another possible solution, but stupid one:
                                    //runMode = InProtocol.GET_NEW_MAILS;
                                    //run();


                                    // Really not beautiful solution, but does work very well --> (redundant)
                                    // Just copy and paste the GET_NEW_MAILS part from run()
                                    long startTime, wholeTime;
                                    String waitTime;
                                    int actual = 0;
                                    String line, sld_box;
                                    startTime = System.currentTimeMillis();
                                    if (getTargetBox().isSyncRunning()) //if sync is running
                                    {
                                        mailsOnServer.clear();
                                    }//we need to recreate a new mailsOnServer lists
                                    //if its server->inbox sync is called then we want all mails
                                    //otherwise we just want to check new mails
                                    String criterium = getTargetBox().isSyncRunning() ? "UNSEEN" : "ALL";
                                    setTitle(Lang.get(Lang.ALRT_INPL_CHECK_MAILS) + account.getEmail());
                                    MessageHeader header;
                                    String mailBoxes = account.getIMAPPprimaryBox();
                                    if (!mailBoxes.endsWith(",")) {
                                        mailBoxes += ",";
                                    }

                                    while (mailBoxes.length() != 0) {
                                        //choose next mailbox
                                        sld_box = mailBoxes.substring(0, mailBoxes.indexOf(','));
                                        //define what's the next mailbox
                                        mailBoxes = mailBoxes.substring(sld_box.length() + 1);
                                        //if selecting a mailbox is unsuccessful skip to the next one
                                        if (!selectMailBox(sld_box)) {
                                            setTitle(Lang.get(Lang.ALRT_PL_CONNECTING) + account.getEmail() + ": " + sld_box + Lang.get(Lang.FAILED));
                                            continue;
                                        }
                                        Vector newMails1 = new Vector();
                                        Vector fetchTags = new Vector();
                                        String fetchTag;
                                        //let's find UIDs of mails
                                        Vector tmp = searchMailsMatching(criterium);
                                        for (int x = 0; x < tmp.size(); ++x) {
                                            header = new MessageHeader(getTargetBox());
                                            header.setMessageID(sld_mailBox_uidval + tmp.elementAt(x));
                                            newMails1.addElement(header); //mark it to potentially new mails
                                        }
                                        int n = 0;
                                        updateProgress(newMails.size(), 0);
                                        //for all potentially new mails, the newest first
                                        for (actual = newMails1.size() - 1; actual >= 0; --actual) {
                                            header = (MessageHeader) newMails1.elementAt(actual);
                                            //let's remember that this mail is stored on the server
                                            mailsOnServer.put(header.getMessageID(), String.valueOf(actual));
                                            getTargetBox().newMailOnServer(); //increase synchronization counter
                                            if (!getTargetBox().wasOnceDownloaded(account.getEmail(), header.getMessageID())) { //check if that mail wasnt already downloaded
                                                fetchTag = execute("UID FETCH " + parseUID(header.getMessageID()) + " (RFC822.SIZE)", false);
                                                if (!Settings.downWholeMail || Settings.safeMode) {
                                                    execute("UID FETCH " + parseUID(header.getMessageID()) + " (RFC822.HEADER)", false);
                                                } else {
                                                    execute("UID FETCH " + parseUID(header.getMessageID()) + " (RFC822)", false);
                                                }
                                                fetchTags.addElement(fetchTag);
                                                ++n;
                                                if (Settings.maxMailsRetrieve > 0 && n >= Settings.maxMailsRetrieve) //limit excessed
                                                {
                                                    break;
                                                }
                                            } else { //if the mail was already downloaded, remove it from
                                                newMails1.removeElementAt(actual); //remove it from potentially new mails list
                                                incActual(1);
                                            }

                                            if (stopped()) {
                                                break;
                                            }

                                        }
                                        int j = fetchTags.size() - 1;
                                        n = newMails1.size() - n; //we will parse only mails whose size and header were required
                                        for (actual = newMails1.size() - 1; actual >= n; --actual) { //now lets parse new mails' headers
                                            header = (MessageHeader) newMails1.elementAt(actual);
                                            fetchTag = (String) fetchTags.elementAt(j--);
                                            do {
                                                //skip useless lines of previous iteration or of fetch response
                                                //until we get tagged BAD or NO response or good UID response
                                                line = connection.getLine();
                                            } while (!(line.startsWith(fetchTag) || line.indexOf("UID " + parseUID(header.getMessageID())) != -1));
                                            if (line.startsWith(fetchTag)) { //bad response
                                                setTitle(account.getEmail() + ": " + line);
                                                continue;
                                            }

                                            int y = line.indexOf("RFC822.SIZE") + 12;
                                            line = line.substring(y);
                                            for (y = 0; y < line.length(); ++y) {
                                                if (!('0' <= line.charAt(y) && line.charAt(y) <= '9')) {
                                                    break;
                                                }
                                            }
                                            header.setSize(Integer.parseInt(line.substring(0, y)));
                                            parseHeaders(header);

                                            if (Settings.downWholeMail && !Settings.safeMode) {
                                                setTitle(Lang.get(Lang.ALRT_INPL_DOWN_MAIL) + header.getSubject());
                                                try {
                                                    parseBody(header, this);
                                                    setTitle(Lang.get(Lang.ALRT_INPL_DOWN_MAIL) + header.getSubject() + Lang.get(Lang.SUCCESS));
                                                } catch (MyException me) {
                                                    setTitle(Lang.get(Lang.ALRT_INPL_DOWN_MAIL) + header.getSubject() + " " + Lang.get(Lang.FAILED) + " " + me.getDetails());
                                                }
                                            }
                                            try {
                                                header.saveHeader();
                                                //cache the mail so next time we can quickly recognize it as already downloaded
                                                getTargetBox().addToOnceDownloaded(header);
                                            } catch (MyException exp) {
                                                clear(header); //markAsDeleted partially downloaded bodies
                                                setTitle(Lang.get(Lang.ALRT_SAVING) + header.getSubject() + " " + Lang.get(Lang.FAILED) + " " + exp.getDetails());
                                                if (getTargetBox().isSyncRunning()) //something's gone wrong, now we have to stop sync
                                                {
                                                    throw exp;
                                                }
                                            }
                                            //also mark this mail as checked
                                            getTargetBox().addToMsgIDs(header);
                                            tempVector.addElement( header ); //store the mail
                                            if (!header.wasRead()) {
                                                getTargetBox().changeUnreadMails(1);
                                            }

                                            incActual(1);
                                            if (stopped()) {
                                                break;
                                            }
                                        }

                                        if (stopped()) {
                                            break;
                                        }
                                    }



                                    wholeTime = System.currentTimeMillis() - startTime;
                                    waitTime = wholeTime > 1000 ? wholeTime / 1000 + "sec" : wholeTime + "msec";
                                    setTitle(Lang.get(Lang.ALRT_INPL_CHECK_MAILS) + account.getEmail() + "" + Lang.get(Lang.IN) + waitTime);

                                    //Threading algorithm has to be called each time
                                    //new mails have been retrieved.
                                    synchronized (getTargetBox().getStorage()) {
            	                        Functions.addMailsInStorageToVector(getTargetBox().getStorage(), tempVector);
            	                        getTargetBox().setStorage( Algorithm.getAlgorithm().invoke( tempVector ) );
                                    }

                                    MujMail.mujmail.getMenu().refreshAll(true);
                                    break;
                                }
                            }
                            connection.clearInput();
                        } // while loop end
                          if (PushTask.DEBUG) System.out.println( "DEBUG PushTask.doRunWork() - box name: " + getTargetBox().getName() );
                    } // if ( open() ) condition end
                System.out.println("End IDLE execution");
                _close(this);

            } catch (MyException ex) {
                ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return;
        }
        
    }
}
