import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getGeoChildren, type GeoRegionNode } from '../api/admin/geo'
import {
  createEnterpriseAddress,
  getEnterpriseProfile,
  listEnterpriseAddresses,
  updateEnterpriseAddress,
  updateEnterpriseAddressStatus,
  updateEnterpriseProfile,
  type EnterpriseAddress,
  type EnterpriseAddressCreate,
  type EnterpriseAddressListQuery,
  type EnterpriseAddressStatus,
  type EnterpriseAddressType,
  type EnterpriseAddressUpdate,
  type EnterpriseProfile,
  type EnterpriseProfileUpdate
} from '../api/admin/enterprise'
import { ApiError } from '../api/request'

export const ENTERPRISE_INDUSTRY_OPTIONS = [
  { label: '互联网 / IT', value: 'INTERNET_IT' },
  { label: '金融', value: 'FINANCE' },
  { label: '制造业', value: 'MANUFACTURING' },
  { label: '零售 / 消费', value: 'RETAIL' },
  { label: '教育', value: 'EDUCATION' },
  { label: '医疗健康', value: 'HEALTHCARE' },
  { label: '其他', value: 'OTHER' }
] as const

export const ENTERPRISE_SCALE_OPTIONS = [
  { label: '1-9 人', value: 'SIZE_1_9' },
  { label: '10-49 人', value: 'SIZE_10_49' },
  { label: '50-199 人', value: 'SIZE_50_199' },
  { label: '200-499 人', value: 'SIZE_200_499' },
  { label: '500-999 人', value: 'SIZE_500_999' },
  { label: '1000 人以上', value: 'SIZE_1000_PLUS' }
] as const

export const ADDRESS_TYPE_OPTIONS = [
  { label: '职位地址', value: 'JOB' as const },
  { label: '面试地址', value: 'INTERVIEW' as const },
  { label: '门店地址', value: 'STORE' as const }
]

export const ADDRESS_TYPE_LABEL: Record<EnterpriseAddressType, string> = {
  JOB: '职位地址',
  INTERVIEW: '面试地址',
  STORE: '门店地址'
}

const DRAFT_PROFILE: EnterpriseProfile = {
  code: 'ENT20250728001',
  name: 'RelayFlow 演示企业',
  industry: 'INTERNET_IT',
  scale: 'SIZE_10_49',
  dataResidency: '中国大陆',
  contactName: '张经理',
  contactEmail: 'contact@example.com',
  contactMobile: '13800138000',
  contactLandline: '010-88886666'
}

const DRAFT_ADDRESSES: EnterpriseAddress[] = [
  {
    id: 'addr_draft_1',
    types: ['JOB', 'INTERVIEW'],
    regionId: '440305',
    regionPath: '中国 / 广东省 / 深圳市 / 南山区',
    detail: '科技园南路 88 号 A 座 12 层',
    status: 'ENABLED',
    createTime: '2026-07-20T08:00:00Z'
  },
  {
    id: 'addr_draft_2',
    types: ['STORE'],
    regionId: '330106',
    regionPath: '中国 / 浙江省 / 杭州市 / 西湖区',
    detail: '文三路 199 号',
    status: 'DISABLED',
    createTime: '2026-07-18T08:00:00Z'
  }
]

/** `-web` draft geo when API unavailable or missing infra:geo:query. */
const DRAFT_GEO: GeoRegionNode[] = [
  { id: '1', code: 'CN', name: '中国', level: 'country_region', parentId: null, hasChildren: true },
  { id: '110000', code: '110000', name: '北京市', level: 'province', parentId: '1', hasChildren: true },
  { id: '440000', code: '440000', name: '广东省', level: 'province', parentId: '1', hasChildren: true },
  { id: '330000', code: '330000', name: '浙江省', level: 'province', parentId: '1', hasChildren: true },
  { id: '110100', code: '110100', name: '北京市', level: 'city', parentId: '110000', hasChildren: true },
  { id: '440300', code: '440300', name: '深圳市', level: 'city', parentId: '440000', hasChildren: true },
  { id: '440100', code: '440100', name: '广州市', level: 'city', parentId: '440000', hasChildren: true },
  { id: '330100', code: '330100', name: '杭州市', level: 'city', parentId: '330000', hasChildren: true },
  { id: '110101', code: '110101', name: '东城区', level: 'district', parentId: '110100', hasChildren: false },
  { id: '110105', code: '110105', name: '朝阳区', level: 'district', parentId: '110100', hasChildren: false },
  { id: '440305', code: '440305', name: '南山区', level: 'district', parentId: '440300', hasChildren: false },
  { id: '440304', code: '440304', name: '福田区', level: 'district', parentId: '440300', hasChildren: false },
  { id: '330106', code: '330106', name: '西湖区', level: 'district', parentId: '330100', hasChildren: false }
]

function cloneProfile(source: EnterpriseProfile): EnterpriseProfile {
  return { ...source }
}

function cloneAddress(source: EnterpriseAddress): EnterpriseAddress {
  return {
    ...source,
    types: [...source.types]
  }
}

function cloneAddresses(source: EnterpriseAddress[]): EnterpriseAddress[] {
  return source.map(cloneAddress)
}

function nextDraftAddressId(): string {
  return `addr_draft_${Date.now()}`
}

function isApiUnavailable(error: unknown): boolean {
  if (error instanceof ApiError) {
    return error.code === 404 || error.code >= 500
  }
  return true
}

function draftGeoChildren(parentId?: string | null): GeoRegionNode[] {
  const normalized = !parentId || parentId === '0' ? null : parentId
  return DRAFT_GEO.filter(node => node.parentId === normalized)
}

function labelOf<T extends { label: string, value: string }>(
  options: readonly T[],
  value: string | null | undefined
): string {
  if (!value) {
    return '—'
  }
  return options.find(item => item.value === value)?.label ?? value
}

export const useEnterpriseStore = defineStore('enterprise', () => {
  const profile = ref<EnterpriseProfile>(cloneProfile(DRAFT_PROFILE))
  const addresses = ref<EnterpriseAddress[]>(cloneAddresses(DRAFT_ADDRESSES))
  const profileLoading = ref(false)
  const addressesLoading = ref(false)
  const profileSaving = ref(false)
  const addressSaving = ref(false)
  const lastError = ref<string | null>(null)
  const profileHydrated = ref(false)
  const addressesHydrated = ref(false)
  const usingLocalDraft = ref(false)
  const geoUsesDraft = ref(false)

  const industryLabel = computed(() =>
    labelOf(ENTERPRISE_INDUSTRY_OPTIONS, profile.value.industry))
  const scaleLabel = computed(() =>
    labelOf(ENTERPRISE_SCALE_OPTIONS, profile.value.scale))

  async function loadProfile() {
    profileLoading.value = true
    lastError.value = null
    try {
      profile.value = cloneProfile(await getEnterpriseProfile())
      profileHydrated.value = true
      usingLocalDraft.value = false
    } catch (error) {
      if (!profileHydrated.value && isApiUnavailable(error)) {
        profile.value = cloneProfile(DRAFT_PROFILE)
        profileHydrated.value = true
        usingLocalDraft.value = true
        return
      }
      lastError.value = error instanceof ApiError || error instanceof Error
        ? error.message
        : '加载企业信息失败'
      throw error
    } finally {
      profileLoading.value = false
    }
  }

  async function saveProfile(payload: EnterpriseProfileUpdate) {
    profileSaving.value = true
    lastError.value = null
    try {
      if (usingLocalDraft.value) {
        profile.value = cloneProfile({
          ...profile.value,
          ...payload
        })
        return cloneProfile(profile.value)
      }
      const data = await updateEnterpriseProfile(payload)
      profile.value = cloneProfile(data)
      return data
    } catch (error) {
      if (isApiUnavailable(error)) {
        usingLocalDraft.value = true
        profile.value = cloneProfile({
          ...profile.value,
          ...payload
        })
        return cloneProfile(profile.value)
      }
      lastError.value = error instanceof ApiError || error instanceof Error
        ? error.message
        : '保存企业信息失败'
      throw error
    } finally {
      profileSaving.value = false
    }
  }

  async function loadAddresses(query?: EnterpriseAddressListQuery) {
    addressesLoading.value = true
    lastError.value = null
    try {
      const rows = await listEnterpriseAddresses(query)
      addresses.value = cloneAddresses(rows)
      addressesHydrated.value = true
      if (usingLocalDraft.value) {
        usingLocalDraft.value = false
      }
    } catch (error) {
      if (!addressesHydrated.value && isApiUnavailable(error)) {
        addresses.value = filterDraftAddresses(query)
        addressesHydrated.value = true
        usingLocalDraft.value = true
        return
      }
      if (usingLocalDraft.value) {
        addresses.value = filterDraftAddresses(query)
        return
      }
      lastError.value = error instanceof ApiError || error instanceof Error
        ? error.message
        : '加载企业地址失败'
      throw error
    } finally {
      addressesLoading.value = false
    }
  }

  function filterDraftAddresses(query?: EnterpriseAddressListQuery): EnterpriseAddress[] {
    let rows = cloneAddresses(DRAFT_ADDRESSES)
    if (query?.keyword?.trim()) {
      const q = query.keyword.trim().toLowerCase()
      rows = rows.filter(item =>
        item.detail.toLowerCase().includes(q)
        || item.regionPath.toLowerCase().includes(q))
    }
    if (query?.types?.length) {
      rows = rows.filter(item => query.types!.some(type => item.types.includes(type)))
    }
    if (query?.status && query.status !== 'ALL') {
      rows = rows.filter(item => item.status === query.status)
    }
    return rows
  }

  async function createAddress(payload: EnterpriseAddressCreate) {
    addressSaving.value = true
    lastError.value = null
    try {
      if (usingLocalDraft.value) {
        const created: EnterpriseAddress = {
          id: nextDraftAddressId(),
          ...payload,
          status: 'ENABLED',
          createTime: new Date().toISOString()
        }
        addresses.value = [created, ...addresses.value]
        DRAFT_ADDRESSES.unshift(created)
        return cloneAddress(created)
      }
      const data = await createEnterpriseAddress(payload)
      addresses.value = [cloneAddress(data), ...addresses.value]
      return data
    } catch (error) {
      if (isApiUnavailable(error)) {
        usingLocalDraft.value = true
        return createAddress(payload)
      }
      lastError.value = error instanceof ApiError || error instanceof Error
        ? error.message
        : '新增地址失败'
      throw error
    } finally {
      addressSaving.value = false
    }
  }

  async function saveAddress(id: string, payload: EnterpriseAddressUpdate) {
    addressSaving.value = true
    lastError.value = null
    try {
      if (usingLocalDraft.value) {
        addresses.value = addresses.value.map(item =>
          item.id === id ? cloneAddress({ ...item, ...payload }) : item)
        const draftIndex = DRAFT_ADDRESSES.findIndex(item => item.id === id)
        if (draftIndex >= 0) {
          DRAFT_ADDRESSES[draftIndex] = { ...DRAFT_ADDRESSES[draftIndex]!, ...payload }
        }
        const updated = addresses.value.find(item => item.id === id)
        if (!updated) {
          throw new Error('地址不存在')
        }
        return cloneAddress(updated)
      }
      const data = await updateEnterpriseAddress(id, payload)
      addresses.value = addresses.value.map(item =>
        item.id === id ? cloneAddress(data) : item)
      return data
    } catch (error) {
      if (isApiUnavailable(error)) {
        usingLocalDraft.value = true
        return saveAddress(id, payload)
      }
      lastError.value = error instanceof ApiError || error instanceof Error
        ? error.message
        : '保存地址失败'
      throw error
    } finally {
      addressSaving.value = false
    }
  }

  async function toggleAddressStatus(id: string, status: EnterpriseAddressStatus) {
    addressSaving.value = true
    lastError.value = null
    try {
      if (usingLocalDraft.value) {
        addresses.value = addresses.value.map(item =>
          item.id === id ? cloneAddress({ ...item, status }) : item)
        const draft = DRAFT_ADDRESSES.find(item => item.id === id)
        if (draft) {
          draft.status = status
        }
        const updated = addresses.value.find(item => item.id === id)
        if (!updated) {
          throw new Error('地址不存在')
        }
        return cloneAddress(updated)
      }
      const data = await updateEnterpriseAddressStatus(id, status)
      addresses.value = addresses.value.map(item =>
        item.id === id ? cloneAddress(data) : item)
      return data
    } catch (error) {
      if (isApiUnavailable(error)) {
        usingLocalDraft.value = true
        return toggleAddressStatus(id, status)
      }
      lastError.value = error instanceof ApiError || error instanceof Error
        ? error.message
        : '更新地址状态失败'
      throw error
    } finally {
      addressSaving.value = false
    }
  }

  async function fetchGeoChildren(parentId?: string | null): Promise<GeoRegionNode[]> {
    try {
      const rows = await getGeoChildren(parentId)
      geoUsesDraft.value = false
      return rows
    } catch {
      geoUsesDraft.value = true
      return draftGeoChildren(parentId)
    }
  }

  function buildRegionPath(names: string[]): string {
    return names.filter(Boolean).join(' / ')
  }

  function resetForTenantSwitch() {
    profile.value = cloneProfile(DRAFT_PROFILE)
    addresses.value = cloneAddresses(DRAFT_ADDRESSES)
    profileHydrated.value = false
    addressesHydrated.value = false
    usingLocalDraft.value = false
    geoUsesDraft.value = false
    lastError.value = null
  }

  return {
    profile,
    addresses,
    profileLoading,
    addressesLoading,
    profileSaving,
    addressSaving,
    lastError,
    profileHydrated,
    addressesHydrated,
    usingLocalDraft,
    geoUsesDraft,
    industryLabel,
    scaleLabel,
    loadProfile,
    saveProfile,
    loadAddresses,
    createAddress,
    saveAddress,
    toggleAddressStatus,
    fetchGeoChildren,
    buildRegionPath,
    resetForTenantSwitch
  }
})
