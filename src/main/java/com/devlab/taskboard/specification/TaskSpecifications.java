package com.devlab.taskboard.specification;

import com.devlab.taskboard.dto.request.TaskFilter;
import com.devlab.taskboard.entity.Label;
import com.devlab.taskboard.entity.Task;
import com.devlab.taskboard.enums.TaskStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TaskSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private TaskSpecifications() {
    }

    /** Bütün filterləri birləşdirir. ownedBy həmişə tətbiq olunur. */
    public static Specification<Task> fromFilter(Long ownerId, TaskFilter filter) {
        List<Specification<Task>> specs = new ArrayList<>();
        specs.add(ownedBy(ownerId));

        if (filter != null) {
            if (filter.status() != null && !filter.status().isEmpty()) {
                specs.add(hasStatusIn(filter.status()));
            }
            if (filter.labelId() != null) {
                specs.add(hasLabel(filter.labelId()));
            }
            if (filter.deadlineFrom() != null) {
                specs.add(deadlineFrom(filter.deadlineFrom()));
            }
            if (filter.deadlineTo() != null) {
                specs.add(deadlineTo(filter.deadlineTo()));
            }
            if (filter.keyword() != null && !filter.keyword().isBlank()) {
                specs.add(keywordLike(filter.keyword()));
            }
        }

        return Specification.allOf(specs);
    }

    /** owner_id sütunu ilə müqayisə: User cədvəlinə join olunmur. */
    public static Specification<Task> ownedBy(Long ownerId) {
        return (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<Task> hasStatusIn(Set<TaskStatus> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    /**
     * EXISTS subquery: join etsəydik, pagination-da dublikat sətir və distinct problemi yaranardı.
     * Label-dəki @SQLRestriction sayəsində silinmiş label üzrə filter heç nə tapmır
     * (task_labels sətirləri qalsa belə).
     */
    public static Specification<Task> hasLabel(Long labelId) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Task> subTask = sub.from(Task.class);
            Join<Task, Label> label = subTask.join("labels");
            sub.select(subTask.get("id"))
                    .where(
                            cb.equal(subTask.get("id"), root.get("id")),
                            cb.equal(label.get("id"), labelId)
                    );
            return cb.exists(sub);
        };
    }

    /** Daxil olmaqla: deadline >= from. Deadline-ı olmayan tasklar düşmür. */
    public static Specification<Task> deadlineFrom(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("deadline"), from);
    }

    /** Daxil olmaqla: deadline <= to. Deadline-ı olmayan tasklar düşmür. */
    public static Specification<Task> deadlineTo(Instant to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("deadline"), to);
    }

    /**
     * title və ya description-da axtarış, hərf böyüklüyü nəzərə alınmır.
     * İstifadəçinin yazdığı %, _ və \ simvolları wildcard kimi yox, adi simvol kimi axtarılır.
     */
    public static Specification<Task> keywordLike(String keyword) {
        String pattern = "%" + escapeLike(keyword.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE),
                cb.like(cb.lower(root.get("description")), pattern, LIKE_ESCAPE)
        );
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
