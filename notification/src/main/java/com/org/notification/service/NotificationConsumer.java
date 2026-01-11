package com.org.notification.service;

import com.org.notification.DTO.EmpruntEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class NotificationConsumer {

    private static final Logger logger = Logger.getLogger(NotificationConsumer.class.getName());

    @KafkaListener(topics = "emprunt-created", groupId = "notification-group")
    public void consumeEmpruntCreated(EmpruntEvent event) {
        logger.info("Notification: Emprunt créé - ID: " + event.getEmpruntId() +
                    ", User: " + event.getUserId() +
                    ", Book: " + event.getBookId() +
                    ", Timestamp: " + event.getTimestamp());
        // Simulation de notification par log/console
    }
}