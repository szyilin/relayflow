-- Hard-cut: remove notify inbox write model (im-bot-notify-foundation).
-- Dev-period 0.x: no data migration; indexes/constraints drop with the table.

DROP TABLE IF EXISTS infra_notify;
