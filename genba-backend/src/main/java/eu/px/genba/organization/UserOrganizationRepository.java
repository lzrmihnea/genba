package eu.px.genba.organization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserOrganizationRepository extends JpaRepository<UserOrganization, UUID> {

    @Query("""
            SELECT uo FROM UserOrganization uo
              JOIN FETCH uo.organization o
              JOIN FETCH uo.user u
            WHERE u.id = :userId AND o.deletedAt IS NULL AND u.deletedAt IS NULL
            """)
    List<UserOrganization> findActiveByUserId(UUID userId);

    @Query("""
            SELECT uo FROM UserOrganization uo
              JOIN FETCH uo.organization o
            WHERE uo.user.id = :userId AND uo.organization.id = :orgId
              AND o.deletedAt IS NULL
            """)
    Optional<UserOrganization> findByUserIdAndOrgId(UUID userId, UUID orgId);

    boolean existsByUserIdAndOrganizationId(UUID userId, UUID orgId);
}
