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

import java.io.DataOutputStream;
import mujmail.util.Callback;
import mujmail.util.StartupModes;
import mujmail.ui.FileSystemBrowser;
import mujmail.ui.TextInputDialog;
import javax.microedition.io.Connector;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextField;
import mujmail.jsr_75.FilesystemFactory;
import mujmail.jsr_75.MyFileConnection;

/**
 * Exports bodyparts to the filesystem.
 * @author David Hauzar
 */
public class FileSystemBodyPartExporter {
    /** Set to true if debug information should be displayed while reporting
     messages using methods report() */
    private static final boolean DEBUG = false;
    
    /**
     * Exports given bodypart to filesystem.
     * @param caller the displayable object that should be displayed when finished
     * @param messageHeader the message that contains bodypart to be exported
     * @param bodyPartID  the ID of the BodyPart to be exported.
     */
    public static void exportBPToFS(Displayable caller, MessageHeader messageHeader, byte bodyPartID) {
        // let user choose the directory to which the bodypart will be exported
        FileSystemBrowser fsBrowser = new FileSystemBrowser(MujMail.mujmail, caller, 
                new ExportBodypartToFS(messageHeader, bodyPartID), FileSystemBrowser.ChoosingModes.DIRECTORIES,
                Lang.get(Lang.FS_BROWSER_SELECT_DIR));
        fsBrowser.startBrowser(StartupModes.IN_NEW_THREAD);
    }
    
    /**
     * The method callback will be called after user selects directory to export
     * the bodypart.
     * 
     * Lets user write the filename of the exported bodypart (default will be 
     * the name of the bodypart).
     * Writes the bodypart to the filesystem.
     * 
     */
    private static class ExportBodypartToFS implements Callback {
        // the id of the bodypart being exported
        private final byte bpID;
        private String urlOfActExportedBP;
        private final MessageHeader messageHeader;
        
        public ExportBodypartToFS(MessageHeader messageHeader, byte bpID) {
            this.messageHeader = messageHeader;
            this.bpID = bpID;
        }

        public void callback(Object called, Object message) {
            // if user did not choose any file, no action is needed
            if (((String) message).equals("canceled"))  return;
            FileSystemBrowser fsBrowser = (FileSystemBrowser) called;
            
            // lets user write the filename of the exported bodypart
            BodyPart bp = messageHeader.getBodyPart(bpID);
            String filename = bp.getHeader().getName();
            TextInputDialog dialog = new TextInputDialog(Lang.get(Lang.FS_BROWSER_ENTER_FILE_NAME), 
                    filename, 55, TextField.ANY);
            dialog.start(((FileSystemBrowser) called).getDisplayable(),
                    new ChooseNameOfExportedBodyPart(this, fsBrowser.getSelectedURL()));
        }
        
        /**
         * Exports bodypart to given url.
         * @param url the url where export bodypart.
         */
        private void exportBodyPart(String url) {
            urlOfActExportedBP = url;
            Runnable runnable = new Runnable() {

                public void run() {
                    BodyPart bp = messageHeader.getBodyPart(bpID);
                    try {

                        // creates the file
                        if (DEBUG) System.out.println("URL to that export the file: " + urlOfActExportedBP);
                        MyFileConnection conn = FilesystemFactory.getFileConnection(urlOfActExportedBP, Connector.READ_WRITE);
                        if (conn.exist()) {
                            // the file already exist
                            MujMail.mujmail.alert.setAlert(Lang.get(Lang.EXP_FS_FILE_EXISTS), AlertType.WARNING);
                            return;
                        } else {
                            // the file does not exist, create new
                            conn.create();
                        }

                        // write the data of the bodypart to the file
                        DataOutputStream outputStream = conn.openDataOutputStream();
                        outputStream.flush();
                        bp.getStorage().getContent(outputStream);
                        outputStream.flush();
                        outputStream.close();
                        conn.close();
                    } catch (Throwable ex) {
                        MujMail.mujmail.alert.setAlert(Lang.get(Lang.EXP_FS_IO_ERROR), AlertType.ERROR);
                        ex.printStackTrace();
                    } finally {
                        MujMail.mujmail.getDisplay().setCurrent(MujMail.mujmail.mailForm);
                    }
                }
            };
            
            
            
            
            Thread thread = new Thread(runnable);
            thread.start();
        }
        
        
        
    }
    
    private static class ChooseNameOfExportedBodyPart implements Callback {
        private final ExportBodypartToFS exportBodyPart;
        private final String url;

        public ChooseNameOfExportedBodyPart(ExportBodypartToFS exportBodyPart, String url) {
            this.exportBodyPart = exportBodyPart;
            this.url = url;
        }
        
        public void callback(Object called, Object message) {
            exportBodyPart.exportBodyPart(url + (String) message);
            
        }
    }

}
