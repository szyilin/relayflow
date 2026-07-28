package com.relayflow.module.agent.skill.task;

import com.relayflow.module.task.api.item.TaskItemApi;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class TaskSearchSkillTest {

    @Test
    void normalizeLimit_capsAndDefaults() {
        assertEquals(10, TaskSearchSkill.normalizeLimit(null, 10));
        assertEquals(10, TaskSearchSkill.normalizeLimit(0, 10));
        assertEquals(50, TaskSearchSkill.normalizeLimit(100, 10));
        assertEquals(7, TaskSearchSkill.normalizeLimit(7, 10));
    }

    @Test
    void descriptor_codeStable() {
        TaskItemApi api = mock(TaskItemApi.class);
        TaskSearchSkill skill = new TaskSearchSkill(api);
        assertEquals(TaskSearchSkill.CODE, skill.descriptor().getCode());
        assertEquals("task_search", skill.toolCallback().getToolDefinition().name());
    }
}
