## ADDED Requirements

### Requirement: Browser-reachable LiveKit URL documentation

Deployment and RTC operations documentation SHALL state that `relayflow.rtc.livekit.url` / `RELAYFLOW_LIVEKIT_URL` is the WebSocket URL used **by browsers**, and MUST warn that `ws://127.0.0.1:7880` (or container-only hostnames) will fail when the client device is not the same machine as the LiveKit process. Docs MUST distinguish this from LiveKit `node-ip` / ICE advertisement settings. Docs MAY include a multi-device LAN example. This change MUST NOT require a paid cloud RTC provider.

#### Scenario: Multi-device warning is discoverable

- **WHEN** a developer opens `docs/dev/rtc-livekit.md` (or equivalent)
- **THEN** the doc explains that the configured LiveKit URL must be reachable from the participant browser
- **AND** distinguishes browser URL from server ICE/`node-ip` settings

### Requirement: Webhook wiring notes for Compose

Deployment documentation (and Compose comments where helpful) SHALL explain how to enable LiveKit webhooks to RelayFlow's `/app-api/rtc/webhook`, including that default `--dev` LiveKit may not deliver webhooks until configured, and that containers may need `host.docker.internal` or a host LAN IP to reach the Java process. V1 MUST NOT require webhook delivery for basic join/leave product correctness (meeting join/leave remains source of truth for `mtg_*`), but operators MUST be able to enable it for `rtc_*` telemetry.

#### Scenario: Webhook enablement is documented

- **WHEN** an operator wants LiveKit participant events to update `rtc_session`
- **THEN** docs describe auth, URL path, and a workable local networking pattern
- **AND** do not claim the default Compose `--dev` stack always delivers webhooks without extra config
