<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import type { Editor } from '@tiptap/core'
import StarterKit from '@tiptap/starter-kit'
import TaskList from '@tiptap/extension-task-list'
import TaskItem from '@tiptap/extension-task-item'
import Placeholder from '@tiptap/extension-placeholder'
import type { TipTapDocJson } from '../../api/app/docs'
import { emptyTipTapDoc } from '../../api/app/docs'

/**
 * TipTap Vue 3: https://tiptap.dev/docs/editor/getting-started/install/vue3
 * Vue Boolean props default to false when omitted — always set editable default.
 */
const props = withDefaults(defineProps<{
  modelValue: TipTapDocJson
  editable?: boolean
}>(), {
  editable: true
})

const emit = defineEmits<{
  'update:modelValue': [value: TipTapDocJson]
}>()

const shellRef = ref<HTMLElement | null>(null)
const insertOpen = ref(false)
const gutterVisible = ref(false)
const gutterTop = ref(0)

type InsertItem = {
  key: string
  label: string
  icon: string
  run: (ed: Editor) => void
}

const basicInsertItems: InsertItem[] = [
  {
    key: 'h1',
    label: '一级标题',
    icon: 'i-lucide-heading-1',
    run: ed => ed.chain().focus().toggleHeading({ level: 1 }).run()
  },
  {
    key: 'h2',
    label: '二级标题',
    icon: 'i-lucide-heading-2',
    run: ed => ed.chain().focus().toggleHeading({ level: 2 }).run()
  },
  {
    key: 'h3',
    label: '三级标题',
    icon: 'i-lucide-heading-3',
    run: ed => ed.chain().focus().toggleHeading({ level: 3 }).run()
  },
  {
    key: 'ordered',
    label: '有序列表',
    icon: 'i-lucide-list-ordered',
    run: ed => ed.chain().focus().toggleOrderedList().run()
  },
  {
    key: 'bullet',
    label: '无序列表',
    icon: 'i-lucide-list',
    run: ed => ed.chain().focus().toggleBulletList().run()
  },
  {
    key: 'task',
    label: '任务列表',
    icon: 'i-lucide-square-check',
    run: ed => ed.chain().focus().toggleTaskList().run()
  },
  {
    key: 'code',
    label: '代码块',
    icon: 'i-lucide-code-xml',
    run: ed => ed.chain().focus().toggleCodeBlock().run()
  },
  {
    key: 'quote',
    label: '引用',
    icon: 'i-lucide-text-quote',
    run: ed => ed.chain().focus().toggleBlockquote().run()
  },
  {
    key: 'hr',
    label: '分割线',
    icon: 'i-lucide-minus',
    run: ed => ed.chain().focus().setHorizontalRule().run()
  }
]

function topLevelBlockPos(editor: Editor): number | null {
  const { $from } = editor.state.selection
  for (let depth = $from.depth; depth > 0; depth--) {
    if ($from.node(depth - 1).type.name === 'doc') {
      return $from.before(depth)
    }
  }
  return null
}

function updateGutter(ed: Editor) {
  if (!props.editable || !shellRef.value || insertOpen.value) {
    if (!insertOpen.value) {
      gutterVisible.value = false
    }
    return
  }

  const pos = topLevelBlockPos(ed)
  if (pos == null) {
    gutterVisible.value = false
    return
  }

  const blockDom = ed.view.nodeDOM(pos)
  if (!(blockDom instanceof HTMLElement)) {
    gutterVisible.value = false
    return
  }

  const shellRect = shellRef.value.getBoundingClientRect()
  const blockRect = blockDom.getBoundingClientRect()
  gutterTop.value = blockRect.top - shellRect.top + shellRef.value.scrollTop
  gutterVisible.value = true
}

const editor = useEditor({
  content: props.modelValue ?? emptyTipTapDoc(),
  editable: props.editable,
  extensions: [
    StarterKit.configure({
      heading: { levels: [1, 2, 3] },
      link: { openOnClick: false }
    }),
    TaskList,
    TaskItem.configure({ nested: true }),
    Placeholder.configure({
      placeholder: '输入正文，或点击左侧 + 插入内容'
    })
  ],
  editorProps: {
    attributes: {
      class: 'focus:outline-none'
    }
  },
  onUpdate: ({ editor: ed }) => {
    emit('update:modelValue', ed.getJSON() as TipTapDocJson)
    updateGutter(ed)
  },
  onSelectionUpdate: ({ editor: ed }) => {
    updateGutter(ed)
  },
  onFocus: ({ editor: ed }) => {
    updateGutter(ed)
  },
  onBlur: () => {
    if (!insertOpen.value) {
      // Keep gutter briefly so click on + still works; hide on next tick if focus left.
      window.setTimeout(() => {
        if (!insertOpen.value && editor.value && !editor.value.isFocused) {
          gutterVisible.value = false
        }
      }, 150)
    }
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
      void nextTick(() => {
        if (editor.value) {
          updateGutter(editor.value)
        }
      })
    }
  }
)

watch(
  () => props.editable,
  (editable) => {
    editor.value?.setEditable(editable)
  }
)

function onShellScroll() {
  if (editor.value) {
    updateGutter(editor.value)
  }
}

function onInsert(item: InsertItem) {
  if (!editor.value) {
    return
  }
  item.run(editor.value)
  insertOpen.value = false
  void nextTick(() => {
    if (editor.value) {
      updateGutter(editor.value)
    }
  })
}

const showGutter = computed(() => props.editable && (gutterVisible.value || insertOpen.value))
</script>

<template>
  <div
    ref="shellRef"
    class="docs-rich-editor relative min-h-0 flex-1 overflow-y-auto"
    @scroll="onShellScroll"
  >
    <UPopover
      v-model:open="insertOpen"
      :content="{ side: 'bottom', align: 'start', sideOffset: 6 }"
      :ui="{ content: 'p-0 overflow-hidden' }"
    >
      <button
        v-show="showGutter"
        type="button"
        class="docs-block-add"
        :style="{ top: `${gutterTop}px` }"
        aria-label="插入内容"
        @mousedown.prevent
      >
        <UIcon
          name="i-lucide-plus"
          class="size-4"
        />
      </button>

      <template #content>
        <div class="docs-insert-panel w-64 py-2">
          <div class="px-3 pb-1.5 text-xs font-medium text-[var(--ws-text-muted)]">
            基础
          </div>
          <div class="grid grid-cols-3 gap-0.5 px-2">
            <button
              v-for="item in basicInsertItems"
              :key="item.key"
              type="button"
              class="docs-insert-item"
              @click="onInsert(item)"
            >
              <UIcon
                :name="item.icon"
                class="size-5 text-[var(--ws-text-muted)]"
              />
              <span class="truncate">{{ item.label }}</span>
            </button>
          </div>
        </div>
      </template>
    </UPopover>

    <EditorContent
      v-if="editor"
      :editor="editor"
      class="docs-editor-content min-h-full px-10 py-4 pl-14 sm:px-12 sm:pl-16"
    />
  </div>
</template>

<style>
.docs-block-add {
  position: absolute;
  left: 0.75rem;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.5rem;
  height: 1.5rem;
  margin-top: 0.15rem;
  border-radius: 0.375rem;
  color: var(--ws-text-muted);
  background: color-mix(in oklab, var(--ws-text-muted) 10%, transparent);
  transition: background 0.15s ease, color 0.15s ease, opacity 0.15s ease;
}

.docs-block-add:hover,
.docs-block-add[data-state='open'] {
  color: var(--ws-text);
  background: color-mix(in oklab, var(--ws-text-muted) 18%, transparent);
}

.docs-insert-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.35rem;
  border-radius: 0.5rem;
  padding: 0.65rem 0.35rem;
  font-size: 0.7rem;
  line-height: 1.2;
  color: var(--ws-text);
  transition: background 0.12s ease;
}

.docs-insert-item:hover {
  background: color-mix(in oklab, var(--ws-text-muted) 10%, transparent);
}

.docs-editor-content .tiptap {
  outline: none;
  min-height: 320px;
  color: var(--ws-text);
  line-height: 1.75;
  caret-color: var(--ws-text);
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
  margin: 0.85rem 0 0.5rem;
  line-height: 1.35;
}

.docs-editor-content .tiptap h2 {
  font-size: 1.35rem;
  font-weight: 650;
  margin: 0.75rem 0 0.4rem;
  line-height: 1.4;
}

.docs-editor-content .tiptap h3 {
  font-size: 1.1rem;
  font-weight: 600;
  margin: 0.6rem 0 0.35rem;
  line-height: 1.45;
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
