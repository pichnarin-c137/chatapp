export const DEFAULT_TIMEZONE = 'Asia/Phnom_Penh'

function toDate(input: string | Date): Date {
  return typeof input === 'string' ? new Date(input) : input
}

export function formatDateTime(
  input: string | Date,
  timezone: string = DEFAULT_TIMEZONE,
): string {
  return new Intl.DateTimeFormat('en-GB', {
    timeZone: timezone,
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: true,
  }).format(toDate(input))
}

export function formatTime(
  input: string | Date,
  timezone: string = DEFAULT_TIMEZONE,
): string {
  return new Intl.DateTimeFormat('en-GB', {
    timeZone: timezone,
    hour: '2-digit',
    minute: '2-digit',
    hour12: true,
  }).format(toDate(input))
}

export function formatDate(
  input: string | Date,
  timezone: string = DEFAULT_TIMEZONE,
): string {
  return new Intl.DateTimeFormat('en-GB', {
    timeZone: timezone,
    year: 'numeric',
    month: 'short',
    day: '2-digit',
  }).format(toDate(input))
}

export function formatRelative(
  input: string | Date,
  timezone: string = DEFAULT_TIMEZONE,
): string {
  const date = toDate(input)
  const diffMs = Date.now() - date.getTime()
  const diffSec = Math.round(diffMs / 1000)
  const rtf = new Intl.RelativeTimeFormat('en', { numeric: 'auto' })

  if (diffSec < 60) return rtf.format(-diffSec, 'second')
  const diffMin = Math.round(diffSec / 60)
  if (diffMin < 60) return rtf.format(-diffMin, 'minute')
  const diffHr = Math.round(diffMin / 60)
  if (diffHr < 24) return rtf.format(-diffHr, 'hour')
  const diffDay = Math.round(diffHr / 24)
  if (diffDay < 7) return rtf.format(-diffDay, 'day')

  return formatDate(date, timezone)
}

export function detectBrowserTimezone(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || DEFAULT_TIMEZONE
  } catch {
    return DEFAULT_TIMEZONE
  }
}
