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

package mujmail.protocols;

import java.util.Vector;

import mujmail.BodyPart;
import mujmail.util.Decode;
import mujmail.util.Functions;
//#ifdef MUJMAIL_FS
import mujmail.FileSystemMailExporter;
//#endif
import mujmail.Lang;
import mujmail.MessageHeader;
import mujmail.MyException;
import mujmail.TheBox;
import mujmail.connections.ConnectionInterface;
import mujmail.protocols.SMTP.SMTPMailSender;
import mujmail.tasks.StoppableProgress;

/**
 * Abstract class providing interface and basic functionality for sending mails
 * to the object of class BasicConnection.
 * 
 * This means that it is possible to send mails to the SMTP connection as well
 * as to the file system connection.
 * 
 * To use this class, implement methods close_() and open_() that must
 * correctly establish and close the connection.
 * 
 * @see MailSender.SMTP_Mode
 * @see SMTPMailSender
 * @see FileSystemMailExporter
 * 
 * @author David Hauzar
 */
public abstract class MailSender {

    private static final String SOURCE_FILE = "mujmail.MailSender";
    /** True if the connection is opened. */
    private boolean connectionOpened = false;
    /** Email will be sent to this connection */
    protected final ConnectionInterface connection;

    /**
     * Constructor.
     * @param connection the object to which the mail will be sent
     */
    protected MailSender(ConnectionInterface connection) {
        this.connection = connection;
    }

    /**
     * Sends given mail to the connection of MailSender.
     * Deparses the content of the mail from entries of the instance 
     * of MessageHeader.
     * 
     * TODO: Warning: return value of capturedMailText:
     * - the return value is there just because we sometimes need (according to settings)
     * to send the content furthermore to the directory SENT on the imap account
     * - the problem is that cumulating the content of the sent mail in string
     * can lead to BufferOverflow
     * - it would be better to sent email to directory SENT separately
     * 
     * - to not cause BufferOverflow the content of attachments is not cumulating
     * now: see methods sendAttachment
     * 
     * TODO2: we are cumulating the content of sent mail even if the content
     * is not needed
     * 
     * @param mail the mail which will be sent to the connection
     * @return the content of sent mail;
     *  empty string if sending of mail was not succesfull (server replied 250..)
     */
    public String sendMailToConnection(MessageHeader message, 
            SendingModes sendingMode, StoppableProgress progress, TheBox reportBox )
        throws MyException, Exception, Throwable {
        
        if (!connectionOpened) {
            throw new MyException(MyException.PROTOCOL_CANNOT_CONNECT, "The connection must be opened before sending the mail");
        }
        
        
        String capturedMailText = "";
        Vector rcps = null;
        String tmpRcp;
        // boundary defined in multipart messages (ie. with attached file)
        String boundary = "16509XY-120sR7729-tree0";
        // -- + boundary used in multipart email body
        String boundaryInBody = "--" + boundary;

        String tmpMailLine = "";

        capturedMailText = ""; // Clear text before capturing new message
               
        tmpMailLine = "MAIL FROM: <" + Functions.emailOnly(message.getFrom()) + ">";
        capturedMailText = captureStrCRLF(capturedMailText, tmpMailLine);
        connection.sendCRLF(tmpMailLine);
        connection.getLine();

        // send recipients
        rcps = message.getRcp();
        for (short i = (short) (rcps.size() - 1); i >= 0; --i) {
            tmpRcp = (String) rcps.elementAt(i);

            tmpMailLine = "RCPT TO: <" + tmpRcp + ">";
            capturedMailText = captureStrCRLF(capturedMailText, tmpMailLine);
            connection.sendCRLF(tmpMailLine);
            connection.getLine();
        }
        
        if (sendingMode.sendDataCommand()) {
            tmpMailLine = "DATA";
            capturedMailText = captureStrCRLF(capturedMailText, tmpMailLine);
            connection.sendCRLF(tmpMailLine);
            connection.getLine();
        }

        tmpMailLine = "Return-Path: <" + Functions.emailOnly(message.getFrom()) + ">";
        capturedMailText = captureStrCRLF(capturedMailText, tmpMailLine);
        connection.sendCRLF(tmpMailLine);
        tmpMailLine = "Date: " + message.getTimeStr() + " " + Functions.getLocalTimeZone();
        capturedMailText = captureStrCRLF(capturedMailText, tmpMailLine);
        connection.sendCRLF(tmpMailLine);
        tmpMailLine = "From: " + message.getFrom();
        capturedMailText = captureStrCRLF(capturedMailText, tmpMailLine);
        connection.sendCRLF(tmpMailLine);

        short i;
        i = (short) message.getRecipients().indexOf("To:");
        if (i != -1) {
            tmpRcp = Functions.encodeRcpNames(message.getRecipients().substring(i, message.getRecipients().indexOf(" *", i + 3)));
            capturedMailText = captureStrCRLF(capturedMailText, tmpRcp);
            connection.sendCRLF(tmpRcp);
        }
        i = (short) message.getRecipients().indexOf("Cc:");
        if (i != -1) {
            tmpRcp = Functions.encodeRcpNames(message.getRecipients().substring(i, message.getRecipients().indexOf(" *", i + 3)));
            capturedMailText = captureStrCRLF(capturedMailText, tmpRcp);
            connection.sendCRLF(tmpRcp);
        }
        i = (short) message.getRecipients().indexOf("Bcc:");
        if (i != -1) {
            tmpRcp = Functions.encodeRcpNames(message.getRecipients().substring(i, message.getRecipients().indexOf(" *", i + 4)));
            capturedMailText = captureStrCRLF(capturedMailText, tmpRcp);
            connection.sendCRLF(tmpRcp);
        }
        tmpMailLine = "Subject: " + sendingMode.encodeHeaderField(message.getSubject());
        capturedMailText = captureStrCRLF(capturedMailText, tmpMailLine);
        connection.sendCRLF(tmpMailLine);

        // plain or multipart email "intro" at end of email header
        if (message.isPlain()) {
            tmpMailLine = "MIME-Version: 1.0";
            capturedMailText = captureStrCRLF(capturedMailText, tmpMailLine);
            connection.sendCRLF(tmpMailLine);

            tmpMailLine = "Content-Type: text/plain; charset=UTF-8";
            capturedMailText = captureStrCRLF(capturedMailText, tmpMailLine);
            connection.sendCRLF(tmpMailLine);

            tmpMailLine = sendingMode.getContentTransferEncodingLine();
            capturedMailText = captureStrCRLF(capturedMailText, tmpMailLine);
            connection.sendCRLF(tmpMailLine);

            tmpMailLine = "Content-Disposition: inline";
            capturedMailText = captureStrCRLF(capturedMailText, tmpMailLine);
            connection.sendCRLF(tmpMailLine);
        } else if (!message.isPlain()) {
            tmpMailLine = "MIME-Version: 1.0";
            capturedMailText = captureStrCRLF(capturedMailText, tmpMailLine);
            connection.sendCRLF(tmpMailLine);

            tmpMailLine = "Content-Type: multipart/mixed; boundary=\"" + boundary + "\"";
            capturedMailText = captureStrCRLF(capturedMailText, tmpMailLine);
            connection.sendCRLF(tmpMailLine);
        }

        connection.sendCRLF("");
        capturedMailText = captureStrCRLF(capturedMailText, "");

        // if multipart email, add below before text body 
        if (!message.isPlain()) {
            tmpMailLine = boundaryInBody;
            capturedMailText = captureStrCRLF(capturedMailText, tmpMailLine);
            connection.sendCRLF(tmpMailLine);

            tmpMailLine = "Content-Type: text/plain; charset=UTF-8";
            capturedMailText = captureStrCRLF(capturedMailText, tmpMailLine);
            connection.sendCRLF(tmpMailLine);

            tmpMailLine = "Content-Transfer-Encoding: base64";
            capturedMailText = captureStrCRLF(capturedMailText, tmpMailLine);
            connection.sendCRLF(tmpMailLine);

            tmpMailLine = "Content-Disposition: inline";
            capturedMailText = captureStrCRLF(capturedMailText, tmpMailLine);
            connection.sendCRLF(tmpMailLine);

            connection.sendCRLF("");
            capturedMailText = captureStrCRLF(capturedMailText, "");
        }

        if (message.getBodyPartCount() >= 1) {
            capturedMailText = message.getBodyPart((byte)0).getStorage().
                    sendContentToConnection(connection, sendingMode, true);
        }

        // send attachements
        // if multi message, add below before base64 encoded attached file
        if (!message.isPlain()) {
            for (int n = 0; n < message.getAttachementCount(); n++) {
                tmpMailLine = sendAttachement(message.getAttachement(n), boundaryInBody,
                        sendingMode);
                capturedMailText = capturedMailText + tmpMailLine;
            }
            tmpMailLine = boundaryInBody + "--";
            capturedMailText = captureStrCRLF(capturedMailText, tmpMailLine);
            connection.sendCRLF(tmpMailLine);
        }

        // send final line
        connection.sendCRLF("\r\n.");
        capturedMailText = captureStrCRLF(capturedMailText, "\r\n.");

        // read the reply of the connection (for example smtp server)
        if (!connection.getLine().startsWith("250")) {
            // System.out.println("some error while sending");
            connection.unGetLine();
            reportBox.report("*" + Lang.get(Lang.ALRT_SMTP_SENDING) + message.getSubject() + Lang.get(Lang.FAILED) + ": " + connection.getLine(), SOURCE_FILE);

            return "";
        }

        // message succesfully sent
        reportBox.report(Lang.get(Lang.ALRT_SMTP_SENDING) + message.getSubject() + Lang.get(Lang.SUCCESS), SOURCE_FILE);
        progress.incActual(1);

        return capturedMailText;
    }

    public static String captureStrCRLF(String capturer, String data) {
        return capturer.concat(data + "\r\n");
    }

    /**
     * Sends body part representing attachement.
     * TODO: better handling of return value
     * @param attachement body part to send
     * @return String with data that were send into the connection
     *  TODO: always returns ""
     */
    private String sendAttachement(BodyPart attachement, String boundaryInBody,
            SendingModes sendingMode)
            throws Throwable {
        
        // send header of the attachement
        sendAttachementHeader(connection, attachement, boundaryInBody);

        // send data
        attachement.getStorage().sendContentToConnection(connection, sendingMode, false);
         
         return "";
    }
    
    /**
     * Sends the header (the beginning) of an attachement.
     * Does not send the data of an attachement.
     * Called by metdhod sendAttachement()
     * 
     * @param attachment the BodyPart representing the attachement which is sent
     * @param boundaryInBody the delimiter used as boundary between attachements
     * 
     * @return String with data that were send into the connection
     * @throws java.lang.Exception
     */
    private String sendAttachementHeader(ConnectionInterface connection, BodyPart attachment, String boundaryInBody)
        throws Exception {
        String fileName = attachment.getHeader().getName();
        String fileExtension = attachment.getHeader().getExtension();
        
        String capturedAttachmentText = "";
        String tmpAttachmentLine = "";
        String tmpSendLine;
        
        tmpSendLine = boundaryInBody;
        capturedAttachmentText = captureStrCRLF(capturedAttachmentText, tmpSendLine);
        connection.sendCRLF(tmpSendLine);
        
        // depending on file extension, set mime type
        fileExtension = fileExtension.toLowerCase();
        if (fileExtension.equals("jpg")) {
            tmpAttachmentLine = "Content-Type: image/jpeg; name=\"" + fileName + "\"";
        } else if (fileExtension.equals("png")) {
            tmpAttachmentLine = "Content-Type: image/png; name=\"" + fileName + "\"";
        } else if (fileExtension.equals("3gp")) {
            tmpAttachmentLine = "Content-Type: video/3gpp; name=\"" + fileName + "\"";
        } else if (fileExtension.equals("txt")) {
            tmpAttachmentLine = "Content-Type: text/plain; name=\"" + fileName + "\"";
        } else {
            tmpAttachmentLine = "Content-Type: application/octet-stream; name=\"" + fileName + "\"";
        }
        connection.sendCRLF(tmpAttachmentLine);
        capturedAttachmentText = captureStrCRLF(capturedAttachmentText, tmpAttachmentLine);

        // encoding
        tmpAttachmentLine = "Content-Transfer-Encoding: base64";
        connection.sendCRLF(tmpAttachmentLine);
        capturedAttachmentText = captureStrCRLF(capturedAttachmentText, tmpAttachmentLine);

        // filename
        tmpAttachmentLine = "Content-Disposition: attachment; filename=\"" + fileName + "\"";
        connection.sendCRLF(tmpAttachmentLine);
        capturedAttachmentText = captureStrCRLF(capturedAttachmentText, tmpAttachmentLine);

        // empty line
        connection.sendCRLF("");
        capturedAttachmentText = captureStrCRLF(capturedAttachmentText, "");
        
        
        
        return capturedAttachmentText;
    }

    /**
     * Opens the connection and sets correct state of the connection.
     * Must be called before the first calling of the method sendMailToConnection.
     * 
     * @return true if the connection was succesfully opened
     * @throws mujmail.MyException
     */
    public boolean open() throws MyException {
        connection.clearInput();
        
        if ( open_() ) {
            connectionOpened = true;
            return true;
        }
        
        return false;
    }

    /**
     * Closes the connection and sets correct state of the connection.
     * Must be called after sending all mails - after last calling of the method
     * sendMailToConnection()
     */
    public void close() {
        connectionOpened = false;

        close_();
    }

    /**
     * Estabilishes the connection.
     * For example if the mail is sent via SMTP_MODE, this method must estabilish
     * the connection with the server.
     * @return true if the connection was succesfully opened
     * @throws mujmail.MyException
     */
    protected abstract boolean open_() throws MyException;

    /**
     * Closes the connection.
     * For example if the mail is sent via SMTP_MODE, this method must close
     * the connection with the server.
     */
    protected abstract void close_();
    
    
    /**
     * Enumeration interface representing sending modes used when sending mail.
     * Sending modes differs by encoding used and whether DATA command is sent
     * to server after sending information about recipeints in header of the mail.
     */
    public static interface SendingModes {
        /**
         * Gets encoded string.
         * @param input the data to be encoded
         * @param isFile true if encoded string is from file from filesystem
         * @return encoded string
         */
        public String toEncoding(String input, boolean isFile);
        /**
         * Encodes the field in the header of the mail.
         * @param input the data from the header of the mail
         * @return encoded string
         */
        public String encodeHeaderField(String input);
        /**
         * Gets the line in mail header with information about encoding.
         * For example: Content-Transfer-Encoding: base64
         * for base64 encoding.
         * @return the line in mail header with information about encoding
         */
        public String getContentTransferEncodingLine();
        /**
         * Returns true if DATA command should be sent. 
         * The DATA command should be sent when using SMTP connection and should 
         * not be sent when exporting the mail into filesystem.
         * @return true if DATA command should be sent.
         */
        public boolean sendDataCommand();
        
        /** SMTP_MODE sending mode. Suitable for sending mails via SMTP_MODE. 
         * Uses BASE64 encoding and sends DATA command. */
        public static final SendingModes SMTP_MODE = new SMTP_Mode();
        /** File system encoding. Suitable when exporting mail to the filesystem.
         * Uses 7 bit encoding and does not send data command. */
        public static final SendingModes FILE_SYSTEM_MODE = new FileSystemMode();
        
    }
    
    private static class SMTP_Mode implements SendingModes {

        public String toEncoding(String input, boolean isFile) {
            return Decode.toBase64(input, isFile);
        }

        public String encodeHeaderField(String input) {
            return Decode.encodeHeaderField(input);
        }

        public String getContentTransferEncodingLine() {
            return "Content-Transfer-Encoding: base64";
        }

        public boolean sendDataCommand() {
            return true;
        }
        
        
        
        
    }
    private static class FileSystemMode implements SendingModes {

        public String toEncoding(String input, boolean isFile) {
            return input;
        }

        public String encodeHeaderField(String input) {
            return input;
        }

        public String getContentTransferEncodingLine() {
            return "Content-Transfer-Encoding: 7bit";
        }
        
        public boolean sendDataCommand() {
            return false;
        }
        
    }
}
