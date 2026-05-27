package eu.px.genba.role;

/**
 * Canonical set of system-managed role codes seeded by Liquibase
 * ({@code db.changelog-002-role.xml}).
 *
 * <p>{@code SUPER_ADMIN} is global and applies across organizations — used
 * for platform administration in Layer 1+. The remaining four are
 * organization-scoped via {@code user_organization.org_role}.
 */
public enum RoleCode {
    SUPER_ADMIN,
    OWNER,
    ADMIN,
    MEMBER,
    GUEST
}
