//#condition MUJMAIL_FS
/*
MujMail - Simple mail client for J2ME
Copyright (C) 2008 David Hauzar <david.hauzar.mujmail@gmail.com>
Copyright (C) 2008 Nodir Yuldashev

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

package mujmail.ui;


import java.io.IOException;
import java.util.Enumeration;

import javax.microedition.io.Connector;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextField;

import mujmail.Lang;
import mujmail.MujMail;
import mujmail.Properties;
import mujmail.jsr_75.FilesystemFactory;
import mujmail.jsr_75.MyFileConnection;
import mujmail.util.Callback;
import mujmail.util.StartupModes;

/**
 * Provides user interface for browsing the filesystem and choosing files and
 * directories.
 * 
 * @author Nodir Yuldashev, David Hauzar
 *  based on file attachment patch by John Dorfman
 */
public class FileSystemBrowser implements CommandListener { 
    /**
     * Enumeration class representing choosing modes of file system browser.
     */
    public static class ChoosingModes {
        private ChoosingModes() {};
        
        /** It will be possible to choose only files */
        public static final ChoosingModes FILES = new ChoosingModes();
        /** It will be possible to choose only directories */
        public static final ChoosingModes DIRECTORIES = new ChoosingModes();
    }
    
    /**
     * The thread that will be started after pressing select command.
     */
    private class SelectAction implements Runnable {
        private FileSystemBrowser fsBrowser;
        private Displayable displayable;
        public SelectAction(FileSystemBrowser fsBrowser, Displayable displayable) {
            this.fsBrowser = fsBrowser;
            this.displayable = displayable;
        }
        
        /**
         * Select command was pressed. If a directory have been chosen, go to this
         * directory. If a file have been chosen, this file is selected
         */
        public void run() {
            List curr = (List) displayable;
            final String tempcurrFile = curr.getString(curr.getSelectedIndex());
            if (tempcurrFile.endsWith(SEP_STR) || tempcurrFile.equals(UP_DIRECTORY)) {
                //Directory is chosen, go to the directory
                fsBrowser.traverseDirectory(tempcurrFile);
            } else {
                //File is chosen, choose this file and exit file system browser
                fileSelectedAction(tempcurrFile);
            }
        }
    }
    
    /**
     * Called when the file or directory was chosen.
     * Choose this file and exit filesystem browser - switch the display 
     * to the displayableCaller.
     * @param file 		chosen file or directory
     */
    private void fileSelectedAction(String file) {
        // set all state variables of the object - the file or directory has been chosen
        if (file.equals("..")) {
            currFile = "";
        } else {
            currFile = file;
        }
        wasFileSelected = true;
        System.out.println("Selected file" + file);
        selectedURL = "file://localhost/" + currDirName + currFile;

        // calls the object passed by user - the file or directory has been chosen
        finalAction.callback(this, "chosen");

        // switch the display: exit from file system browser
        mujMail.getDisplay().setCurrent(displayableCaller);
    }

    /** indicates whether the URL has been chosen until now */
    private boolean wasFileSelected;
    private final ChoosingModes choosingMode;
    private String currFile = "";
    private Callback finalAction;
    private MujMail mujMail;
    private Displayable displayableCaller;
    private String currDirName;
    private String selectedURL = "";
    private Command select = new Command(Lang.get(Lang.BTN_SELECT), Command.OK, 1);
    private Command confirm = new Command(Lang.get(Lang.BTN_CONFIRM), Command.SCREEN, 3);
    private Command cancel = new Command(Lang.get(Lang.BTN_CANCEL), Command.EXIT, 2);
    private Command createDir = new Command(Lang.get(Lang.BTN_FS_CREATE_DIR), Command.ITEM, 4);
    private final static String UP_DIRECTORY = "..";
    private final static String MEGA_ROOT = "";
    private final static String SEP_STR = "/";
    private final static char SEP = '/';
    private List browser;

    /**
     * Creates the instance of filesystem browser.
     * @param main the main object of the midlet - needed for changing focus, ...
     * @param caller the displayable caller - after browsing the filesystem,
     *  the focus will be returned to this object
     * @param finalAction method callback() of this object will be called before 
     *  exiting the browser;
     *  the parameter called will be this instance of FileSystemBrowser
     *  the parameter message will be of type String and will be "canceled" when
     *  user canceled the selection and "chosen" when user selected something
     * @param header the header of the filesystem browser form
     */
    public FileSystemBrowser(MujMail main, Displayable caller, Callback finalAction, 
            ChoosingModes choosingMode, String header) {
        this.choosingMode = choosingMode;
        this.finalAction = finalAction;
        mujMail = main;
        displayableCaller = caller;
        currDirName = MEGA_ROOT;
    }
    
    /**
     * Shows the browser window. Check for JSR75 availability has to be
     * performed before calling this function.
     * 
     * @param runMode the runmode of the filesystem browser
     */
    public void startBrowser(StartupModes runMode) {
        if (runMode == StartupModes.IN_THE_SAME_THREAD) {
            startBrowser();
        } else {
            // start in the new thread
            Runnable r = new Runnable() {

                public void run() {
                    startBrowser();
                }
            };
            Thread t = new Thread(r);
            t.start();
        }
    }
    
    /**
     * Starts the browsing of the filesystem with listing of all root directories.
     */
    private void startBrowser() {
        wasFileSelected = false;
        try {
            showCurrDir();
        } catch (SecurityException e) {
        } catch (Exception e) {
        }
    }

    /**
     * Gets the chosen URL. It can be URL of either file or directory according
     * to choosing mode of the file system browser.
     * @return the url choosen by user.
     */
    public String getSelectedURL() {
        return selectedURL;
    }
    /**
     * Gets the name of chosen file or directory.
     * @return the name of chosen file or directory.
     */
    public String getSelectedFileOrDirName() {
        return currFile;
    }
    
    /**
     * Gets true if some file or directory was chosen by FileSystem browser.
     * @return true if some file or directory was chosen, 
     *  false if not (canceled button) was pressed
     */
    public boolean wasFileSelected() {
        return wasFileSelected;
    }
    
    /**
     * Returns screen of filesystem browser to be displayed to user.
     *
     * @return the screen of filesystem browser to be displayed to user.
     */
    public Displayable getDisplayable() {
        return browser;
    }
    
    /**
     * Called from command action if select command was pressed.
     * If it was chosen a directory, go to this directory. If it was chosen 
     * a file, choose this file and exit - return focus to the caller.
     * @param caller the displayable caller - after browsing the filesystem,
     *  the focus will be returned to this object
     */
    private void selectAction(Displayable caller) {
        SelectAction r = new SelectAction(this, caller);
        Thread t = new Thread(r);
        t.start();
    }

    public void commandAction(Command c, Displayable d) {
        if (c == confirm) {
            // directory have been chosen
            List curr = (List) d;
            final String tempcurrFile = curr.getString(curr.getSelectedIndex());
            fileSelectedAction(tempcurrFile);
        }
        else if (c == select) {
            // file or directory was chosen
            selectAction(d);
        } else if (c == createDir) {
            // create new directory
            createDir(d);
        } else if (c == cancel) {
//TODO if possible destroy the instance of FileSystemBrowser
            mujMail.getDisplay().setCurrent(displayableCaller);
        }
    }
    
    /**
     * The callback object used to create new directory and switch focus back to
     * the filesystem browser.
     */
    private class CreateDirectory implements Callback {
        private String sMessage;
        private Displayable d;

        public CreateDirectory(FileSystemBrowser fsBrowser, Displayable d) {
            this.d = d;
        }
        
        public void callback(Object called, Object message) {
            this.sMessage = (String) message;
            Runnable runnable = new Runnable() {

                public void run() {
                    // creates the directory
                    try {
                        System.out.println(currDirName + sMessage);
                        MyFileConnection conn = FilesystemFactory.getFileConnection(
                                "file://localhost/" + currDirName + sMessage + SEP);
                        conn.mkdir();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        mujMail.alert.setAlert("The directory cannot by created", 
                                AlertType.ERROR);
                    }
                    // switch the focus back to the filesystem browser
                    mujMail.getDisplay().setCurrent(d);
                    traverseDirectory("");
                }
            };
            Thread thr = new Thread(runnable);
            thr.start();
        }
    }
    
    
    /**
     * Creates new directory.
     */
    private void createDir(Displayable d) {
        Callback callback = new CreateDirectory(this, d);
        TextInputDialog dialog = new TextInputDialog(Lang.get(Lang.FS_BROWSER_ENTER_DIR_NAME), 
                "", Properties.directoryLength, TextField.URL);
        dialog.start(getDisplayable(), callback);
    }

    /**
     * Shows current directory. This is directory given by currDirName.
     */
    private void showCurrDir() {
        Enumeration e;
        MyFileConnection currDir = null;
        try {
            browser = new List(currDirName, List.IMPLICIT);
            
            browser.setSelectCommand(select);
            browser.addCommand(cancel);
            browser.addCommand(select);
            if (choosingMode == ChoosingModes.DIRECTORIES) {
                browser.addCommand(confirm);
                if (currDirName != MEGA_ROOT) {
                    browser.addCommand(createDir);
                }
            }
            
            if (MEGA_ROOT.equals(currDirName)) {
                // we are on the top
                e = FilesystemFactory.getFileSystemRegistry().listRoots();
            } else {
                // we are in some nested directory
                currDir = FilesystemFactory.getFileConnection("file://localhost/" + currDirName, Connector.READ);
                e = currDir.list();
                browser.append(UP_DIRECTORY, null);
            }
            while (e.hasMoreElements()) {
                String fileName = (String) e.nextElement();
                if (fileName.charAt(fileName.length() - 1) == SEP) {
                    browser.append(fileName, null);
                } else if (choosingMode == ChoosingModes.FILES) {
                    browser.append(fileName, null);
                }
            }
            
            browser.setCommandListener(this);
            if (currDir != null) {
                currDir.close();
            }
            mujMail.getDisplay().setCurrent(browser);
        } catch (IOException ioe) {
            System.out.println("IO Exception occured");
        } catch (SecurityException se) {
            System.out.println("Security Exception occured");
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Exception occured");
        }
    }

    
    private void traverseDirectory(String fileName) {
        if (currDirName.equals(MEGA_ROOT)) {
            if (fileName.equals(UP_DIRECTORY)) {
                // can not go up from MEGA_ROOT
                finalAction.callback(this, "canceled");
                return;
            }
            currDirName = fileName;
        } else if (fileName.equals(UP_DIRECTORY)) {
            //Go up one directory
//TODO use setFileConnection when implemented
            int i = currDirName.lastIndexOf(SEP, currDirName.length() - 2);
            if (i != -1) {
                currDirName = currDirName.substring(0, i + 1);
            } else {
                currDirName = MEGA_ROOT;
            }
        } else {
            currDirName = currDirName + fileName;
        }
        showCurrDir();
    }
}
