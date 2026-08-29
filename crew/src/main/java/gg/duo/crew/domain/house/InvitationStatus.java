package gg.duo.crew.domain.house;

/**
 * House 초대 상태.
 *
 * 가입 신청(JoinStatus)과 초대는 서로 다른 흐름이므로 별도 상태로 관리한다.
 */
public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
