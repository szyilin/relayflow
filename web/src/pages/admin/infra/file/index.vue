<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'
import { useDirectUpload } from '../../../../composables/useDirectUpload'
import { usePermission } from '../../../../composables/usePermission'
import { useFileStore, type FileListRecord } from '../../../../stores/file'

const toast = useToast()
const fileStore = useFileStore()
const { hasPermission } = usePermission()
const { uploading, upload } = useDirectUpload()

const fileInputRef = ref<HTMLInputElement | null>(null)
const keywordInput = ref('')
const deleteOpen = ref(false)
const deleting = ref(false)
const deletingRecord = ref<FileListRecord | null>(null)

const canList = computed(() => hasPermission('infra:file:list'))
const canUpload = computed(() => hasPermission('infra:file:upload'))
const canDelete = computed(() => hasPermission('infra:file:delete'))

const columns: TableColumn<FileListRecord>[] = [{
  accessorKey: 'originalName',
  header: '文件名'
}, {
  accessorKey: 'mimeType',
  header: '类型'
}, {
  accessorKey: 'sizeLabel',
  header: '大小'
}, {
  accessorKey: 'accessLevel',
  header: '访问级别'
}, {
  accessorKey: 'provider',
  header: 'Provider'
}, {
  accessorKey: 'createTime',
  header: '上传时间'
}, {
  id: 'actions',
  header: '操作'
}]

async function loadPage(options?: { page?: number, keyword?: string }) {
  if (!canList.value) {
    return
  }

  try {
    await fileStore.fetchPage({
      pageNo: options?.page,
      keyword: options?.keyword
    })
  } catch {
    toast.add({
      title: '加载失败',
      description: fileStore.lastError ?? '无法获取文件列表',
      color: 'error'
    })
  }
}

function handleSearch() {
  loadPage({ page: 1, keyword: keywordInput.value.trim() })
}

function openFilePicker() {
  fileInputRef.value?.click()
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }

  try {
    await upload(file)
    toast.add({
      title: '上传成功',
      description: file.name,
      color: 'success'
    })
    await loadPage({ page: 1 })
  } catch (error) {
    toast.add({
      title: '上传失败',
      description: error instanceof Error ? error.message : '请稍后重试',
      color: 'error'
    })
  }
}

function openDelete(record: FileListRecord) {
  deletingRecord.value = record
  deleteOpen.value = true
}

async function confirmDelete() {
  if (!deletingRecord.value) {
    return
  }

  deleting.value = true
  try {
    await fileStore.remove(deletingRecord.value.id)
    deleteOpen.value = false
    deletingRecord.value = null
    toast.add({ title: '文件已删除', color: 'success' })
  } catch (error) {
    toast.add({
      title: '删除失败',
      description: error instanceof Error ? error.message : '操作失败',
      color: 'error'
    })
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  keywordInput.value = fileStore.keyword
  loadPage()
})

watch(() => fileStore.page, (page) => {
  loadPage({ page })
})
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-infra-file">
    <template #header>
      <AdminNavbar title="文件管理" />
    </template>

    <template #body>
      <div class="space-y-4">
        <AdminPageHeader
          title="文件列表"
          description="Presigned 直传至对象存储，元数据保存在 infra_file"
        >
          <template #actions>
            <input
              ref="fileInputRef"
              type="file"
              class="hidden"
              @change="handleFileChange"
            >
            <UButton
              v-if="canUpload"
              icon="i-lucide-upload"
              :loading="uploading"
              @click="openFilePicker"
            >
              上传文件
            </UButton>
          </template>
        </AdminPageHeader>

        <div v-if="!canList" class="max-w-5xl">
          <UCard>
            <UEmpty
              icon="i-lucide-shield-alert"
              title="无访问权限"
              description="需要 infra:file:list 权限"
            />
          </UCard>
        </div>

        <template v-else>
          <UCard>
            <div class="flex flex-wrap items-end gap-3">
              <UFormField label="文件名" class="min-w-56 flex-1">
                <UInput
                  v-model="keywordInput"
                  placeholder="搜索文件名"
                  icon="i-lucide-search"
                  @keydown.enter="handleSearch"
                />
              </UFormField>
              <UButton color="neutral" variant="soft" @click="handleSearch">
                搜索
              </UButton>
            </div>
          </UCard>

          <UCard>
            <div v-if="fileStore.loading" class="space-y-3">
              <USkeleton class="h-10 w-full" />
              <USkeleton class="h-10 w-full" />
              <USkeleton class="h-10 w-full" />
            </div>

            <UEmpty
              v-else-if="fileStore.list.length === 0"
              icon="i-lucide-folder-open"
              title="暂无文件"
              description="点击「上传文件」通过 Presigned URL 直传"
            />

            <div v-else class="space-y-4">
              <UTable :data="fileStore.list" :columns="columns" :loading="fileStore.loading">
                <template #originalName-cell="{ row }">
                  <div class="flex items-center gap-2">
                    <UIcon name="i-lucide-file" class="size-4 text-muted" />
                    <span class="font-medium">{{ row.original.originalName }}</span>
                  </div>
                </template>

                <template #accessLevel-cell="{ row }">
                  <UBadge
                    :color="row.original.accessLevel === '公开' ? 'success' : 'neutral'"
                    variant="subtle"
                  >
                    {{ row.original.accessLevel }}
                  </UBadge>
                </template>

                <template #actions-cell="{ row }">
                  <UButton
                    v-if="canDelete"
                    icon="i-lucide-trash-2"
                    color="error"
                    variant="ghost"
                    size="xs"
                    @click="openDelete(row.original)"
                  >
                    删除
                  </UButton>
                </template>
              </UTable>

              <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <p class="text-sm text-muted">
                  共 {{ fileStore.total }} 条
                </p>
                <UPagination
                  v-model:page="fileStore.page"
                  :total="fileStore.total"
                  :items-per-page="fileStore.pageSize"
                />
              </div>
            </div>
          </UCard>
        </template>
      </div>

      <UModal v-model:open="deleteOpen" title="删除文件">
        <template #body>
          <p class="text-sm text-muted">
            确定删除「{{ deletingRecord?.originalName }}」吗？此操作仅删除元数据记录，对象存储中的文件 V1 不会自动清理。
          </p>
        </template>

        <template #footer>
          <div class="flex justify-end gap-2">
            <UButton color="neutral" variant="ghost" @click="deleteOpen = false">
              取消
            </UButton>
            <UButton color="error" :loading="deleting" @click="confirmDelete">
              删除
            </UButton>
          </div>
        </template>
      </UModal>
    </template>
  </UDashboardPanel>
</template>
