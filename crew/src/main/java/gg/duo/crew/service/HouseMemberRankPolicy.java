package gg.duo.crew.service;

/** 신규 회원의 활동 기반 일반 회원 승급 정책. */
public final class HouseMemberRankPolicy {

    private HouseMemberRankPolicy() {}

    public static boolean qualifiesForRegularMember(int gameCount, int chatCount) {
        return gameCount >= 5 || (gameCount >= 3 && chatCount >= 10);
    }
}
