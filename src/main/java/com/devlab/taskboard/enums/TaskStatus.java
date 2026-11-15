package com.devlab.taskboard.enums;

import java.util.EnumSet;
import java.util.Set;

public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    BLOCKED,
    DONE;

    /**
     * Bu statusdan keçmək olan statuslar.
     * BLOCKED birbaşa DONE ola bilməz (əvvəl blok açılmalıdır), DONE yalnız TODO-ya qayıdır.
     * Eyni statusa "keçid" də icazəli deyil.
     */
    public Set<TaskStatus> allowedTransitions() {
        return switch (this) {
            case TODO -> EnumSet.of(IN_PROGRESS, BLOCKED, DONE);
            case IN_PROGRESS -> EnumSet.of(TODO, BLOCKED, DONE);
            case BLOCKED -> EnumSet.of(TODO, IN_PROGRESS);
            case DONE -> EnumSet.of(TODO);
        };
    }

    public boolean canTransitionTo(TaskStatus target) {
        return target != null && allowedTransitions().contains(target);
    }
}
