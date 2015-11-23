package mujmail.ordering.comparator;

import mujmail.MessageHeader;
import mujmail.ordering.Ordering;

/** 
 * Comparing messages by internal threading message identification.
 */
public class ThreadingMessageIDComparator extends MessageHeaderComparator {

    /** Creates comparator */
    public ThreadingMessageIDComparator(Ordering ordering) {
        super(ordering);
    }

    protected int compare(MessageHeader messageHeader1, MessageHeader messageHeader2) {
        return messageHeader1.getThreadingMessageID().compareTo( messageHeader2.getThreadingMessageID() );
    }
}
