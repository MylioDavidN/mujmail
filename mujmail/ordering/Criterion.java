package mujmail.ordering;

import mujmail.util.Functions;


/**
 * Represents different types of sorting email in mail foders.
 */
public final class Criterion {
    /** Sort mode. Sorts by time */ 
    public static final Criterion TIME = new Criterion(0, "time");
    /** Sort mode. Sorts by mail subject */ 
    public static final Criterion SUBJECT = new Criterion(1, "subject");
    /** Sort mode. Sort mails by recipient */ 
    public static final Criterion TO = new Criterion(2, "to");
    /** Sort mode. Sorts by mail sender */ 
    public static final Criterion FROM = new Criterion(3, "from");
    /** Sort mode. Sorts mails by size */ 
    public static final Criterion SIZE = new Criterion(4, "size");
    /** Sort mode. Sorts by mail position in persisten datapase position */ 
    public static final Criterion RECORD_ID = new Criterion(5, "recordID");
    /** Sort mode. Sorts by mail messageID mail header entry */ 
    public static final Criterion MESSAGE_ID = new Criterion(6, "messageID");
    /** Sort mode. Sort mails by internal threding identification */ 
    public static final Criterion THREADING_MESSAGE_ID = new Criterion(7, "threadingMessageID");

    /** Number of sorting modes */
    private static int COUNT = 8;
    private static int INT_PART = Integer.MAX_VALUE/COUNT - Integer.MIN_VALUE/COUNT;

    /** Identification of sorting mode */
    public final int ordinal;
    /** Human readable name of sorting mode */
    public final String name;

    /** 
     * Create sorting type object.
     * @param ordinal index of sotring type
     * @param name Sorting mode internal name
     */
    private Criterion(int ordinal, String name) {
        this.ordinal = ordinal;
        this.name = name;
    }

    /** 
     * For given sorting constant from {@link mujmail.util.Functions} SRT_HDR_ entries gives criterion object.
     *
     * @param criterion Header sorting mode from {@link mujmail.util.Functions}
     * @return Criterion for given sorting
     */
    public static Criterion valueOf( int criterion ) {
        switch (criterion) {
        case Functions.SRT_HDR_TIME: return TIME;
        case Functions.SRT_HDR_SUBJECT: return SUBJECT;
        case Functions.SRT_HDR_TO: return TO;
        case Functions.SRT_HDR_FROM: return FROM;
        case Functions.SRT_HDR_SIZE: return SIZE;
        case Functions.SRT_HDR_RECORD_ID: return RECORD_ID;
        case Functions.SRT_HDR_MSGID: return MESSAGE_ID;
        default:
            StringBuffer text = new StringBuffer("Unknown criterion for " + criterion + " expected on from [");
            text.append("Functions.SRT_HDR_TIME(").append(Functions.SRT_HDR_TIME)
                .append("), Functions.SRT_HDR_SUBJECT(").append(Functions.SRT_HDR_SUBJECT)
                .append("), Functions.SRT_HDR_TO(").append(Functions.SRT_HDR_TO)
                .append("), Functions.SRT_HDR_FROM(").append(Functions.SRT_HDR_FROM)
                .append("), Functions.SRT_HDR_SIZE(").append(Functions.SRT_HDR_SIZE)
                .append("), Functions.SRT_HDR_RECORD_ID(").append(Functions.SRT_HDR_RECORD_ID)
                .append("), Functions.SRT_HDR_MSGID(").append(Functions.SRT_HDR_MSGID)
                ;
            throw new IllegalArgumentException( text.toString() );
        }
    }
    /* **********************
     *    Object methods    *
     ************************/
    public String toString() {
        return name;
    }

    public boolean equals(Object obj) {
        return this == obj;
    }

    public int hashCode() {
        return Integer.MIN_VALUE + INT_PART * ordinal;
    }
}
