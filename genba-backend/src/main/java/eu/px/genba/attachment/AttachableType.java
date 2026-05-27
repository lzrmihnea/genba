package eu.px.genba.attachment;

/**
 * Canonical {@code attachable_type} values. Kept as constants (not an enum)
 * so feature modules can register new types without depending on this enum.
 */
public final class AttachableType {

    public static final String PROJECT = "PROJECT";
    public static final String OFFER = "OFFER";
    public static final String OFFER_LINE = "OFFER_LINE";
    public static final String VENDOR = "VENDOR";

    private AttachableType() {
        // no instances
    }
}
