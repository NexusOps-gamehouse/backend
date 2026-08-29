package gg.duo.crew.dto;

import gg.duo.crew.domain.HouseQuest;
import gg.duo.crew.domain.QuestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HouseQuestResponseDto {

    private Long questId;
    private String title;
    private String description;
    private Integer currentProgress;
    private Integer targetProgress;
    private Boolean isCompleted;
    private Boolean rewardClaimed;
    private Long rewardXp;
    private Long rewardHc;

    public static HouseQuestResponseDto from(HouseQuest quest) {
        QuestType questType = quest.getQuestType();

        return HouseQuestResponseDto.builder()
                .questId(quest.getId())
                .title(questType != null ? questType.name() : "")
                .description(questType != null ? questType.name() : "")
                .currentProgress(quest.getCurrentCount()) // 👈 진행도 필드명 반영
                .targetProgress(questType != null ? questType.getTargetCount() : 10)
                .isCompleted(quest.isCompleted()) // 👈 getIsCompleted() 대신 isCompleted() 사용
                .rewardClaimed(quest.isRewardClaimed()) // 👈 getIsClaimed() 대신 isRewardClaimed() 사용
                .rewardXp(questType != null ? (long) questType.getRewardXp() : 0L)
                .rewardHc(questType != null ? (long) questType.getRewardHc() : 0L)
                .build();
    }
}