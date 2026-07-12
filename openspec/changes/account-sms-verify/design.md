# 设计：手机号短信验证码（account-sms-verify）

## Context

- 注册 API：[`AuthRegisterServiceImpl`](../../../relayflow-module-system/relayflow-module-system-biz/src/main/java/com/relayflow/module/system/service/auth/AuthRegisterServiceImpl.java)
- Redis：Compose 已提供；用于验证码存储与限流
- 多租户：仅当 `tenant.enabled=true` 且 `allow-open-register=true` 时注册需验证码（可配置）

## Goals / Non-Goals

**Goals:**

- 可配置开关；dev 默认 `sms.mock=true` 在日志打印验证码
- 注册流程：发送验证码 → 用户填写 → 注册 API 校验
- 频率限制：同手机号 60s 内不可重发；每日上限（如 10 次）可配置

**Non-Goals:**

- 生产 SMS 厂商 SDK（仅 SPI）
- 登录 2FA、图形验证码
- 国际手机号区号（V1 默认大陆 11 位）

## D1：配置 `SmsProperties`

```yaml
relayflow:
  sms:
    enabled: false          # base / 自托管默认关闭
    mock: true              # dev profile true
    code-ttl: 5m
    resend-interval: 60s
    daily-limit-per-mobile: 10
```

| Profile | 建议 |
|---------|------|
| `application-dev.yml` | `enabled=true, mock=true` |
| `application.yml` | `enabled=false` |

`enabled=false` 时：注册不要求 `smsCode`；`send` API 返回 404 或 `SMS_DISABLED`。

## D2：SmsSender SPI

```java
public interface SmsSender {
    void send(String mobile, String scene, String code);
}
```

| 实现 | 说明 |
|------|------|
| `MockSmsSender` | `@ConditionalOnProperty(mock=true)`，日志 `INFO` 输出 |
| `DelegatingSmsSender` | `enabled=true && mock=false` 时抛 `UnsupportedOperationException` + 文档指引接厂商 |

## D3：SmsCodeService

```text
send(scene, mobile):
  1. 校验 mobile 格式
  2. Redis 检查 resend-interval
  3. 检查 daily-limit
  4. 生成 6 位数字码
  5. SET sms:code:{scene}:{mobile} = code, TTL
  6. SmsSender.send

verify(scene, mobile, code):
  1. GET Redis key
  2. 比对；成功则 DEL key（一次性）
```

**scene 枚举**：`register`、`reset_password`（V1 仅实现 register 前端）

## D4：API

| 端点 | 鉴权 | body |
|------|------|------|
| `POST /app-api/system/auth/sms/send` | permitAll | `{ mobile, scene }` |
| `POST /app-api/system/auth/register` | permitAll | 增加 `smsCode`（enabled 时必填） |

错误码：

| code | 含义 |
|------|------|
| `SMS_DISABLED` | 未启用 |
| `SMS_SEND_TOO_FREQUENT` | 60s 内重发 |
| `SMS_DAILY_LIMIT` | 日上限 |
| `SMS_CODE_INVALID` | 不匹配 |
| `SMS_CODE_EXPIRED` | 无 key |

## D5：前端 `/app/register`

```text
手机号 UInput
验证码 UInput + UButton「获取验证码」（倒计时 60s）
密码 / 确认密码 / 昵称 / 企业名称（不变）
```

- `enabled=false` 时隐藏验证码行（或后端忽略 `smsCode`）
- store：`sendRegisterSms(mobile)`、`register` payload 带 `smsCode`

## D6：安全

- 验证码仅数字 6 位；Redis 存储，不落库
- `send` 不区分手机号是否已注册（防枚举：统一返回成功文案「若号码有效将收到短信」— V1 可简化直接返回成功，mock 环境无所谓）
- 与 `org-member-invite-notify` 独立，不阻塞站内邀请

## 验证

```bash
openspec validate account-sms-verify --strict
./mvnw -pl relayflow-server -am compile
cd web && pnpm build
# dev：注册页发送 → 日志见验证码 → 填写注册成功
```

## 实施顺序（单 change 内）

```text
account-sms-verify-api   [平台] SmsCodeService + send API + register 校验
account-sms-verify-web   注册页 UI + contract
account-sms-verify-integrate  去 Mock、dev profile 冒烟
```
