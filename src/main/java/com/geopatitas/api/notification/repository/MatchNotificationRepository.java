package com.geopatitas.api.notification.repository;

import com.geopatitas.api.notification.entity.MatchNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchNotificationRepository extends JpaRepository<MatchNotification, UUID> {

    List<MatchNotification> findByUserIdOrderByFechaCreacionDesc(UUID userId);

    int countByUserIdAndLeidaFalse(UUID userId);
}
