package eu.px.genba.compare;

import eu.px.genba.common.exception.GenbaException;

/** Domain errors for match-group operations, each carrying its HTTP status. */
public final class MatchGroupExceptions {

    private MatchGroupExceptions() {
    }

    /** A line is already a member of a different group within the project. */
    public static final class LineAlreadyGrouped extends GenbaException {
        public LineAlreadyGrouped() {
            super("compare.error.lineAlreadyGrouped", "LINE_ALREADY_GROUPED", 409);
        }
    }

    /** Fewer than two lines supplied to create a group. */
    public static final class TooFewLines extends GenbaException {
        public TooFewLines() {
            super("compare.error.tooFewLines", "TOO_FEW_LINES", 422);
        }
    }

    /** A line belongs to an Offer in a different Project than the group. */
    public static final class CrossProject extends GenbaException {
        public CrossProject() {
            super("compare.error.crossProject", "CROSS_PROJECT", 422);
        }
    }
}
