package com.devlab.taskboard.scheduler;

import com.devlab.taskboard.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final NotificationService notificationService;
    private final int batchSize;

    public ReminderScheduler(NotificationService notificationService,
                             @Value("${app.reminder.batch-size:100}") int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("app.reminder.batch-size must be positive");
        }
        this.notificationService = notificationService;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.reminder.fixed-delay:PT1M}")
    public void sendDueReminders() {
        int total = 0;
        try {
            int created;
            do {
                created = notificationService.createDueReminders(batchSize);
                total += created;
            } while (created == batchSize);
        } catch (RuntimeException ex) {
            // Məs. optimistic lock (user eyni anda taskı yeniləyib): batch rollback olub, növbəti run təkrarlayır.
            log.warn("Reminder run failed after {} notification(s), will retry on next run", total, ex);
            return;
        }
        if (total > 0) {
            log.info("Created {} reminder notification(s)", total);
        }
    }
}
