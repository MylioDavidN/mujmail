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

import java.util.Hashtable;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import mujmail.Lang;
import mujmail.MessageHeader;
import mujmail.MujMail;
import mujmail.MyException;
import mujmail.Settings;
import mujmail.account.MailAccount;
import mujmail.tasks.BackgroundTask;
import mujmail.threading.Algorithm;
import mujmail.util.Functions;

/**
 * Implements InProtocol using POP3.
 * All operations can be invoked at once and we have shared resource 
 * BasicConnection. So we must synchronize the method run. we use lock() instead 
 * of making run() synchronized in order to allow forced disconnecting from 
 * servers even some job are still running.
 */
public class POP3 extends InProtocol {
    /** The name of this source file */
    private static final String SOURCE_FILE = "POP3";
    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false;
    
    Hashtable deleted;
    Timer connectionKeeper;

    private void resolveMyExceptionWhileRunning(MyException ex) {
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

    public POP3(MailAccount account) {
        super(account);
        END_OF_MAIL = ".\r\n";
        deleted = new Hashtable();
    }

    //add a mail to a queue of mails that are going to be deleted from the server
    public void addDeleted(MessageHeader header) {
        deleted.put(header.getMessageID(), header);
    }

    public boolean isConnected() {
        try {
            if (connection.isConnected()) {
                connection.clearInput();
                connection.sendCRLF("NOOP");
                if (connection.getLine().startsWith("+OK")) {
                    return true;
                }
            }
        } catch (Exception ex) {
            connection.close();
            //TODO: Close connection
        }
        return false;
    }

    public int countNew() throws MyException {
        connection.sendCRLF("STAT");
        String reply = connection.getLine();
        if (!reply.startsWith("+OK")) {
            return 0;
        }
        return Integer.parseInt(reply.substring(4, reply.lastIndexOf(' ')).trim());
    }

    private int getSize(int msgNum) throws MyException {
        connection.sendCRLF("LIST " + msgNum);
        String reply = connection.getLine();
        if (!reply.startsWith("+OK")) {
            return 0;
        }
        return Integer.parseInt(reply.substring(reply.lastIndexOf(' ') + 1).trim());
    }

    private String getMsgID(int index) throws MyException {
        connection.sendCRLF("UIDL " + index);
        String reply = connection.getLine();
        if (reply.startsWith("+OK")) {
            return reply.substring(reply.lastIndexOf(' ') + 1).trim();
        } else {
            return "";
        }
    }

    /**
     * Gets the index of given mail on the server.
     * @param header the header of the mail
     * @return the index of the mail on the server
     * @throws MyException
     */
    private String getMsgNum(MessageHeader header) throws MyException {
        String reply, ID, num;
        String index = (String) mailsOnServer.get(header.getMessageID());
        if (index != null) {
            connection.sendCRLF("UIDL " + index); //lets try if the mail is from actual session		
            reply = connection.getLine();
            if (reply.startsWith("+OK")) {
                ID = reply.substring(reply.lastIndexOf(' ') + 1).trim();
                if (ID.equals(header.getMessageID())) {
                    return index;
                }
            }
        }
        //we have to test and update info about all mails :(
        //note: if a message is marked as deleted or as read by pop3 , it will never be listed by UIDL again and we can get it anymore?
        connection.sendCRLF("UIDL");
        connection.getLine();
        boolean found = false;
        while (!(reply = connection.getLine()).startsWith(".")) {
            ID = reply.substring(reply.indexOf(' ') + 1).trim();
            num = reply.substring(0, reply.indexOf(' '));

            if (mailsOnServer.containsKey(ID)) //let's remove old information
            {
                mailsOnServer.remove(ID);
            }
            mailsOnServer.put(ID, num);

            if (ID.equals(header.getMessageID())) {
                found = true;
            }
        }
        return found ? (String) mailsOnServer.get(header.getMessageID()) : "0";
    }

    // Note task can be null for polling task, getUrl
    protected boolean open(BackgroundTask task) throws MyException {
        if (isConnected()) {
            return true;
        }
        _close(task); //we'd better close inactive connections
        try {
            if (task != null) { task.setTitle(Lang.get(Lang.ALRT_PL_CONNECTING) + " " + account.getEmail()); }
            connection.open(account.getServer() + ":" + account.getPort(),account.isSSL(), account.getSSLType());
            if (!connection.getLine().startsWith("+OK")) {
                if (task != null) { task.setTitle(Lang.get(Lang.ALRT_PL_CONNECTING) + account.getEmail() + Lang.get(Lang.FAILED)); }
                return false;
            }
            connection.sendCRLF("USER " + account.getUserName());
            if (!connection.getLine().startsWith("+OK")) {
                connection.unGetLine();
                getReportBox().report("100: " + Lang.get(Lang.PL_NOTAUTHORIZED) + account.getUserName() + "/ " + connection.getLine(), SOURCE_FILE);
                return false;
            }
            connection.sendCRLF("PASS " + account.getPassword());
            if (!connection.getLine().startsWith("+OK")) {
                getReportBox().report("100: " + Lang.get(Lang.PL_NOTAUTHORIZED) + account.getUserName(), SOURCE_FILE);
                return false;
            }
            if (task != null) { task.setTitle(Lang.get(Lang.ALRT_PL_CONNECTING) + account.getEmail() + Lang.get(Lang.SUCCESS)); }
            connectionKeeper = new Timer();
            connectionKeeper.scheduleAtFixedRate(new Keeper(), Settings.noopPeriod, Settings.noopPeriod);
            return true;

        } catch (MyException e) {
            if (task != null) { task.setTitle(Lang.get(Lang.ALRT_PL_CONNECTING) + account.getEmail() + Lang.get(Lang.FAILED)); }
            throw e;
        } catch (Exception e) {
            if (task != null) { task.setTitle(Lang.get(Lang.ALRT_PL_CONNECTING) + account.getEmail() + Lang.get(Lang.FAILED)); }
            throw new MyException(MyException.COM_UNKNOWN, "100: " + e);
        }
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
        try {
            connection.sendCRLF("QUIT");
        } catch (MyException e) {
            if (task != null) { task.setTitle(Lang.get(Lang.ALRT_PL_CLOSING) + account.getEmail() + ": " + e.getDetails()); }
        }
        try {
            connection.close();
        } catch (Exception e) {
            if (task != null) { task.setTitle(Lang.get(Lang.ALRT_PL_CLOSING) + account.getEmail() + ": " + e); }
        }
        if (task != null) { task.setTitle(Lang.get(Lang.ALRT_PL_CLOSING) + account.getEmail() + Lang.get(Lang.SUCCESS)); }
    }

    protected void findFirstNewMailWhilePolling() throws MyException {
        int count = countNew();
        for (; count > 0; --count) {
            String ID = getMsgID(count);
            if (ID.length() > 0) {
                if (handleMailDiscoveredByPolling(ID)) break;
            }
        }
    }

    protected void getNewMails() {
        try {
            // TODO: data overheads when we want to receive only X new mails?
            // check out countNew(), while (actual < max) {...} and
            //  while (actual > 0) {..}
            // the same is in IMAP4

            long startTime = System.currentTimeMillis();
            if (!open(inProtocolTask)) { //in case of server->inbox sync we need to notify about this error
                //otherwise the synchronization will think that no mails are on the server
                //throw new MyException(MyException.PROTOCOL_CANNOT_CONNECT);
            	return;
            }
            if (getTargetBox().isSyncRunning()) {
                mailsOnServer.clear();
            }//we need to recreate a new mailsOnServer lists

            _close(null);
            open(null);

            int newMailsCount = countNew();
            int max = newMailsCount;
            if (!targetBox.isSyncRunning() &&
            	Settings.maxMailsRetrieve > 0 && newMailsCount >= Settings.maxMailsRetrieve) //limit exceeded
            {
            	max = Settings.maxMailsRetrieve;
            }

            inProtocolTask.setTitle(Lang.get(Lang.ALRT_INPL_CHECK_MAILS) + account.getEmail());
            inProtocolTask.updateProgress(max, 0);
            int actual = 0, mailsCount = 0;
            String idLine, sizeLine;
            MessageHeader header;
            Vector newMails = new Vector(actual);
            Stack msgNumbers = new Stack();

            //lets send commands to get msgIDs and size for the 
            //mails and parse MsgIDs and size from input stream
            while (mailsCount < max && actual < newMailsCount) {
            	//sending command
                connection.sendCRLF("UIDL " + (actual + 1) + "\r\n" + "LIST " + (actual + 1));
                //parsing reply
                idLine = connection.getLine();
                sizeLine = connection.getLine();
                //if we could get its MsgID and size, put it to newMails - mark it as a potentially new mail
                if (idLine.startsWith("+OK") && sizeLine.startsWith("+OK")) {
                    header = new MessageHeader(targetBox);
                    header.setMessageID(idLine.substring(idLine.lastIndexOf(' ') + 1).trim());
                    header.setSize(Integer.parseInt(sizeLine.substring(sizeLine.lastIndexOf(' ') + 1).trim()));
                    if (targetBox.isSyncRunning()) {
	                    newMails.addElement(header);
	                    
	                    //save message-numbers to be used for TOP command
	                    msgNumbers.push(new Integer(actual + 1));
                    }
                    //check if that mail wasnt already downloaded
                    else if (!targetBox.wasOnceDownloaded(account.getEmail(), header.getMessageID())) {
	                    newMails.addElement(header);
	                    
	                    //save message-numbers to be used for TOP command
	                    msgNumbers.push(new Integer(actual + 1));
	                    
                    	mailsCount++;
                    }
                }
                ++actual;
                if (inProtocolTask.stopped()) {
                    _close(inProtocolTask, false);
                	return;
                }
            }

            Vector tempStorage = new Vector();
            int n = 0;
            for (actual = newMails.size() - 1; actual >= 0; --actual) { //for all potentially new mails in the list, the newest first
                header = (MessageHeader) newMails.elementAt(actual);
                mailsOnServer.put(header.getMessageID(), String.valueOf(actual + 1));
                getTargetBox().newMailOnServer();//increase synchronization counter

                if (!targetBox.wasOnceDownloaded(account.getEmail(), header.getMessageID())) {
	                if (!Settings.downWholeMail || Settings.safeMode) {
	                    connection.sendCRLF("TOP " + (Integer)msgNumbers.pop() + " 0");
	                } else {
	                    //we send connection.sendCRLF("TOP "+n+" 1000000");
	                    //instead of connection.sendCRLF("RETR "+n); here because RETR returns data in a messed order
	                    //not the same as we wanted!!!
	                    //ei: RETR 1,RETR 2, RETR3 are returned as result_of_RETR1 half_of_RETR2, result_RETR3 and then rest of RETR2
	                    //very annoying thing. I spent 3 days with this (Tung).
	                    connection.sendCRLF("TOP " + (actual + 1) + " 1000000");
	                }
	                ++n;
	                if (Settings.maxMailsRetrieve > 0 && n >= Settings.maxMailsRetrieve) //limit excessed
	                {
	                    break;
	                }
                }
                else { //if the mail was already downloaded, remove it from the potentially new mails list 
					newMails.removeElementAt(actual);
					inProtocolTask.incActual(1);		
                }

                if (inProtocolTask.stopped()) {
                    break;
                }
            }

            n = newMails.size() - n;
            for (actual = newMails.size() - 1; actual >= n; --actual) { //now lets parse new mails' headers
                header = (MessageHeader) newMails.elementAt(actual);
                String line;
                //skip useless lines of previous iteration
                do {
                    line = connection.getLine();
                } while (!line.startsWith("+OK") && !line.startsWith("-ERR"));
                if (line.startsWith("-ERR")) {
                	inProtocolTask.setTitle(account.getEmail() + ": " + line);
                    continue;
                }

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
                    //cache the mail so next time we can quickly recognize it as already downloaded
                    getTargetBox().addToOnceDownloaded(header);
                    getTargetBox().addToMsgIDs(header);
                    tempStorage.addElement( header ); //store the mail to box's storage vector
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
                    if (DEBUG) System.out.println("DEBUG GetMailsTask.doRunWork(): stopped.");
                    break;
                }
            }
            
            if (inProtocolTask.stopped()) {
            	_close(inProtocolTask, false);
            }

            if (DEBUG) {
                System.out.println( "DEBUG GetMailsTask.doRunWork() - box name: " + getTargetBox() );
                System.out.println( "DEBUG GetMailsTask.doRunWork() - tempStorage: " + tempStorage);
            }
            
            // In case this method is called from serversSync() method
            // addMailsInStorageToVector() and setStorage() methods
            // have to be atomic, otherwise TheBox.storage can be
            // overwritten by the last thread running getNewMails() method
            synchronized (getTargetBox().getStorage()) {
            	Functions.addMailsInStorageToVector( getTargetBox().getStorage(), tempStorage );
            	getTargetBox().setStorage( Algorithm.getAlgorithm().invoke( tempStorage ) );
            }
            if (DEBUG) System.out.println("DEBUG GetMailsTask.doRunWork() - saved to storage.");

            long wholeTime = System.currentTimeMillis() - startTime;
            String waitTime = wholeTime > 1000 ? wholeTime / 1000 + "sec" : wholeTime + "msec";
            inProtocolTask.setTitle(Lang.get(Lang.ALRT_INPL_CHECK_MAILS) + account.getEmail() + "" + Lang.get(Lang.IN) + waitTime);
        } catch (MyException myException) {
            if (POP3.DEBUG) { myException.printStackTrace(); }
            resolveMyExceptionWhileRunning(myException);
        } catch (Exception ex) {
            if (POP3.DEBUG) { ex.printStackTrace(); }
            resolveExceptions("100: " + ex + "/ " + account.getEmail(), SOURCE_FILE);
        }
    }

    protected void downloadBody() {
        try {
            long startTime = System.currentTimeMillis();
            if (!open(inProtocolTask)) {
                synchronized (actHeader) {
                    actHeader.notify();
                    actHeader = null;
                }
                return;
            }

            inProtocolTask.setTitle(Lang.get(runMode == RETRIEVE_BODY ? Lang.ALRT_INPL_DOWN_MAIL : Lang.ALRT_INPL_REDOWN_MAIL) + actHeader.getSubject());
            inProtocolTask.updateProgress(actHeader.getSize(), 0);
            connection.sendCRLF("RETR " + getMsgNum(actHeader));
            if (connection.getLine().startsWith("-ERR")) {
                throw new MyException(MyException.PROTOCOL_CANNOT_RETRIEVE_BODY, "200: " + Lang.get(Lang.ALRT_INPL_NO_LONGER_ON_SERVER));
            }

            // parse body
            parseBody(actHeader, inProtocolTask);

            actHeader.saveHeader(); //update new data into DBase
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
            resolveMyExceptionWhileRunning(myException);
        }
    }

    protected void removeMails() {
        try {
            long startTime = System.currentTimeMillis();
            if (deleted.isEmpty() || !open(inProtocolTask)) {
                return;
            }
            int max = deleted.size();
            int actual = 0;
            inProtocolTask.setTitle(Lang.get(Lang.ALRT_INPL_DEL_MAILS) + account.getEmail());
            inProtocolTask.updateProgress(max, actual);
            connection.sendCRLF("UIDL");
            connection.getLine();
            String reply,
                    ID,
                    num;
            //browse the whole UIDL list and try to markAsDeleted all marked mails
            while (!(reply = connection.getLine()).startsWith(".")) {
                ID = reply.substring(reply.indexOf(' ') + 1).trim(); //get its ID on the server
                if (deleted.containsKey(ID)) { //was is marked as deleted?
                    num = reply.substring(0, reply.indexOf(' ')).trim(); //get its number offset on the server
                    connection.sendCRLF("DELE " + num);
                    mailsOnServer.remove(ID);
                    MujMail.mujmail.getMailDBSeen().deleteSeen(account.getEmail(), ID);
                    inProtocolTask.incActual(1);
                    actual++;
                }
                if (inProtocolTask.stopped()) {
                    break;
                }
            }
            while (actual-- > 0) {
                connection.getLine();
            } //get server's response line.
            //We don't check the responses here just because of laziness :)
            _close(inProtocolTask);
            deleted.clear(); //all mails marked as deleted should be deleted by now

            long wholeTime = System.currentTimeMillis() - startTime;
            String waitTime = wholeTime > 1000 ? wholeTime / 1000 + "sec" : wholeTime + "msec";
            inProtocolTask.setTitle(Lang.get(Lang.ALRT_INPL_DEL_MAILS) + account.getEmail() + Lang.get(Lang.IN) + waitTime);
        } catch (MyException myException) {
            resolveMyExceptionWhileRunning(myException);
        }
    }

    protected void setFlags() {
        // not supported by pop3
    }

    protected void removeFlags() {
        // not supported by pop3
    }

    protected void getURL() throws MyException {
        // not supported by pop3
    }
} //end of POP3 class

