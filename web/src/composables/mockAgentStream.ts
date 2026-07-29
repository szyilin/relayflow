/**
 * Temporary mock stream for workspace-agent-chat-ui-web.
 * Removed in workspace-agent-chat-integrate when real SSE lands.
 */

export type MockAgentStreamEvent =
  | { type: 'reasoning-delta'; text: string }
  | { type: 'reasoning-end' }
  | { type: 'tool-start'; toolName: string; input?: string }
  | { type: 'tool-result'; toolName: string; output: string }
  | { type: 'text-delta'; text: string }
  | { type: 'done' }

function sleep(ms: number, signal?: AbortSignal) {
  return new Promise<void>((resolve, reject) => {
    if (signal?.aborted) {
      reject(new DOMException('Aborted', 'AbortError'))
      return
    }
    const timer = setTimeout(() => resolve(), ms)
    signal?.addEventListener(
      'abort',
      () => {
        clearTimeout(timer)
        reject(new DOMException('Aborted', 'AbortError'))
      },
      { once: true }
    )
  })
}

async function emitChunks(
  text: string,
  chunkSize: number,
  delayMs: number,
  signal: AbortSignal | undefined,
  onChunk: (chunk: string) => void
) {
  for (let i = 0; i < text.length; i += chunkSize) {
    if (signal?.aborted) {
      throw new DOMException('Aborted', 'AbortError')
    }
    onChunk(text.slice(i, i + chunkSize))
    await sleep(delayMs, signal)
  }
}

/** Demo reasoning + tool while waiting for sync reply. */
export async function runMockAgentPreamble(
  onEvent: (event: MockAgentStreamEvent) => void,
  signal?: AbortSignal
) {
  const reasoning =
    '用户提出了一个工作相关问题。我应先理解意图，必要时调用任务相关工具，再给出可执行的简洁回答。'
  await emitChunks(reasoning, 12, 18, signal, (chunk) => {
    onEvent({ type: 'reasoning-delta', text: chunk })
  })
  onEvent({ type: 'reasoning-end' })

  onEvent({
    type: 'tool-start',
    toolName: 'task.search',
    input: '{"keyword":"相关任务"}'
  })
  await sleep(420, signal)
  onEvent({
    type: 'tool-result',
    toolName: 'task.search',
    output: '（演示）已检索任务列表，准备组织回复…'
  })
}

/** Stream final assistant markdown text in chunks. */
export async function runMockAgentTextStream(
  markdown: string,
  onEvent: (event: MockAgentStreamEvent) => void,
  signal?: AbortSignal
) {
  const body = markdown.trim() || '（空回复）'
  await emitChunks(body, 8, 16, signal, (chunk) => {
    onEvent({ type: 'text-delta', text: chunk })
  })
  onEvent({ type: 'done' })
}

/** Full offline demo when sync send is unavailable. */
export async function runMockAgentFullReply(
  userText: string,
  onEvent: (event: MockAgentStreamEvent) => void,
  signal?: AbortSignal
) {
  await runMockAgentPreamble(onEvent, signal)
  const markdown = [
    '## 收到',
    '',
    `你说：**${userText.trim()}**`,
    '',
    '这是 **Mock 流式** 演示（Reasoning / Tool / Markdown）。',
    '',
    '后续 `workspace-agent-chat-stream-api` 会改为真实 SSE。',
    '',
    '```ts',
    'await agent.sendMessage(input)',
    '```'
  ].join('\n')
  await runMockAgentTextStream(markdown, onEvent, signal)
}
