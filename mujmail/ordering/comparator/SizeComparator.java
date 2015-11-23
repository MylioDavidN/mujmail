package mujmail.ordering.comparator;

import mujmail.MessageHeader;
import mujmail.ordering.Ordering;

/** 
 * Comparing messages by overall message size.
 */
public class SizeComparator extends MessageHeaderComparator {

    /** Creates comparator */
    public SizeComparator(final Ordering ordering) {
        super( ordering );
    }

    protected int compare(MessageHeader messageHeader1, MessageHeader messageHeader2) {
        return messageHeader1.getSize() - messageHeader2.getSize();
    }

}
