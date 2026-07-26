<script setup lang="ts">
/**
 * Date / time field: click anywhere opens the native picker.
 * Uses readonly so Chromium does not select「年/月/日」segments on focus.
 */
import { computed, useTemplateRef } from 'vue'

export type WsDateTimeInputType = 'date' | 'datetime-local' | 'time' | 'month' | 'week'

defineOptions({ inheritAttrs: false })

const model = defineModel<string>({ default: '' })

const props = withDefaults(defineProps<{
  type?: WsDateTimeInputType
  disabled?: boolean
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'
  class?: string
  ui?: Record<string, unknown>
}>(), {
  type: 'datetime-local'
})

const emit = defineEmits<{
  change: [event: Event]
  blur: [event: FocusEvent]
}>()

const inputRoot = useTemplateRef<{ inputRef?: HTMLInputElement | null }>('inputRoot')

const trailingIcon = computed(() =>
  props.type === 'time' ? 'i-lucide-clock' : 'i-lucide-calendar'
)

function nativeInput(): HTMLInputElement | null {
  return inputRoot.value?.inputRef ?? null
}

function clearSegmentSelection(el: HTMLInputElement) {
  try {
    // datetime-local segment highlight is not always a DOM Selection;
    // collapse caret / clear ranges when supported.
    el.setSelectionRange?.(el.value.length, el.value.length)
  } catch {
    // Some date types throw on setSelectionRange.
  }
  window.getSelection()?.removeAllRanges()
}

function openPicker(event: PointerEvent) {
  if (event.button !== 0) {
    return
  }
  const el = nativeInput()
  if (!el || el.disabled || props.disabled) {
    return
  }
  // Prevent default focus/select behavior on the value field.
  event.preventDefault()
  try {
    if (typeof el.showPicker === 'function') {
      el.showPicker()
    } else {
      el.focus({ preventScroll: true })
    }
  } catch {
    el.focus({ preventScroll: true })
  }
  clearSegmentSelection(el)
  requestAnimationFrame(() => clearSegmentSelection(el))
}

function onKeydown(event: KeyboardEvent) {
  if (event.key !== 'Enter' && event.key !== ' ' && event.key !== 'ArrowDown') {
    return
  }
  const el = nativeInput()
  if (!el || el.disabled || props.disabled) {
    return
  }
  event.preventDefault()
  try {
    el.showPicker?.()
  } catch {
    // ignore
  }
}
</script>

<template>
  <div
    class="ws-date-time-input min-w-0"
    :class="props.class"
    @pointerdown="openPicker"
  >
    <UInput
      ref="inputRoot"
      v-model="model"
      v-bind="$attrs"
      :type="type"
      :disabled="disabled"
      :size="size"
      :ui="ui"
      :trailing-icon="trailingIcon"
      readonly
      class="w-full cursor-pointer"
      @change="emit('change', $event)"
      @blur="emit('blur', $event)"
      @keydown="onKeydown"
    />
  </div>
</template>
