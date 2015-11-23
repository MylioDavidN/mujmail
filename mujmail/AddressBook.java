package mujmail;

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

import mujmail.util.Functions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;

//#ifdef MUJMAIL_TOUCH_SCR
import mujmail.pointer.MujMailPointerEventProducer;
import mujmail.pointer.MujMailPointerEventListener;
//#endif

/**
 * Makes possible to administer, display and search the contacts.
 */
public class AddressBook extends Canvas implements Runnable {

    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false;

    public //to flip recipients when Insert Recipients from sendMail
    Vector getAddresses() {
        return addresses;
    }

    public void setAddresses(Vector addresses) {
        this.addresses = addresses;
    }

    public //to map contacts marked as recipients
    Hashtable getEmailHash() {
        return emailHash;
    }

    public void setEmailHash(Hashtable emailHash) {
        this.emailHash = emailHash;
    }

    /**
     * Represents one contact stored in database / vector addresses.
     */
    public static class Contact {

        String name;
        private String email;
        String notes;
        int DBIndex;

        public String getName() {
            return name;
        }

        public Contact(String name, String email, String notes) {
            this.name = name;
            this.email = email;
            this.notes = notes;
        }
        
        //#ifdef MUJMAIL_SYNC
        public String toString() {
        	return "Name: " + this.name + "\n" +
        	       "Email: " + this.getEmail() + "\n" +
        	       "Notes: " + this.notes + "\n\n";
        }
        
        public static Contact parseContact(String contactStr)
        {
        	String name, email, notes;
	
        	name = contactStr.substring(contactStr.indexOf("Name: ") + 6, contactStr.indexOf('\n'));
        	contactStr = contactStr.substring(contactStr.indexOf("\n") + 1);
        	email = contactStr.substring(contactStr.indexOf("Email: ") + 7, contactStr.indexOf('\n'));
        	contactStr = contactStr.substring(contactStr.indexOf("\n") + 1);
        	notes = contactStr.substring(contactStr.indexOf("Notes: ") + 7, contactStr.indexOf('\n'));

        	return new Contact(name, email, notes);
        }
        //#endif

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
    //to confirm a pressed key after a given period defined in keyTimer.schedule()
    private class KeyConfirmer extends TimerTask {

        public void run() {
            if (keyMajor != -1) {
                if (!shift) {
                    input.insert(textCur, KEYS[keyMajor].charAt(keyMinor));
                } else {
                    input.insert(textCur, KEYS[keyMajor].charAt(KEYS[keyMajor].length() - 1));
                }
                textCur++;
                keyMajor = -1;
                inputChanged = true;
            }
            repaint();
        }
    }
    //it refreshes the canvas in a given period defined in curTimer.schedule() to have the blinking textcursor efect
    private class CursorShow extends TimerTask {

        public void run() {
            textCurShow = !textCurShow;
            repaint();
        }
    }
    static final byte MODE_DEFAULT = 0;
    static final byte MODE_EDIT = 1; //when editing a contact
    static final byte MODE_SENDMAIL_BROWSE = 2; //when selecting a contact to be paste in SendMail
    byte mode = MODE_DEFAULT;
    MujMail mujMail;
    Form cntForm, viewForm; //a form to create a contact; to view contact's info	
    Command mark, done, back, delete, edit, add, delAll, view, sendMail;
    Command cfBack, cfSave; //for contact editing form
    Command vBack; //for contact viewing form
    Command flipRcps; //to flip recipients when Insert Recipients from sendMail
    private Vector addresses; //a container to store contacts
    Hashtable nameHash; //store first letters of some names for faster finding of a contact's name
    boolean busy, btnHidden;
    Hashtable marked; //to map contacts marked as recipients 
    private Hashtable emailHash; //to map contacts's emails that are in the addressbook, preventing multiple entries having the same email
    int cur;
    Displayable nextDisplay; //a displayable that should be displayed(MailForm, SendMail) after some action (saving a contact, adding recipients)			
    Image img_search = Functions.getIcon("search.png");
    //input key reading stuff
    final String[] KEYS = {" 0", "._,'?!\"*1", "abc2", "def3", "ghi4", "jkl5", "mno6", "pqrs7", "tuv8", "wxyz9"};
    Timer keyTimer;
    byte keyMajor = -1;
    byte keyMinor;
    boolean inputChanged, shift;
    StringBuffer input;
    //text cursors stuff
    Timer curTimer;
    byte textCur;
    boolean textCurShow = true;
    int recipientChoice = 0; 
    String recipientChoiceStr = "To:";
    private final PointerEventListener pointerEventListener = new PointerEventListener();
    //#ifdef MUJMAIL_TOUCH_SCR
    private final MujMailPointerEventProducer pointerEventProducer;
    //#endif

    public AddressBook(MujMail mujMail) {
        this.mujMail = mujMail;
        addresses = new Vector();
        nextDisplay = this;
        marked = new Hashtable();
        emailHash = new Hashtable();
        nameHash = new Hashtable();
        input = new StringBuffer();

        cfSave = new Command(Lang.get(Lang.BTN_SAVE), Command.OK, 1);
        cfBack = new Command(Lang.get(Lang.BTN_BACK), Command.BACK, 2);
        vBack = new Command(Lang.get(Lang.BTN_BACK), Command.BACK, 1);
        mark = new Command(Lang.get(Lang.BTN_AD_MARK), Command.OK, 2);
        done = new Command(Lang.get(Lang.BTN_AD_DONE), Command.ITEM, 1);
        back = new Command(Lang.get(Lang.BTN_BACK), Command.BACK, 3);
        sendMail = new Command(Lang.get(Lang.BTN_AD_SEND_MAIL), Command.OK, 2);
        add = new Command(Lang.get(Lang.BTN_AD_ADD_NEW), Command.ITEM, 3);
        edit = new Command(Lang.get(Lang.BTN_EDIT), Command.ITEM, 4);
        delete = new Command(Lang.get(Lang.BTN_DELETE), Command.ITEM, 5);
        view = new Command(Lang.get(Lang.BTN_AD_VIEW), Command.ITEM, 6);
        delAll = new Command(Lang.get(Lang.BTN_CLEAR), Command.ITEM, 7);
        flipRcps = new Command(Lang.get(Lang.BTN_AD_FLIPRCP), Command.ITEM,8);

        addCommand(sendMail);
        addCommand(view);
        addCommand(back);
        addCommand(edit);
        addCommand(add);
        addCommand(delete);
        addCommand(delAll);
        setCommandListener(mujMail);
        Thread t = new Thread(this);
        t.start();
        t.setPriority(Thread.MAX_PRIORITY);
        //#ifdef MUJMAIL_TOUCH_SCR
        pointerEventProducer = new MujMailPointerEventProducer(pointerEventListener, getWidth(), getHeight());
        //#endif
        
    }

    public void run() {
        busy = true;
        try {
            RecordStore ADRS = RecordStore.openRecordStore("AddressBook", true);

            try {
                  if (DEBUG) System.out.println("DEBUG AddressBook.run() - loading AddressBook");
                if (ADRS.getNumRecords() > 0) {
                    int id, sizeOfRecord;
                    byte[] data = new byte[50];
                    DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(data));
                    RecordEnumeration enumeration = ADRS.enumerateRecords(null, null, false);
                    getAddresses().ensureCapacity(enumeration.numRecords());
                    while (enumeration.hasNextElement()) {
                        try {

                            id = enumeration.nextRecordId();
                            sizeOfRecord = ADRS.getRecordSize(id);
                            if (sizeOfRecord > data.length) {
                                data = new byte[sizeOfRecord + 20];
                                inputStream = new DataInputStream(new ByteArrayInputStream(data));
                            }
                            ADRS.getRecord(id, data, 0);
                            inputStream.reset();

                            Contact contact = new Contact( inputStream.readUTF(), inputStream.readUTF(), inputStream.readUTF() );
                            contact.DBIndex = id;

                            getAddresses().addElement(contact);
                            getEmailHash().put(contact.getEmail(), contact);
                        } catch (Exception exp) {
                        //try another one
                        }
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    data = null;
                }
                  if (DEBUG) System.out.println("DEBUG AddressBook.run() - loading AddressBook done");
            } catch (Exception ex) {
                mujMail.alert.setAlert(this, this, Lang.get(Lang.ALRT_AD_LOAD) + Lang.get(Lang.FAILED), MyAlert.DEFAULT, AlertType.ERROR);
            } catch (Error er) {
                mujMail.alert.setAlert(this, this, Lang.get(Lang.ALRT_AD_LOAD) + Lang.get(Lang.FAILED), MyAlert.DEFAULT, AlertType.ERROR);
            }

            ADRS.closeRecordStore();
        } catch (Exception ex) {
            mujMail.alert.setAlert(this, this, Lang.get(Lang.ALRT_AD_LOAD) + Lang.get(Lang.FAILED), MyAlert.DEFAULT, AlertType.ERROR);
        }
        Functions.sort(getAddresses(), Functions.SRT_ORDER_INC, Functions.SRT_CNT_NAME);
        initHash();
        busy = false;
    }

    public int getSelectedIndex() {
        return cur;
    }

    public void setSelectedIndex(int i) {
        if (!addresses.isEmpty()) {
            cur = (cur + i + getAddresses().size()) % getAddresses().size();
        }
        repaint();
    }

    protected synchronized void keyPressed(int keyCode) {
        switch (getGameAction(keyCode)) {
            case UP:
                if (keyCode != KEY_NUM2) {
                    pointerEventListener.up();
                    return;
                }
                break;
            case DOWN:
                if (keyCode != KEY_NUM8) {
                    pointerEventListener.down();
                    return;
                }
                break;
            case RIGHT:
                if (keyCode != KEY_NUM6) {
                    pointerEventListener.right();
                    return;
                }
                break;
            case LEFT:
                if (keyCode != KEY_NUM4) {
                    pointerEventListener.left();
                    return;
                }
                break;
            case FIRE:
                if (keyCode != KEY_NUM5) {
                    pointerEventListener.fire();
                    return;
                }
                break;
        }

        if (keyCode == '#') {
            pointerEventListener.slash();
        } else if (keyCode == '*' && input.length() >= textCur && textCur != 0) {
            pointerEventListener.star();
        }

        byte index = (byte) (keyCode - KEY_NUM0);
        if (keyTimer != null && keyMajor == index) //its repeated key
        {
            keyTimer.cancel();
        } //cancel confirming input

        if (index < 0 || index > KEYS.length) {
            keyMajor = -1;
        } else {
            if (index != keyMajor) { //again, test if its a repeated key
                keyMinor = 0; //if no take the key code
                keyMajor = index;
            } else { //if yes, change keyMinor
                keyMinor = (byte) ((keyMinor + 1) % KEYS[keyMajor].length());
            }
            keyTimer = new Timer(); //created new Timer and/or confirm the previously task(can not be canceled anymore)
            keyTimer.schedule(new KeyConfirmer(), 500);// and schedule new task
        }
        repaint();
    }

    //#ifdef MUJMAIL_TOUCH_SCR
    protected void pointerPressed(int arg0, int arg1) {
        super.pointerPressed(arg0, arg1);
        pointerEventProducer.pointerPressed(arg0, arg1);
    }
    //#endif
    
    

    private void cancelTimers() {
        if (curTimer != null) {
            curTimer.cancel();
            curTimer = null;
        }
        if (keyTimer != null) {
            keyTimer.cancel();
        }
    }

    private void hideButtons() {
        if (!btnHidden) {
            removeCommand(view);
            removeCommand(sendMail);
            removeCommand(edit);
            removeCommand(delete);
            if (mode == MODE_SENDMAIL_BROWSE) {
                removeCommand(add);
                removeCommand(delAll);
            }
            btnHidden = true;
        }
    }

    private void showButtons() {
        if (btnHidden) {
            addCommand(view);
            addCommand(sendMail);
            addCommand(edit);
            addCommand(delete);
            addCommand(delAll);
            btnHidden = false;
        }
    }

    protected void hideNotify() {
        cancelTimers();
    }

    public void paint(Graphics g) {
        if (busy) {
            mujMail.alert.setAlert(this,mujMail.getMenu(), Lang.get(Lang.ALRT_AD_LOAD) + Lang.get(Lang.ALRT_WAIT), MyAlert.DEFAULT, AlertType.INFO);
            return;
        }

        if (getAddresses().isEmpty() || mode == MODE_SENDMAIL_BROWSE) {
            hideButtons();
        } else {
            showButtons();
        }

        if (inputChanged) {
            int index = search(input.toString());
            if (index != -1) {
                cur = index;
            } else {
                cur = 0;
            }
            inputChanged = false;
        }

        short sh = (short) getHeight(), sw = (short) getWidth();
        short size = (short) getAddresses().size();
        int y = 0;
        short fh = (short) g.getFont().getHeight();
        int item = 0;

        g.setColor(0x00ffffff); //background color
        g.fillRect(0, 0, sw, sh);

        if (Settings.fontSize == Settings.FONT_NORMAL)
        	g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM)); //set font size for box's headline
        else
        	g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE)); //set font size for box's headline
        
        int maxItemsPerPage = (sh - g.getFont().getHeight() * 2 - 10) / (2 * fh + 2); //headline+searchbar takes some space						
        if (cur >= maxItemsPerPage) {
            int currentPage = cur / maxItemsPerPage;
            item = currentPage * maxItemsPerPage;
        }

        //headline
        g.setColor(184, 179, 255); //headline background color
        y += g.getFont().getHeight() + 3;
        g.fillRect(0, 0, sw, y);
        String boxName = mode == MODE_SENDMAIL_BROWSE ? Lang.get(Lang.AD_ADDDING_RCPS) : Lang.get(Lang.AD_ADDRESSBOOK);
        if (size != 0) {
            boxName = boxName + " (" + (cur + 1) + "/" + size + ") - " + recipientChoiceStr;
        }
        g.setColor(0x00000000);	//text color	
        g.drawString(boxName, sw / 2 - g.getFont().stringWidth(boxName) / 2, 1, Graphics.TOP | Graphics.LEFT);

        //search bar
        g.setColor(217, 236, 255); //search tab background color 						
        g.fillRect(0, y, sw, y + 4);
        g.setColor(0x00000000);
        
        if (Settings.fontSize == Settings.FONT_NORMAL)
        	g.setFont(Font.getDefaultFont());
        else
        	g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE));
        
        g.drawImage(img_search, 4, y + 4, Graphics.TOP | Graphics.LEFT);
        if (keyMajor != -1) { //draw the pressed key				
            char c = shift ? KEYS[keyMajor].charAt(KEYS[keyMajor].length() - 1) : KEYS[keyMajor].charAt(keyMinor);
            g.setColor(0x0033cc00);
            g.drawChar(c, sw - 17, y + 4, Graphics.TOP | Graphics.LEFT);
        }
        g.setColor(0x00ffffff); //input text background color
        byte img_search_width = (byte) img_search.getWidth();
        g.fillRect(6 + img_search_width, y + 3, sw - 34 - img_search_width, y - 2);
        g.setColor(0x00000000);
        
        if (Settings.fontSize == Settings.FONT_NORMAL)
        	g.setFont(Font.getDefaultFont());
        else
        	g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE));
        
        if (!textCurShow) //show the blinking cursor |
        {
            g.drawString(input.toString(), 8 + img_search_width, y + 4, Graphics.TOP | Graphics.LEFT);
        } else {
            StringBuffer inputString = new StringBuffer(input.toString());
            inputString.insert(textCur, '|');
            g.drawString(inputString.toString(), 8 + img_search_width, y + 4, Graphics.TOP | Graphics.LEFT);
        }

        y = y * 2 + 4;
        while (y < sh && item < size) {
            Contact contact = (Contact) getAddresses().elementAt(item);
            if (item == cur) {
                g.setColor(121, 111, 255);
                g.fillRect(0, y, sw, 2 * fh + 1);
                g.setColor(255, 255, 255);
            } else {
                g.setColor(0x00000000);
            }
            String cn = contact.name;
            if (marked.containsKey(contact.getEmail())) //if the contact was marked 				
            {
                cn = "[x] " + contact.name;
            }
            if (g.getFont().stringWidth(cn) > sw - 2) {
                cn = Functions.cutString(cn, 0, sw - 2 - g.getFont().stringWidth(".."), g).trim() + "..";
            }
            g.drawString(cn, 2, y, Graphics.TOP | Graphics.LEFT);
            y += fh;
            String ce = contact.getEmail();
            if (g.getFont().stringWidth(contact.getEmail()) > sw - 2) {
                ce = Functions.cutString(contact.getEmail(), 0, sw - 2 - g.getFont().stringWidth(".."), g).trim() + "..";
            }
            g.drawString(ce, 2, y, Graphics.TOP | Graphics.LEFT);
            y += fh;
            g.setColor(228, 228, 228);
            y++;
            g.drawLine(0, y, sw, y);
            y++;

            ++item;
        }

        //recursively call repaint() after 500sec to have blinking textcursor	
        if (curTimer == null) {
            curTimer = new Timer();
            curTimer.schedule(new CursorShow(), 0, 500);
        }
    }
    //when user exits from the addressbook, defaults values
    public void back() {
        showButtons();
        if (mode == MODE_SENDMAIL_BROWSE) {
            removeCommand(mark);
            removeCommand(done);
            removeCommand(flipRcps);
            addCommand(add);
            addCommand(delAll);
            marked.clear();
        }
        if (nextDisplay != this) {
            mujMail.getDisplay().setCurrent(nextDisplay);
        } else {
            mujMail.mainMenu();
        }
        mode = MODE_DEFAULT;
        nextDisplay = this;
        input.delete(0, input.length());
        keyMajor = -1;
        textCur = 0;
        cur = 0;
        cntForm = null;
        viewForm = null;
    }


    //deletes a selected contact
    public void delete(int index, boolean sure) {
        if (0 <= index && index < getAddresses().size()) {
            Contact contact = (Contact) getAddresses().elementAt(index);
            ;
            if (!sure) {
                mujMail.alert.setAlert(this, this, Lang.get(Lang.ALRT_SYS_DEL_CONFIRM) + contact.getEmail() + "?", MyAlert.DEL_CONFIRM, AlertType.CONFIRMATION);
                return;
            }
            try {
                delFromDB(contact);
            } catch (MyException ex) {
                mujMail.alert.setAlert(this, this, ex.getDetails(), MyAlert.DEFAULT, AlertType.ERROR);
                return;
            }
            cur = 0;
            getEmailHash().remove(contact.getEmail());
            getAddresses().removeElementAt(index);
            initHash();
            repaint();
        }
    }
    //deletes a selected contact from DB only		
    public void delFromDB(Contact contact) throws MyException {
        MyException exception = null;
        RecordStore ADRS = Functions.openRecordStore("AddressBook", true);
        try {
            ADRS.deleteRecord(contact.DBIndex);
        } catch (Exception ex) {
            exception = new MyException(MyException.DB_CANNOT_DEL_CONTACT);
        }
        Functions.closeRecordStore(ADRS);
        if (exception != null) {
            throw exception;
        }
    }

    public void deleteAll(boolean sure) {
        if (!sure) {
            mujMail.alert.setAlert(this, this, Lang.get(Lang.ALRT_SYS_DEL_ALL_CONFIRM), MyAlert.DB_CLEAR_CONFIRM, AlertType.CONFIRMATION);
            return;
        }
        try {
            RecordStore.deleteRecordStore("AddressBook");
        } catch (Exception ex) {
            mujMail.alert.setAlert(this, this, Lang.get(Lang.ALRT_AD_DELETE) + Lang.get(Lang.FAILED), MyAlert.DEFAULT, AlertType.ERROR);
            return;
        }
        getAddresses().removeAllElements();
        getEmailHash().clear();
        nameHash.clear();
        repaint();
    }

    /**
     * Saves a contact from the form; returns a saved contact or null
     * (when something's gone wrong);
     */
    public void saveContactForm() {
        if (cntForm != null) {
            Contact contact = new Contact(
                    ((TextField) cntForm.get(0)).getString().trim(),
                    ((TextField) cntForm.get(1)).getString().trim(),
                    ((TextField) cntForm.get(2)).getString().trim());
            try {
                saveContact(contact);
                mujMail.getDisplay().setCurrent(nextDisplay);
            } catch (MyException ex) {
                mujMail.alert.setAlert(this, cntForm, ex.getDetailsNocode(), MyAlert.DEFAULT, AlertType.INFO);
            }
        }
    }

    /**
     * Saves to DB and containers, hashes
     * @param contact the contact to be saved
     * @throws MyException
     */
    public void saveContact(Contact contact) throws MyException {
        if (contact == null) {
            throw new MyException(MyException.DB_CANNOT_SAVE_CONTACT);
        }

        contact = saveContactToDB(contact);
        if (mode == MODE_EDIT) {
            Contact cnt = (Contact) getAddresses().elementAt(cur);
            getAddresses().removeElementAt(cur); //remove the old one
            getEmailHash().remove(cnt.getEmail());
            mode = MODE_DEFAULT;
        }
        //refresh hashes and container
        getAddresses().addElement(contact);
        getEmailHash().put(contact.getEmail(), contact);
        Functions.sort(getAddresses(), Functions.SRT_ORDER_INC, Functions.SRT_CNT_NAME);
        initHash();
    }

    //saves a given contact do DB.
    public Contact saveContactToDB(Contact contact) throws MyException {
        //for the sake of simplicity we just try to determine if there's any charakter before the @
        if (contact.getEmail() == null || contact.getEmail().indexOf("@") < 1) {
            throw new MyException(MyException.VARIOUS_BAD_EMAIL);
        }
        if ((getEmailHash().containsKey(contact.getEmail())) && mode != MODE_EDIT) {
            throw new MyException(MyException.VARIOUS_AB_MULTIPLE_ENTRIES);
        }
        MyException exception = null;
        RecordStore ADRS = Functions.openRecordStore("AddressBook", true);
        try {
            ByteArrayOutputStream byteStream;
            DataOutputStream outputStream;
            byteStream = new ByteArrayOutputStream();
            outputStream = new DataOutputStream(byteStream);
            if (mode == MODE_EDIT) {
                contact.DBIndex = ((Contact) getAddresses().elementAt(cur)).DBIndex;
            }
            //if the name is not presented or invalid, try to guest it from the email address
            if (contact.name == null || contact.name.length() == 0) {
                contact.name = contact.getEmail().substring(0, contact.getEmail().indexOf("@"));
            }

            outputStream.writeUTF(contact.name);
            outputStream.writeUTF(contact.getEmail());
            outputStream.writeUTF(contact.notes);
            outputStream.flush();
            /*buggy setRecord() method not only on the WTK emulator
            if (mode != MODE_EDIT) 
            contact.DBIndex = ADRS.addRecord( byteStream.toByteArray(), 0, byteStream.size() );			
            else 
            ADRS.setRecord(contact.DBIndex, byteStream.toByteArray(), 0, byteStream.size() );
             */
            int oldIndex = contact.DBIndex;
            contact.DBIndex = ADRS.addRecord(byteStream.toByteArray(), 0, byteStream.size());
            if (mode == MODE_EDIT) {
                ADRS.deleteRecord(oldIndex);
            }

            outputStream.close();
            byteStream.close();
        } catch (Exception ex) {
            exception = new MyException(MyException.DB_CANNOT_SAVE_CONTACT);
        }

        Functions.closeRecordStore(ADRS);
        if (exception != null) {
            throw exception;
        }
        return contact;
    }

    //edits a selected contact
    public void edit(int index) {
        if (0 <= index && index < getAddresses().size()) {
            Contact contact = (Contact) getAddresses().elementAt(index);
            mode = MODE_EDIT;
            cntForm = createCntForm();
            ((TextField) cntForm.get(0)).setString(contact.name);
            ((TextField) cntForm.get(1)).setString(contact.getEmail());
            ((TextField) cntForm.get(2)).setString(contact.notes);
            mujMail.getDisplay().setCurrent(cntForm);
        }
    }

    //creates a Contact form so we can add or edit a contact
    private Form createCntForm() {
        TextField name, email, notes;
        cntForm = new Form(Lang.get(Lang.AD_CONTACT_INFO));
        name = new TextField(Lang.get(Lang.AD_NAME), "", 50, TextField.ANY);
        if ( Properties.textFieldMailIncorrect ) {
            email = new TextField(Lang.get(Lang.AD_EMAIL), "@", 512, TextField.ANY);
        } else { 
            email = new TextField(Lang.get(Lang.AD_EMAIL), "@", 512, TextField.EMAILADDR);
         }
        notes = new TextField(Lang.get(Lang.AD_NOTES), "", 1000, TextField.ANY);

        cntForm.append(name);
        cntForm.append(email);
        cntForm.append(notes);
        cntForm.addCommand(cfBack);
        cntForm.addCommand(cfSave);
        cntForm.setCommandListener(mujMail);
        return cntForm;
    }

    //if its called from mailForm and a mail address is given, then the string MailFormAddress is the email address
    //so after saving the email address, we can return display focus to MailForm; otherwise string MailFormAddress is null
    public void showCntForm(String MailFormAddress) {
        cntForm = createCntForm();
        nextDisplay = mujMail.getDisplay().getCurrent();
        mujMail.getDisplay().setCurrent(cntForm);
        if (MailFormAddress != null) { //was is call by MailForm?
            ((TextField) cntForm.get(0)).setString(MailFormAddress);
        }
    }

    //creates a Hashtable for faster searching using the first letter of each name as index 
    //something like the indexes in the real life phonebook
    private void initHash() {
        nameHash.clear();
        if (getAddresses().isEmpty()) {
            return;
        }

        char firstLetter = ((Contact) getAddresses().firstElement()).name.charAt(0);
        firstLetter = Character.toLowerCase(firstLetter);
        nameHash.put(new Character(firstLetter), new Integer(0));
        int size = getAddresses().size();
        char cntNameFL; //first letter of a contact's name

        for (int i = 1; i < size; i++) {
            cntNameFL = ((Contact) getAddresses().elementAt(i)).name.charAt(0);
            cntNameFL = Character.toLowerCase(cntNameFL);

            if (firstLetter != cntNameFL) {
                firstLetter = cntNameFL;
                nameHash.put(new Character(firstLetter), new Integer(i));
            }
        }
    }

    //returns index of the most matched contact
    public int search(String name) {
        if (name == null || name.length() == 0) {
            return -1;
        }
        name.toLowerCase();
        Integer i = (Integer) nameHash.get(new Character(name.charAt(0))); //get the closest index
        if (i == null) //if its first letter was never indexed
        {
            return -1;
        }

        int size = getAddresses().size(), index = i.intValue();
        String contactName = null;
        //lets find its correct position (index)
        while (index < size) {
            contactName = ((Contact) getAddresses().elementAt(index)).name.toLowerCase();
            if (contactName.charAt(0) != name.charAt(0)) //has different first letter
            {
                return -1;
            } //not found
            if (contactName.compareTo(name) < 0) //name should be after the contactName
            {
                index++;
            } else {
                break;
            } //found or name lies before contactName
        }
        if (contactName.startsWith(name)) //now check if it really matches
        {
            return index;
        } else {
            return -1;
        }
    }
    
    //views a contact info
    public void view(int index) {
        if (0 <= index && index < getAddresses().size()) {
            viewForm = new Form(Lang.get(Lang.AD_CONTACT_INFO));
            Contact contact = (Contact) getAddresses().elementAt(index);
            viewForm.append(new StringItem(Lang.get(Lang.AD_NAME), contact.name));
            viewForm.append(new StringItem(Lang.get(Lang.AD_EMAIL),contact.getEmail()));
            viewForm.append(new StringItem(Lang.get(Lang.AD_NOTES), contact.notes));
            viewForm.addCommand(vBack);
            viewForm.setCommandListener(mujMail);

            mujMail.getDisplay().setCurrent(viewForm);
        }
    }
    //sends a selected contact a mail
    public void sendMail(int index) {
        if (0 <= index && index < getAddresses().size()) {
            Contact contact = (Contact) getAddresses().elementAt(index);
            mujMail.sendMail.to.setString("\"" + contact.name + "\" <" + contact.getEmail() + ">");

            mujMail.getDisplay().setCurrent(mujMail.sendMail);
        }
    }

    //is called when a Form wants to add emails from the addressbook
    public void addEmails(Form form) {
        nextDisplay = form;
        mode = MODE_SENDMAIL_BROWSE;
        addCommand(mark);
        addCommand(done);
        addCommand(flipRcps);
        mujMail.getDisplay().setCurrent(this);
    }

    //is called when the user confirms adding emails and wants to paste all those emails addresses to a form;
    public void pasteEmails() {
        StringBuffer emailBf = new StringBuffer();
        Contact contact;
        //probably old - and doesn't work
        TextField tfield = mujMail.sendMail.getSelectedItem() == null? mujMail.sendMail.to: mujMail.sendMail.getSelectedItem();

        //newer
        switch(recipientChoice) {
            case 0:
                tfield = mujMail.sendMail.to;
                break;
            case 1:
                tfield = mujMail.sendMail.cc;
                break;
            case 2:
                tfield = mujMail.sendMail.bcc;
                break;
        }
        for (Enumeration e = marked.elements(); e.hasMoreElements(); ) {
            contact = (Contact) e.nextElement();
            if (tfield.getString().indexOf(contact.getEmail()) == -1) {
                emailBf.append("\"" + contact.name + "\" <" + contact.getEmail() + ">, ");
            }
        }
        
        // 
        if ((tfield == mujMail.sendMail.cc || tfield == mujMail.sendMail.bcc) && emailBf.length() > 0 )
            mujMail.sendMail.addBc();

        if (tfield != mujMail.sendMail.body) {
            tfield.setString(emailBf.toString() + tfield.getString());
        } else {
            //tfield.setString(tfield.getString() + emailBf.toString());
            tfield.insert(emailBf.toString(), tfield.getCaretPosition());
        }
        back();
    }

    //is called when we want to flip recipients if inserting
    public void flipRecipients() {
        recipientChoice++;
        if (recipientChoice > 2) 
            recipientChoice = 0;
        switch(recipientChoice) {
            case 0:
                recipientChoiceStr = "To:";
                break;
            case 1:
                recipientChoiceStr = "Cc:";
                break;
            case 2:
                recipientChoiceStr = "Bcc:";
                break;
        }
            
        //System.out.println("flipRecipients " + recipientChoice);
    }
        
    //is called when the user un/mark a contact in the addressbook
    public void markEmail(int index) {
        if (index < getAddresses().size()) {
            Contact contact = (Contact) getAddresses().elementAt(index);
            if (!marked.containsKey(contact.email)) {
                marked.put(contact.getEmail(), contact);
            } else {
                marked.remove(contact.getEmail());
            }
            repaint();
        }
    }

    public String getContactName(String email) {
        if (email == null) {
            return null;
        }
        if (!emailHash.containsKey(email)) {
            return null;
        }
        return ((Contact) getEmailHash().get(email)).name;
    }
    
    //#ifdef MUJMAIL_TOUCH_SCR
    private class PointerEventListener extends MujMailPointerEventListener.MujMailPointerEventListenerAdapter {
    //#else
//#     private class PointerEventListener  {
    //#endif
         
         public void left() {
            textCur = (byte) ((textCur - 1 + input.length() + 1) % (input.length() + 1));
            cancelTimers();
            repaint();
         }

        public void right() {
            textCur = (byte) ((textCur + 1) % (input.length() + 1));
            cancelTimers();
            repaint();
        }

        public void up() {
            setSelectedIndex(-1);
            repaint();
        }

        public void down() {
            setSelectedIndex(1);
            repaint();
        }

        public void fire() {
            if (mode != MODE_SENDMAIL_BROWSE) {
                sendMail(cur);
            } else {
                markEmail(cur);
            }
            repaint();
        }

        public void slash() {
            shift = !shift;
        }

        public void star() {
            input.deleteCharAt(textCur - 1);
            textCur--;
            inputChanged = true;
        }
        
    }
}
