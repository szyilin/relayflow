/** UI field types for list custom fields (create/edit modal). */

export type CustomFieldUiType =
  | 'SINGLE_SELECT'
  | 'MULTI_SELECT'
  | 'PERSON'
  | 'NUMBER'
  | 'DATE'
  | 'TEXT'

/** Currently persisted by `/list-field` API (V1). */
export const PERSISTABLE_FIELD_TYPES: ReadonlySet<CustomFieldUiType> = new Set([
  'SINGLE_SELECT'
])

export interface CustomFieldTypeMeta {
  value: CustomFieldUiType
  label: string
  icon: string
  description?: string
}

export const BASIC_FIELD_TYPES: CustomFieldTypeMeta[] = [
  { value: 'SINGLE_SELECT', label: '单选', icon: 'i-lucide-circle-dot' },
  { value: 'MULTI_SELECT', label: '多选', icon: 'i-lucide-list' },
  { value: 'PERSON', label: '人员', icon: 'i-lucide-user' },
  { value: 'NUMBER', label: '数值', icon: 'i-lucide-hash' },
  { value: 'DATE', label: '日期', icon: 'i-lucide-calendar' },
  { value: 'TEXT', label: '文本', icon: 'i-lucide-whole-word' }
]

export interface RecommendedFieldTemplate {
  id: string
  label: string
  icon: string
  fieldType: CustomFieldUiType
  name: string
  options?: string[]
}

export const RECOMMENDED_FIELD_TEMPLATES: RecommendedFieldTemplate[] = [
  {
    id: 'priority',
    label: '优先级',
    icon: 'i-lucide-circle-dot',
    fieldType: 'SINGLE_SELECT',
    name: '优先级',
    options: ['高', '中', '低']
  },
  {
    id: 'risk',
    label: '风险级',
    icon: 'i-lucide-circle-dot',
    fieldType: 'SINGLE_SELECT',
    name: '风险级',
    options: ['高', '中', '低']
  },
  {
    id: 'price',
    label: '价格',
    icon: 'i-lucide-hash',
    fieldType: 'NUMBER',
    name: '价格'
  },
  {
    id: 'cost',
    label: '成本',
    icon: 'i-lucide-hash',
    fieldType: 'NUMBER',
    name: '成本'
  }
]

/** Option color palette (hex), cycled for new options. */
export const OPTION_COLOR_PALETTE = [
  '#F472B6',
  '#FB923C',
  '#FACC15',
  '#4ADE80',
  '#38BDF8',
  '#A78BFA',
  '#94A3B8',
  '#F87171'
] as const

export function optionColorAt(index: number): string {
  return OPTION_COLOR_PALETTE[index % OPTION_COLOR_PALETTE.length]!
}

export function fieldTypeMeta(type: CustomFieldUiType): CustomFieldTypeMeta {
  return BASIC_FIELD_TYPES.find(t => t.value === type) ?? BASIC_FIELD_TYPES[0]!
}

export function fieldTypeLabel(type: string): string {
  const hit = BASIC_FIELD_TYPES.find(t => t.value === type)
  return hit?.label ?? '单选'
}

export function isSelectUiType(type: CustomFieldUiType): boolean {
  return type === 'SINGLE_SELECT' || type === 'MULTI_SELECT'
}

export function isPersistableFieldType(type: CustomFieldUiType): boolean {
  return PERSISTABLE_FIELD_TYPES.has(type)
}

export type NumberValueKind = 'number' | 'percent' | 'currency'
export type DateFormatId = 'yyyy-MM-dd' | 'yyyy/MM/dd' | 'MM/dd/yyyy' | 'dd/MM/yyyy'

export const DATE_FORMAT_OPTIONS: { value: DateFormatId, label: string }[] = [
  { value: 'yyyy-MM-dd', label: '2023-01-30' },
  { value: 'yyyy/MM/dd', label: '2023/01/30' },
  { value: 'MM/dd/yyyy', label: '01/30/2023' },
  { value: 'dd/MM/yyyy', label: '30/01/2023' }
]

export const NUMBER_KIND_OPTIONS: { value: NumberValueKind, label: string, icon: string }[] = [
  { value: 'number', label: '数字', icon: 'i-lucide-hash' },
  { value: 'percent', label: '百分比', icon: 'i-lucide-percent' },
  { value: 'currency', label: '货币', icon: 'i-lucide-banknote' }
]

export function formatNumberPreview(
  kind: NumberValueKind,
  decimals: number,
  thousands: boolean
): string {
  const n = 1000
  const fixed = n.toFixed(Math.max(0, Math.min(6, decimals)))
  const [intPart, frac] = fixed.split('.')
  const grouped = thousands
    ? (intPart ?? '0').replace(/\B(?=(\d{3})+(?!\d))/g, ',')
    : (intPart ?? '0')
  const body = frac != null && decimals > 0 ? `${grouped}.${frac}` : grouped
  if (kind === 'percent') {
    return `${body}%`
  }
  if (kind === 'currency') {
    return `¥${body}`
  }
  return body
}
