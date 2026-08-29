package gg.duo.crew.repository;

import gg.duo.crew.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByHouseIdAndItemId(Long houseId, Long itemId);

    List<Inventory> findAllByHouseId(Long houseId);

    Optional<Inventory> findByIdAndHouseId(Long id, Long houseId);
}