/**
 * Client-side TipTap JSON → Markdown for -web demo.
 * Integrate MUST prefer GET …/export?format=md.
 */
import type { TipTapDocJson, TipTapNodeJson } from '../../api/app/docs'

function marksToMd(text: string, marks?: TipTapNodeJson['marks']): string {
  if (!marks?.length) {
    return text
  }
  let out = text
  for (const mark of marks) {
    if (mark.type === 'bold') {
      out = `**${out}**`
    } else if (mark.type === 'italic') {
      out = `*${out}*`
    } else if (mark.type === 'code') {
      out = `\`${out}\``
    } else if (mark.type === 'link' && mark.attrs?.href) {
      out = `[${out}](${String(mark.attrs.href)})`
    }
  }
  return out
}

function inlineText(nodes?: TipTapNodeJson[]): string {
  if (!nodes?.length) {
    return ''
  }
  return nodes.map((n) => {
    if (n.type === 'text') {
      return marksToMd(n.text ?? '', n.marks)
    }
    if (n.type === 'hardBreak') {
      return '  \n'
    }
    return inlineText(n.content)
  }).join('')
}

function blockToMd(node: TipTapNodeJson, depth = 0): string {
  const indent = '  '.repeat(depth)
  switch (node.type) {
    case 'heading': {
      const level = Math.min(3, Math.max(1, Number(node.attrs?.level ?? 1)))
      return `${'#'.repeat(level)} ${inlineText(node.content)}\n\n`
    }
    case 'paragraph':
      return `${inlineText(node.content)}\n\n`
    case 'bulletList':
      return (node.content ?? []).map((item: TipTapNodeJson) => blockToMd(item, depth)).join('')
    case 'orderedList':
      return (node.content ?? []).map((item: TipTapNodeJson, i: number) => {
        const text = inlineText(item.content?.[0]?.content)
        return `${indent}${i + 1}. ${text}\n`
      }).join('') + '\n'
    case 'listItem': {
      const text = inlineText(node.content?.[0]?.content)
      return `${indent}- ${text}\n`
    }
    case 'taskList':
      return (node.content ?? []).map((item: TipTapNodeJson) => blockToMd(item, depth)).join('') + '\n'
    case 'taskItem': {
      const checked = Boolean(node.attrs?.checked)
      const text = inlineText(node.content?.[0]?.content)
      return `${indent}- [${checked ? 'x' : ' '}] ${text}\n`
    }
    case 'blockquote':
      return (node.content ?? [])
        .map((child: TipTapNodeJson) => blockToMd(child, depth).trimEnd().split('\n').map(l => `> ${l}`).join('\n'))
        .join('\n') + '\n\n'
    case 'codeBlock':
      return `\`\`\`\n${inlineText(node.content)}\n\`\`\`\n\n`
    case 'horizontalRule':
      return '---\n\n'
    default:
      return (node.content ?? []).map((c: TipTapNodeJson) => blockToMd(c, depth)).join('')
  }
}

export function tipTapJsonToMarkdown(doc: TipTapDocJson, title?: string): string {
  const body = (doc.content ?? []).map((n: TipTapNodeJson) => blockToMd(n)).join('').trimEnd()
  if (title?.trim()) {
    return `# ${title.trim()}\n\n${body}\n`
  }
  return `${body}\n`
}
