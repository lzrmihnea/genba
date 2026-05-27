package eu.px.genba.organization;

/**
 * Per-organization role carried by {@link UserOrganization#getOrgRole()}.
 *
 * <p>Distinct from the global {@code SUPER_ADMIN} which is not an org-scoped
 * concept and lives only in the {@code role} reference table for Layer 1+
 * platform-administration features.
 */
public enum OrgRole {
    OWNER,
    ADMIN,
    MEMBER,
    GUEST
}
