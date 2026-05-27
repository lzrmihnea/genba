package eu.px.genba.security;

/**
 * Stored in the {@code type} claim so the filter can reject a refresh token
 * presented as a Bearer credential and vice versa.
 */
public enum JwtTokenType {
    ACCESS,
    REFRESH
}
