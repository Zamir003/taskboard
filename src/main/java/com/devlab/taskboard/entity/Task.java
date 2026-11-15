package com.devlab.taskboard.entity;

import com.devlab.taskboard.enums.TaskStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "tasks",
        indexes = {
                @Index(name = "idx_tasks_owner_status", columnList = "owner_id, status"),
                @Index(name = "idx_tasks_owner_deadline", columnList = "owner_id, deadline"),
                @Index(name = "idx_tasks_reminder_at", columnList = "reminder_at")
        }
)
@SQLRestriction("deleted_at IS NULL")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tasks_owner"))
    private User owner;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Column(name = "deadline")
    private Instant deadline;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "reminder_at")
    private Instant reminderAt;

    @Column(name = "reminder_sent", nullable = false)
    private boolean reminderSent;

    @ManyToMany
    @JoinTable(
            name = "task_labels",
            joinColumns = @JoinColumn(name = "task_id", foreignKey = @ForeignKey(name = "fk_task_labels_task")),
            inverseJoinColumns = @JoinColumn(name = "label_id", foreignKey = @ForeignKey(name = "fk_task_labels_label"))
    )
    private Set<Label> labels = new HashSet<>();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Task() {
    }

    public Task(User owner, String title) {
        this.owner = owner;
        this.title = title;
        this.status = TaskStatus.TODO;
        this.reminderSent = false;
    }

    // ---------------- Davranış metodları ----------------

    public void changeStatus(TaskStatus newStatus, Instant now) {
        this.status = newStatus;
        this.completedAt = (newStatus == TaskStatus.DONE) ? now : null;
    }

    public void setReminderAt(Instant reminderAt) {
        this.reminderAt = reminderAt;
        this.reminderSent = false;
    }

    public void markReminderSent() {
        this.reminderSent = true;
    }

    public void addLabel(Label label) {
        labels.add(label);
    }

    public void removeLabel(Label label) {
        labels.remove(label);
    }

    public void replaceLabels(Collection<Label> newLabels) {
        labels.clear();
        labels.addAll(newLabels);
    }

    public void markDeleted(Instant now) {
        this.deletedAt = now;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    // ---------------- Getter / setter ----------------

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getReminderAt() {
        return reminderAt;
    }

    public boolean isReminderSent() {
        return reminderSent;
    }

    /** Dəyişdirilə bilməyən görünüş: label-lər yalnız add/remove/replaceLabels ilə dəyişir. */
    public Set<Label> getLabels() {
        return Collections.unmodifiableSet(labels);
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
