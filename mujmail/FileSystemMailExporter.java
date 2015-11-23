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

package mujmail;

import mujmail.util.Callback;
import mujmail.util.StartupModes;
import mujmail.ui.FileSystemBrowser;
import javax.microedition.io.Connector;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.TextField;

import mujmail.ui.TextInputDialog;
import mujmail.jsr_75.FilesystemFactory;
import mujmail.jsr_75.MyFileConnection;
import mujmail.connections.ConnectionInterface;
import mujmail.connections.ConnectorFileSystemOutput;
import mujmail.connections.ConnectionCompressed;
import mujmail.protocols.MailSender;
import mujmail.tasks.StoppableBackgroundTask;


/**
 * Exports mails to the filesystem.
 * 
 * Uses object of class MailSender which sends data to object of class
 * FileSystemOutputConnection.
 * @author David Hauzar
 */
public class FileSystemMailExporter implements Callback {
    
    private final MujMail mujMail;
    private TheBox box;
    /** Mail which is currently exported */
    private MessageHeader actExportedMail;

    public FileSystemMailExporter(MujMail mujMail) {
        this.mujMail = mujMail;
    }
    
    /**
     * Exports given mail to the filesystem.
     * User will be asked to enter url where export the mail and the
     * filename of exported mail.
     * When the export will be finished, the display will be switched to the box.
     * @param box used to report warnings; when the export will be finished,
     *  the display will be switched to this box
     * @param mail the mail which will be exported
     */
    public void exportMail(TheBox box, MessageHeader mail) {
        this.box = box;
        // will be used in callback method of FileSystemBrowser
        this.actExportedMail = mail;
        
        // let user choose the url (and filename) to which export the mail
        FileSystemBrowser fsBrowser = new FileSystemBrowser(mujMail, box, this, 
                FileSystemBrowser.ChoosingModes.DIRECTORIES, 
                Lang.get(Lang.FS_BROWSER_SELECT_DIR));
        fsBrowser.startBrowser(StartupModes.IN_NEW_THREAD);
        
    }

    public void callback(Object called, Object message) {
        // callback method of FileSystemBrowser;
        
        // if user did not choose any file, no action is needed
        if (((String) message).equals("canceled"))  return;
        FileSystemBrowser fsBrowser = (FileSystemBrowser) called;
        
        // callback object exports the mail after user enters the filename
        Callback callback = new 
                ChooseExportedMailFileNameCallback(fsBrowser.getSelectedURL());
        // display the dialog to set the filename of exported mail
        TextInputDialog dialog = new TextInputDialog(Lang.get(Lang.FS_BROWSER_ENTER_FILE_NAME), 
                actExportedMail.getMessageIDWithoutSpecCharacters() + ".txt", 35, TextField.ANY);
        dialog.start(fsBrowser.getDisplayable(), callback);
    }
    
    
    /**
     * Exports the mail to the file given by url.
     * Starts new thread before exporting.
     * @param mail the mail which will be exported
     * @param url the url where export the mail
     */
    public void exportMail(MessageHeader mail, String url)  {
        // the action must be run in new thread
        StoppableBackgroundTask writeMailToFSTask = new WriteMailToFS(mail, url);
        writeMailToFSTask.start();
    }
    
    /**
     * Class which object will be passed as callback to dialog which enables
     * user to set the file name of exported mail.
     * This object will use the filename to finish the export of the message
     * to the filesystem.
     */
    private class ChooseExportedMailFileNameCallback implements Callback {
        private final String actPath;

        /**
         * Constructor.
         * 
         * @param actPath the path to the url where export the mail
         */
        public ChooseExportedMailFileNameCallback(String actPath) {
            this.actPath = actPath;
        }
        
        public void callback(Object called, Object message) {
            // exports the mail
            System.out.println("Exporting mail: " + actPath + (String) message);
            exportMail(actExportedMail, actPath + (String) message);
            mujMail.getDisplay().setCurrent(box);
          }
        
    }
    
    
    /**
     * Used to write mail to filesystem in new thread
     */
    private class WriteMailToFS extends StoppableBackgroundTask {
        private final String url;
        private final MessageHeader mail;

        public WriteMailToFS(MessageHeader mail, String url) {
            super(Lang.get(Lang.BTN_MF_EXPORT_BP_TO_FS) + " " + url + " WriteMailToFS");
            this.mail = mail;
            this.url = url;
        }
        
        
        public void doWork() {
            try {
                // creates MailSender
                MyFileConnection fileConnection = FilesystemFactory.getFileConnection(url, Connector.READ_WRITE);
                System.out.println("Creating file: " + url);
                if (fileConnection.exist()) {
                    // the file already exist
                    mujMail.alert.setAlert(Lang.get(Lang.EXP_FS_FILE_EXISTS), AlertType.WARNING);
                    return;
                } else {
                    // the file does not exist, create new
                    fileConnection.create();
                }
                ConnectionInterface fileSystemConnection = new ConnectionCompressed( new ConnectorFileSystemOutput(fileConnection), ConnectionCompressed.COMPRESSION_TYPE_NONE);
                MailSender mailSender = new MailSender(fileSystemConnection) {

                        protected boolean open_() throws MyException {
                            // no action is needed to establish filesystem connection
                            return true;
                        }

                        protected void close_() {
                                connection.close();
                        }
                    };

                // exports the mail
                mailSender.open();
                mailSender.sendMailToConnection(mail, MailSender.SendingModes.FILE_SYSTEM_MODE, this, box);
                mailSender.close();

            } catch(Throwable ex) {
                ex.printStackTrace();
                setTitle(ex.getMessage());
            }
        }
        
    }
    
}
