/**
 * Contact list sort letter: Latin A–Z, everything else `#`.
 * Full pinyin grouping is deferred to -api (`sortLetter` field).
 */
export function contactSortLetter(name: string): string {
  const ch = name.trim().charAt(0)
  if (!ch) {
    return '#'
  }
  if (/[a-z]/i.test(ch)) {
    return ch.toUpperCase()
  }
  return '#'
}

export function compareContactName(a: string, b: string): number {
  return a.localeCompare(b, 'zh-CN') || a.localeCompare(b)
}

export interface LetterGroup<T> {
  letter: string
  items: T[]
}

export function groupBySortLetter<T>(
  items: T[],
  getName: (item: T) => string
): LetterGroup<T>[] {
  const sorted = [...items].sort((a, b) => compareContactName(getName(a), getName(b)))
  const map = new Map<string, T[]>()
  for (const item of sorted) {
    const letter = contactSortLetter(getName(item))
    const bucket = map.get(letter)
    if (bucket) {
      bucket.push(item)
    } else {
      map.set(letter, [item])
    }
  }
  const letters = [...map.keys()].sort((a, b) => {
    if (a === '#') {
      return 1
    }
    if (b === '#') {
      return -1
    }
    return a.localeCompare(b)
  })
  return letters.map(letter => ({
    letter,
    items: map.get(letter) ?? []
  }))
}
