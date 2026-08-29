package gg.duo.crew.domain.house;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HouseInvitationRepository
        extends JpaRepository<HouseInvitation, Long> {

    Optional<HouseInvitation> findByHouseIdAndInvitedUserId(
            Long houseId,
            Long invitedUserId
    );

    @EntityGraph(attributePaths = "house")
    List<HouseInvitation> findByInvitedUserIdAndStatusOrderByCreatedAtDesc(
            Long invitedUserId,
            InvitationStatus status
    );

    @EntityGraph(attributePaths = "house")
    @Query("""
            select i
            from HouseInvitation i
            where i.id = :id
            """)
    Optional<HouseInvitation> findByIdWithHouse(@Param("id") Long id);
}
