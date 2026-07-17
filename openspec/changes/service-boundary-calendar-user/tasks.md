## 1. Task ApiImpl boundary

- [x] 1.1 Service search/dueRange return DO; Controller converts to VO; ApiImpl maps DO→DTO; extend TaskConvert

## 2. IM ApiImpl boundary

- [x] 2.1 Introduce ConversationListItem; rewire Service/Controller/ApiImpl; add ImConversationConvert

## 3. Calendar list

- [x] 3.1 Add listVisible + CalCalendarConvert; thin CalCalendarController

## 4. CalEvent recurrence extract

- [x] 4.1 Extract CalEventRecurrenceSupport; update CalEventServiceImpl + tests

## 5. Verify

- [x] 5.1 `./mvnw -pl relayflow-server -am compile`
