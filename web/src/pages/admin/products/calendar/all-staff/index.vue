<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AdminNavbar from '../../../../../components/admin/AdminNavbar.vue'
import AdminCalendarProductShell from '../../../../../components/admin/products/AdminCalendarProductShell.vue'
import { usePermission } from '../../../../../composables/usePermission'
import { getUserPage } from '../../../../../api/admin/user'
import {
  type CalAllStaffCalendar,
  type CalAllStaffScopeItem
} from '../../../../../api/admin/calProductSettings'
import { useCalProductSettingsStore } from '../../../../../stores/calProductSettings'
import { useDeptStore } from '../../../../../stores/dept'

const COLORS = ['#3B82F6', '#10B981', '#EC4899', '#F59E0B', '#8B5CF6', '#06B6D4', '#EF4444']

const toast = useToast()
const { hasPermission } = usePermission()
const store = useCalProductSettingsStore()
const deptStore = useDeptStore()

const canQuery = computed(() => hasPermission('calendar:product-settings:query'))
const canUpdate = computed(() => hasPermission('calendar:product-settings:update'))

const editorOpen = ref(false)
const submitting = ref(false)
const editingId = ref<string | null>(null)

const form = reactive({
  name: '',
  color: COLORS[0]!,
  description: '',
  scopeMode: 'ALL' as 'ALL' | 'CUSTOM',
  scopes: [] as CalAllStaffScopeItem[]
})

interface SubjectOption {
  label: string
  value: string
  kind: 'DEPT' | 'USER'
}

const userOptions = ref<SubjectOption[]>([])
const selectedSubject = ref<SubjectOption | undefined>()

const subjectItems = computed<SubjectOption[]>(() => [
  ...deptStore.list.map(d => ({ label: `部门 · ${d.name}`, value: d.id, kind: 'DEPT' as const })),
  ...userOptions.value
])

async function loadUsers(keyword: string) {
  try {
    const page = await getUserPage({ pageNo: 1, pageSize: 20, keyword: keyword || undefined })
    userOptions.value = (page.list ?? []).map(u => ({
      label: `成员 · ${u.nickname || u.username}`,
      value: String(u.id),
      kind: 'USER' as const
    }))
  } catch {
    userOptions.value = []
  }
}

async function load() {
  if (!canQuery.value) {
    return
  }
  try {
    await Promise.all([store.load(), deptStore.fetchList(), loadUsers('')])
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '加载失败',
      color: 'error'
    })
  }
}

function openCreate() {
  editingId.value = null
  form.name = ''
  form.color = COLORS[3]!
  form.description = ''
  form.scopeMode = 'ALL'
  form.scopes = []
  editorOpen.value = true
}

function openEdit(cal: CalAllStaffCalendar) {
  editingId.value = cal.id
  form.name = cal.name
  form.color = cal.color
  form.description = cal.description ?? ''
  const hasAll = cal.scopes.some(s => s.scopeType === 'ALL')
  form.scopeMode = hasAll ? 'ALL' : 'CUSTOM'
  form.scopes = hasAll
    ? []
    : cal.scopes.map(s => ({
        scopeType: s.scopeType,
        scopeId: s.scopeId != null ? String(s.scopeId) : null,
        scopeLabel: s.scopeLabel
      }))
  editorOpen.value = true
}

function addSubject() {
  if (!selectedSubject.value) {
    return
  }
  const s = selectedSubject.value
  if (form.scopes.some(x => x.scopeType === s.kind && String(x.scopeId) === s.value)) {
    return
  }
  form.scopes.push({
    scopeType: s.kind,
    scopeId: s.value,
    scopeLabel: s.label
  })
  selectedSubject.value = undefined
}

function removeScope(index: number) {
  form.scopes.splice(index, 1)
}

function buildScopes(): CalAllStaffScopeItem[] {
  if (form.scopeMode === 'ALL') {
    return [{ scopeType: 'ALL', scopeId: null }]
  }
  return form.scopes
}

async function saveEditor() {
  const name = form.name.trim()
  if (!name) {
    toast.add({ title: '请输入名称', color: 'error' })
    return
  }
  const scopes = buildScopes()
  if (form.scopeMode === 'CUSTOM' && scopes.length === 0) {
    toast.add({ title: '请至少选择一个可见范围', color: 'error' })
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await store.updateAllStaff({
        id: editingId.value,
        name,
        color: form.color,
        description: form.description,
        scopes
      })
    } else {
      await store.createAllStaff({
        name,
        color: form.color,
        description: form.description,
        scopes
      })
    }
    editorOpen.value = false
    toast.add({ title: '已保存', color: 'success' })
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '保存失败',
      color: 'error'
    })
  } finally {
    submitting.value = false
  }
}

async function remove(cal: CalAllStaffCalendar) {
  submitting.value = true
  try {
    await store.removeAllStaff(cal.id)
    toast.add({ title: '已删除', color: 'success' })
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '删除失败',
      color: 'error'
    })
  } finally {
    submitting.value = false
  }
}

function scopeSummary(cal: CalAllStaffCalendar): string {
  if (cal.scopes.some(s => s.scopeType === 'ALL')) {
    return '全员'
  }
  return cal.scopes.map(s => s.scopeLabel || `${s.scopeType}:${s.scopeId}`).join('、') || '—'
}

onMounted(() => {
  void load()
})
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-products-calendar-all-staff">
    <template #header>
      <AdminNavbar title="日历" />
    </template>
    <template #body>
      <div class="flex min-h-0 flex-1 flex-col p-6">
        <UAlert
          v-if="!canQuery"
          color="warning"
          title="无权限"
          description="需要 calendar:product-settings:query"
        />
        <AdminCalendarProductShell
          v-else
          title="全员日历"
          description="管理员创建后，可见范围内成员自动订阅；默认颜色映射到未设置本机色的成员。"
        >
          <template
            v-if="canUpdate"
            #actions
          >
            <UButton
              color="primary"
              icon="i-lucide-plus"
              @click="openCreate"
            >
              新建全员日历
            </UButton>
          </template>

          <UCard>
            <div
              v-if="store.loading && !store.settings.allStaffCalendars.length"
              class="py-8 text-center text-sm text-muted"
            >
              加载中…
            </div>
            <UEmpty
              v-else-if="!store.settings.allStaffCalendars.length"
              icon="i-lucide-calendar-range"
              title="暂无全员日历"
              description="创建后可用于放假安排、全员宣讲等组织级日程。"
            />
            <ul
              v-else
              class="divide-y divide-default"
            >
              <li
                v-for="cal in store.settings.allStaffCalendars"
                :key="cal.id"
                class="flex flex-wrap items-center gap-3 py-3 first:pt-0 last:pb-0"
              >
                <span
                  class="size-3 shrink-0 rounded-full"
                  :style="{ backgroundColor: cal.color }"
                />
                <div class="min-w-0 flex-1">
                  <p class="truncate font-medium">
                    {{ cal.name }}
                  </p>
                  <p class="truncate text-xs text-muted">
                    可见范围：{{ scopeSummary(cal) }}
                  </p>
                </div>
                <div
                  v-if="canUpdate"
                  class="flex gap-1"
                >
                  <UButton
                    size="xs"
                    color="neutral"
                    variant="ghost"
                    icon="i-lucide-pencil"
                    @click="openEdit(cal)"
                  />
                  <UButton
                    size="xs"
                    color="error"
                    variant="ghost"
                    icon="i-lucide-trash-2"
                    :loading="submitting"
                    @click="remove(cal)"
                  />
                </div>
              </li>
            </ul>
          </UCard>
        </AdminCalendarProductShell>
      </div>

      <UModal
        v-model:open="editorOpen"
        :title="editingId ? '编辑全员日历' : '新建全员日历'"
      >
        <template #body>
          <div class="space-y-4">
            <UFormField label="名称">
              <UInput v-model="form.name" placeholder="例如：公司假期" />
            </UFormField>
            <UFormField label="默认颜色">
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="c in COLORS"
                  :key="c"
                  type="button"
                  class="size-7 rounded-full ring-offset-2"
                  :class="form.color === c ? 'ring-2 ring-primary' : 'ring-1 ring-transparent'"
                  :style="{ backgroundColor: c }"
                  @click="form.color = c"
                />
              </div>
              <p class="mt-1 text-xs text-muted">
                自动映射到未设置本机色的订阅者
              </p>
            </UFormField>
            <UFormField label="描述">
              <UTextarea
                v-model="form.description"
                :rows="2"
                placeholder="暂无描述"
              />
            </UFormField>
            <UFormField label="可见范围">
              <div class="flex flex-wrap gap-2">
                <UButton
                  size="sm"
                  :color="form.scopeMode === 'ALL' ? 'primary' : 'neutral'"
                  :variant="form.scopeMode === 'ALL' ? 'solid' : 'soft'"
                  @click="form.scopeMode = 'ALL'"
                >
                  全员
                </UButton>
                <UButton
                  size="sm"
                  :color="form.scopeMode === 'CUSTOM' ? 'primary' : 'neutral'"
                  :variant="form.scopeMode === 'CUSTOM' ? 'solid' : 'soft'"
                  @click="form.scopeMode = 'CUSTOM'"
                >
                  指定部门/成员
                </UButton>
              </div>
              <div
                v-if="form.scopeMode === 'CUSTOM'"
                class="mt-3 space-y-2"
              >
                <div class="flex gap-2">
                  <USelectMenu
                    v-model="selectedSubject"
                    class="min-w-0 flex-1"
                    :items="subjectItems"
                    placeholder="搜索部门或成员"
                    by="value"
                  />
                  <UButton
                    color="neutral"
                    variant="soft"
                    @click="addSubject"
                  >
                    添加
                  </UButton>
                </div>
                <ul class="space-y-1">
                  <li
                    v-for="(s, idx) in form.scopes"
                    :key="`${s.scopeType}-${s.scopeId}`"
                    class="flex items-center justify-between rounded-md bg-elevated px-2 py-1.5 text-sm"
                  >
                    <span>{{ s.scopeLabel || `${s.scopeType}:${s.scopeId}` }}</span>
                    <UButton
                      size="xs"
                      color="neutral"
                      variant="ghost"
                      icon="i-lucide-x"
                      @click="removeScope(idx)"
                    />
                  </li>
                </ul>
              </div>
            </UFormField>
          </div>
        </template>
        <template #footer>
          <div class="flex justify-end gap-2">
            <UButton
              color="neutral"
              variant="ghost"
              @click="editorOpen = false"
            >
              取消
            </UButton>
            <UButton
              color="primary"
              :loading="submitting"
              @click="saveEditor"
            >
              保存
            </UButton>
          </div>
        </template>
      </UModal>
    </template>
  </UDashboardPanel>
</template>
