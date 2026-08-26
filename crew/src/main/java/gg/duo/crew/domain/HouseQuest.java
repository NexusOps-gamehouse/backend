package gg.duo.crew.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "house_quests")
@Getter
@NoArgsConstructor
public class HouseQuest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long houseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestType questType;

    private int currentCount = 0;

    private boolean isCompleted = false;

    private boolean isRewardClaimed = false;

    private LocalDateTime weekStartDate;

    public HouseQuest(Long houseId, QuestType questType, LocalDateTime weekStartDate) {
        this.houseId = houseId;
        this.questType = questType;
        this.weekStartDate = weekStartDate;
    }

    public void addProgress(int count) {
        if (this.isCompleted) return;
        this.currentCount += count;
        if (this.currentCount >= questType.getTargetCount()) {
            this.currentCount = questType.getTargetCount();
            this.isCompleted = true;
        }
    }

    public void claimReward() {
        this.isRewardClaimed = true;
    }
}