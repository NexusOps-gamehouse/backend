package gg.duo.crew.domain.house;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HouseRepository extends JpaRepository<House, Long> {

    /**
     * 목록 조회. members 를 함께 읽는다.
     *
     * fetch join 이 없으면 House 하나마다 members 를 다시 조회한다(N+1).
     * 목록 응답이 멤버 수를 보여주므로 어차피 전부 필요하다.
     * distinct 는 join 으로 늘어난 House 중복 행을 접는다.
     */
    @Query("SELECT DISTINCT h FROM House h LEFT JOIN FETCH h.members ORDER BY h.id DESC")
    List<House> findAllWithMembers();

    @Query("SELECT h FROM House h LEFT JOIN FETCH h.members WHERE h.id = :id")
    Optional<House> findByIdWithMembers(Long id);

    boolean existsByName(String name);
}
