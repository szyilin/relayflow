package com.relayflow.module.task.service.notify;

import com.relayflow.module.im.api.bot.dto.card.ImBotCardDocument;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds task-bot cards ({@code generic.v1}) with {@code open_url} deep links.
 */
public final class TaskBotCardFactory {

    public static final String TEMPLATE_GENERIC_V1 = "generic.v1";
    private static final DateTimeFormatter DUE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TaskBotCardFactory() {
    }

    public static ImBotCardDocument assigned(TaskItemDO task) {
        String title = safeTitle(task);
        String route = taskRoute(task.getId());

        ImBotCardDocument card = baseCard("c_task_assign_" + task.getId(), "任务指派", title);
        List<ImBotCardDocument.ImBotCardField> fields = new ArrayList<>();
        fields.add(field("任务", title));
        if (task.getDueTime() != null) {
            fields.add(field("截止", task.getDueTime().format(DUE_TIME_FORMAT)));
        }
        card.setFields(fields);
        card.setActions(List.of(viewAction(route)));
        card.setMeta(taskMeta(task.getId()));
        return card;
    }

    public static ImBotCardDocument dueSoon(TaskItemDO task) {
        String title = safeTitle(task);
        String route = taskRoute(task.getId());
        String due = task.getDueTime() != null ? task.getDueTime().format(DUE_TIME_FORMAT) : "—";

        ImBotCardDocument card = baseCard("c_task_due_" + task.getId(), "任务即将到期", title);
        card.setFields(List.of(
                field("任务", title),
                field("截止", due)));
        card.setActions(List.of(viewAction(route)));
        card.setMeta(taskMeta(task.getId()));
        return card;
    }

    private static ImBotCardDocument baseCard(String cardId, String headerTitle, String subtitle) {
        ImBotCardDocument card = new ImBotCardDocument();
        card.setSchema(1);
        card.setCardId(cardId);
        card.setTemplate(TEMPLATE_GENERIC_V1);
        ImBotCardDocument.ImBotCardHeader header = new ImBotCardDocument.ImBotCardHeader();
        header.setTitle(headerTitle);
        header.setSubtitle(subtitle);
        card.setHeader(header);
        return card;
    }

    private static ImBotCardDocument.ImBotCardField field(String label, String value) {
        ImBotCardDocument.ImBotCardField field = new ImBotCardDocument.ImBotCardField();
        field.setLabel(label);
        field.setValue(value);
        return field;
    }

    private static ImBotCardDocument.ImBotCardAction viewAction(String route) {
        ImBotCardDocument.ImBotCardAction action = new ImBotCardDocument.ImBotCardAction();
        action.setId("view");
        action.setLabel("查看任务");
        action.setStyle("primary");
        ImBotCardDocument.ImBotCardBehavior behavior = new ImBotCardDocument.ImBotCardBehavior();
        behavior.setType("open_url");
        behavior.setRoute(route);
        action.setBehavior(behavior);
        return action;
    }

    private static ImBotCardDocument.ImBotCardMeta taskMeta(Long taskId) {
        ImBotCardDocument.ImBotCardMeta meta = new ImBotCardDocument.ImBotCardMeta();
        meta.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30).toString());
        ImBotCardDocument.ImBotCardSource source = new ImBotCardDocument.ImBotCardSource();
        source.setDomain("task");
        source.setEntityType("task");
        source.setEntityId(String.valueOf(taskId));
        meta.setSource(source);
        return meta;
    }

    private static String safeTitle(TaskItemDO task) {
        return task != null && StringUtils.hasText(task.getTitle()) ? task.getTitle().trim() : "未命名任务";
    }

    private static String taskRoute(Long taskId) {
        return "/app/tasks?taskId=" + taskId;
    }
}
