## Decisions

1. Task search/dueRange Service returns `TaskItemDO`; Controller and ApiImpl convert separately.
2. IM introduces `ConversationListItem` internal model (not controller VO).
3. `CalCalendarService.listVisible()` owns owned+shared merge.
4. Extract only `CalEventRecurrenceSupport` this wave.
5. Skip UserServiceImpl split.

## Non-Goals

User god-service split, full calendar/im MapStruct, Outbox/MQ.
