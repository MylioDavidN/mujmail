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

package mujmail.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import mujmail.Lang;
import mujmail.MujMail;
import mujmail.util.Callback;

/**
 * Dialog that allows user to allow or not to allow given action.
 * After user presses OK or Cancel button, the focus will be on the screen
 * where it was when the dialog was started.
 * 
 * @author David Hauzar
 */
public class OKCancelDialog extends MVCComponent {
    private Displayable nextScreen;
    private final Callback okAction;
    private Callback cancelAction;
    private String dialogText;
    private String dialogTitle;
    private Command OK = new Command(Lang.get(Lang.BTN_OK), Command.OK, 0);
    private Command CANCEL = new Command(Lang.get(Lang.BTN_CANCEL), Command.CANCEL, 1);

    /**
     * Creates the dialog.
     *
     * If user presses Cancel button only focus will be setted to the screen
     * where it was before the dialog was started.
     *
     * @param dialogTitle the title of the dialog.
     * @param dialogText the text in the dialog
     * @param okAction the action when user press OK button.
     */
    public OKCancelDialog(String dialogTitle, String dialogText, Callback okAction) {
        this(dialogTitle, dialogText, okAction, Callback.NO_ACTION);
    }

    /**
     * Creates the dialog.
     * 
     * @param dialogTitle the title of the dialog.
     * @param dialogText the text in the dialog
     * @param okAction the action when user press OK button.
     * @param cancelAction the action that will be performed when user press
     *  Cancel button.
     */
    public OKCancelDialog(String dialogTitle, String dialogText, Callback okAction, Callback cancelAction) {
        this.cancelAction = cancelAction;
        this.okAction = okAction;
        this.dialogText = dialogText;
        this.dialogTitle = dialogTitle;
    }

    public void showScreen() {
        nextScreen = MujMail.mujmail.getDisplay().getCurrent();
        super.showScreen();
        MujMail.mujmail.getDisplay().disableSettingCurrent();
    }



    public void commandAction(Command c, Displayable d) {
        if (c == OK) {
            okAction.callback(this, null);
        }

        if (c == CANCEL) {
            cancelAction.callback(this, null);
        }

        MujMail.mujmail.getDisplay().enableSettingCurrent();
        MujMail.mujmail.getDisplay().setCurrent(nextScreen);
    }
    

    protected void createView() {
    }

    protected Displayable getView() {
        Form dialogForm = new Form(dialogTitle);
        dialogForm.append(new StringItem("", dialogText));
        dialogForm.addCommand(OK);
        dialogForm.addCommand(CANCEL);

        return dialogForm;
    }

    protected void initModel() {
    }

    protected void updateView() {
    }
    

}
