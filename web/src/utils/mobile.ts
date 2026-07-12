/** 去除分段输入中的空格，得到 11 位数字手机号 */
export function normalizeMobile(raw: string): string {
  return raw.replace(/\s+/g, '')
}

/** 允许 `191 5315 7339` 等分段格式，校验时按去空格后的 11 位判断 */
export function isValidMobile(raw: string): boolean {
  const normalized = normalizeMobile(raw.trim())
  return /^1\d{10}$/.test(normalized)
}
