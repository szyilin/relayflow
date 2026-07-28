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

const EMPTY_PROFILE: EnterpriseProfile = {
  code: '',
  name: '',
  industry: null,
  scale: null,
  dataResidency: '中国大陆',
  contactName: null,
  contactEmail: null,
  contactMobile: null,
  contactLandline: null
}

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

function labelOf<T extends { label: string, value: string }>(
  options: readonly T[],
  value: string | null | undefined
): string {
  if (!value) {
    return '—'
  }
  return options.find(item => item.value === value)?.label ?? value
}

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError || error instanceof Error) {
    return error.message
  }
  return fallback
}

export const useEnterpriseStore = defineStore('enterprise', () => {
  const profile = ref<EnterpriseProfile>(cloneProfile(EMPTY_PROFILE))
  const addresses = ref<EnterpriseAddress[]>([])
  const profileLoading = ref(false)
  const addressesLoading = ref(false)
  const profileSaving = ref(false)
  const addressSaving = ref(false)
  const lastError = ref<string | null>(null)
  const profileHydrated = ref(false)
  const addressesHydrated = ref(false)

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
    } catch (error) {
      lastError.value = toErrorMessage(error, '加载企业信息失败')
      throw error
    } finally {
      profileLoading.value = false
    }
  }

  async function saveProfile(payload: EnterpriseProfileUpdate) {
    profileSaving.value = true
    lastError.value = null
    try {
      const data = await updateEnterpriseProfile(payload)
      profile.value = cloneProfile(data)
      return data
    } catch (error) {
      lastError.value = toErrorMessage(error, '保存企业信息失败')
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
    } catch (error) {
      lastError.value = toErrorMessage(error, '加载企业地址失败')
      throw error
    } finally {
      addressesLoading.value = false
    }
  }

  async function createAddress(payload: EnterpriseAddressCreate) {
    addressSaving.value = true
    lastError.value = null
    try {
      const data = await createEnterpriseAddress(payload)
      addresses.value = [cloneAddress(data), ...addresses.value]
      return data
    } catch (error) {
      lastError.value = toErrorMessage(error, '新增地址失败')
      throw error
    } finally {
      addressSaving.value = false
    }
  }

  async function saveAddress(id: string, payload: EnterpriseAddressUpdate) {
    addressSaving.value = true
    lastError.value = null
    try {
      const data = await updateEnterpriseAddress(id, payload)
      addresses.value = addresses.value.map(item =>
        item.id === id ? cloneAddress(data) : item)
      return data
    } catch (error) {
      lastError.value = toErrorMessage(error, '保存地址失败')
      throw error
    } finally {
      addressSaving.value = false
    }
  }

  async function toggleAddressStatus(id: string, status: EnterpriseAddressStatus) {
    addressSaving.value = true
    lastError.value = null
    try {
      const data = await updateEnterpriseAddressStatus(id, status)
      addresses.value = addresses.value.map(item =>
        item.id === id ? cloneAddress(data) : item)
      return data
    } catch (error) {
      lastError.value = toErrorMessage(error, '更新地址状态失败')
      throw error
    } finally {
      addressSaving.value = false
    }
  }

  async function fetchGeoChildren(parentId?: string | null): Promise<GeoRegionNode[]> {
    return getGeoChildren(parentId)
  }

  function buildRegionPath(names: string[]): string {
    return names.filter(Boolean).join(' / ')
  }

  function resetForTenantSwitch() {
    profile.value = cloneProfile(EMPTY_PROFILE)
    addresses.value = []
    profileHydrated.value = false
    addressesHydrated.value = false
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
