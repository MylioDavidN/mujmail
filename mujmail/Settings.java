package mujmail;

/*
MujMail - Simple mail client for J2ME
Copyright (C) 2006 Nguyen Son Tung <n.sontung@gmail.com>
Copyright (C) 2006 Martin Stefan <martin.stefan@centrum.cz>
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.TextField;
import javax.microedition.rms.RecordStore;

import mujmail.ordering.Criterion;
import mujmail.ordering.Ordering;
//#ifdef MUJMAIL_SSL
import mujmail.ui.SSLTypeChooser;
//#endif
public class Settings implements ItemStateListener {

    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false;

    //determine actual settings format, change it always we modify settings format.
    //otherwise loading a different format will cause a crash!
    public final long SETTINGSVERSION = 20090319;

    private ChoiceGroup retrievingSettingsChG;
    private ChoiceGroup otherSettingsChG;
    private ChoiceGroup storingMailsSettingsChG;
    private ChoiceGroup viewMailsSmallFontChG;
    
    public static final int FONT_NORMAL = 0;
    public static final int FONT_LARGE = 1;
    
    public static final int CONST_PORT_SMTP = 25;
    public static final int CONST_PORT_SMTPS = 465;

    //ChoiceGroup optionsCG;
    ChoiceGroup smtpSSLCG;
    ChoiceGroup pollCG;
    ChoiceGroup fontSizeCG;
    //#ifdef MUJMAIL_SSL    
    SSLTypeChooser sslTypeChooser;
    //#endif
    TextField mujMailSrvAddrTF;
    TextField mujMailSrvPortTF;
    TextField mujMailSrvLoginTF;
    TextField mujMailSrvPasswdTF;
    
    TextField smtpAuthNameTF;
    TextField smtpAuthPassTF;
    TextField smtpServerTF;
    TextField smtpPortTF;
    //TextField minInBoxDBSpaceTF;
    TextField maxMailsTF;
    TextField maxLinesTF;
    TextField maxSizeOfBodyPartTF;
    TextField pollInvlTF;
    TextField timeoutTF;
    TextField signatureTF;
    TextField passwordTF;

    Command back, ok;

      // TODO (betlista): next comment is correct, but it breaks good OOP design
    //all attributes are static for easier access from other classes, without
    //having a reference (mujMail.settings.)	
    
    public static String mujMailSrvAddr;
    public static String mujMailSrvPort;
    public static String mujMailSrvLogin;
    public static String mujMailSrvPasswd = "";

    public static int fontSize;
    public static final String notSetPE = "not_set.see@Accounts.menu";
    public static String primaryEmail;
    public static volatile String smtpServer;
    public static boolean smtpSSL;
    public static byte smtpSSLType;
    public static short smtpPort;
    // TODO: delete all default values
    public static String smtpAuthName;
    public static String smtpAuthPass = "";
    public static boolean downWholeMail;
    public static boolean downOnlyNeverSeen; //download only mails that were never seen in all seasons
    public static boolean delMailFromServer;
    public static boolean delOnExit;
    public static boolean addToAddressbook;
    public static boolean smallFontMailForm;
    public static boolean replaceTabs;//if mailForm should display ' ' instead of '\t'
    public static boolean moveToTrash; //use trash or not	
    public static boolean safeMode; //recycles only one record store to store mail bodies
    public static String password = ""; /** Password that must be entered before mujMail starts. If empty, no password must be entered. */
    //public static short minInBoxDBSpace = -1; //auto clear inbox if out of DB space
    public static short maxMailsRetrieve; //how many mails should be downloaded once per an account
    public static short maxLinesRetrieve; //how many lines of a mail to retrieve
    /** The maximum size of bodypart. -1 means unlimited size. */
    private static long maxSizeOfBodypart;
    public static boolean pollDownloadsMails;
    public static boolean pollPlaysSound;
    public static int pollInvl; //polling interval in sec	
    public static int timeout; //communication timeout	
    public static String signature;
    public static final int noopPeriod = 1000 * 30; //a period in which protocols sends "noop" or its equivalent to keep connection alive
    public static final int noopIMAPPeriod = 1000 * 60 * 5; //noop period of IMAP 29minutes
    //represents sort modes of the boxes. 
    //the most right 4bits are for inBox sortMode, other 4bits are for draft, other 4 are for outBox, other 4 for sentBox
    //see more in the TheBox.sortMode comments
    public static int theBoxSortModes;
    //if debug is set to true most of alerts and information exchanged with server are printed to SYSTEM.OUT
    public static final boolean debug = false;

    /** Defines whether or not e-mail have to be ordered in threads. */
    public static boolean threading;
    /* * Defines if the empty root messages for threads have to be shown. */
    //public static boolean rootsVisible;
    /** Automatically delete mails when headers database is full. */
    public static boolean deleteMailsWhenHeaderDBIsFull;
    /** Automatically delete body of mails when body database is full. */
    public static boolean deleteMailsBodyWhenBodyDBIsFull;

    MujMail mujMail;
    SortForm sortForm;

    /**
     * Gets maximum size of bodypart.
     * @return maximum size of bodypart.
     */
    public static long getMaxSizeOfBodypart() {
        if (maxSizeOfBodypart == -1) return Long.MAX_VALUE;

        return maxSizeOfBodypart;
    }

    /**
     * Loads default values of static variables.
     */
    private void loadDefaultValues() {
        mujMailSrvAddr   = "server-dev.mujmail.org";
        mujMailSrvPort   = "143";
        mujMailSrvLogin  = "@server-dev.mujmail.org";
        mujMailSrvPasswd = "";

        fontSize = FONT_NORMAL;
        primaryEmail = notSetPE;
        smtpServer = "smtp_of_your_mobile_operator.com";
        smtpSSL = false;
        smtpSSLType = 0;
        smtpPort = 25;
        smtpAuthName = "";
        smtpAuthPass = "****";
        downWholeMail = false;
        downOnlyNeverSeen = true; //download only mails that were never seen in all seasons
        delMailFromServer = true;
        delOnExit = false;
        addToAddressbook = true;
        smallFontMailForm = true;
        replaceTabs = true;//if mailForm should display ' ' instead of '\t'
        moveToTrash = false; //use trash or not
        safeMode = false; //recycles only one record store to store mail bodies
        password = ""; /** Password that must be entered before mujMail starts. If empty, no password must be entered. */
        //public static short minInBoxDBSpace = -1; //auto clear inbox if out of DB space
        maxMailsRetrieve = 150; //how many mails should be downloaded once per an account
        maxLinesRetrieve = -1; //how many lines of a mail to retrieve
        /** The maximum size of bodypart. -1 means unlimited size. */
        maxSizeOfBodypart = 100000;
        pollDownloadsMails = false;
        pollPlaysSound = true;
        pollInvl = 60 * 5; //polling interval in sec
        timeout = 10 * 1000; //communication timeout
        signature = "";

        //represents sort modes of the boxes.
        //the most right 4bits are for inBox sortMode, other 4bits are for draft, other 4 are for outBox, other 4 for sentBox
        //see more in the TheBox.sortMode comments
        theBoxSortModes = 0;

        threading = false;
        deleteMailsWhenHeaderDBIsFull = false;
        deleteMailsBodyWhenBodyDBIsFull = false;
    }

    /**
     * Restores settings to default values.
     */
    public void restoreSettings() {
        loadDefaultValues();
        saveSettings(false);
        updateValuesToForms();
    }

    //#ifdef MUJMAIL_SYNC
    public String toString() {
    	StringBuffer sb = new StringBuffer();
    	sb.append("SettingsVersion: ").append(SETTINGSVERSION).append('\n');
    	sb.append("mujMailSrvAddr: ").append(mujMailSrvAddr).append('\n');
    	sb.append("mujMailSrvPort: ").append(mujMailSrvPort).append('\n');
    	sb.append("mujMailSrvLogin: ").append(mujMailSrvLogin).append('\n');
    	sb.append("primaryEmail: ").append(primaryEmail).append('\n');
    	sb.append("smtpServer: ").append(smtpServer).append('\n');
    	sb.append("smtpSSL: ").append((smtpSSL ? "1" : "0")).append('\n');
        //#ifdef MUJMAIL_SSL
        sb.append("smtpSSLType: ").append(smtpSSLType).append('\n');
        //#endif
    	sb.append("smtpPort: ").append(smtpPort).append('\n');
    	sb.append("smtpAuthName: ").append(smtpAuthName).append('\n');
    	sb.append("fontSize: ").append(fontSize).append('\n');
    	sb.append("downWholeMail: ").append((downWholeMail ? "1" : "0")).append('\n');
    	sb.append("downOnlyNeverSeen: ").append((downOnlyNeverSeen ? "1" : "0")).append('\n');
    	sb.append("delMailFromServer: ").append((delMailFromServer ? "1" : "0")).append('\n');
    	sb.append("delOnExit: ").append((delOnExit ? "1" : "0")).append('\n');
    	sb.append("addToAddressbook: ").append((addToAddressbook ? "1" : "0")).append('\n');
    	sb.append("smallFontMailForm: ").append((smallFontMailForm ? "1" : "0")).append('\n');
    	sb.append("replaceTabs: ").append((replaceTabs ? "1" : "0")).append('\n');
    	sb.append("moveToTrash: ").append((moveToTrash ? "1" : "0")).append('\n');
    	sb.append("safeMode: ").append((safeMode ? "1" : "0")).append('\n');
        sb.append("deleteMailsWhenHeaderDBIsFull: ").append((deleteMailsWhenHeaderDBIsFull ? "1" : "0")).append('\n');
        sb.append("deleteMailsBodyWhenBodyDBIsFull: ").append((deleteMailsBodyWhenBodyDBIsFull ? "1" : "0")).append('\n');
    	//sb.append("minInBoxDBSpace: ").append(minInBoxDBSpace).append('\n');
    	sb.append("maxMailsRetrieve: ").append(maxMailsRetrieve).append('\n');
    	sb.append("maxLinesRetrieve: ").append(maxLinesRetrieve).append('\n');
        sb.append("maxSizeOfBodypart: ").append(maxSizeOfBodypart).append('\n');
    	sb.append("pollPlaysSound: ").append((pollPlaysSound ? "1" : "0")).append('\n');
    	sb.append("pollDownloadsMails: ").append((pollDownloadsMails ? "1" : "0")).append('\n');
    	sb.append("pollInvl: ").append(pollInvl).append('\n');
    	sb.append("timeout: ").append(timeout).append('\n');
    	sb.append("theBoxSortModes: ").append(theBoxSortModes).append('\n');
    	sb.append("signature: ").append(signature).append("\n\n");		//NOTE: last line has to contain double newline
    	return sb.toString();
    }
    //#endif
    
    //#ifdef MUJMAIL_SYNC
    public void parseAndSetup(String setupStr)
    {
    	//skip SETTINGSVERSION for now
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	mujMailSrvAddr = setupStr.substring(setupStr.indexOf("mujMailSrvAddr: ") + 16, setupStr.indexOf('\n'));
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	mujMailSrvPort = setupStr.substring(setupStr.indexOf("mujMailSrvPort: ") + 16, setupStr.indexOf('\n'));
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	mujMailSrvLogin = setupStr.substring(setupStr.indexOf("mujMailSrvLogin: ") + 17, setupStr.indexOf('\n'));
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	primaryEmail = setupStr.substring(setupStr.indexOf("primaryEmail: ") + 14, setupStr.indexOf('\n'));
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	smtpServer = setupStr.substring(setupStr.indexOf("smtpServer: ") + 12, setupStr.indexOf('\n'));
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	smtpSSL = "1".equals(setupStr.substring(setupStr.indexOf("smtpSSL: ") + 9, setupStr.indexOf('\n')));
        //#ifdef MUJMAIL_SSL
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
        smtpSSLType = Integer.valueOf(setupStr.substring(setupStr.indexOf("smtpSSLType: ") + 13, setupStr.indexOf('\n'))).byteValue();
        //#endif
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	smtpPort = Integer.valueOf(setupStr.substring(setupStr.indexOf("smtpPort: ") + 10, setupStr.indexOf('\n'))).shortValue();
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	smtpAuthName = setupStr.substring(setupStr.indexOf("smtpAuthName: ") + 14, setupStr.indexOf('\n'));
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	fontSize = Integer.parseInt(setupStr.substring(setupStr.indexOf("fontSize: ") + 10, setupStr.indexOf('\n')));
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	downWholeMail = "1".equals(setupStr.substring(setupStr.indexOf("downWholeMail: ") + 15, setupStr.indexOf('\n')));
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	downOnlyNeverSeen = "1".equals(setupStr.substring(setupStr.indexOf("downOnlyNeverSeen: ") + 19, setupStr.indexOf('\n')));    	
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	delMailFromServer = "1".equals(setupStr.substring(setupStr.indexOf("delMailFromServer: ") + 19, setupStr.indexOf('\n')));    	
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	delOnExit = "1".equals(setupStr.substring(setupStr.indexOf("delOnExit: ") + 11, setupStr.indexOf('\n')));    	
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	addToAddressbook = "1".equals(setupStr.substring(setupStr.indexOf("addToAddressbook: ") + 18, setupStr.indexOf('\n')));    	
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	smallFontMailForm = "1".equals(setupStr.substring(setupStr.indexOf("smallFontMailForm: ") + 19, setupStr.indexOf('\n')));    	
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	replaceTabs = "1".equals(setupStr.substring(setupStr.indexOf("replaceTabs: ") + 13, setupStr.indexOf('\n')));    	
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	moveToTrash = "1".equals(setupStr.substring(setupStr.indexOf("moveToTrash: ") + 13, setupStr.indexOf('\n')));    	
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	safeMode = "1".equals(setupStr.substring(setupStr.indexOf("safeMode: ") + 10, setupStr.indexOf('\n')));    	
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
        deleteMailsWhenHeaderDBIsFull = "1".equals(setupStr.substring(setupStr.indexOf("deleteMailsWhenHeaderDBIsFull: ") + 31, setupStr.indexOf('\n')));
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
        deleteMailsBodyWhenBodyDBIsFull = "1".equals(setupStr.substring(setupStr.indexOf("deleteMailsBodyWhenBodyDBIsFull: ") + 33, setupStr.indexOf('\n')));
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	//minInBoxDBSpace = Integer.valueOf(setupStr.substring(setupStr.indexOf("minInBoxDBSpace: ") + 17, setupStr.indexOf('\n'))).shortValue();
    	//setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	maxMailsRetrieve = Integer.valueOf(setupStr.substring(setupStr.indexOf("maxMailsRetrieve: ") + 18, setupStr.indexOf('\n'))).shortValue();
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	maxLinesRetrieve = Integer.valueOf(setupStr.substring(setupStr.indexOf("maxLinesRetrieve: ") + 18, setupStr.indexOf('\n'))).shortValue();    	
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
        maxSizeOfBodypart = Long.parseLong(setupStr.substring(setupStr.indexOf("maxSizeOfBodypart: ") + 19, setupStr.indexOf('\n')));
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	pollPlaysSound = "1".equals(setupStr.substring(setupStr.indexOf("pollPlaysSound: ") + 16, setupStr.indexOf('\n')));    	
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	pollDownloadsMails = "1".equals(setupStr.substring(setupStr.indexOf("pollDownloadsMails: ") + 20, setupStr.indexOf('\n')));    	
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	pollInvl = Integer.valueOf(setupStr.substring(setupStr.indexOf("pollInvl: ") + 10, setupStr.indexOf('\n'))).intValue();    	
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	timeout = Integer.valueOf(setupStr.substring(setupStr.indexOf("timeout: ") + 9, setupStr.indexOf('\n'))).intValue();    	
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	theBoxSortModes = Integer.valueOf(setupStr.substring(setupStr.indexOf("theBoxSortModes: ") + 17, setupStr.indexOf('\n'))).intValue();
    	setupStr = setupStr.substring(setupStr.indexOf('\n') + 1);
    	signature = setupStr.substring(setupStr.indexOf("signature: ") + 11, setupStr.indexOf('\n'));

    	  if (DEBUG) System.out.println("DEBUG Settings.parseAndSetup(String) - SETTINGS="+toString());
    	saveSettings(false);
    	mujMail.getMenu().refresh(Menu.SETTINGS, false);
    	loadSettings();
    }
    //#endif
    
    class SortForm extends Form implements ItemStateListener {

    	/** Index for the "threading enabled" option */
    	private static final byte THREADING_ENABLED_CHACKBOX = 0;

        ChoiceGroup sortByCG;
        ChoiceGroup sortOrderCG;
        TheBox box;
        ChoiceGroup threadingGroup;

        public SortForm() {
            super(Lang.get(Lang.ST_SORT_SETTINGS));

            sortByCG = new ChoiceGroup(Lang.get(Lang.ST_SORT_BY), Choice.EXCLUSIVE);
            sortByCG.append(Lang.get(Lang.ST_SORT_TIME), null);
            sortByCG.append(Lang.get(Lang.ST_SORT_SUBJECT), null);
            sortByCG.append(Lang.get(Lang.ST_SORT_RECIPIENTS), null);
            sortByCG.append(Lang.get(Lang.ST_SORT_SENDER), null);
            sortByCG.append(Lang.get(Lang.ST_SORT_SIZE), null);

            sortOrderCG = new ChoiceGroup(Lang.get(Lang.ST_SORT_ORDER), Choice.EXCLUSIVE);
            sortOrderCG.append("older first", null); // have to represent unnatural ordering
            sortOrderCG.append("newer first", null); // have to represent natural ordering

            threadingGroup = new ChoiceGroup( Lang.get( Lang.ST_THREADING), Choice.MULTIPLE );
              // this is why the value of THREADING_ENABLED_CHACKBOX is 0
            threadingGroup.append( Lang.get( Lang.ST_THREADING_ENABLE), null );
            threadingGroup.setSelectedFlags( new boolean[] { Settings.threading } );

            append(sortByCG);
            append(sortOrderCG);
            append( threadingGroup );
            addCommand(back);
            addCommand(ok);

            setCommandListener(mujMail);
            setItemStateListener( this );
        }

        public void setOrderingLabels( int selectedCriterion ) {
            switch ( selectedCriterion ) {
                case 0: // time
                    sortOrderCG.set(0, "older first", null);
                    sortOrderCG.set(1, "newer first", null);
                    break;
                case 1: // subject
                case 2: // recipients
                case 3: // sender
                    sortOrderCG.set(0, "alphabetically", null);
                    sortOrderCG.set(1, "reverse order", null);
                    break;
                case 4: // size
                    sortOrderCG.set(0, "smaller first", null);
                    sortOrderCG.set(1, "bigger first", null);
                    break;
            }
        }

        public void updateValuesToForm() {
            threadingGroup.setSelectedFlags( new boolean[] { Settings.threading } );
        }

        /* ***********************
         *    listener method    *
         *************************/
        public void itemStateChanged(Item src) {
            if ( src == sortByCG ) {
        	    int selected = sortByCG.getSelectedIndex();
        	    setOrderingLabels( selected );
        	}
        }
    }

    public Settings(MujMail mujMail) {
    	loadDefaultValues();
        //super(Lang.get(Lang.ST_SETTINGS));
        this.mujMail = mujMail;

        back = new Command(Lang.get(Lang.BTN_BACK), Command.BACK, 0);
        ok = new Command(Lang.get(Lang.BTN_OK), Command.OK, 1);
        sortForm = new SortForm();

        smtpSettingsForm = new SMTPSettingsForm();
        otherSettingsForm = new OtherSettingsForm();
        pollingSettingsForm = new PollingSettingsForm();
        appearanceSettingsForm = new AppearanceSettingsForm();
        retrievingSettingsForm = new RetrievingSettingsForm();
        storingSettingsForm = new StoringSettingsForm();
        mujMailSettingsForm = new MujMailServerSettingsForm();

        loadSettings();
        //setTitle(Lang.get(Lang.ST_SETTINGS));
    }
    
    //sets sortMode for all boxes
    public void initSortMode() {
          if (DEBUG) System.out.println("DEBUG Settings.initSortMode() - sort modes: " + Integer.toHexString( theBoxSortModes ) );
        //theBoxSortModes;
        int tmp = theBoxSortModes;
        mujMail.getSentBox().setOrdering( Ordering.valueOf( tmp & 0x1 ) );
        mujMail.getSentBox().setCriterion( Criterion.valueOf( (tmp & 0xE) >> 1 ) );
        //#ifdef MUJMAIL_SEARCH
        //mujMail.getSearchBox().setCriterion( Criterion.valueOf( tmp & 0xE ) );
        //#endif
        tmp = tmp >> 4;
        mujMail.outBox.setOrdering( Ordering.valueOf( tmp & 0x1 ) );
        mujMail.outBox.setCriterion( Criterion.valueOf( (tmp & 0xE) >> 1 ) );
        tmp = tmp >> 4;
        mujMail.draft.setOrdering( Ordering.valueOf( tmp & 0x1 ) );
        mujMail.draft.setCriterion( Criterion.valueOf( (tmp & 0xE) >> 1 ) );
        tmp = tmp >> 4;
        mujMail.getInBox().setOrdering( Ordering.valueOf( tmp & 0x1 ) );
        mujMail.getInBox().setCriterion( Criterion.valueOf( (tmp & 0xE) >> 1 ) );
        tmp = tmp >> 4;
        mujMail.getTrash().setOrdering( Ordering.valueOf( tmp & 0x1 ) );
        mujMail.getTrash().setCriterion( Criterion.valueOf( (tmp & 0xE) >> 1 ) );
    }

    public void saveSortSettings(final TheBox box) {
        //get selected modes and recalculate sortMode
        int sortBy = sortForm.sortByCG.getSelectedIndex();
        box.setCriterion( Criterion.valueOf( sortBy ) );
        int sortOrder = sortForm.sortOrderCG.getSelectedIndex();
        box.setOrdering( Ordering.valueOf( sortOrder ) );
        TheBox tmpBox = mujMail.getTrash();
        theBoxSortModes = (tmpBox.getCriterion().ordinal << 1) + tmpBox.getOrdering().ordinal;
        tmpBox = mujMail.getInBox();
        theBoxSortModes = (theBoxSortModes << 4) + (tmpBox.getCriterion().ordinal << 1) + tmpBox.getOrdering().ordinal;
        tmpBox = mujMail.draft;
        theBoxSortModes = (theBoxSortModes << 4) + (tmpBox.getCriterion().ordinal << 1) + tmpBox.getOrdering().ordinal;
        tmpBox = mujMail.outBox;
        theBoxSortModes = (theBoxSortModes << 4) + (tmpBox.getCriterion().ordinal << 1) + tmpBox.getOrdering().ordinal;
        tmpBox = mujMail.getSentBox();
        theBoxSortModes = (theBoxSortModes << 4) + (tmpBox.getCriterion().ordinal << 1) + tmpBox.getOrdering().ordinal;

        saveThreadingSettings();

        saveSettings(true);
    }

    /**
     * Saves settings for threading - whether the threading is enabled and when
     * it's enabled whether the empty root messages have to be shows.
     *  
     * @param box
     */
    private void saveThreadingSettings() {
    	  // assumes that THREADING_SHOW_ROOTS is the last check box index 
    	boolean[] selected = new boolean[SortForm.THREADING_ENABLED_CHACKBOX + 1];
    	sortForm.threadingGroup.getSelectedFlags( selected );

    	threading = selected[SortForm.THREADING_ENABLED_CHACKBOX];
    }

    public void showSortFrm(TheBox box) {
        sortForm.box = box;
        sortForm.sortOrderCG.setSelectedIndex(box.getOrdering().ordinal, true);
        sortForm.sortByCG.setSelectedIndex(box.getCriterion().ordinal, true);
        sortForm.setOrderingLabels( box.getCriterion().ordinal );
        mujMail.getDisplay().setCurrent(sortForm);
    }

    /**
     * Updates values of settings variables to forms that displays settings.
     */
    public void updateValuesToForms() {
        appearanceSettingsForm.updateValuesToForm();
        mujMailSettingsForm.updateValuesToForm();
        smtpSettingsForm.updateValuesToForm();
        retrievingSettingsForm.updateValuesToForm();
        otherSettingsForm.updateValuesToForm();
        storingSettingsForm.updateValuesToForm();
        pollingSettingsForm.updateValuesToForm();
        sortForm.updateValuesToForm();
    }

    public void saveSettings(boolean init) {
          if (DEBUG) System.out.println("DEBUG Settings.saveSettings(boolean " + init + ")");
        // initialize variables
    	if (init) {
	        try {
	        	appearanceSettingsForm.updateValuesFromForm();
                mujMailSettingsForm.updateValuesFromForm();
                smtpSettingsForm.updateValuesFromForm();
                retrievingSettingsForm.updateValuesFromForm();
                otherSettingsForm.updateValuesFromForm();
                storingSettingsForm.updateValuesFromForm();
                pollingSettingsForm.updateValuesFromForm();
	        } catch (Exception ex) { //bad input may occur, empty port fields..
	            ex.printStackTrace();
	            updateValuesToForms(); //set defaults
	            mujMail.alert.setAlert(this,mujMail.getMenu(), Lang.get(Lang.ALRT_ST_SAVING) + Lang.get(Lang.FAILED), MyAlert.DEFAULT, AlertType.ERROR);
	            //return;
	        } 
    	}
          System.out.println("deleteMailsWhenHeaderDBIsFull: " + deleteMailsWhenHeaderDBIsFull);

        // save to RecordStore
        RecordStore rs;
        try {
            rs = RecordStore.openRecordStore("SETTINGS", true);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(buffer);

            stream.writeLong(SETTINGSVERSION);
            
            stream.writeUTF(mujMailSrvAddr);
            stream.writeUTF(mujMailSrvPort);
            stream.writeUTF(mujMailSrvLogin);
            stream.writeUTF(mujMailSrvPasswd);
            
            stream.writeUTF(primaryEmail);
            stream.writeUTF(smtpServer);
            stream.writeBoolean(smtpSSL);
            //#ifdef MUJMAIL_SSL
            stream.writeByte(smtpSSLType);
            //#endif
            stream.writeShort(smtpPort);
            stream.writeUTF(smtpAuthName);
            stream.writeUTF(smtpAuthPass);
            stream.writeInt(fontSize);
            stream.writeBoolean(downWholeMail);
            stream.writeBoolean(downOnlyNeverSeen);
            stream.writeBoolean(delMailFromServer);
            stream.writeBoolean(delOnExit);
            stream.writeBoolean(addToAddressbook);
            stream.writeBoolean(smallFontMailForm);
            stream.writeBoolean(replaceTabs);
            stream.writeBoolean(moveToTrash);
            stream.writeBoolean(safeMode);
            stream.writeBoolean(deleteMailsWhenHeaderDBIsFull);
            stream.writeBoolean(deleteMailsBodyWhenBodyDBIsFull);
            //stream.writeShort(minInBoxDBSpace);
            stream.writeShort(maxMailsRetrieve);
            stream.writeShort(maxLinesRetrieve);
            stream.writeLong(maxSizeOfBodypart);
            stream.writeBoolean(pollPlaysSound);
            stream.writeBoolean(pollDownloadsMails);
            stream.writeInt(pollInvl);
            stream.writeInt(timeout);
            stream.writeInt(theBoxSortModes);
            stream.writeUTF(signature);
            stream.writeUTF(password);

              // save the threading settings
            stream.writeBoolean( threading );

            stream.flush();
            if (rs.getNumRecords() == 1) {
                rs.setRecord(1, buffer.toByteArray(), 0, buffer.size());
            } else {
                rs.addRecord(buffer.toByteArray(), 0, buffer.size());
            }
            rs.closeRecordStore();
            if (mujMail.getDisplay().getCurrent() != mujMail.mailForm) {
                mujMail.mainMenu();
            }
        } catch (Exception e) {
            e.printStackTrace();
            mujMail.alert.setAlert(this,mujMail.getMenu(), Lang.get(Lang.ALRT_ST_SAVING) + Lang.get(Lang.FAILED), MyAlert.DEFAULT, AlertType.ERROR);
            return;
        }
    }

    /**
     * This is called only at the beginning in the mujMail.startApp() method.
     */
    public void loadSettings() {
          if (DEBUG) System.out.println("DEBUG Setting.loadSettings()");
        // load from the RecordStore
        RecordStore rs;
        try {
            rs = RecordStore.openRecordStore("SETTINGS", true);
            // settings has already been saved
            if (rs.getNumRecords() == 1) {
                  if (DEBUG) System.out.println("DEBUG Settings.loadSettings() - loading settings");
                ByteArrayInputStream buffer = new ByteArrayInputStream(rs.getRecord(1));
                DataInputStream stream = new DataInputStream(buffer);

                long sv = stream.readLong();
                if (sv != this.SETTINGSVERSION) { //previous settings format is different
                    rs.closeRecordStore();
                    updateValuesToForms();
                    saveSettings(true); //create new format;
                    return; //otherwise it may cause a crash if we read different format
                }

                mujMailSrvAddr   = stream.readUTF();
                mujMailSrvPort   = stream.readUTF();
                mujMailSrvLogin  = stream.readUTF();
                mujMailSrvPasswd = stream.readUTF();
                
                primaryEmail = stream.readUTF();
                smtpServer = stream.readUTF();
                smtpSSL = stream.readBoolean();
                //#ifdef MUJMAIL_SSL
                smtpSSLType = stream.readByte();
                //#endif
                smtpPort = stream.readShort();
                smtpAuthName = stream.readUTF();
                smtpAuthPass = stream.readUTF();
                fontSize = stream.readInt();
                downWholeMail = stream.readBoolean();
                downOnlyNeverSeen = stream.readBoolean();
                delMailFromServer = stream.readBoolean();
                delOnExit = stream.readBoolean();
                addToAddressbook = stream.readBoolean();
                smallFontMailForm = stream.readBoolean();
                replaceTabs = stream.readBoolean();
                moveToTrash = stream.readBoolean();
                safeMode = stream.readBoolean();
                deleteMailsWhenHeaderDBIsFull = stream.readBoolean();
                deleteMailsBodyWhenBodyDBIsFull = stream.readBoolean();
                //minInBoxDBSpace = stream.readShort();
                maxMailsRetrieve = stream.readShort();
                maxLinesRetrieve = stream.readShort();
                maxSizeOfBodypart = stream.readLong();
                pollPlaysSound = stream.readBoolean();
                pollDownloadsMails = stream.readBoolean();
                pollInvl = stream.readInt();
                timeout = stream.readInt();
                theBoxSortModes = stream.readInt();
                signature = stream.readUTF();
                password = stream.readUTF();

                threading = stream.readBoolean();

                rs.closeRecordStore();
                  if (DEBUG) System.out.println("DEBUG Settings.loadSettings() - loading settings..successful");
            }
        } catch (Exception e) {
            e.printStackTrace();
            mujMail.alert.setAlert(this,mujMail.getMenu(), Lang.get(Lang.ALRT_ST_LOADING) + Lang.get(Lang.FAILED), MyAlert.DEFAULT, AlertType.ERROR);
        }
        updateValuesToForms();
    }

    public void itemStateChanged(Item item) {
        if (item == smtpSSLCG) {
            int smtpPortOld = 0;
            try { 
                smtpPortOld = Short.parseShort(smtpPortTF.getString());
            } catch (Exception e) {}
            if (smtpSSLCG.isSelected(0)) {
                //#ifdef MUJMAIL_SSL
                sslTypeChooser.insertToForm();
                //#endif
                if ( smtpPortOld == CONST_PORT_SMTP ) smtpPortTF.setString( new Integer(CONST_PORT_SMTPS).toString());
            } else {
                //#ifdef MUJMAIL_SSL
                sslTypeChooser.deleteFromForm();
                //#endif
                if ( smtpPortOld == CONST_PORT_SMTPS ) smtpPortTF.setString( new Integer(CONST_PORT_SMTP).toString());
            }
        }

    }

    private static SMTPSettingsForm smtpSettingsForm;
    public void showSMTPSettingsForm() {
        mujMail.getDisplay().setCurrent(smtpSettingsForm);
    }

    private static AppearanceSettingsForm appearanceSettingsForm;
    public void showAppearanceSettingsForm() {
        mujMail.getDisplay().setCurrent(appearanceSettingsForm);
    }

    private static RetrievingSettingsForm retrievingSettingsForm;
    public void showRetrievingSettingsForm() {
        mujMail.getDisplay().setCurrent(retrievingSettingsForm);
    }

    private static MujMailServerSettingsForm mujMailSettingsForm;
    public void showMujMailServerSettingsForm() {
        mujMail.getDisplay().setCurrent(mujMailSettingsForm);
    }

    private static StoringSettingsForm storingSettingsForm;
    public void showStoringMailsSettingsForm() {
        mujMail.getDisplay().setCurrent(storingSettingsForm);
    }

    private static PollingSettingsForm pollingSettingsForm;
    public void showPollingSettingsForm() {
        mujMail.getDisplay().setCurrent(pollingSettingsForm);
    }

    private static OtherSettingsForm otherSettingsForm;
    public void showOtherSettingsForm() {
        mujMail.getDisplay().setCurrent(otherSettingsForm);
    }

    /**
     * Represents the form that displays settins variables.
     */
    private abstract class SettingsForm extends Form {
        public SettingsForm(String title) {
            super(title);
            addCommand(back);
            addCommand(ok);
            setCommandListener(mujMail);
            setItemStateListener(Settings.this);
        }

        /**
         * Displays values of settings variables in the form.
         */
        protected abstract void updateValuesToForm();
        /**
         * Update values of settings variable according to values in form.
         */
        protected abstract void updateValuesFromForm() throws Exception;

    }

    private class SMTPSettingsForm extends SettingsForm {

        public SMTPSettingsForm() {
            super("Sending mails settings");

            smtpSSLCG = new ChoiceGroup("SMTP SSL", Choice.MULTIPLE);
            smtpSSLCG.append(Lang.get(Lang.AS_SSL), null);
        }

        protected void updateValuesFromForm() throws NumberFormatException {
            smtpServer = smtpServerTF.getString();
            smtpSSL = smtpSSLCG.isSelected(0);
            //#ifdef MUJMAIL_SSL
            smtpSSLType = (byte) sslTypeChooser.getSSLTypeNumberChosen();
            //#endif
            smtpPort = Short.parseShort(smtpPortTF.getString());
            smtpAuthName = smtpAuthNameTF.getString();
            smtpAuthPass = smtpAuthPassTF.getString();
        }

        protected void updateValuesToForm() {
            smtpServerTF = new TextField("SMTP " + Lang.get(Lang.AS_SERVER), smtpServer, 1000, TextField.URL);

            smtpPortTF = new TextField("SMTP " + Lang.get(Lang.AS_PORT), String.valueOf(smtpPort), 1000, TextField.NUMERIC);
            smtpAuthNameTF = new TextField("SMTP " + Lang.get(Lang.AS_USR_NAME), smtpAuthName, 1000, TextField.ANY);
            smtpAuthPassTF = new TextField("SMTP " + Lang.get(Lang.AS_PASS), smtpAuthPass, 1000, TextField.PASSWORD);

            smtpSSLCG.setSelectedIndex(0, smtpSSL);

            //#ifdef MUJMAIL_SSL
            sslTypeChooser = new SSLTypeChooser(smtpSettingsForm, 2);
            //#endif
            smtpSSLCG.setSelectedIndex(0, smtpSSL);
            //#ifdef MUJMAIL_SSL
            if (smtpSSL) {
                sslTypeChooser.setSelectedType(smtpSSLType);
            }
            //#endif


            deleteAll();

            append(smtpServerTF);
            append(smtpSSLCG);
            //#ifdef MUJMAIL_SSL
            if (smtpSSL) {
                sslTypeChooser.appendToForm();
            }
            //#endif
            append(smtpPortTF);
            append(smtpAuthNameTF);
            append(smtpAuthPassTF);
        }
    }

    private class AppearanceSettingsForm extends SettingsForm {

        public AppearanceSettingsForm() {
            super("Appearance settings");

            fontSizeCG = new ChoiceGroup(Lang.get(Lang.ST_FONT_SIZE), Choice.EXCLUSIVE);
            fontSizeCG.append(Lang.get(Lang.ST_FONT_NORMAL), null);
            fontSizeCG.append(Lang.get(Lang.ST_FONT_LARGE), null);

            viewMailsSmallFontChG = new ChoiceGroup(Lang.get(Lang.ST_FONT_SIZE), Choice.MULTIPLE);
            viewMailsSmallFontChG.append(Lang.get(Lang.ST_ML_SMALL_FONT), null);

            append(fontSizeCG);
            append(viewMailsSmallFontChG);
        }

        protected void updateValuesToForm() {
            fontSizeCG.setSelectedIndex(fontSize, true);

            viewMailsSmallFontChG.setSelectedIndex(0, smallFontMailForm);
        }

        protected void updateValuesFromForm() {
            fontSize = fontSizeCG.getSelectedIndex();
            smallFontMailForm = viewMailsSmallFontChG.isSelected(0);
        }
    }

    private class MujMailServerSettingsForm extends SettingsForm {
        MujMailServerSettingsForm() {
            super("mujMail server settings");
        }

        protected void updateValuesToForm() {
            mujMailSrvAddrTF = new TextField(Lang.get(Lang.ST_MUJMAIL_SRV_ADDRESS), mujMailSrvAddr, 1000, TextField.ANY);
            mujMailSrvPortTF = new TextField(Lang.get(Lang.ST_MUJMAIL_SRV_PORT), mujMailSrvPort, 1000, TextField.NUMERIC);
            mujMailSrvLoginTF = new TextField(Lang.get(Lang.ST_MUJMAIL_SRV_LOGIN), mujMailSrvLogin, 1000, TextField.ANY);
            mujMailSrvPasswdTF = new TextField(Lang.get(Lang.ST_MUJMAIL_SRV_PSWD), mujMailSrvPasswd, 1000, TextField.PASSWORD);

            deleteAll();

            append(mujMailSrvAddrTF);
            append(mujMailSrvPortTF);
            append(mujMailSrvLoginTF);
            append(mujMailSrvPasswdTF);
        }

        protected void updateValuesFromForm() {
            mujMailSrvAddr = mujMailSrvAddrTF.getString();
            mujMailSrvPort = mujMailSrvPortTF.getString();
            mujMailSrvLogin = mujMailSrvLoginTF.getString();
            mujMailSrvPasswd = mujMailSrvPasswdTF.getString();
        }
    }

    private class RetrievingSettingsForm extends SettingsForm {

        public RetrievingSettingsForm() {
            super("Retrieving mails settings");

            retrievingSettingsChG = new ChoiceGroup("", Choice.MULTIPLE);
            retrievingSettingsChG.append(Lang.get(Lang.ST_DOWN_WHOLE_MAIL), null);
            retrievingSettingsChG.append(Lang.get(Lang.ST_DOWNLOAD_ONLY_NEVER_SEEN), null);
        }

        protected void updateValuesFromForm() {
            downWholeMail = retrievingSettingsChG.isSelected(0);
            downOnlyNeverSeen = retrievingSettingsChG.isSelected(1);

            maxMailsRetrieve = Short.parseShort(maxMailsTF.getString());
            maxLinesRetrieve = Short.parseShort(maxLinesTF.getString());
            maxSizeOfBodypart = Long.parseLong(maxSizeOfBodyPartTF.getString());
        }

        protected void updateValuesToForm() {
            retrievingSettingsChG.setSelectedIndex(0, downWholeMail);
            retrievingSettingsChG.setSelectedIndex(1, downOnlyNeverSeen);

            maxMailsTF = new TextField(Lang.get(Lang.ST_RETR_MAX_MAILS), String.valueOf(maxMailsRetrieve), 1000, TextField.NUMERIC);

            maxLinesTF = new TextField(Lang.get(Lang.ST_RETR_MAX_LINES), String.valueOf(maxLinesRetrieve), 1000, TextField.NUMERIC);
            maxSizeOfBodyPartTF = new TextField("Maximum size of bodypart in characters (-1=unlimited)", String.valueOf(maxSizeOfBodypart), 1000, TextField.NUMERIC);

            deleteAll();

            append(retrievingSettingsChG);
            append(maxMailsTF);
            append(maxLinesTF);
            append(maxSizeOfBodyPartTF);
        }
    }
    
    private class OtherSettingsForm extends SettingsForm {

        public OtherSettingsForm() {
            super("Other settings");

            otherSettingsChG = new ChoiceGroup("", Choice.MULTIPLE);
            otherSettingsChG.append(Lang.get(Lang.ST_DEL_FROM_SERVER), null);
            otherSettingsChG.append(Lang.get(Lang.ST_DEL_ON_EXIT), null);
            otherSettingsChG.append(Lang.get(Lang.ST_ADD_TO_ADBOOK), null);
            otherSettingsChG.append(Lang.get(Lang.ST_REPLACE_TABS), null);
            otherSettingsChG.append(Lang.get(Lang.ST_MOVE_TO_TRASH), null);
        }


        protected void updateValuesFromForm() {
            delMailFromServer = otherSettingsChG.isSelected(0);
            delOnExit = otherSettingsChG.isSelected(1);
            addToAddressbook = otherSettingsChG.isSelected(2);
            replaceTabs = otherSettingsChG.isSelected(3);
            moveToTrash = otherSettingsChG.isSelected(4);

            timeout = Integer.parseInt(timeoutTF.getString());
            signature = signatureTF.getString();
            password = passwordTF.getString();

            //minInBoxDBSpace = Short.parseShort(minInBoxDBSpaceTF.getString());
        }

        protected void updateValuesToForm() {
            otherSettingsChG.setSelectedIndex(0, delMailFromServer);
            otherSettingsChG.setSelectedIndex(1, delOnExit);
            otherSettingsChG.setSelectedIndex(2, addToAddressbook);
            otherSettingsChG.setSelectedIndex(3, replaceTabs);
            otherSettingsChG.setSelectedIndex(4, moveToTrash);

            //minInBoxDBSpaceTF = new TextField(Lang.get(Lang.ST_AUTO_CLEAR_INBOX), String.valueOf(minInBoxDBSpace), 1000, TextField.NUMERIC);

            timeoutTF = new TextField(Lang.get(Lang.ST_TIMEOUT), String.valueOf(timeout), 1000, TextField.NUMERIC);

            signatureTF = new TextField(Lang.get(Lang.ST_SIGN), signature, 1000, TextField.ANY);
            passwordTF = new TextField("Password for the application", password, 1000, TextField.PASSWORD);

            deleteAll();

            append(otherSettingsChG);
            //append(minInBoxDBSpaceTF);
            append(timeoutTF);
            append(signatureTF);
            append(passwordTF);
        }
    }
    
    private class StoringSettingsForm extends SettingsForm {

        public StoringSettingsForm() {
            super("Storing mails settings");

            storingMailsSettingsChG = new ChoiceGroup("", Choice.MULTIPLE);
            storingMailsSettingsChG.append(Lang.get(Lang.ST_SAFE_MODE), null);
            storingMailsSettingsChG.append("Delete mails when cannot save header.", null);
            storingMailsSettingsChG.append("Delete bodyparts when cannot save bodypart.", null);

            append(storingMailsSettingsChG);
        }

        protected void updateValuesFromForm() {
            safeMode = storingMailsSettingsChG.isSelected(0);
            deleteMailsWhenHeaderDBIsFull = storingMailsSettingsChG.isSelected(1);
            deleteMailsBodyWhenBodyDBIsFull = storingMailsSettingsChG.isSelected(2);
        }

        protected void updateValuesToForm() {
            storingMailsSettingsChG.setSelectedIndex(0, safeMode);
            storingMailsSettingsChG.setSelectedIndex(1, deleteMailsWhenHeaderDBIsFull);
            storingMailsSettingsChG.setSelectedIndex(2, deleteMailsBodyWhenBodyDBIsFull);
        }
    }
    
    private class PollingSettingsForm extends SettingsForm {

        public PollingSettingsForm() {
            super("Polling settings");

            pollCG = new ChoiceGroup(Lang.get(Lang.ST_POLLING_BEHAVIOUR), Choice.MULTIPLE);
            pollCG.append(Lang.get(Lang.ST_POLLING_PLAY_SOUND), null);
            pollCG.append(Lang.get(Lang.ST_POLLING_DOWN), null);
        }

        protected void updateValuesToForm() {
            pollInvlTF = new TextField(Lang.get(Lang.ST_POLLING_INTERVAL), String.valueOf(pollInvl), 1000, TextField.NUMERIC);

            pollCG.setSelectedIndex(0, pollPlaysSound);
            pollCG.setSelectedIndex(1, pollDownloadsMails);

            deleteAll();

            append(pollInvlTF);
            append(pollCG);
        }

        protected void updateValuesFromForm() throws NumberFormatException {
            pollPlaysSound = pollCG.isSelected(0);
            pollDownloadsMails = pollCG.isSelected(1);
            pollInvl = Integer.parseInt(pollInvlTF.getString());
        }
    }


    
}
