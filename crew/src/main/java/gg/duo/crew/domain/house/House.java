package gg.duo.crew.domain.house;

import gg.duo.common.exception.BusinessException;
import gg.duo.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "houses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class House {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private HouseType type;

    /**
     * 방장 — users.id.
     *
     * post.authorId 와 같은 이유로 @ManyToOne 이 아니다. users 는 user_svc 스키마에
     * 있고 duo_crew 계정에는 권한이 없어, JPA 연관을 걸면 조인 자체가 막힌다.
     * 닉네임 같은 표시용 정보가 필요하면 user 서비스를 호출해 묶음 조회한다.
     */
    @Column(name = "leader_id", nullable = false)
    private Long leaderId;

    /** 정원. 승인 시점에 이 값을 넘기면 거절한다. */
    @Column(nullable = false)
    private Integer maxMembers;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * cascade + orphanRemoval 로 House 가 멤버의 수명을 소유한다.
     * 멤버는 House 없이 존재할 수 없으므로 별도 리포지토리 저장을 쓰지 않는다.
     */
    @OneToMany(mappedBy = "house", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HouseMember> members = new ArrayList<>();

    @Builder
    public House(String name, String description, HouseType type, Long leaderId, Integer maxMembers) {
        this.name = name;
        this.description = description;
        this.type = type != null ? type : HouseType.PUBLIC;
        this.leaderId = leaderId;
        this.maxMembers = (maxMembers == null || maxMembers <= 0) ? 20 : maxMembers;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    /** 양방향 연관을 한 곳에서 맞춘다. 한쪽만 세팅하면 flush 후에야 어긋난 게 드러난다. */
    public void addMember(HouseMember member) {
        members.add(member);
        member.assignHouse(this);
    }

    public void removeMember(HouseMember member) {
        members.remove(member);
        member.assignHouse(null);
    }

    public void update(String name, String description, HouseType type, Integer maxMembers) {
        if (name != null && !name.isBlank()) this.name = name;
        if (description != null) this.description = description;
        if (type != null) this.type = type;
        if (maxMembers != null && maxMembers > 0) {
            if (maxMembers < approvedMemberCount()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT,
                        "이미 가입한 인원보다 적은 정원으로는 바꿀 수 없습니다.");
            }
            this.maxMembers = maxMembers;
        }
    }

    public long approvedMemberCount() {
        return members.stream().filter(m -> m.getStatus() == JoinStatus.APPROVED).count();
    }

    public boolean isFull() {
        return approvedMemberCount() >= maxMembers;
    }

    /** PUBLIC 은 신청 즉시 가입, PRIVATE 은 승인 대기. */
    public JoinStatus initialJoinStatus() {
        return type == HouseType.PUBLIC ? JoinStatus.APPROVED : JoinStatus.PENDING;
    }
}
