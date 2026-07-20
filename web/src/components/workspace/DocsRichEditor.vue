<script setup lang="ts">
import { onBeforeUnmount, watch } from 'vue'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import TaskList from '@tiptap/extension-task-list'
import TaskItem from '@tiptap/extension-task-item'
import Link from '@tiptap/extension-link'
import Placeholder from '@tiptap/extension-placeholder'
import type { TipTapDocJson } from '../../api/app/docs'
import { emptyTipTapDoc } from '../../api/app/docs'

const props = defineProps<{
  modelValue: TipTapDocJson
  editable?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: TipTapDocJson]
}>()

const editor = useEditor({
  content: props.modelValue ?? emptyTipTapDoc(),
  editable: props.editable !== false,
  extensions: [
    StarterKit.configure({
      heading: { levels: [1, 2, 3] }
    }),
    TaskList,
    TaskItem.configure({ nested: true }),
    Link.configure({ openOnClick: false }),
    Placeholder.configure({ placeholder: '输入正文，或使用工具栏格式化…' })
  ],
  onUpdate: ({ editor: ed }) => {
    emit('update:modelValue', ed.getJSON() as TipTapDocJson)
  }
})

watch(
  () => props.modelValue,
  (next) => {
    if (!editor.value) {
      return
    }
    const current = JSON.stringify(editor.value.getJSON())
    const incoming = JSON.stringify(next ?? emptyTipTapDoc())
    if (current !== incoming) {
      editor.value.commands.setContent(next ?? emptyTipTapDoc(), { emitUpdate: false })
    }
  }
)

watch(
  () => props.editable,
  (editable) => {
    editor.value?.setEditable(editable !== false)
  }
)

onBeforeUnmount(() => {
  editor.value?.destroy()
})

function run(command: () => boolean) {
  command()
}
</script>

<template>
  <div class="docs-rich-editor flex min-h-0 flex-1 flex-col">
    <div
      v-if="editor"
      class="flex flex-wrap gap-1 border-b border-[var(--ws-border-subtle)] px-2 py-1.5"
    >
      <UButton
        size="xs"
        color="neutral"
        variant="ghost"
        label="H1"
        @click="run(() => editor!.chain().focus().toggleHeading({ level: 1 }).run())"
      />
      <UButton
        size="xs"
        color="neutral"
        variant="ghost"
        label="H2"
        @click="run(() => editor!.chain().focus().toggleHeading({ level: 2 }).run())"
      />
      <UButton
        size="xs"
        color="neutral"
        variant="ghost"
        label="H3"
        @click="run(() => editor!.chain().focus().toggleHeading({ level: 3 }).run())"
      />
      <UButton
        size="xs"
        color="neutral"
        variant="ghost"
        label="粗体"
        @click="run(() => editor!.chain().focus().toggleBold().run())"
      />
      <UButton
        size="xs"
        color="neutral"
        variant="ghost"
        label="斜体"
        @click="run(() => editor!.chain().focus().toggleItalic().run())"
      />
      <UButton
        size="xs"
        color="neutral"
        variant="ghost"
        label="列表"
        @click="run(() => editor!.chain().focus().toggleBulletList().run())"
      />
      <UButton
        size="xs"
        color="neutral"
        variant="ghost"
        label="编号"
        @click="run(() => editor!.chain().focus().toggleOrderedList().run())"
      />
      <UButton
        size="xs"
        color="neutral"
        variant="ghost"
        label="任务"
        @click="run(() => editor!.chain().focus().toggleTaskList().run())"
      />
      <UButton
        size="xs"
        color="neutral"
        variant="ghost"
        label="引用"
        @click="run(() => editor!.chain().focus().toggleBlockquote().run())"
      />
      <UButton
        size="xs"
        color="neutral"
        variant="ghost"
        label="代码"
        @click="run(() => editor!.chain().focus().toggleCodeBlock().run())"
      />
      <UButton
        size="xs"
        color="neutral"
        variant="ghost"
        label="分割线"
        @click="run(() => editor!.chain().focus().setHorizontalRule().run())"
      />
    </div>
    <EditorContent
      :editor="editor"
      class="docs-editor-content min-h-0 flex-1 overflow-y-auto px-6 py-4"
    />
  </div>
</template>

<style>
.docs-editor-content .tiptap {
  outline: none;
  min-height: 240px;
  color: var(--ws-text);
  line-height: 1.7;
}

.docs-editor-content .tiptap p.is-editor-empty:first-child::before {
  color: var(--ws-text-muted);
  content: attr(data-placeholder);
  float: left;
  height: 0;
  pointer-events: none;
}

.docs-editor-content .tiptap h1 {
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0.75rem 0 0.5rem;
}

.docs-editor-content .tiptap h2 {
  font-size: 1.35rem;
  font-weight: 650;
  margin: 0.65rem 0 0.4rem;
}

.docs-editor-content .tiptap h3 {
  font-size: 1.1rem;
  font-weight: 600;
  margin: 0.5rem 0 0.35rem;
}

.docs-editor-content .tiptap ul {
  list-style: disc;
  padding-left: 1.25rem;
}

.docs-editor-content .tiptap ol {
  list-style: decimal;
  padding-left: 1.25rem;
}

.docs-editor-content .tiptap ul[data-type='taskList'] {
  list-style: none;
  padding-left: 0;
}

.docs-editor-content .tiptap ul[data-type='taskList'] li {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
}

.docs-editor-content .tiptap blockquote {
  border-left: 3px solid var(--ws-border-subtle);
  margin: 0.5rem 0;
  padding-left: 0.75rem;
  color: var(--ws-text-muted);
}

.docs-editor-content .tiptap pre {
  background: var(--ws-canvas-bg, #f4f4f5);
  border-radius: 0.5rem;
  padding: 0.75rem 1rem;
  overflow-x: auto;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.875rem;
}

.docs-editor-content .tiptap hr {
  border: none;
  border-top: 1px solid var(--ws-border-subtle);
  margin: 1rem 0;
}

.docs-editor-content .tiptap a {
  color: var(--ui-primary);
  text-decoration: underline;
}
</style>
