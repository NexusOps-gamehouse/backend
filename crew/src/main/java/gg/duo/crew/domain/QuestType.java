package gg.duo.crew.domain;

public enum QuestType {
    WIN_TOGETHER(7, 200, "House 공동 승리 7회"),
    PLAY_TOGETHER(5, 150, "House 멤버 2명 이상 함께 플레이 5회"),
    SCHEDULE_JOIN(3, 150, "House 일정 참여 완료 3회"),
    DAILY_ACTIVE(3, 150, "주간 서로 다른 3일 함께 플레이");

    private final int targetCount;
    private final int xpReward;
    private final String description;

    QuestType(int targetCount, int xpReward, String description) {
        this.targetCount = targetCount;
        this.xpReward = xpReward;
        this.description = description;
    }

    public int getTargetCount() { return targetCount; }
    public int getXpReward() { return xpReward; }
    public String getDescription() { return description; }
}