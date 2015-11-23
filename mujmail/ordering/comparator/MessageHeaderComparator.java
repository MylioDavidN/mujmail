package mujmail.ordering.comparator;

import mujmail.MessageHeader;
import mujmail.ordering.Comparator;
import mujmail.ordering.Ordering;

/**
 * Specialized verion of comparator, tha is instended for comparing masseges in boxes.
 * Check types and call abstract specialized compare method.
 */
abstract class MessageHeaderComparator implements Comparator {

    private final int sign;

    /**
     * Creates comparator that is intended for comparing MessageHeader objects.
     * @param ordering Natural of Nonnatural ordering type
     */
    protected MessageHeaderComparator( final Ordering ordering ) {
        if ( ordering == Ordering.NATURAL) {
            sign = +1;
        } else {
            sign = -1;
        }
    }

    public int compare(Object o1, Object o2) throws IllegalArgumentException {
        if ( o1 instanceof MessageHeader
             && o2 instanceof MessageHeader) {
            return sign * compare( (MessageHeader)o1, (MessageHeader)o2 );
        }
        final String MSG_HEADER_CLASS_NAME = MessageHeader.class.getName();
        throw new IllegalArgumentException(
            "Expected " + MSG_HEADER_CLASS_NAME
            + " and " + MSG_HEADER_CLASS_NAME
            + " got " + o1.getClass().getName()
            + " and " + o2.getClass().getName()
        );
    }

    /**
     * Method for comparing message headers in natural ordering.
     * 
     * @param messageHeader1 message header to be compared
     * @param messageHeader2 message header to compare with
     * @return <ul>
     *   <li>number < 0 if messageHeader1 < messageHeader2</li>
     *   <li>number == 0 if messageHeader1 == messageHeader2</li>
     *   <li>number > 0 if messageHeader1 > messageHeader2</li>
     * </ul>
     */
    protected abstract int compare(MessageHeader messageHeader1, MessageHeader messageHeader2);

    public String toString() {
        final StringBuffer buff = new StringBuffer();
        buff.append( this.getClass().getName() )
            .append( "[ordering=")
            .append( (sign==1)?Ordering.NATURAL:Ordering.UNNATURAL )
            .append( ']' );
        return buff.toString();
    }
}
