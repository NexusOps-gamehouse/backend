package gg.duo.crew.domain.house;

/** House 안에서의 역할. LEADER 는 House 당 한 명이다. */
public enum MemberRole {
    LEADER, SUB_LEADER, MEMBER;

    /** 가입 승인 · 역할 변경 · 강퇴 권한이 있는가. */
    public boolean canManage() {
        return this == LEADER || this == SUB_LEADER;
    }
}
