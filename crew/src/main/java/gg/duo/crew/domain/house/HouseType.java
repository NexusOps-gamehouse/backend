package gg.duo.crew.domain.house;

/**
 * House 공개 범위.
 *
 * PUBLIC  — 목록에 노출되고 신청하면 바로 가입된다.
 * PRIVATE — 목록에는 이름만 보이고, 리더/부리더 승인을 받아야 가입된다.
 */
public enum HouseType {
    PUBLIC, PRIVATE
}
