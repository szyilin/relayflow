<script setup lang="ts">
import { computed, onMounted, reactive, ref, useTemplateRef, watch } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'
import AdminGeoCascader from '../../../../components/admin/enterprise/AdminGeoCascader.vue'
import { usePermission } from '../../../../composables/usePermission'
import type {
  EnterpriseAddress,
  EnterpriseAddressStatus,
  EnterpriseAddressType,
  EnterpriseProfileUpdate
} from '../../../../api/admin/enterprise'
import {
  ADDRESS_TYPE_LABEL,
  ADDRESS_TYPE_OPTIONS,
  ENTERPRISE_INDUSTRY_OPTIONS,
  ENTERPRISE_SCALE_OPTIONS,
  useEnterpriseStore
} from '../../../../stores/enterprise'
import { resolveAvatarUrl } from '../../../../utils/avatar'

type EnterpriseTab = 'info' | 'address'

interface AddressTableRow extends EnterpriseAddress {
  typeLabels: string
}

const toast = useToast()
const enterpriseStore = useEnterpriseStore()
const { hasPermission } = usePermission()

const activeTab = ref<EnterpriseTab>('info')
const infoEditing = ref(false)

const addressFormOpen = ref(false)
const editingAddressId = ref<string | null>(null)

const addressFilters = reactive({
  keyword: '',
  types: [] as EnterpriseAddressType[],
  status: 'ALL' as EnterpriseAddressStatus | 'ALL'
})

const profileForm = reactive<EnterpriseProfileUpdate>({
  name: '',
  industry: null,
  scale: null,
  logoFileId: null,
  contactName: null,
  contactEmail: null,
  contactMobile: null,
  contactLandline: null
})

const logoInput = useTemplateRef<HTMLInputElement>('logoInput')

const addressForm = reactive({
  types: [] as EnterpriseAddressType[],
  regionId: null as string | null,
  regionPath: '',
  detail: ''
})

const canQuery = computed(() => hasPermission('system:enterprise:query'))
const canUpdate = computed(() => hasPermission('system:enterprise:update'))

const tabItems = [
  { label: '企业信息管理', value: 'info' as const },
  { label: '企业地址管理', value: 'address' as const }
]

const statusFilterItems = [
  { label: '全部状态', value: 'ALL' as const },
  { label: '启用', value: 'ENABLED' as const },
  { label: '停用', value: 'DISABLED' as const }
]

const selectedStatusFilter = computed({
  get: () => statusFilterItems.find(item => item.value === addressFilters.status)
    ?? statusFilterItems[0]!,
  set: (item: { label: string, value: EnterpriseAddressStatus | 'ALL' } | undefined) => {
    if (item) {
      addressFilters.status = item.value
    }
  }
})

const selectedAddressTypes = computed({
  get: () => ADDRESS_TYPE_OPTIONS.filter(item => addressFilters.types.includes(item.value)),
  set: (items: Array<{ label: string, value: EnterpriseAddressType }> | undefined | null) => {
    addressFilters.types = (items ?? []).map(item => item.value)
  }
})

const formAddressTypes = computed({
  get: () => ADDRESS_TYPE_OPTIONS.filter(item => addressForm.types.includes(item.value)),
  set: (items: Array<{ label: string, value: EnterpriseAddressType }> | undefined | null) => {
    addressForm.types = (items ?? []).map(item => item.value)
  }
})

const selectedIndustry = computed({
  get: () => ENTERPRISE_INDUSTRY_OPTIONS.find(item => item.value === profileForm.industry) ?? undefined,
  set: (item: { label: string, value: string } | undefined) => {
    profileForm.industry = item?.value ?? null
  }
})

const selectedScale = computed({
  get: () => ENTERPRISE_SCALE_OPTIONS.find(item => item.value === profileForm.scale) ?? undefined,
  set: (item: { label: string, value: string } | undefined) => {
    profileForm.scale = item?.value ?? null
  }
})

const addressModalTitle = computed(() =>
  editingAddressId.value ? '编辑企业地址' : '新增企业地址')

const enterpriseNameInitial = computed(() => {
  const name = (infoEditing.value ? profileForm.name : enterpriseStore.profile.name).trim()
  return name ? name.slice(0, 1) : '企'
})

const enterpriseLogoUrl = computed(() =>
  resolveAvatarUrl(enterpriseStore.profile.logoFileId))

const addressTableRows = computed<AddressTableRow[]>(() =>
  enterpriseStore.addresses.map(item => ({
    ...item,
    typeLabels: item.types.map(type => ADDRESS_TYPE_LABEL[type]).join('、')
  })))

function displayText(value: string | null | undefined): string {
  const text = value?.trim()
  return text ? text : '—'
}

const columns: TableColumn<AddressTableRow>[] = [{
  accessorKey: 'typeLabels',
  header: '地址类型'
}, {
  accessorKey: 'regionPath',
  header: '行政区划'
}, {
  accessorKey: 'detail',
  header: '详细地址'
}, {
  accessorKey: 'status',
  header: '状态'
}, {
  id: 'actions',
  header: '操作'
}]

function applyProfileFromStore() {
  const p = enterpriseStore.profile
  profileForm.name = p.name
  profileForm.industry = p.industry
  profileForm.scale = p.scale
  profileForm.logoFileId = p.logoFileId
  profileForm.contactName = p.contactName
  profileForm.contactEmail = p.contactEmail
  profileForm.contactMobile = p.contactMobile
  profileForm.contactLandline = p.contactLandline
}

function resetAddressForm() {
  addressForm.types = []
  addressForm.regionId = null
  addressForm.regionPath = ''
  addressForm.detail = ''
}

async function loadProfileTab() {
  if (!canQuery.value) {
    return
  }
  try {
    await enterpriseStore.loadProfile()
    applyProfileFromStore()
  } catch {
    toast.add({
      title: '加载失败',
      description: enterpriseStore.lastError ?? '无法加载企业信息',
      color: 'error'
    })
  }
}

async function loadAddressTab() {
  if (!canQuery.value) {
    return
  }
  try {
    await enterpriseStore.loadAddresses({
      keyword: addressFilters.keyword.trim() || undefined,
      types: addressFilters.types.length ? addressFilters.types : undefined,
      status: addressFilters.status
    })
  } catch {
    toast.add({
      title: '加载失败',
      description: enterpriseStore.lastError ?? '无法加载企业地址',
      color: 'error'
    })
  }
}

function startInfoEdit() {
  applyProfileFromStore()
  infoEditing.value = true
}

function cancelInfoEdit() {
  applyProfileFromStore()
  infoEditing.value = false
}

async function saveInfo() {
  if (!canUpdate.value) {
    toast.add({ title: '无保存权限', color: 'warning' })
    return
  }
  if (!profileForm.name.trim()) {
    toast.add({ title: '请填写企业名称', color: 'warning' })
    return
  }
  try {
    await enterpriseStore.saveProfile({
      ...profileForm,
      name: profileForm.name.trim(),
      logoFileId: enterpriseStore.profile.logoFileId
    })
    applyProfileFromStore()
    infoEditing.value = false
    toast.add({
      title: '已保存',
      description: '企业信息已更新',
      color: 'success'
    })
  } catch {
    toast.add({
      title: '保存失败',
      description: enterpriseStore.lastError ?? '请检查表单',
      color: 'error'
    })
  }
}

function pickLogo() {
  if (!canUpdate.value) {
    return
  }
  logoInput.value?.click()
}

async function onLogoSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !canUpdate.value) {
    return
  }
  if (!file.type.startsWith('image/')) {
    toast.add({ title: '请选择图片文件', color: 'warning' })
    return
  }
  try {
    await enterpriseStore.uploadLogo(file)
    applyProfileFromStore()
    toast.add({ title: '企业头像已更新', color: 'success' })
  } catch {
    toast.add({
      title: '上传失败',
      description: enterpriseStore.lastError ?? '请稍后重试',
      color: 'error'
    })
  }
}

function openCreateAddress() {
  editingAddressId.value = null
  resetAddressForm()
  addressFormOpen.value = true
}

function openEditAddress(row: EnterpriseAddress) {
  editingAddressId.value = row.id
  addressForm.types = [...row.types]
  addressForm.regionId = row.regionId
  addressForm.regionPath = row.regionPath
  addressForm.detail = row.detail
  addressFormOpen.value = true
}

async function submitAddressForm() {
  if (!canUpdate.value) {
    toast.add({ title: '无编辑权限', color: 'warning' })
    return
  }
  if (addressForm.types.length === 0) {
    toast.add({ title: '请选择地址类型', color: 'warning' })
    return
  }
  if (!addressForm.regionId) {
    toast.add({ title: '请选择完整行政区划', color: 'warning' })
    return
  }
  if (!addressForm.detail.trim()) {
    toast.add({ title: '请填写详细地址', color: 'warning' })
    return
  }

  const payload = {
    types: [...addressForm.types],
    regionId: addressForm.regionId,
    regionPath: addressForm.regionPath,
    detail: addressForm.detail.trim()
  }

  try {
    if (editingAddressId.value) {
      await enterpriseStore.saveAddress(editingAddressId.value, payload)
      toast.add({ title: '地址已更新', color: 'success' })
    } else {
      await enterpriseStore.createAddress(payload)
      toast.add({ title: '地址已新增', color: 'success' })
    }
    addressFormOpen.value = false
    await loadAddressTab()
  } catch {
    toast.add({
      title: '保存失败',
      description: enterpriseStore.lastError ?? '请检查表单',
      color: 'error'
    })
  }
}

async function toggleAddressStatus(row: EnterpriseAddress) {
  if (!canUpdate.value) {
    toast.add({ title: '无操作权限', color: 'warning' })
    return
  }
  const nextStatus: EnterpriseAddressStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    await enterpriseStore.toggleAddressStatus(row.id, nextStatus)
    toast.add({
      title: nextStatus === 'ENABLED' ? '已启用' : '已停用',
      color: 'success'
    })
    await loadAddressTab()
  } catch {
    toast.add({
      title: '操作失败',
      description: enterpriseStore.lastError ?? '请稍后重试',
      color: 'error'
    })
  }
}

watch(activeTab, (tab) => {
  if (tab === 'info') {
    void loadProfileTab()
  } else {
    void loadAddressTab()
  }
})

watch(
  () => [addressFilters.keyword, addressFilters.types, addressFilters.status] as const,
  () => {
    if (activeTab.value === 'address') {
      void loadAddressTab()
    }
  },
  { deep: true }
)

onMounted(() => {
  void loadProfileTab()
})
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel>
    <template #header>
      <AdminNavbar title="企业信息" />
    </template>

    <template #body>
      <div class="flex flex-col gap-6 p-6">
        <AdminPageHeader
          title="企业信息"
          description="维护当前租户的企业档案与办公地址；企业编号创建后不可修改"
        />

        <UAlert
          v-if="!canQuery"
          color="warning"
          title="无查看权限"
          description="需要 system:enterprise:query 权限"
        />

        <template v-else>
          <div class="space-y-4">
            <UTabs
              v-model="activeTab"
              :items="tabItems"
              class="w-full max-w-md"
            />

            <UCard v-if="activeTab === 'info'">
              <div
                v-if="enterpriseStore.profileLoading"
                class="space-y-6"
              >
                <USkeleton class="h-20 w-full" />
                <USkeleton class="h-28 w-full" />
                <USkeleton class="h-28 w-full" />
              </div>

              <div
                v-else
                class="space-y-8"
              >
                <!-- 基础信息 -->
                <section class="space-y-4">
                  <h3 class="flex items-center gap-2 text-sm font-semibold text-highlighted">
                    <span class="h-3.5 w-0.5 rounded-full bg-primary" />
                    基础信息
                  </h3>

                  <div class="flex flex-col gap-4 rounded-lg bg-elevated/60 p-4 ring-1 ring-default sm:flex-row sm:items-center">
                    <div class="relative shrink-0">
                      <button
                        type="button"
                        class="group relative size-14 overflow-hidden rounded-lg bg-primary/15 text-xl font-semibold text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                        :disabled="!canUpdate || enterpriseStore.logoUploading"
                        :title="canUpdate ? '更换企业头像' : undefined"
                        @click="pickLogo"
                      >
                        <UAvatar
                          v-if="enterpriseLogoUrl"
                          :src="enterpriseLogoUrl"
                          :alt="enterpriseStore.profile.name"
                          size="3xl"
                          class="size-14 rounded-lg"
                        />
                        <span
                          v-else
                          class="flex size-full items-center justify-center"
                        >
                          {{ enterpriseNameInitial }}
                        </span>
                        <span
                          v-if="canUpdate"
                          class="absolute inset-0 flex items-center justify-center bg-black/45 text-xs text-white opacity-0 transition-opacity group-hover:opacity-100"
                        >
                          {{ enterpriseStore.logoUploading ? '上传中' : '更换' }}
                        </span>
                      </button>
                      <input
                        ref="logoInput"
                        type="file"
                        accept="image/*"
                        class="hidden"
                        @change="onLogoSelected"
                      >
                    </div>

                    <div class="min-w-0 flex-1 space-y-3">
                      <div v-if="infoEditing && canUpdate">
                        <UFormField label="企业名称">
                          <UInput
                            v-model="profileForm.name"
                            placeholder="请输入企业名称"
                            class="w-full max-w-md"
                          />
                        </UFormField>
                      </div>
                      <div
                        v-else
                        class="truncate text-base font-semibold text-highlighted"
                      >
                        {{ displayText(enterpriseStore.profile.name) }}
                      </div>

                      <div class="space-y-1">
                        <p class="text-xs text-muted">
                          企业编号
                        </p>
                        <p class="font-mono text-sm text-default">
                          {{ displayText(enterpriseStore.profile.code) }}
                        </p>
                      </div>
                    </div>
                  </div>
                </section>

                <!-- 更多信息 -->
                <section class="space-y-4">
                  <div class="flex items-center justify-between gap-3">
                    <h3 class="flex items-center gap-2 text-sm font-semibold text-highlighted">
                      <span class="h-3.5 w-0.5 rounded-full bg-primary" />
                      更多信息
                    </h3>

                    <div class="flex items-center gap-2">
                      <template v-if="infoEditing">
                        <UButton
                          color="neutral"
                          variant="ghost"
                          size="sm"
                          @click="cancelInfoEdit"
                        >
                          取消
                        </UButton>
                        <UButton
                          v-if="canUpdate"
                          color="primary"
                          size="sm"
                          :loading="enterpriseStore.profileSaving"
                          @click="saveInfo"
                        >
                          保存
                        </UButton>
                      </template>
                      <UButton
                        v-else-if="canUpdate"
                        color="neutral"
                        variant="ghost"
                        size="sm"
                        icon="i-lucide-pencil"
                        @click="startInfoEdit"
                      >
                        编辑
                      </UButton>
                    </div>
                  </div>

                  <div
                    v-if="infoEditing && canUpdate"
                    class="grid gap-4 sm:grid-cols-2"
                  >
                    <UFormField label="所属行业">
                      <USelectMenu
                        v-model="selectedIndustry"
                        :items="[...ENTERPRISE_INDUSTRY_OPTIONS]"
                        placeholder="请选择行业"
                        class="w-full"
                      />
                    </UFormField>

                    <UFormField label="企业规模">
                      <USelectMenu
                        v-model="selectedScale"
                        :items="[...ENTERPRISE_SCALE_OPTIONS]"
                        placeholder="请选择规模"
                        class="w-full"
                      />
                    </UFormField>
                  </div>

                  <div
                    v-else
                    class="grid gap-x-8 gap-y-5 sm:grid-cols-2"
                  >
                    <div class="space-y-1">
                      <p class="text-xs text-muted">
                        所属行业
                      </p>
                      <p class="text-sm text-default">
                        {{ displayText(enterpriseStore.industryLabel) }}
                      </p>
                    </div>
                    <div class="space-y-1">
                      <p class="text-xs text-muted">
                        企业规模
                      </p>
                      <p class="text-sm text-default">
                        {{ displayText(enterpriseStore.scaleLabel) }}
                      </p>
                    </div>
                  </div>
                </section>

                <!-- 企业联系人 -->
                <section class="space-y-4">
                  <h3 class="flex items-center gap-2 text-sm font-semibold text-highlighted">
                    <span class="h-3.5 w-0.5 rounded-full bg-primary" />
                    企业联系人
                  </h3>

                  <div
                    v-if="infoEditing && canUpdate"
                    class="grid gap-4 sm:grid-cols-2"
                  >
                    <UFormField label="联系人姓名">
                      <UInput
                        :model-value="profileForm.contactName ?? ''"
                        placeholder="请输入联系人姓名"
                        class="w-full"
                        @update:model-value="profileForm.contactName = $event || null"
                      />
                    </UFormField>

                    <UFormField label="联系人邮箱">
                      <UInput
                        :model-value="profileForm.contactEmail ?? ''"
                        type="email"
                        placeholder="name@company.com"
                        class="w-full"
                        @update:model-value="profileForm.contactEmail = $event || null"
                      />
                    </UFormField>

                    <UFormField label="联系人手机">
                      <UInput
                        :model-value="profileForm.contactMobile ?? ''"
                        placeholder="11 位手机号"
                        class="w-full"
                        @update:model-value="profileForm.contactMobile = $event || null"
                      />
                    </UFormField>

                    <UFormField label="联系人座机">
                      <UInput
                        :model-value="profileForm.contactLandline ?? ''"
                        placeholder="区号-号码"
                        class="w-full"
                        @update:model-value="profileForm.contactLandline = $event || null"
                      />
                    </UFormField>
                  </div>

                  <div
                    v-else
                    class="grid gap-x-8 gap-y-5 sm:grid-cols-2"
                  >
                    <div class="space-y-1">
                      <p class="text-xs text-muted">
                        联系人姓名
                      </p>
                      <p class="text-sm text-default">
                        {{ displayText(enterpriseStore.profile.contactName) }}
                      </p>
                    </div>
                    <div class="space-y-1">
                      <p class="text-xs text-muted">
                        联系人邮箱
                      </p>
                      <p class="text-sm text-default">
                        {{ displayText(enterpriseStore.profile.contactEmail) }}
                      </p>
                    </div>
                    <div class="space-y-1">
                      <p class="text-xs text-muted">
                        联系人手机
                      </p>
                      <p class="text-sm text-default">
                        {{ displayText(enterpriseStore.profile.contactMobile) }}
                      </p>
                    </div>
                    <div class="space-y-1">
                      <p class="text-xs text-muted">
                        联系人座机
                      </p>
                      <p class="text-sm text-default">
                        {{ displayText(enterpriseStore.profile.contactLandline) }}
                      </p>
                    </div>
                  </div>
                </section>
              </div>
            </UCard>

            <UCard v-else>
              <div class="mb-4 flex flex-wrap items-end gap-3">
                <UFormField
                  label="搜索"
                  class="!w-auto"
                >
                  <UInput
                    v-model="addressFilters.keyword"
                    placeholder="区划或详细地址"
                    icon="i-lucide-search"
                    class="w-56"
                  />
                </UFormField>

                <UFormField
                  label="地址类型"
                  class="!w-auto"
                >
                  <USelectMenu
                    v-model="selectedAddressTypes"
                    :items="ADDRESS_TYPE_OPTIONS"
                    multiple
                    placeholder="全部类型"
                    class="w-56"
                  />
                </UFormField>

                <UFormField
                  label="状态"
                  class="!w-auto"
                >
                  <USelectMenu
                    v-model="selectedStatusFilter"
                    :items="statusFilterItems"
                    class="w-40"
                  />
                </UFormField>

                <UButton
                  v-if="canUpdate"
                  icon="i-lucide-plus"
                  color="primary"
                  class="ml-auto"
                  @click="openCreateAddress"
                >
                  新增地址
                </UButton>
              </div>

              <div
                v-if="enterpriseStore.addressesLoading && enterpriseStore.addresses.length === 0"
                class="space-y-2 py-4"
              >
                <USkeleton class="h-10 w-full" />
                <USkeleton class="h-10 w-full" />
              </div>

              <UEmpty
                v-else-if="enterpriseStore.addresses.length === 0"
                icon="i-lucide-map-pin"
                title="暂无企业地址"
                description="点击「新增地址」添加职位、面试或门店地址"
              />

              <UTable
                v-else
                :data="addressTableRows"
                :columns="columns"
                :loading="enterpriseStore.addressesLoading"
              >
                <template #typeLabels-cell="{ row }">
                  <div class="flex flex-wrap gap-1">
                    <UBadge
                      v-for="type in row.original.types"
                      :key="type"
                      color="primary"
                      variant="subtle"
                      size="sm"
                    >
                      {{ ADDRESS_TYPE_LABEL[type] }}
                    </UBadge>
                  </div>
                </template>

                <template #status-cell="{ row }">
                  <UBadge
                    :color="row.original.status === 'ENABLED' ? 'success' : 'neutral'"
                    variant="subtle"
                    size="sm"
                  >
                    {{ row.original.status === 'ENABLED' ? '启用' : '停用' }}
                  </UBadge>
                </template>

                <template #actions-cell="{ row }">
                  <div class="flex flex-wrap items-center gap-1">
                    <UButton
                      v-if="canUpdate"
                      size="xs"
                      color="neutral"
                      variant="ghost"
                      @click="openEditAddress(row.original)"
                    >
                      编辑
                    </UButton>
                    <UButton
                      v-if="canUpdate"
                      size="xs"
                      :color="row.original.status === 'ENABLED' ? 'warning' : 'success'"
                      variant="ghost"
                      @click="toggleAddressStatus(row.original)"
                    >
                      {{ row.original.status === 'ENABLED' ? '停用' : '启用' }}
                    </UButton>
                  </div>
                </template>
              </UTable>
            </UCard>
          </div>
        </template>
      </div>

      <UModal
        v-model:open="addressFormOpen"
        :title="addressModalTitle"
      >
        <template #body>
          <div class="space-y-4">
            <UFormField
              label="地址类型"
              required
            >
              <USelectMenu
                v-model="formAddressTypes"
                :items="ADDRESS_TYPE_OPTIONS"
                multiple
                placeholder="可多选：职位 / 面试 / 门店"
                class="w-full"
              />
            </UFormField>

            <UFormField
              label="行政区划"
              required
            >
              <AdminGeoCascader
                v-model="addressForm.regionId"
                v-model:region-path="addressForm.regionPath"
                :load-children="enterpriseStore.fetchGeoChildren"
                :build-region-path="enterpriseStore.buildRegionPath"
              />
            </UFormField>

            <UFormField
              label="详细地址"
              required
            >
              <UInput
                v-model="addressForm.detail"
                placeholder="街道、门牌号、楼层等"
                class="w-full"
              />
            </UFormField>
          </div>
        </template>

        <template #footer>
          <div class="flex justify-end gap-2">
            <UButton
              color="neutral"
              variant="ghost"
              @click="addressFormOpen = false"
            >
              取消
            </UButton>
            <UButton
              :loading="enterpriseStore.addressSaving"
              @click="submitAddressForm"
            >
              保存
            </UButton>
          </div>
        </template>
      </UModal>
    </template>
  </UDashboardPanel>
</template>
