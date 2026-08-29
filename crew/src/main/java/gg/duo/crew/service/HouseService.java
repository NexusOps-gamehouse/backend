package gg.duo.crew.service;

import gg.duo.common.exception.BusinessException;
import gg.duo.common.exception.ErrorCode;
import gg.duo.crew.domain.house.*;
import gg.duo.crew.dto.HouseDto;
import gg.duo.crew.event.publisher.CrewEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HouseService {

    private final HouseRepository houseRepository;
    private final HouseMemberRepository houseMemberRepository;
    private final CrewEventPublisher eventPublisher;
    private final JdbcTemplate jdbcTemplate;

    // ---------------------------------------------------------------- 조회

    @Transactional(readOnly = true)
    public List<HouseDto.Summary> list(Long viewerId) {
        return houseRepository.findAllWithMembers().stream()
                .map(h -> HouseDto.Summary.of(
                        h,
                        findMe(h, viewerId),
                        resolveNickname(h.getLeaderId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public HouseDto.Detail get(Long houseId, Long viewerId) {
        House house = loadWithMembers(houseId);
        return detailOf(house, findMe(house, viewerId));
    }

    /** 내가 속한(승인된) House 들. */
    @Transactional(readOnly = true)
    public List<HouseDto.Summary> myHouses(Long userId) {
        return houseMemberRepository
                .findByUserIdAndStatus(userId, JoinStatus.APPROVED)
                .stream()
                .map(HouseMember::getHouse)
                .map(h -> HouseDto.Summary.of(
                        h,
                        findMe(h, userId),
                        resolveNickname(h.getLeaderId())
                ))
                .toList();
    }

    // ---------------------------------------------------------------- 생성·수정

    @Transactional
    public HouseDto.Detail create(Long leaderId, HouseDto.WriteRequest req) {
        House house = House.builder()
                .name(req.name())
                .description(req.description())
                .type(req.type())
                .activityType(req.activityType())
                .representativeGame(req.representativeGame())
                .leaderId(leaderId)
                .maxMembers(req.maxMembers())
                .build();

        house.addMember(HouseMember.builder()
                .userId(leaderId)
                .role(MemberRole.LEADER)
                .status(JoinStatus.APPROVED)
                .build());

        houseRepository.save(house);
        eventPublisher.crewFormed(house);

        return detailOf(house, findMe(house, leaderId));
    }

    @Transactional
    public HouseDto.Detail update(
            Long houseId,
            Long requesterId,
            HouseDto.WriteRequest req
    ) {
        House house = loadWithMembers(houseId);
        requireLeader(house, requesterId);

        house.update(
                req.name(),
                req.description(),
                req.type(),
                req.activityType(),
                req.representativeGame(),
                req.maxMembers()
        );

        return detailOf(house, findMe(house, requesterId));
    }

    // ---------------------------------------------------------------- 가입

    /**
     * PUBLIC 은 바로 APPROVED,
     * PRIVATE 은 PENDING.
     */
    @Transactional
    public HouseDto.Detail apply(Long houseId, Long userId) {
        House house = loadWithMembers(houseId);
        HouseMember existing = findMe(house, userId);

        if (existing != null && existing.getStatus() != JoinStatus.REJECTED) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    existing.getStatus() == JoinStatus.APPROVED
                            ? "이미 가입한 House 입니다."
                            : "이미 신청한 House 입니다."
            );
        }

        JoinStatus initial = house.initialJoinStatus();

        if (initial == JoinStatus.APPROVED && house.isFull()) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "정원이 가득 찼습니다."
            );
        }

        if (existing != null) {
            existing.changeRole(MemberRole.MEMBER);

            if (initial == JoinStatus.APPROVED) {
                existing.approve();
            } else {
                existing.reapply();
            }
        } else {
            house.addMember(HouseMember.builder()
                    .userId(userId)
                    .role(MemberRole.MEMBER)
                    .status(initial)
                    .build());
        }

        return detailOf(house, findMe(house, userId));
    }

    /** 대기 중인 가입 신청 목록. */
    @Transactional(readOnly = true)
    public List<HouseDto.Member> pendingRequests(
            Long houseId,
            Long requesterId
    ) {
        House house = loadWithMembers(houseId);
        requireManager(house, requesterId);

        return houseMemberRepository
                .findByHouseIdAndStatus(houseId, JoinStatus.PENDING)
                .stream()
                .map(member -> HouseDto.Member.of(
                        member,
                        resolveNickname(member.getUserId())
                ))
                .toList();
    }

    @Transactional
    public HouseDto.Member approve(
            Long houseId,
            Long requesterId,
            Long targetUserId
    ) {
        House house = loadWithMembers(houseId);
        requireManager(house, requesterId);

        HouseMember target =
                requireMember(house, targetUserId, "신청 내역이 없습니다.");

        if (target.getStatus() == JoinStatus.APPROVED) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "이미 승인된 멤버입니다."
            );
        }

        if (house.isFull()) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "정원이 가득 찼습니다."
            );
        }

        target.approve();

        return HouseDto.Member.of(
                target,
                resolveNickname(target.getUserId())
        );
    }

    @Transactional
    public HouseDto.Member reject(
            Long houseId,
            Long requesterId,
            Long targetUserId
    ) {
        House house = loadWithMembers(houseId);
        requireManager(house, requesterId);

        HouseMember target =
                requireMember(house, targetUserId, "신청 내역이 없습니다.");

        if (target.getRole() == MemberRole.LEADER) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "방장은 거절할 수 없습니다."
            );
        }

        target.reject();

        return HouseDto.Member.of(
                target,
                resolveNickname(target.getUserId())
        );
    }

    // ---------------------------------------------------------------- 멤버 관리

    @Transactional
    public HouseDto.Member changeRole(
            Long houseId,
            Long requesterId,
            Long targetUserId,
            MemberRole role
    ) {
        House house = loadWithMembers(houseId);

        requireLeader(house, requesterId);

        if (role == null || role == MemberRole.LEADER) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "방장 위임은 이 API 로 할 수 없습니다."
            );
        }

        HouseMember target =
                requireMember(house, targetUserId, "멤버가 아닙니다.");

        if (target.getRole() == MemberRole.LEADER) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "방장의 역할은 바꿀 수 없습니다."
            );
        }

        target.changeRole(role);

        return HouseDto.Member.of(
                target,
                resolveNickname(target.getUserId())
        );
    }

    /** 강퇴, 또는 본인이 스스로 나가기. */
    @Transactional
    public void removeMember(
            Long houseId,
            Long requesterId,
            Long targetUserId
    ) {
        House house = loadWithMembers(houseId);

        boolean self = requesterId.equals(targetUserId);

        if (!self) {
            requireManager(house, requesterId);
        }

        HouseMember target =
                requireMember(house, targetUserId, "멤버가 아닙니다.");

        if (target.getRole() == MemberRole.LEADER) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    self
                            ? "방장은 House 를 떠날 수 없습니다. 먼저 방장을 위임하세요."
                            : "방장은 강퇴할 수 없습니다."
            );
        }

        if (!self
                && target.getRole() == MemberRole.SUB_LEADER
                && !house.getLeaderId().equals(requesterId)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "부리더는 방장만 내보낼 수 있습니다."
            );
        }

        house.removeMember(target);
    }

    // ---------------------------------------------------------------- 내부

    /**
     * House API 응답에 사용자 닉네임을 포함하기 위해
     * 인증된 userId 기준으로 user_svc의 닉네임을 조회한다.
     */
    private String resolveNickname(Long userId) {
        if (userId == null) {
            return null;
        }

        List<String> nicknames = jdbcTemplate.query(
                """
                SELECT nickname
                FROM user_svc.users
                WHERE id = ?
                """,
                (rs, rowNum) -> rs.getString("nickname"),
                userId
        );

        if (nicknames.isEmpty()) {
            return null;
        }

        String nickname = nicknames.get(0);

        if (nickname == null || nickname.isBlank()) {
            return null;
        }

        return nickname;
    }

    private HouseDto.Detail detailOf(
            House house,
            HouseMember me
    ) {
        return HouseDto.Detail.of(
                house,
                me,
                this::resolveNickname
        );
    }

    private House loadWithMembers(Long houseId) {
        return houseRepository.findByIdWithMembers(houseId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "House 를 찾을 수 없습니다."
                ));
    }

    private HouseMember findMe(House house, Long userId) {
        if (userId == null) {
            return null;
        }

        return house.getMembers().stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    private HouseMember requireMember(
            House house,
            Long userId,
            String message
    ) {
        HouseMember member = findMe(house, userId);

        if (member == null) {
            throw new BusinessException(
                    ErrorCode.NOT_FOUND,
                    message
            );
        }

        return member;
    }

    /** 가입 승인 · 강퇴 권한(방장 또는 부리더). */
    private void requireManager(House house, Long requesterId) {
        HouseMember me = findMe(house, requesterId);

        if (me == null
                || me.getStatus() != JoinStatus.APPROVED
                || !me.getRole().canManage()) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "권한이 없습니다."
            );
        }
    }

    private void requireLeader(House house, Long requesterId) {
        if (!house.getLeaderId().equals(requesterId)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "방장만 할 수 있습니다."
            );
        }
    }

    /** 다른 서비스(일정·공지·채팅)가 쓰는 멤버십 검사. */
    @Transactional(readOnly = true)
    public void requireApprovedMember(Long houseId, Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        HouseMember member = houseMemberRepository
                .findByHouseIdAndUserId(houseId, userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.FORBIDDEN,
                        "House 멤버가 아닙니다."
                ));

        if (member.getStatus() != JoinStatus.APPROVED) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "가입 승인 대기 중입니다."
            );
        }
    }

    /** 공지 작성 등 관리 권한이 필요한 자리에서 쓴다. */
    @Transactional(readOnly = true)
    public void requireManagerOf(Long houseId, Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        HouseMember member = houseMemberRepository
                .findByHouseIdAndUserId(houseId, userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.FORBIDDEN,
                        "권한이 없습니다."
                ));

        if (member.getStatus() != JoinStatus.APPROVED
                || !member.getRole().canManage()) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "권한이 없습니다."
            );
        }
    }
}
