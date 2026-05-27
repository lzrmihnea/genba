package eu.px.genba.attachment;

/**
 * Storage shape of an attachment. Orthogonal to {@code source_channel},
 * which describes provenance (WhatsApp / email / PDF / ...).
 */
public enum AttachmentKind {
    /** Persisted to disk; {@code file_url} + {@code mime_type} populated. */
    FILE,
    /** Inline text (e.g. WhatsApp paste); {@code raw_text} populated. */
    TEXT,
    /** External URL (e.g. Google Drive link); {@code file_url} populated. */
    LINK
}
