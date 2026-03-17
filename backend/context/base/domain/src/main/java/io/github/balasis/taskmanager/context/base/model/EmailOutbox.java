package io.github.balasis.taskmanager.context.base.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

// transactional outbox pattern for email. instead of sending emails inline
// during a request (which could fail and leave the transaction in a weird state)
// we insert a row here inside the same transaction as the business operation.
// the EmailQueueDrainer picks these up every 5 seconds and actually sends them.
// this way if the email service is down the business operation still succeeds
// and emails get retried later.
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "EmailOutbox", indexes = {
        @Index(name = "IX_EmailOutbox_status_createdAt", columnList = "status, createdAt"),
        @Index(name = "IX_EmailOutbox_sentAt", columnList = "sentAt")
})
public class EmailOutbox extends BaseModel {

    @Column(nullable = false, length = 320)
    private String toAddress;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String body;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant sentAt;

    @Column(nullable = false)
    private int retryCount;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "PENDING";
    }
}
