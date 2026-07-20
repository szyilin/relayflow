package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskItemGroupMoveReqVO {

    @NotNull
    private Long id;

    /** status | dueTime | assigneeId | custom:{fieldId} */
    @NotBlank
    private String fieldKey;

    /**
     * Target bucket value. Null or {@code __empty__} clears dueTime / assigneeId / custom field.
     * For status: TODO | IN_PROGRESS | DONE (required, cannot clear).
     * For dueTime: YYYY-MM-DD.
     * For assigneeId: user id string.
     * For custom:{fieldId}: option value_key.
     */
    private String value;

    /** Required when fieldKey is custom:{fieldId} (list-scoped EAV). */
    private Long listId;

    /** Optional: insert before this root task within the target status column. */
    private Long beforeId;
}
