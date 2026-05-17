package com.geopatitas.api.notification.service;

import com.geopatitas.api.notification.entity.MatchNotification;
import com.geopatitas.api.notification.repository.MatchNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final MatchNotificationRepository notificationRepository;

    public NotificationService(MatchNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<MatchNotification> getMyNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByFechaCreacionDesc(userId);
    }

    public int getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndLeidaFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        MatchNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));
                
        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("No tienes permiso para ver esta notificación");
        }
        
        notification.setLeida(true);
        notificationRepository.save(notification);
    }
}
