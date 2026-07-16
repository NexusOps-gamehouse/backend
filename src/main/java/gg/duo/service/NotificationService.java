package gg.duo.service;

import gg.duo.dto.NotificationDto;
import gg.duo.entity.Notification;
import gg.duo.entity.User;
import gg.duo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void notify(User recipient, String message, String link) {
        Notification n = new Notification();
        n.setUser(recipient);
        n.setMessage(message);
        n.setLink(link);
        notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(Long userId) {
        List<NotificationDto> items = notificationRepository
                .findTop30ByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(NotificationDto::from).toList();
        long unread = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return Map.of("items", items, "unreadCount", unread);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.findByUserIdAndIsReadFalse(userId)
                .forEach(n -> n.setRead(true));
    }
}
