package gg.duo.crew.dto;

import gg.duo.crew.domain.HouseQuest;
import gg.duo.crew.domain.QuestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class HouseQuestResponseDto {

    private Long questId;
    private Boolean rewardClaimed;
    private Long rewardXp;
    private Long rewardHc;

    public static HouseQuestResponseDto from(HouseQuest houseQuest) {
        QuestType questType = houseQuest.getQuestType();

        return HouseQuestResponseDto.builder()
                .questId(houseQuest.getId())
                .rewardClaimed(houseQuest.isRewardClaimed())
                .rewardXp(questType != null ? questType.getRewardXp() : 0L)
                .rewardHc(questType != null ? questType.getRewardHc() : 0L)
                .build();
    }
}