package gg.duo.crew.service;

import gg.duo.common.exception.BusinessException;
import gg.duo.common.exception.ErrorCode;
import gg.duo.crew.domain.notice.HouseNotice;
import gg.duo.crew.domain.notice.HouseNoticeRepository;
import gg.duo.crew.dto.NoticeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final HouseNoticeRepository noticeRepository;
    private final HouseService houseService;

    @Transactional(readOnly = true)
    public List<NoticeDto.Response> list(Long houseId, Long userId) {
        houseService.requireApprovedMember(houseId, userId);
        return noticeRepository.findByHouseIdOrderByIsPinnedDescIdDesc(houseId).stream()
                .map(NoticeDto.Response::of)
                .toList();
    }

    /** 공지는 방장·부리더만 쓴다. */
    @Transactional
    public NoticeDto.Response create(Long houseId, Long userId, NoticeDto.WriteRequest req) {
        houseService.requireManagerOf(houseId, userId);
        HouseNotice notice = HouseNotice.builder()
                .houseId(houseId)
                .authorId(userId)
                .title(req.title())
                .content(req.content())
                .isPinned(req.pinned())
                .build();
        return NoticeDto.Response.of(noticeRepository.save(notice));
    }

    @Transactional
    public NoticeDto.Response pin(Long houseId, Long noticeId, Long userId, boolean pinned) {
        houseService.requireManagerOf(houseId, userId);
        HouseNotice notice = load(houseId, noticeId);
        notice.pin(pinned);
        return NoticeDto.Response.of(notice);
    }

    @Transactional
    public void delete(Long houseId, Long noticeId, Long userId) {
        houseService.requireManagerOf(houseId, userId);
        noticeRepository.delete(load(houseId, noticeId));
    }

    private HouseNotice load(Long houseId, Long noticeId) {
        HouseNotice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "공지를 찾을 수 없습니다."));
        if (!notice.getHouseId().equals(houseId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "공지를 찾을 수 없습니다.");
        }
        return notice;
    }
}
