/** 雪花 ID 在 JS number 中会丢精度，统一用 string 传递与比较。 */
export function normalizeId(value: unknown): string | null {
  if (value == null || value === '') {
    return null
  }
  return String(value)
}

export function idsEqual(a: unknown, b: unknown): boolean {
  const left = normalizeId(a)
  const right = normalizeId(b)
  return left != null && left === right
}

export function hashId(value: string): number {
  let hash = 0
  for (let i = 0; i < value.length; i++) {
    hash = (hash * 31 + value.charCodeAt(i)) | 0
  }
  return Math.abs(hash)
}
