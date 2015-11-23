package mujmail;



import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;

/**
 * Menu entry class
 */
public class Debug extends Form implements CommandListener, Runnable {

    private Command back;
    private Command test;
    private MujMail mujmail;
            
    public Debug(MujMail mujMail) {
        super(Lang.get(Lang.DEB_MENU));
        mujmail = mujMail;

        append("platform:" + Properties.platform + "\n");
        append("model:" + Properties.model + "\n");
        append("textFieldMailIncorrect:" + new Boolean(Properties.textFieldMailIncorrect).toString() + "\n");      
        //#ifdef MUJMAIL_FS
        append("Jsr75:" + new Boolean(Properties.JSR75Available()).toString() + "\n");      
        //#endif
        back = new Command(Lang.get(Lang.BTN_BACK), Command.BACK, 0);
        test = new Command( "run test", Command.OK, 1);
        addCommand(back);
        addCommand(test);
        setCommandListener(this);
    }

    public void run() {
           // Do test
//        MailAccount account = (MailAccount)MujMail.mujmail.getMailAccounts().elements().nextElement();
//        IMAP4 imap = (IMAP4)account.getProtocol();
// Need Extended IMAP4 to work
//        System.out.println( imap.getURL("www.seznam.cz"));
//           System.out.println( imap.getURL("www.seznam.cy"));
//           System.out.println( imap.getURL("u-pl8.ms.mff.cuni.cz"));
//           System.out.println( imap.getURL("www.root.cz"));
    }

    public synchronized void commandAction(Command c, Displayable d) {
        if ( d == this ) {
            if ( c == test ) {
                Thread t = new Thread(this);
                t.start();
                t.setPriority(Thread.MAX_PRIORITY);
            } else { // back
                mujmail.getDisplay().setCurrent(mujmail.getMenu());
            }       
        }
    }
}
