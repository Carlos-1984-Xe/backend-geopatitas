package com.geopatitas.api.notification.service;

import com.geopatitas.api.notification.entity.MatchNotification;
import com.geopatitas.api.notification.repository.MatchNotificationRepository;
import com.geopatitas.api.notification.dto.NotificationDTO;
import com.geopatitas.api.notification.dto.PetSummaryDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final MatchNotificationRepository notificationRepository;

    public NotificationService(MatchNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<NotificationDTO> getMyNotifications(UUID userId) {
        List<MatchNotification> notifications = notificationRepository.findByUserIdOrderByFechaCreacionDesc(userId);
        return notifications.stream()
            .map(n -> new NotificationDTO(
                n.getId(),
                new PetSummaryDTO(n.getPetReportado().getId(), n.getPetReportado().getNombre(), n.getPetReportado().getEspecie()),
                new PetSummaryDTO(n.getPetCoincidencia().getId(), n.getPetCoincidencia().getNombre(), n.getPetCoincidencia().getEspecie()),
                n.getPorcentajeSimilitud(),
                n.getDistanciaKm(),
                n.getLeida(),
                n.getFechaCreacion()
            ))
            .collect(Collectors.toList());
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
