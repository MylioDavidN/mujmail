package mujmail;

/*
MujMail - Simple mail client for J2ME
Copyright (C) 2005 Pavel Machek <pavel@ucw.cz>
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

//this class is used to define native language for labels, texts, buttons and alerts
//use is as Lang.get(Lang.SOMECONST); to get a string in the appropriate language
/**
 * Converts lingual codes used elsewhere in program into associated text.
 * Change string constants in this class to translate mujmail into new language.
 * <p>
 * To obtain string call {@link #get} method. 
 */
public class Lang {

    /******************* Lingual constants for labels and texts ************************/
    //basics constants for whole mujmail
    public static final short MUJMAIL_BASE = 100;
    public static final short EMPTY = MUJMAIL_BASE + 1;
    public static final short FAILED = MUJMAIL_BASE + 2;
    public static final short SUCCESS = MUJMAIL_BASE + 3;
    public static final short IN = MUJMAIL_BASE + 4;
    public static final short ACTIVE = MUJMAIL_BASE + 5;
    public static final short INACTIVE = MUJMAIL_BASE + 6;
    public static final short MSGIDS_CACHE = MUJMAIL_BASE + 7;
    public static final short DB_SPACE_LEFT = MUJMAIL_BASE + 8;
    //account settings constants	
    public static final short AS_ACCOUNTS = MUJMAIL_BASE + 100;
    public static final short AS_ACTIVATION = MUJMAIL_BASE + 101;
    public static final short AS_EMAIL = MUJMAIL_BASE + 102;
    public static final short AS_PROTOCOL = MUJMAIL_BASE + 103;
    public static final short AS_SERVER = MUJMAIL_BASE + 104;
    public static final short AS_PORT = MUJMAIL_BASE + 105;
    public static final short AS_USR_NAME = MUJMAIL_BASE + 106;
    public static final short AS_PASS = MUJMAIL_BASE + 107;
    public static final short AS_PRIMARY = MUJMAIL_BASE + 108;
    public static final short AS_SSL = MUJMAIL_BASE + 109;
    public static final short AS_IMAP_MAILBOXES = MUJMAIL_BASE + 110;
    public static final short AS_COPY_TO_SERVER = MUJMAIL_BASE + 111;
    public static final short AS_COPY_TO_SRV_TRASH = MUJMAIL_BASE + 112;
    public static final short AS_COPY_TO_SRV_SENT = MUJMAIL_BASE + 113;
    public static final short AS_COPY_TO_SRV_TRASH_MAILBOX = MUJMAIL_BASE + 114;
    public static final short AS_COPY_TO_SRV_SENT_MAILBOX = MUJMAIL_BASE + 115;
    public static final short AS_SSL_TYPE = MUJMAIL_BASE + 116;
    public static final short AS_MUJMAIL_SSL = MUJMAIL_BASE + 117;
    public static final short AS_FIRMWARE_SSL = MUJMAIL_BASE + 118;
    //Addressbook constants
    public static final short AD_ADDRESSBOOK = MUJMAIL_BASE + 200;
    public static final short AD_CONTACT_INFO = MUJMAIL_BASE + 201;
    public static final short AD_NAME = MUJMAIL_BASE + 202;
    public static final short AD_EMAIL = MUJMAIL_BASE + 203;
    public static final short AD_NOTES = MUJMAIL_BASE + 204;
    public static final short AD_ADDDING_RCPS = MUJMAIL_BASE + 205;
    //theBox, folders constants
    public static final short TB_FOLDERS = MUJMAIL_BASE + 300;
    public static final short TB_INBOX = MUJMAIL_BASE + 301;
    public static final short TB_OUTBOX = MUJMAIL_BASE + 302;
    public static final short TB_DRAFT = MUJMAIL_BASE + 303;
    public static final short TB_SENTBOX = MUJMAIL_BASE + 304;
    public static final short TB_TRASH = MUJMAIL_BASE + 305;
    public static final short TB_NO_SUBJECT = MUJMAIL_BASE + 306;
    public static final short TB_READ_MAILS = MUJMAIL_BASE + 307;
    public static final short TB_UNREAD_MAILS = MUJMAIL_BASE + 308;
    public static final short TB_ALL_MAILS = MUJMAIL_BASE + 309;
    
    public static final short TB_USERFOLDERS = MUJMAIL_BASE + 310;
    public static final short TB_FOLDER_NAME = MUJMAIL_BASE + 311;
    public static final short TB_RETRIVE_ACCOUNTS = MUJMAIL_BASE + 312;
    public static final short TB_IMAP_FOLDERS = MUJMAIL_BASE + 313;

    //settings constants
    public static final short ST_SETTINGS = MUJMAIL_BASE + 400;
    public static final short ST_SORT_SETTINGS = MUJMAIL_BASE + 401;
    public static final short ST_SORT_BY = MUJMAIL_BASE + 402;
    public static final short ST_SORT_TIME = MUJMAIL_BASE + 403;
    public static final short ST_SORT_SUBJECT = MUJMAIL_BASE + 404;
    public static final short ST_SORT_RECIPIENTS = MUJMAIL_BASE + 405;
    public static final short ST_SORT_SENDER = MUJMAIL_BASE + 406;
    public static final short ST_SORT_SIZE = MUJMAIL_BASE + 407;
    public static final short ST_SORT_ORDER = MUJMAIL_BASE + 408;
    public static final short ST_SORT_INCREASE = MUJMAIL_BASE + 409;
    public static final short ST_SORT_DECREASE = MUJMAIL_BASE + 410;
    public static final short ST_LANG = MUJMAIL_BASE + 411;
    public static final short ST_SMTP_AUTH = MUJMAIL_BASE + 414;
    public static final short ST_SMTP_AUTH_NEEDED = MUJMAIL_BASE + 415;
    public static final short ST_DOWN_WHOLE_MAIL = MUJMAIL_BASE + 418;
    public static final short ST_DEL_FROM_SERVER = MUJMAIL_BASE + 419;
    public static final short ST_ADD_TO_ADBOOK = MUJMAIL_BASE + 420;
    public static final short ST_NOT_SET = MUJMAIL_BASE + 421;
    public static final short ST_POLLING_INTERVAL = MUJMAIL_BASE + 422;
    public static final short ST_TIMEOUT = MUJMAIL_BASE + 423;
    public static final short ST_SIGN = MUJMAIL_BASE + 424;
    public static final short ST_OTHER = MUJMAIL_BASE + 425;
    public static final short ST_POLLING_BEHAVIOUR = MUJMAIL_BASE + 426;
    public static final short ST_POLLING_PLAY_SOUND = MUJMAIL_BASE + 427;
    public static final short ST_POLLING_DOWN = MUJMAIL_BASE + 428;
    public static final short ST_DEL_ON_EXIT = MUJMAIL_BASE + 429;
    public static final short ST_ML_SMALL_FONT = MUJMAIL_BASE + 430;
    public static final short ST_TICK_TEXT = MUJMAIL_BASE + 431;
    public static final short ST_SAFE_MODE = MUJMAIL_BASE + 432;
    public static final short ST_RETR_MAX_MAILS = MUJMAIL_BASE + 433;
    public static final short ST_SORT_RECORD_ID = MUJMAIL_BASE + 434;
    public static final short ST_REPLACE_TABS = MUJMAIL_BASE + 435;
    public static final short ST_MOVE_TO_TRASH = MUJMAIL_BASE + 436;
    public static final short ST_DOWNLOAD_ONLY_NEVER_SEEN = MUJMAIL_BASE + 438;
    public static final short ST_AUTO_CLEAR_INBOX = MUJMAIL_BASE + 439;
    public static final short ST_RETR_MAX_LINES = MUJMAIL_BASE + 440;
    public static final short ST_FONT_SIZE = MUJMAIL_BASE + 441;
    public static final short ST_FONT_NORMAL = MUJMAIL_BASE + 442;
    public static final short ST_FONT_LARGE = MUJMAIL_BASE + 443;
    public static final short ST_MUJMAIL_SRV_ADDRESS = MUJMAIL_BASE + 444;
    public static final short ST_MUJMAIL_SRV_PORT = MUJMAIL_BASE + 445;
    public static final short ST_MUJMAIL_SRV_LOGIN = MUJMAIL_BASE + 446;
    public static final short ST_MUJMAIL_SRV_PSWD = MUJMAIL_BASE + 447;
    public static final short ST_SMTP =  MUJMAIL_BASE + 448;
    public static final short ST_PRIMARY_MAIL = MUJMAIL_BASE + 449;

      /** Code for English text "Threading" in sort form */
    public static final short ST_THREADING = MUJMAIL_BASE + 450;
      /** Code for English text "enable" in sort form */
    public static final short ST_THREADING_ENABLE = MUJMAIL_BASE + 451;
      /** Code for English text "show roots" in sort form */
    public static final short ST_ROOTS_VISIBLE = MUJMAIL_BASE + 452;

    //mail concerning constants
    public static final short ML_TO = MUJMAIL_BASE + 500;
    public static final short ML_SUBJECT = MUJMAIL_BASE + 501;
    public static final short ML_BODY = MUJMAIL_BASE + 502;
    public static final short ML_FROM = MUJMAIL_BASE + 503;
    public static final short ML_ATTACHMENTS = MUJMAIL_BASE + 504;
    public static final short ML_SIZE = MUJMAIL_BASE + 505;
    public static final short ML_TIME = MUJMAIL_BASE + 506;
    public static final short ML_HEADERS_DETAIL = MUJMAIL_BASE + 507;
    public static final short ML_ATTCHMENTS_DETAIL = MUJMAIL_BASE + 508;
    public static final short ML_FILENAME = MUJMAIL_BASE + 509;
    public static final short ML_WIDTH = MUJMAIL_BASE + 510;
    public static final short ML_HEIGHT = MUJMAIL_BASE + 511;
    public static final short ML_RECIPIENTS = MUJMAIL_BASE + 512;
    public static final short ML_FILE_WILL_OR_WAS_ATTACHED = MUJMAIL_BASE + 513;
    public static final short ML_MAIL_ACCOUNT = MUJMAIL_BASE + 514;

    //FileSystem browser constants
    //#ifdef MUJMAIL_FS
    public static final short FS_BROWSER_SELECT_FILE = MUJMAIL_BASE + 600;
    public static final short FS_BROWSER_SELECT_DIR = MUJMAIL_BASE + 601;
    public static final short FS_BROWSER_ENTER_FILE_NAME = MUJMAIL_BASE + 602;
    public static final short FS_BROWSER_ENTER_DIR_NAME = MUJMAIL_BASE + 603;
    //#endif
    //Configuration synchronization constants
    //#ifdef MUJMAIL_SYNC
    public static final short SYNC_SELECT_MODE = MUJMAIL_BASE + 700;
    public static final short SYNC_MODE_LOCAL = MUJMAIL_BASE + 701;
    public static final short SYNC_MODE_REMOTE = MUJMAIL_BASE + 702;
    //#endif
    //Search constants
    //#ifdef MUJMAIL_SEARCH
    public static final short SEA_SEARCHING = MUJMAIL_BASE + 800;
    public static final short SEA_SEARCH_RESULTS = MUJMAIL_BASE + 801;
    public static final short SEA_SEARCHING_IN_BOX = MUJMAIL_BASE + 802;
    public static final short SEA_ADVANCED_SETTINGS = MUJMAIL_BASE + 803;
    public static final short SEA_DATE_INTERVAL = MUJMAIL_BASE + 804;
    public static final short SEA_DATE_FROM = MUJMAIL_BASE + 805;
    public static final short SEA_DATE_TO = MUJMAIL_BASE + 806;
    public static final short SEA_SEARCH_IN_MAILBOXES = MUJMAIL_BASE + 807;
    public static final short SEA_ENTER_SEARCH_PHRASE = MUJMAIL_BASE + 808;
    public static final short SEA_SEARCH_PHRASE = MUJMAIL_BASE + 809;
    public static final short SEA_REQUIRED = MUJMAIL_BASE + 810;
    public static final short SEA_WHOLE_WORDS = MUJMAIL_BASE + 811;
    public static final short SEA_SEARCH_IN_MSG_PARTS = MUJMAIL_BASE + 812;
    //#endif
    //Utilities constants
    public static final short UT_UTILS = MUJMAIL_BASE + 900;
    public static final short UT_TASKS_PROGRESS_BAR = MUJMAIL_BASE + 901;
    //Menu Actions constants
    public static final short AC_ACTIONS = MUJMAIL_BASE + 1000;
    public static final short AC_WRITE_MAIL = MUJMAIL_BASE + MUJMAIL_BASE + 1001;
    public static final short AC_RETRIEVE_MAILS = MUJMAIL_BASE + MUJMAIL_BASE + 1002;
    public static final short AC_SENDALL = MUJMAIL_BASE + MUJMAIL_BASE + 1003;
    public static final short AC_POLLING = MUJMAIL_BASE + MUJMAIL_BASE + 1004;
    public static final short AC_FORWARD = MUJMAIL_BASE + MUJMAIL_BASE + 1005;
    public static final short AC_CLEAR_DB = MUJMAIL_BASE + MUJMAIL_BASE + 1006;
    public static final short AC_DISCONNECT = MUJMAIL_BASE + MUJMAIL_BASE + 1007;
    public static final short AC_SERVERS_INBOX_SYNC = MUJMAIL_BASE + MUJMAIL_BASE + 1008;
    public static final short AC_BACKUP_SETTINGS = MUJMAIL_BASE + MUJMAIL_BASE + 1009;
    public static final short AC_RESTORE_SETTINGS = MUJMAIL_BASE + MUJMAIL_BASE + 1010;
    //#ifdef MUJMAIL_SEARCH
    public static final short AC_SEARCH_MAILS = MUJMAIL_BASE + MUJMAIL_BASE + 1011;
    //#endif
    public static final short AC_PUSH = MUJMAIL_BASE + MUJMAIL_BASE + 1012;
    //About constants
    public static final short ABT_ABOUT = MUJMAIL_BASE + 1050;
    public static final short ABT_TEXT = MUJMAIL_BASE + 1051;
    // Debug constants
    public static final short DEB_MENU = MUJMAIL_BASE + 1060;
    
    //Sendmails constants
    public static final short SM_FORWARDED_MESSAGE = MUJMAIL_BASE + 1080;
    public static final short SM_SENDING_ACCOUNT = MUJMAIL_BASE + 1081;
    public static final short SM_ATTACHMENT = MUJMAIL_BASE + 1082;
    public static final short SM_FILE_SIZE = MUJMAIL_BASE + 1083;
    public static final short SM_EDIT_BODY = MUJMAIL_BASE + 1084;
    //protocols constants
    public static final short PL_NOTAUTHORIZED = MUJMAIL_BASE + 1150;
    /******************* Lingual constants for buttons  ************************/
    //basic buttons constants			
    public static final short BTN_BASE = 5000;
    public static final short BTN_OK = BTN_BASE + 1;
    public static final short BTN_CANCEL = BTN_BASE + 2;
    public static final short BTN_BACK = BTN_BASE + 3;
    public static final short BTN_CLEAR = BTN_BASE + 4;
    public static final short BTN_SAVE = BTN_BASE + 5;
    public static final short BTN_EDIT = BTN_BASE + 10;
    public static final short BTN_DELETE = BTN_BASE + 11;
    public static final short BTN_DEL_UNDEL = BTN_BASE + 12;
    public static final short BTN_EXIT = BTN_BASE + 13;
    public static final short BTN_RTV_NEW_MAILS = BTN_BASE + 14;
    public static final short BTN_SELECT = BTN_BASE + 15;
    public static final short BTN_CLR = BTN_BASE + 16;
    public static final short BTN_CONFIRM = BTN_BASE + 17;
 
    //buttons for addressbook
    public static final short BTN_AD_MARK = BTN_BASE + 100;
    public static final short BTN_AD_UNMARK = BTN_BASE + 101;
    public static final short BTN_AD_DONE = BTN_BASE + 102;
    public static final short BTN_AD_SEND_MAIL = BTN_BASE + 103;
    public static final short BTN_AD_ADD_NEW = BTN_BASE + 104;
    public static final short BTN_AD_VIEW = BTN_BASE + 105;
    public static final short BTN_AD_FLIPRCP = BTN_BASE + 106;
    //thebox, folders buttons
    public static final short BTN_TB_VIEW_MESS = BTN_BASE + 200;
    public static final short BTN_TB_FORWARD = BTN_BASE + 201;
    public static final short BTN_TB_DEL_NOW = BTN_BASE + 202;
    public static final short BTN_TB_SORT = BTN_BASE + 203;
    public static final short BTN_TB_STOP = BTN_BASE + 204;
    public static final short BTN_TB_MARK_SEEN = BTN_BASE + 205;
    public static final short BTN_TB_MARK_FLAGGED = BTN_BASE + 206;
    //inbox
    public static final short BTN_TB_REPLY = BTN_BASE + 215;
    public static final short BTN_TB_QUOTED_REPLY = BTN_BASE + 216;
    public static final short BTN_TB_REDOWNLOAD = BTN_BASE + 217;
    public static final short BTN_TB_REPLY_ALL = BTN_BASE + 218;
    //outboxs
    public static final short BTN_OB_SEND = BTN_BASE + 220;
    public static final short BTN_OB_SENDALL = BTN_BASE + 221;
    //trash
    public static final short BTN_TR_RESTORE = BTN_BASE + 230;
    public static final short BTN_TR_RESTORE_NOW = BTN_BASE + 231;
    //sendmail
    public static final short BTN_SM_SEND = BTN_BASE + 300;
    public static final short BTN_SM_USE_AB = BTN_BASE + 301;
    public static final short BTN_SM_SEND_LATTER = BTN_BASE + 302;
    public static final short BTN_SM_SAVE = BTN_BASE + 303;
    public static final short BTN_SM_BC = BTN_BASE + 304;
    public static final short BTN_SM_CLEAR = BTN_BASE + 305;
    public static final short BTN_SM_CHOOSE_FROM = BTN_BASE + 306;
    public static final short BTN_SM_ADD_ATTACHMENT = BTN_BASE + 307;
    public static final short BTN_SM_MODIFY_ATTACHMENT = BTN_BASE + 308;
    public static final short BTN_SM_REMOVE_ATTACHMENT = BTN_BASE + 309;
    public static final short BTN_SM_EDIT_BODY_FULLSCREEN = BTN_BASE + 310;
    public static final short BTN_SM_UPDATE_BODY = BTN_BASE + 311;
    public static final short BTN_SM_CANCEL_BODY = BTN_BASE + 312;
    public static final short BTN_SM_REMOVE_ALL_ATTACHMENTS = BTN_BASE + 313;
    public static final short BTN_SM_PREVIEW = BTN_BASE + 314;
    //account settings 
    public static final short BTN_AS_NEW = BTN_BASE + 400;
    public static final short BTN_AS_SET_PRIMARY = BTN_BASE + 401;
    //mailform
    public static final short BTN_MF_ATTACHMENTS = BTN_BASE + 500;
    public static final short BTN_MF_HEADERS_DETAILS = BTN_BASE + 501;
    public static final short BTN_MF_EMAIL_ADDRS = BTN_BASE + 502;
    public static final short BTN_MF_DEL_ATTACHMENTS = BTN_BASE + 503;
    public static final short BTN_MF_DETAILS = BTN_BASE + 504;
    public static final short BTN_MF_ADD_EMAIL = BTN_BASE + 505;
    public static final short BTN_MF_REDOWN_ATTACHMENTS = BTN_BASE + 506;
    //#ifdef MUJMAIL_FS
    public static final short BTN_MF_EXPORT_MAIL_TO_FS = BTN_BASE + 507;
    //#endif
    public static final short BTN_MF_DISPLAY_AS_TEXT = BTN_BASE + 508;
    public static final short BTN_MF_EXPORT_BP_TO_FS = BTN_BASE + 509;
    public static final short BTN_MF_VIEW_CONVERTED = BTN_BASE + 510;
    //search
    //#ifdef MUJMAIL_SEARCH
    public static final short BTN_SEA_NEW_SEARCH = BTN_BASE + 530;
    public static final short BTN_SEA_STOP_SEARCH = BTN_BASE + 531;
    public static final short BTN_SEA_SEARCH = BTN_BASE + 532;
    public static final short BTN_SEA_ADD_NEW_PHRASE = BTN_BASE + 533;
    //#endif    
    
    //filesystem browser
    //#ifdef MUJMAIL_FS
    public static final short BTN_FS_CREATE_DIR = BTN_BASE + 540;
    //#endif
    //about
    public static final short BTN_ABT_FEEDBACK = BTN_BASE + 550;

    //user folders
    //#ifdef MUJMAIL_USR_FOLDERS
    public static final short BTN_USR_FLD_ADD = BTN_BASE + 560;
    //#endif
    /******************* Lingual constants for alerts & activity reports ************************/
    //basic alerts
    public static final short ALRT_BASE = 10000;
    public static final short ALRT_WAIT = ALRT_BASE + 1;
    public static final short ALRT_LOADING = ALRT_BASE + 2;
    public static final short ALRT_SAVING = ALRT_BASE + 3;
    public static final short ALRT_DELETING = ALRT_BASE + 4;
    public static final short ALRT_BAD_EMAIL = ALRT_BASE + 5;
    public static final short ALRT_SORTING = ALRT_BASE + 6;
    public static final short ALRT_INITIATING = ALRT_BASE + 7;
    public static final short ALRT_SYNCHRONIZING = ALRT_BASE + 8;
    public static final short ALRT_NEW_MAILS = ALRT_BASE + 9;
    //settings
    public static final short ALRT_ST_SAVING = ALRT_BASE + 50;
    public static final short ALRT_ST_LOADING = ALRT_BASE + 51;

    //sendmail	
    public static final short ALRT_SM_NO_RCP = ALRT_BASE + 100;
    public static final short ALRT_SM_CAN_NOT_ATTACH_BODY = ALRT_BASE + 101;
    //inprotocols
    public static final short ALRT_INPL_NO_LONGER_ON_SERVER = ALRT_BASE + 150;
    public static final short ALRT_INPL_CHECK_MAILS = ALRT_BASE + 151;
    public static final short ALRT_INPL_DEL_MAILS = ALRT_BASE + 152;
    public static final short ALRT_INPL_DOWN_MAIL = ALRT_BASE + 153;
    public static final short ALRT_INPL_NEWMAILS = ALRT_BASE + 154;
    public static final short ALRT_INPL_REDOWN_MAIL = ALRT_BASE + 155;
    public static final short ALRT_INPL_IMAP_CANNOT_SELECT_MAILBOX = ALRT_BASE + 156;
    public static final short ALRT_INPL_IMAP_UIDVALIDITY_DIFFERS = ALRT_BASE + 157;
    public static final short ALRT_INPL_IMAP_GETURL_NO_PAGE = ALRT_BASE + 158;
    public static final short ALRT_INPL_SAVE_SRV_SENT = ALRT_BASE+159; 
        
    //concerning the system
    public static final short ALRT_SYS_BUSY = ALRT_BASE + 200;
    public static final short ALRT_SYS_DEL_ALL_CONFIRM = ALRT_BASE + 201;
    public static final short ALRT_SYS_NO_DBSPACE = ALRT_BASE + 202;
    public static final short ALRT_SYS_CHECK_COM_SETTINGS = ALRT_BASE + 203;
    public static final short ALRT_SYS_SHUTDOWN_CONFIRM = ALRT_BASE + 204;
    public static final short ALRT_SYS_PROCESS_INTERRUPTED = ALRT_BASE + 205;
    public static final short ALRT_SYS_EXCEPTION_AROSED = ALRT_BASE + 206;
    public static final short ALRT_SYS_LOW_MEMORY = ALRT_BASE + 207;
    public static final short ALRT_SYS_DEL_CONFIRM = ALRT_BASE + 208;
    public static final short ALRT_SYS_TASK_FINISHED = ALRT_BASE + 209;
    //mailform
    public static final short ALRT_MF_LOAD_BODY = ALRT_BASE + 250;
    public static final short ALRT_MF_VIEW_ATT = ALRT_BASE + 251;
    public static final short ALRT_MF_NO_TEXT = ALRT_BASE + 252;
    public static final short ALRT_MF_NO_ATTACHMENTS = ALRT_BASE + 253;
    public static final short ALRT_MF_DEL_ATT = ALRT_BASE + 254;
    public static final short ALRT_MF_UNSUPPORTED_FORMAT = ALRT_BASE + 255;
    public static final short ALRT_MF_SAVE_CNTS = ALRT_BASE + 256;
    public static final short ALRT_MF_NO_VIEWABLE_BODY = ALRT_BASE + 257;
    public static final short ALRT_MF_UNSUPPORTED_CONVERSION = ALRT_BASE + 258;
    //account settings alert
    public static final short ALRT_AS_NONEXIST = ALRT_BASE + 300;
    public static final short ALRT_AS_NO_ACCOUNT_SET_ACTIVE = ALRT_BASE + 301;
    public static final short ALRT_AS_LOAD = ALRT_BASE + 302;
    public static final short ALRT_AS_ALREADY_EXITS = ALRT_BASE + 303;
    public static final short ALRT_AS_SET_DEFAULT_ACCOUNT = ALRT_BASE + 304;
    //adressbook
    public static final short ALRT_AD_LOAD = ALRT_BASE + 400;
    public static final short ALRT_AD_DELETE = ALRT_BASE + 401;
    //protocol concerning constants
    public static final short ALRT_PL_CONNECTING = ALRT_BASE + 500;
    public static final short ALRT_PL_CLOSING = ALRT_BASE + 501;
    //SMTP constants
    public static final short ALRT_SMTP_SENDING = ALRT_BASE + 600;
    // Mobile abilities warnings
    public static final short ALRT_MOB_JSR75 = ALRT_BASE + 650;
    //Malboxes exceptions
    public static final short ALRT_MB_CREATING = ALRT_BASE + 700;
    
    /******************* Lingual constants for Exceptions  ************************/
    public static final short EXP_BASE = 12000;
    //!!! The offset of these values must be corresponding to the offset defined in MyException class
    //ie: LANG.EXP_COM_BASE = 1 (+ EXP_BASE)
    //   MYEXCEPTION.COM_BASE = 1	
    //in this example both offsets are 1
    public static final short EXP_COM_BASE = EXP_BASE + 1;
    public static final short EXP_COM_IN = EXP_COM_BASE + 1;
    public static final short EXP_COM_OUT = EXP_COM_BASE + 2;
    public static final short EXP_COM_TIMEOUT = EXP_COM_BASE + 3;
    public static final short EXP_COM_HALTED = EXP_COM_BASE + 4;
    public static final short EXP_COM_UNKNOWN = EXP_COM_BASE + 5;
    public static final short EXP_SYSTEM_BASE = EXP_BASE + 10;
    public static final short EXP_SYS_OUT_OF_MEMORY = EXP_SYSTEM_BASE + 1;
    public static final short EXP_SYS_IMAGE_FAILED = EXP_SYSTEM_BASE + 2;
    public static final short EXP_PROTOCOL_BASE = EXP_BASE + 20;
    public static final short EXP_PROTOCOL_CANNOT_CONNECT = EXP_PROTOCOL_BASE + 1;
    public static final short EXP_PROTOCOL_CANNOT_RETRIEVE_BODY = EXP_PROTOCOL_BASE + 2;
    public static final short EXP_PROTOCOL_COMMAND_NOT_EXECUTED = EXP_PROTOCOL_BASE + 3;
    public static final short EXP_PROTOCOL_CANNOT_DELETE_MAILS = EXP_PROTOCOL_BASE + 4;
    public static final short EXP_VARIOUS_BASE = EXP_BASE + 30;
    public static final short EXP_VARIOUS_BAD_EMAIL = EXP_VARIOUS_BASE + 1;
    public static final short EXP_VARIOUS_AB_MULTIPLE_ENTRIES = EXP_VARIOUS_BASE + 2;
    public static final short EXP_VARIOUS_DECODE_ILLEGAL_MIME = EXP_VARIOUS_BASE + 3;
    //#ifdef MUJMAIL_FS
    public static final short EXP_FS_BASE = EXP_BASE + 40;
    public static final short EXP_FS_IO_ERROR = EXP_FS_BASE + 1;
    public static final short EXP_FS_FILE_EXISTS = EXP_FS_BASE + 2;
    public static final short EXP_FS_CLOSE_FILE = EXP_FS_BASE + 3;
    public static final short EXP_FS_OPEN_FILE = EXP_FS_BASE + 4;
    public static final short EXP_FS_FILE_READ_ERROR = EXP_FS_BASE + 5;
    //#endif
    public static final short EXP_DB_BASE = EXP_BASE + 50;
    public static final short EXP_DB_NOSPACE = EXP_DB_BASE + 1;
    public static final short EXP_DB_CANNOT_CLEAR = EXP_DB_BASE + 2;
    public static final short EXP_DB_CANNOT_SAVE_BODY = EXP_DB_BASE + 3;
    public static final short EXP_DB_CANNOT_SAVE_HEADER = EXP_DB_BASE + 4;
    public static final short EXP_DB_CANNOT_DEL_HEADER = EXP_DB_BASE + 5;
    public static final short EXP_DB_CANNOT_DEL_BODY = EXP_DB_BASE + 6;
    public static final short EXP_DB_CANNOT_DEL_MAIL = EXP_DB_BASE + 7;
    public static final short EXP_DB_CANNOT_LOAD_BODY = EXP_DB_BASE + 8;
    public static final short EXP_DB_CANNOT_LOAD_CONTACT = EXP_DB_BASE + 9;
    public static final short EXP_DB_CANNOT_SAVE_CONTACT = EXP_DB_BASE + 10;
    public static final short EXP_DB_CANNOT_DEL_CONTACT = EXP_DB_BASE + 11;
    public static final short EXP_DB_CANNOT_UPDATE_HEADER = EXP_DB_BASE + 12;
    public static final short EXP_DB_CANNOT_LOAD_SETTINGS = EXP_DB_BASE + 13;
    public static final short EXP_DB_CANNOT_SAVE_SETTINGS = EXP_DB_BASE + 14;
    public static final short EXP_DB_CANNOT_LOAD_ACCOUNTS = EXP_DB_BASE + 15;
    public static final short EXP_DB_CANNOT_SAVE_ACCOUNT = EXP_DB_BASE + 16;
    public static final short EXP_DB_CANNOT_DELETE_ACCOUNT = EXP_DB_BASE + 17;
    public static final short EXP_DB_CANNOT_OPEN_DB = EXP_DB_BASE + 18;
    public static final short EXP_DB_CANNOT_LOAD_HEADERS = EXP_DB_BASE + 19;
    public static final short EXP_DB_CANNOT_CLOSE_DB = EXP_DB_BASE + 20;
    public static final short EXP_DB_CANNOT_SAVE_MSGID = EXP_DB_BASE + 21;
    public static final short EXP_DB_CANNOT_LOAD_MSGID = EXP_DB_BASE + 22;
    public static final short EXP_DB_CANNOT_DEL_MSGID = EXP_DB_BASE + 23;
    public final static short EXP_DB_CANNOT_SAVE = EXP_DB_BASE + 24;
    public final static short EXP_DB_CANNOT_DEL = EXP_DB_BASE + 25;

    //the main method of the class, is used to get the appropriate lingual string
    /**
     * Converts Lingual codes into strings.
     * @param lang_code Code to be converted into string.
     * @return Text associtated with code.
     */
    public static String get(short lang_code) {

        switch (lang_code) {

            case EMPTY:
                return "empty";
            case FAILED:
                return ".. failed.";
            case SUCCESS:
                return ".. successful.";
            case IN:
                return " in ";
            case ACTIVE:
                return "Active";
            case INACTIVE:
                return "Inactive";
            case MSGIDS_CACHE:
                return "Message-IDs cache";
            case DB_SPACE_LEFT:
                return "DB space left: ";

            case ML_RECIPIENTS:
                return "Recipients:";
            case ML_TO:
                return "To:";
            case ML_SUBJECT:
                return "Subject:";
            case ML_BODY:
                return "Body:";
            case ML_FROM:
                return "From:";
            case ML_ATTACHMENTS:
                return "Bodyparts:";
            case ML_SIZE:
                return "Size:";
            case ML_TIME:
                return "Time:";
            case ML_HEADERS_DETAIL:
                return "Header's details:";
            case ML_ATTCHMENTS_DETAIL:
                return "Bodypart's details:";
            case ML_FILENAME:
                return "Filename:";
            case ML_WIDTH:
                return "Width:";
            case ML_HEIGHT:
                return "Height:";
            case ML_FILE_WILL_OR_WAS_ATTACHED:
                return "File was/will be attached:";
            case ML_MAIL_ACCOUNT:
            	return "Account:";

            //#ifdef MUJMAIL_FS    
            case FS_BROWSER_SELECT_FILE:
            	return "Select file";
            case FS_BROWSER_ENTER_FILE_NAME:
            	return "Enter file name";
            case FS_BROWSER_ENTER_DIR_NAME:
            	return "Enter directory name";
            //#endif

            //#ifdef MUJMAIL_SYNC
            case SYNC_SELECT_MODE:
            	return "Select mode";
            case SYNC_MODE_LOCAL:
            	return "Local";
            case SYNC_MODE_REMOTE:
            	return "Remote";
            //#endif
            
            //#ifdef MUJMAIL_SEARCH
            case SEA_SEARCHING:
            	return "Searching...";
            case SEA_SEARCH_RESULTS:
            	return "Search results";
            case SEA_SEARCHING_IN_BOX:
            	return "Searching in box ";
            case SEA_ADVANCED_SETTINGS:
            	return "Advanced settings";
            case SEA_DATE_INTERVAL:
            	return "Date interval";
            case SEA_DATE_FROM:
            	return "From: ";
            case SEA_DATE_TO:
            	return "To: ";
            case SEA_SEARCH_IN_MAILBOXES:
            	return "Search in mailboxes";
            case SEA_ENTER_SEARCH_PHRASE:
            	return "Enter search phrase";
            case SEA_SEARCH_PHRASE:
            	return "Search phrase";
            case SEA_REQUIRED:
            	return "Required";
            case SEA_WHOLE_WORDS:
            	return "Whole words";
            case SEA_SEARCH_IN_MSG_PARTS:
            	return "Search in message parts";
            //#endif
            
            case AS_ACCOUNTS:
                return "Accounts";
            case AS_PRIMARY:
                return "Primary";
            case AS_ACTIVATION:
                return "Account activation:";
            case AS_EMAIL:
                return "Email:";
            case AS_PROTOCOL:
                return "Protocol:";
            case AS_SERVER:
                return "Server:";
            case AS_PORT:
                return "Port:";
            case AS_USR_NAME:
                return "User name:";
            case AS_PASS:
                return "Password:";
            case AS_SSL:
                return "Use SSL";
            case AS_IMAP_MAILBOXES:
                return "IMAP4 mailboxes separated by commas:";
            case AS_COPY_TO_SERVER:
                return "Save mails to server";
            case AS_COPY_TO_SRV_TRASH:
                return "Deleted mails to Trash";
            case AS_COPY_TO_SRV_SENT:
                return "Sent mail to Sent";
            case AS_COPY_TO_SRV_SENT_MAILBOX:
                return "Sentbox folder server name";
            case AS_COPY_TO_SRV_TRASH_MAILBOX:
                return "Delete folder server name";
            case AS_SSL_TYPE:
            	return "SSL type";
            case AS_MUJMAIL_SSL:
            	return "Use mujMail SSL";
            case AS_FIRMWARE_SSL:
            	return "Use firmware SSL";

            case AC_ACTIONS:
                return "Actions";
            case AC_WRITE_MAIL:
                return "Write a mail";
            case AC_RETRIEVE_MAILS:
                return "Retrieve mails";
            case AC_SENDALL:
                return "Send all";
            case AC_POLLING:
                return "Polling";
            case AC_FORWARD:
                return "Forward a mail";
            case AC_CLEAR_DB:
                return "Clear Databases";
            case AC_DISCONNECT:
                return "Disconnect";
            case AC_SERVERS_INBOX_SYNC:
                return "Sync w/ servers";
            case AC_BACKUP_SETTINGS:
            	return "Backup settings";
            case AC_RESTORE_SETTINGS:
            	return "Restore settings";
            //#ifdef MUJMAIL_SEARCH
            case AC_SEARCH_MAILS:
                return "Search mails";
            //#endif
            case AC_PUSH:
                return "Push";
                        
            case UT_UTILS:
                return "Utilities";
            case UT_TASKS_PROGRESS_BAR:
            	return "Progress bar";
                
            case AD_ADDRESSBOOK:
                return "AddressBook";
            case AD_CONTACT_INFO:
                return "Contact info";
            case AD_NAME:
                return "Name:";
            case AD_EMAIL:
                return "Email:";
            case AD_NOTES:
                return "Notes:";
            case AD_ADDDING_RCPS:
                return "Adding recipients";

            case TB_FOLDERS:
                return "Folders";
            case TB_INBOX:
                return "Inbox";
            case TB_OUTBOX:
                return "Outbox";
            case TB_DRAFT:
                return "Drafts";
            case TB_SENTBOX:
                return "Sentbox";
            case TB_TRASH:
                return "Trash";
            case TB_NO_SUBJECT:
                return "<no subject>";
            case TB_READ_MAILS:
                return "Read mails";
            case TB_UNREAD_MAILS:
                return "Unread mails";
            case TB_ALL_MAILS:
                return "All mails";
            case TB_USERFOLDERS:
                return "User folders";
            case TB_FOLDER_NAME:
                return "Folder name";
            case TB_RETRIVE_ACCOUNTS:
                return "Accounts to retrive";
            case TB_IMAP_FOLDERS:
                return "IMAP folders";
                
            case ST_SETTINGS:
                return "Settings";
            case ST_SORT_SETTINGS:
                return "Sort settings";
            case ST_SORT_BY:
                return "Sort by:";
            case ST_SORT_TIME:
                return "by time";
            case ST_SORT_SUBJECT:
                return "by subject";
            case ST_SORT_RECIPIENTS:
                return "by recipients";
            case ST_SORT_SENDER:
                return "by sender";
            case ST_SORT_SIZE:
                return "by size";
            case ST_SORT_RECORD_ID:
                return "by record id";
            case ST_SORT_ORDER:
                return "Sort order:";
            case ST_SORT_INCREASE:
                return "Increasing";
            case ST_SORT_DECREASE:
                return "Decreasing";
            case ST_NOT_SET:
                return "<not set>";
            case ST_LANG:
                return "Language:";
            case ST_SMTP_AUTH:
                return "SMTP authentization:";
            case ST_SMTP_AUTH_NEEDED:
                return "authentization needed";
            case ST_DOWN_WHOLE_MAIL:
                return "Always retrieve whole mails";
            case ST_DEL_FROM_SERVER:
                return "Delete mails from server when they're deleted from InBox";
            case ST_ADD_TO_ADBOOK:
                return "Auto-add contacts to AddressBook";
            case ST_POLLING_INTERVAL:
                return "Polling interval (sec):";
            case ST_TIMEOUT:
                return "Communication timeout (msec):";
            case ST_SIGN:
                return "Signature:";
            case ST_OTHER:
                return "Other settings:";
            case ST_POLLING_BEHAVIOUR:
                return "If polling detects new mails:";
            case ST_POLLING_PLAY_SOUND:
                return "Plays a sound";
            case ST_POLLING_DOWN:
                return "Auto-downloads";
            case ST_DEL_ON_EXIT:
                return "Delete mails from InBox without confirming by \"Delete Now!\"";
            case ST_ML_SMALL_FONT:
                return "View mails w/ small fonts";
            case ST_TICK_TEXT:
                return "Tick long text";
            case ST_SAFE_MODE:
                return "Economical use of DataBase (don't store messages' bodies)";
            case ST_RETR_MAX_MAILS:
                return "Retrieve max. mails from one account (0=unlimited):";
            case ST_REPLACE_TABS:
                return "Display spaces instead of tabs";
            case ST_MOVE_TO_TRASH:
                return "Move mails to Trash";
            case ST_DOWNLOAD_ONLY_NEVER_SEEN:
                return "Retrieve only never seen mails";
            case ST_AUTO_CLEAR_INBOX:
                return "Clear InBox's DataBase if DB space left is less than (-1=off) kB:";
            case ST_RETR_MAX_LINES:
                return "Retrieve max. lines of a mail (-1=unlimited):";
            case ST_FONT_SIZE:
            	return "Font size";
            case ST_FONT_NORMAL:
            	return "Normal";
            case ST_FONT_LARGE:
            	return "Large";
            case ST_MUJMAIL_SRV_ADDRESS:
            	return "MujMail server address";
            case ST_MUJMAIL_SRV_PORT:
            	return "MujMail server port";
            case ST_MUJMAIL_SRV_LOGIN:
            	return "MujMail server login";
            case ST_MUJMAIL_SRV_PSWD:
            	return "MujMail server password";
            case ST_SMTP:
                return "Smtp ";
            case ST_PRIMARY_MAIL:
                return "Primary email";

            case ST_THREADING:
            	return "Threading";
            case ST_THREADING_ENABLE:
            	return "enabled";
            case ST_ROOTS_VISIBLE:
            	return "show roots";

            case ABT_ABOUT:
                return "About";
            case ABT_TEXT:
                return "mujMail " + MujMail.VERSION + " (" + String.valueOf(MujMail.BUILD) + ")\n" +
                        "\n" +
                        "Authors:\n" +
                        "Pavel Machek\n" +
                        "Martin Stefan\n" +
                        "Nguyen Son Tung\n" +
                        "Martin Suska\n" +
                        "David Hauzar\n" +
                        "Nodir Yuldashev\n" +
                        "Pavel Jancik\n" +
                        "\n" +
                        "Special thanks:\n" +
                        "Petr Spatka for initiation of the project\n" +
                        //					"Localization:\n"+
                        //					"Dear translator, please type in your name here\n"+
                        "\n" +
                        "For more information, help and newer version please visit www.mujmail.org\n" +
                        "Also, if you have any questions or comments, write us a mail by pressing the Feedback button.\n" +
                        "\n";
            case DEB_MENU:
                return "Debug";
 
            case PL_NOTAUTHORIZED:
                return "Bad login information: ";

            case SM_FORWARDED_MESSAGE:
                return "Forwarded message";
            case SM_SENDING_ACCOUNT:
                return "Send from account";
            case SM_ATTACHMENT:
                return "Attachment:";
            case SM_FILE_SIZE:
                return "File Size:";
            case SM_EDIT_BODY:
            	return "Edit body";
                
            case BTN_OK:
                return "OK";
            case BTN_CANCEL:
                return "Cancel";
            case BTN_BACK:
                return "Back";
            case BTN_CLEAR:
                return "Clear Database";
            case BTN_SAVE:
                return "Save";
            case BTN_EDIT:
                return "Edit";
            case BTN_DELETE:
                return "Delete";
            case BTN_DEL_UNDEL:
                return "(Un)Delete";
            case BTN_EXIT:
                return "Exit";
            case BTN_SELECT:
                return "Select";
            case BTN_CLR:
            	return "Clear";
            case BTN_CONFIRM:
            	return "Confirm";

            case BTN_RTV_NEW_MAILS:
                return "Retrieve mails";

            case BTN_AD_MARK:
                return "Mark";
            case BTN_AD_UNMARK:
                return "Unmark";
            case BTN_AD_DONE:
                return "Done";
            case BTN_AD_SEND_MAIL:
                return "Send a mail";
            case BTN_AD_ADD_NEW:
                return "Add new";
            case BTN_AD_VIEW:
                return "View";
            case BTN_AD_FLIPRCP:
                return "Flip recipients";

            case BTN_TB_VIEW_MESS:
                return "View mail";
            case BTN_TB_FORWARD:
                return "Forward";
            case BTN_TB_DEL_NOW:
                return "Delete now!";
            case BTN_TB_STOP:
                return "Stop";
            case BTN_TB_SORT:
                return "Sort";
            case BTN_TB_MARK_SEEN:
            	return "Mark (un)seen";
            case BTN_TB_MARK_FLAGGED:
            	return "Mark (un)flagged";

            case BTN_OB_SEND:
                return "Send this";
            case BTN_OB_SENDALL:
                return "Send all";

            case BTN_TR_RESTORE:
                return "Restore";
            case BTN_TR_RESTORE_NOW:
                return "Restore now!";

            case BTN_SM_SEND:
                return "Send now";
            case BTN_SM_USE_AB:
                return "Insert contact";
            case BTN_SM_SEND_LATTER:
                return "Send later";
            case BTN_SM_SAVE:
                return "Save";
            case BTN_SM_BC:
                return "Add Bcc/Cc";
            case BTN_SM_CLEAR:
                return "Clear fields";
            case BTN_SM_CHOOSE_FROM:
                return "Select sending account";
            case BTN_SM_ADD_ATTACHMENT:
            	return "Add attachment";
            case BTN_SM_MODIFY_ATTACHMENT:
            	return "Modify attachment";
            case BTN_SM_REMOVE_ATTACHMENT:
            	return "Remove attachment";
            case BTN_SM_EDIT_BODY_FULLSCREEN:
                return "Edit body fullscreen";
            case BTN_SM_UPDATE_BODY:
                return "Update body text";
            case BTN_SM_CANCEL_BODY:
                return "Cancel body update";
            case BTN_SM_REMOVE_ALL_ATTACHMENTS:
            	return "Remove all attachment";
            case BTN_SM_PREVIEW:
            	return "Preview";

            case BTN_AS_NEW:
                return "New account";
            case BTN_AS_SET_PRIMARY:
                return "Set as primary";

            case BTN_TB_REPLY:
                return "Reply";
            case BTN_TB_QUOTED_REPLY:
                return "Quoted reply";
            case BTN_TB_REPLY_ALL:
                return "Reply all";
            case BTN_TB_REDOWNLOAD:
                return "Redownload";

            case BTN_MF_ATTACHMENTS:
                return "Bodyparts";
            case BTN_MF_HEADERS_DETAILS:
                return "Header details";
            case BTN_MF_EMAIL_ADDRS:
                return "E-mail addresses";
            case BTN_MF_DEL_ATTACHMENTS:
                return "Delete bodypart";
            case BTN_MF_DETAILS:
                return "Details";
            case BTN_MF_ADD_EMAIL:
                return "Save addresses";
            case BTN_MF_REDOWN_ATTACHMENTS:
                return "Redownload bodypart";
            //#ifdef MUJMAIL_FS
            case BTN_MF_EXPORT_MAIL_TO_FS:
            	return "Export mail to filesystem";
            //#endif
            case BTN_MF_DISPLAY_AS_TEXT:
            	return "Display as text";
            case BTN_MF_EXPORT_BP_TO_FS:
            	return "Export to filesystem";
            case BTN_MF_VIEW_CONVERTED:
            	return "View converted";

            //#ifdef MUJMAIL_SEARCH	
            case BTN_SEA_NEW_SEARCH:
            	return "New search";
            case BTN_SEA_STOP_SEARCH:
            	return "Stop search";
            case BTN_SEA_ADD_NEW_PHRASE:
            	return "Add new search phrase";
            case BTN_SEA_SEARCH:
            	return "Search";
            //#endif

            //#ifdef MUJMAIL_FS
            case BTN_FS_CREATE_DIR:
            	return "Create directory";
            //#endif
            case BTN_ABT_FEEDBACK:
                return "Feedback";

            //#ifdef MUJMAIL_USR_FOLDERS
            case BTN_USR_FLD_ADD: 
                return "Add folder";
            //#endif
            
            
            case ALRT_WAIT:
                return "..please wait.";
            case ALRT_LOADING:
                return "Loading ";
            case ALRT_SAVING:
                return "Saving ";
            case ALRT_DELETING:
                return "Deleting ";
            case ALRT_BAD_EMAIL:
                return "Invalid email format.";
            case ALRT_SORTING:
                return "Sorting ";
            case ALRT_INITIATING:
                return "Initiating ";
            case ALRT_SYNCHRONIZING:
                return "Servers->InBox Synchronizing ";


            case ALRT_AS_NONEXIST:
                return "Account does not exist or is inactive";
            case ALRT_AS_NO_ACCOUNT_SET_ACTIVE:
                return "You have no account set or all accounts are inactive";
            case ALRT_AS_LOAD:
                return "Loading accounts";
            case ALRT_AS_ALREADY_EXITS:
                return "Account already exists: ";
            case ALRT_AS_SET_DEFAULT_ACCOUNT:
                return "Please set up an account for receiving emails.";

            case ALRT_ST_SAVING:
                return "Savings settings ";
            case ALRT_ST_LOADING:
                return "Loading settings ";

            case ALRT_SM_NO_RCP:
                return "No recipient specified.";
            case ALRT_SM_CAN_NOT_ATTACH_BODY:
                return "Mail body is empty or can not be attached.";

            case ALRT_INPL_NO_LONGER_ON_SERVER:
                return "The mail may be no longer on the server ";
            case ALRT_INPL_CHECK_MAILS:
                return "Checking mails for ";
            case ALRT_INPL_DEL_MAILS:
                return "Deleting mails from ";
            case ALRT_INPL_DOWN_MAIL:
                return "Downloading body for ";
            case ALRT_INPL_NEWMAILS:
                return "New mails for ";
            case ALRT_INPL_REDOWN_MAIL:
                return "Redownloading body for ";
            case ALRT_INPL_IMAP_CANNOT_SELECT_MAILBOX:
                return "Can not select mail box ";
            case ALRT_INPL_IMAP_UIDVALIDITY_DIFFERS:
                return "UIDVALIDITY of the mail box differs";
            case ALRT_INPL_IMAP_GETURL_NO_PAGE:
                return "Can not obtain www page";
            case ALRT_INPL_SAVE_SRV_SENT:
                return "Saving mail to server Sent "; 

            case ALRT_NEW_MAILS:
                return "New mails have arrived.";
            case ALRT_SYS_BUSY:
                return "Previous task still active. ";
            case ALRT_SYS_DEL_ALL_CONFIRM:
                return "Do you really want to clear the database?";
            case ALRT_SYS_NO_DBSPACE:
                return "Not enough of space on the device.";
            case ALRT_SYS_CHECK_COM_SETTINGS:
                return "Try to check account settings or network access.";
            case ALRT_SYS_SHUTDOWN_CONFIRM:
                return "Some tasks are still incomplete, are you sure to exit?";
            case ALRT_SYS_PROCESS_INTERRUPTED:
                return "The process was interreputed.";
            case ALRT_SYS_EXCEPTION_AROSED:
                return "An exception arosed.";
            case ALRT_SYS_LOW_MEMORY:
                return "Low memory";
            case ALRT_SYS_DEL_CONFIRM:
                return "Do you really want to delete ";
            case ALRT_SYS_TASK_FINISHED:
            	return "Task has finished";

            case ALRT_AD_LOAD:
                return "Loading addressbook ";
            case ALRT_AD_DELETE:
                return "Deleting addressbook ";

            case ALRT_MF_LOAD_BODY:
                return "Loading body ";
            case ALRT_MF_VIEW_ATT:
                return "Displaying bodypart ";
            case ALRT_MF_NO_TEXT:
                return "The mail has no text";
            case ALRT_MF_NO_ATTACHMENTS:
                return "The mail has no bodyparts";
            case ALRT_MF_DEL_ATT:
                return "Deleting attachments";
            case ALRT_MF_UNSUPPORTED_FORMAT:
                return "Unsupported or damaged email";
            case ALRT_MF_SAVE_CNTS:
                return "Saving addersses";
            case ALRT_MF_NO_VIEWABLE_BODY:
                return "The mail has no viewable body";
            case ALRT_MF_UNSUPPORTED_CONVERSION:
            	return "Unsupported conversion";

            case ALRT_PL_CONNECTING:
                return "Connecting to ";
            case ALRT_PL_CLOSING:
                return "Closing ";

            case ALRT_SMTP_SENDING:
                return "Sending mail ";
            
            case ALRT_MOB_JSR75:
                return "Your phone does not support file system access";

            case ALRT_MB_CREATING:
                return "User mailbox creating ";

            case EXP_COM_IN:
                return "100: Incomming communication problem ";
            case EXP_COM_OUT:
                return "100: Outgouing communication problem ";
            case EXP_COM_TIMEOUT:
                return "100: communication timeout ";
            case EXP_COM_HALTED:
                return "*communication interrupted ";
            case EXP_COM_UNKNOWN:
                return "100: unknown error while communicating with the server ";


            case EXP_DB_NOSPACE:
                return "100: not enough of space on the device.";
            case EXP_DB_CANNOT_CLEAR:
                return "100: can not deleted the database.";
            case EXP_DB_CANNOT_SAVE_BODY:
                return "100: can not save mail's body to the database ";
            case EXP_DB_CANNOT_UPDATE_HEADER:
                return "100: can not update mail's header to the database ";
            case EXP_DB_CANNOT_SAVE_HEADER:
                return "100: can not save mail's header to the database ";
            case EXP_DB_CANNOT_DEL_HEADER:
                return "100: can not delete the header from the database ";
            case EXP_DB_CANNOT_DEL_BODY:
                return "100: can not delete the body of the mail from the database ";
            case EXP_DB_CANNOT_DEL_MAIL:
                return "100: can not delete the mail from the database ";
            case EXP_DB_CANNOT_LOAD_BODY:
                return "100: can not load the body of the mail from the database ";
            case EXP_DB_CANNOT_LOAD_CONTACT:
                return "100: can not load a contact from the database ";
            case EXP_DB_CANNOT_SAVE_CONTACT:
                return "100: can not save the contact to the database ";
            case EXP_DB_CANNOT_DEL_CONTACT:
                return "100: can not delete the contact from the database ";
            case EXP_DB_CANNOT_LOAD_SETTINGS:
                return "100: can not load settings from the database.";
            case EXP_DB_CANNOT_SAVE_SETTINGS:
                return "100: can not save settings to the database.";
            case EXP_DB_CANNOT_LOAD_ACCOUNTS:
                return "100: can not load accounts";
            case EXP_DB_CANNOT_SAVE_ACCOUNT:
                return "100: can not save acount";
            case EXP_DB_CANNOT_DELETE_ACCOUNT:
                return "100: can not delete account";
            case EXP_DB_CANNOT_OPEN_DB:
                return "100: Can not open the database ";
            case EXP_DB_CANNOT_LOAD_HEADERS:
                return "100: Can not load headers from the database ";
            case EXP_DB_CANNOT_CLOSE_DB:
                return "100: Can not close the database ";
            case EXP_DB_CANNOT_SAVE_MSGID:
                return "100: Can not save message ID to the database";
            case EXP_DB_CANNOT_LOAD_MSGID:
                return "100: Can not load message IDs from the database";
            case EXP_DB_CANNOT_DEL_MSGID:
                return "100: Can not delete message ID from the database";
            case EXP_DB_CANNOT_SAVE:
                return "100: Can not save data into database";
            case EXP_DB_CANNOT_DEL:
                return "100: Can remove from database";

            case EXP_SYS_OUT_OF_MEMORY:
                return "100: not enough of RAM memmory.";
            case EXP_SYS_IMAGE_FAILED:
                return "100: Loading image failed.";

            case EXP_PROTOCOL_CANNOT_CONNECT:
                return "200: can not connect to the server ";
            case EXP_PROTOCOL_CANNOT_RETRIEVE_BODY:
                return "200: can not receive the body of the mail ";
            case EXP_PROTOCOL_COMMAND_NOT_EXECUTED:
                return "The protocol command was not correctly executed";
            case EXP_PROTOCOL_CANNOT_DELETE_MAILS:
                return "200: can not delete selected mails ";

            case EXP_VARIOUS_BAD_EMAIL:
                return "300: bad email format";
            case EXP_VARIOUS_AB_MULTIPLE_ENTRIES:
                return "300: the email already exists in Addressbook.";
            case EXP_VARIOUS_DECODE_ILLEGAL_MIME:
                return "100: the email contains illegal MIME characters.";

            //#ifdef MUJMAIL_FS
            case EXP_FS_IO_ERROR:
            	return "400: I/O error occurred.";
            case EXP_FS_FILE_EXISTS:
            	return "400: File already exists.";
            case EXP_FS_CLOSE_FILE:
            	return "400: Error occurred while closing file.";
            case EXP_FS_OPEN_FILE:
            	return "400: Error occurred while opening file.";
            case EXP_FS_FILE_READ_ERROR:
            	return "400: Error reading from file.";
            //#endif
        }

        return "Lingual code " + lang_code + " not defined.";

    }
}
