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

import java.util.Vector;

import mujmail.AddressBook;
import mujmail.Lang;
import mujmail.MessageHeader;
import mujmail.MujMail;
import mujmail.MyException;
import mujmail.PersistentBox;
import mujmail.Settings;
import mujmail.account.MailAccount;
import mujmail.account.MailAccountPrimary;
import mujmail.connections.ConnectionCompressed;
import mujmail.connections.ConnectionInterface;
import mujmail.tasks.StoppableBackgroundTask;
import mujmail.tasks.StoppableProgress;
import mujmail.util.Decode;
import mujmail.util.Functions;

/**
 * Singleton class. It is only one instance of this class in whole program.
 * TODO: or consider the concept in which each instance of Outbox has own
 * instance of SMTP
 * 
 * Can be used for sending messages from given box using the method 
 * sendMessages() for sending arbitrary message using method 
 * sendMessage(MessageHeader).
 */
public class SMTP extends StoppableBackgroundTask {
    /** The name of this source file */
    private static final String SOURCE_FILE = "SMTP";

    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false;

    public static final byte CLOSE = 1;
    public static final byte SEND = 2;
    byte runMode;
    private final MujMail mujMail;
    /** The box the messages of which are actually sent. Updates with each calling
     of methods sendMail, sendMails etc.*/
    private PersistentBox box;
    //private TheBox reportBox;
    private boolean busy;
    private MessageHeader singleMail; //if we just want to send a single mail
    private ConnectionInterface connection;
    boolean locked; //for thread synchronizing
    boolean forcedDisc; //if it should disconnect from the server unconditionally	
    private MailAccount account;
    /** Used to send mails via smtp */
    private SMTPMailSender mailSender;
    private static SMTP SMTPSingleton = null;

    /**
     * Creates new instance of SMTP_MODE class.
     * 
     * @param box the box which messages will SMTP_MODE send
     * @param mujMail the main class of the program
     */
    private SMTP(MujMail mujMail) {
        super("SMTP singleton");
        //this.box = box;
        this.mujMail = mujMail;
        this.connection = new ConnectionCompressed();
        this.mailSender = new SMTPMailSender(connection);
        //initAccount cannot go here, no hashtable yet; 
    }
    
    /**
     * Gets the singleton instance of SMTP class.
     * @param mujMail the main object of the application
     * @return the SMTP singleton instance
     */
    public static SMTP getSMTPSingleton(MujMail mujMail) {
        if (SMTPSingleton != null) return SMTPSingleton;
        
        SMTPSingleton = new SMTP(mujMail);
        return SMTPSingleton;
    }
    
    /** Search account for sending mails
     *  Currently we're using primary mail account for sending mails
     */
    public void initAccount() {
        String primaryMailAccountName = Settings.primaryEmail;
        account = (MailAccount)mujMail.getMailAccounts().get(primaryMailAccountName);
        if (account == null) {
            // Error primary account not found
            // Safe choice ... using first mail account
            if (mujMail.getMailAccounts().size() > 0) {
                account = (MailAccount)mujMail.getMailAccounts().elements().nextElement();
            } else {
                account = new MailAccountPrimary();
            }
        }
        if (DEBUG) System.out.println("DEBUG SMTP.initAccount():\n" + getAccount());
    }

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

    public void stop() {
        if (isBusy()) {
            connection.quit();
        }
    }

     public boolean isConnected() {
        try {
            if (connection.isConnected()) {
                connection.sendCRLF("NOOP");
                if (connection.getLine().compareTo("") != 0) {
                    return true;
                }
            }
        } catch (Exception ex) {
            return false;
        }
        return false;
    }

    public boolean isBusy() {
        return busy;
    }

    
    /**
     * Sends all mails in the box.
     */
    public void sendMails(PersistentBox box) {
        this.box = box;
        // send mails in the box, not single mail
        singleMail = null;
        sendSMTP();
    }
    
    /**
     * Sends single mail.
     * @param mail the mail to send
     */
    public void sendMail(MessageHeader mail, PersistentBox box) {
        this.box = box;
        singleMail = mail;
        sendSMTP();
    }
    
    /**
     * Sends single mail or all mails in the box - according to setting of
     * the variable singleMail.
     */
    private synchronized void sendSMTP() {
        runMode = SEND;
        start();
    }

    public synchronized void close(boolean forcedDisc, PersistentBox box) {
        this.box = box;
        if (this.forcedDisc) {
            return;
        }
        this.forcedDisc = forcedDisc;
        runMode = CLOSE;
        start();
    }



    public void doWork() {
        if (runMode == CLOSE && forcedDisc) {
            busy = true;
            mailSender.close();
            forcedDisc = false;
            busy = false;
            box.repaint();
            return;
        }
        lock();
        busy = true;
        connection.unQuit();
        switch (runMode) {
            case CLOSE:
                mailSender.close();
                break;
            case SEND:
                // send mails
                sendMails(this);
                break;
        }
        
        busy = false;
        singleMail = null;
        unlock();
        box.repaint();
    }
    
    /**
     * Adds recipients to the addressbook.
     * @param mail the mail which recipients should be added
     */
    private void addRecipients(MessageHeader mail) {
        Vector rcps = mail.getRcp();
        for (short i = (short) (rcps.size() - 1); i >= 0; --i) {
            String tmpRcp = (String) rcps.elementAt(i);
            try {
                mujMail.getAddressBook().saveContact(new AddressBook.Contact("", tmpRcp, ""));
            } catch (MyException ex) {
            }
        }
        
    }
    
    public String captureStrCRLF(String capturer, String data) {
        return capturer.concat(data + "\r\n");
    }

    /**
     * Sends mails and displays the progress.
     *
     */
    private void sendMails(StoppableProgress progress) {
        try {
              if (DEBUG) System.out.println("DEBUG SMTP.sendMails(StoppableProgress) - send mails");
            
            // determine the number of messages being sent and set progress bar
            short max = (singleMail == null) ? (short) box.getMessageCount() : 1;
            updateProgress(max, 0);
            setTitle(Lang.get(Lang.ALRT_SMTP_SENDING) + Lang.get(Lang.ALRT_PL_CONNECTING)); // Todo account name

            mailSender.open();

            // for all messages being sent
            MessageHeader message;
            // used for sending the mail again to the sent box of IMAP server
            // not nice (Buffer overflow)
            // TODO: refactor
            String capturedMailText;
            for (short j = max; j > 0; j--) {
                // choose message to sent
                if (singleMail != null) {
                    message = singleMail;
                } else {
                    message = box.getMessageHeaderAt(j - 1);
                }
                // add recipients to the addressbook
                if (Settings.addToAddressbook) {
                    addRecipients(message);
                }
                // send message
                //box.report(Lang.get(Lang.ALRT_SMTP_SENDING) + " " + message.getSubject(), SOURCE_FILE);
                setTitle(Lang.get(Lang.ALRT_SMTP_SENDING) + " " + message.getSubject());

                capturedMailText = mailSender.sendMailToConnection(message, MailSender.SendingModes.SMTP_MODE, progress, box);
                // if the message was not sent
                if ( "".equals( capturedMailText ) ) {
                    message.setFailedToSend();
                    try {
                        message.saveHeader();
                    } catch (MyException ex) {
                        ex.printStackTrace();
                    }
                    continue;
                }

                  if (DEBUG) System.out.println("Account: " + getAccount());
                // send message to sent folder in the IMAP server
                if (getAccount().isCopyToSrvSent()) {
                      if (DEBUG) System.out.println("Copy to sent");
                    getAccount().getProtocol().saveMailToSent(capturedMailText, mujmail.MujMail.mujmail.getInBox()); // Alf: Use outcomming folder of message instead of inbox
                }
                // set the status of the message, mark it as deleted
                message.setSent();
                if (!message.wasDeleted()) {
                    // TODO: this two lines cause that sending mails does not work
                    // the problem is while deleting mails from the box: box.deleteMarkedFromBoxAndDB();
                    message.markAsDeleted();
                    box.incDeleted();
                }

                // save the message to the sent folder in the phone
                if (!Settings.safeMode) {
                    mujMail.getSentBox().storeMail(message);
                }


                updateProgress(max, max - j + 1);
                if (stopped()) {
                    break;
                }
            }
            Functions.sleep(500);
        } catch (MyException ex) {
            ex.printStackTrace();
            //_close() prevents a deadlock in the next session,
            //as now when we are in the middle of the transaction, the server is waiting for data from us
            //but the next session we are waiting for a response from the server
            mailSender.close();
            box.report(ex.getDetails(), SOURCE_FILE, ex);
            //setTitle(ex.getDetails() + " " + SOURCE_FILE + " " + ex);
            if (ex.getErrorCode() == MyException.COM_HALTED) {
                connection.unQuit();
            }

        } catch (Throwable ex) {
            ex.printStackTrace();
            mailSender.close();
            box.report("100: " + ex, SOURCE_FILE);
        }

        // markAsDeleted all messages marked as deleted
        box.deleteMarkedFromBoxAndDB();
    }

    /**
     * Gets account for sent copy.
     * @return the account
     */
    private MailAccount getAccount() {
        if (account == null) {
            initAccount();
        }

        return account;
    }
    
    
    /**
     * Mail sender which sends mails to smtp connection.
     */
    public class SMTPMailSender extends MailSender {

        public SMTPMailSender(ConnectionInterface connection) {
            super(connection);
        }

        protected synchronized void close_() {
            if (!this.connection.isConnected()) {
                return;
            }
            setTitle(Lang.get(Lang.ALRT_PL_CLOSING) + Settings.smtpServer);
            forcedDisc = false;
            try {
                this.connection.sendCRLF("QUIT");
            } catch (Exception e) {
                 setTitle(Lang.get(Lang.ALRT_PL_CLOSING) + Settings.smtpServer + Lang.get(Lang.FAILED) + " : " + e);
            }
            try {
                this.connection.close();
            } catch (Exception e) {
                setTitle(Lang.get(Lang.ALRT_PL_CLOSING) + Settings.smtpServer + Lang.get(Lang.FAILED) + " : " + e);
            }
            setTitle(Lang.get(Lang.ALRT_SMTP_SENDING) + Lang.get(Lang.ALRT_PL_CLOSING) + Settings.smtpServer + Lang.get(Lang.SUCCESS));
        }

        protected boolean open_() throws MyException {
            if (isConnected()) {
                return true;
            }
            try {
                close_();
                setTitle(Lang.get(Lang.ALRT_SMTP_SENDING) + Lang.get(Lang.ALRT_PL_CONNECTING) + Settings.smtpServer);
                
                  if (DEBUG) System.out.println("DEBUG SMTP.open_() - before connecting");
                this.connection.open(Settings.smtpServer + ":" + Settings.smtpPort, Settings.smtpSSL, Settings.smtpSSLType);
                  if (DEBUG) System.out.println("DEBUG SMTP.open_() - after connecting");
                String serverID = this.connection.getLine();

                // Helo phase
                // note - this check is not taken from ESMTP (RFC2821) specification ... only my decision ... can be removed with else part 
                if (serverID.indexOf("ESMTP") > 0) {
                    // Try to use extended capabilities - EHLO
                    // It's necessary for pochta.ru - if not used we aren't able to login

                    if (DEBUG) { setTitle("Saying EHLO"); }
                    this.connection.sendCRLF("EHLO mujmail.org");

                    boolean flagError = false; // EHLO command not successful flag
                    // Check server response
                    String smtpEHLOReply = this.connection.getLine();
                    if (smtpEHLOReply.startsWith("250") == false) {
                        flagError = true;
                    }
                    this.connection.clearInput();
                    
                    if (flagError == true) {
                        // Falling back to HELO command
                        //box.report("Saying HELO", SOURCE_FILE);
                        if (DEBUG) { setTitle("Saying HELO"); }
                        this.connection.sendCRLF("HELO mujmail.org");
                        //box.report("Server: " + this.connection.getLine(), SOURCE_FILE);
                        String tmp =  this.connection.getLine(); 
                        if (DEBUG) { setTitle("Server: " + tmp); }
                    }
                } else {
                    //box.report("Saying HELO", SOURCE_FILE);
                    if (DEBUG) { setTitle("Saying HELO"); }
                    this.connection.sendCRLF("HELO mujmail.xf.cz");
                    //box.report("Server: " + this.connection.getLine(), SOURCE_FILE);
                    String tmp = this.connection.getLine();
                    if (DEBUG) { setTitle("Server: " + tmp); }
                }
                
                
                if (Settings.smtpAuthName.length() != 0) { //TODO: test this and return false/true
                    //box.report("Authorizing...", SOURCE_FILE);
                    setTitle(Lang.get(Lang.ALRT_SMTP_SENDING) + " authorizing...");
                    this.connection.sendCRLF("AUTH PLAIN " + Decode.toBase64("\000" + Settings.smtpAuthName + "\000" + Settings.smtpAuthPass, false));
                    this.connection.getLine();
                }
                setTitle(Lang.get(Lang.ALRT_SMTP_SENDING) + Lang.get(Lang.ALRT_PL_CONNECTING) + Settings.smtpServer + Lang.get(Lang.SUCCESS));                
            } catch (MyException e) {
                e.printStackTrace();
                setTitle(Lang.get(Lang.ALRT_PL_CONNECTING) + Settings.smtpServer + Lang.get(Lang.FAILED));
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
                setTitle(Lang.get(Lang.ALRT_PL_CONNECTING) + Settings.smtpServer + Lang.get(Lang.FAILED));
                throw new MyException(MyException.COM_UNKNOWN, "100: " + e.toString());
            }
            return true;
        }
    }
}
