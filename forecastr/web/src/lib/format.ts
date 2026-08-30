import type { BetStatus, EventStatus, Money, Outcome } from '@/core/types'
import { canonicalMoney } from '@/lib/money'

const dateFormatter = new Intl.DateTimeFormat('de-DE', {
  dateStyle: 'short',
  timeStyle: 'short',
})

export function formatMoney(value: Money | null | undefined): string {
  const fixed = canonicalMoney(value)
  const isNegative = fixed.startsWith('-')
  const unsigned = isNegative ? fixed.slice(1) : fixed
  const [integer, fraction] = unsigned.split('.')
  const grouped = integer.replace(/\B(?=(\d{3})+(?!\d))/g, '.')
  return `${isNegative ? '-' : ''}${grouped},${fraction} €`
}

export function formatDate(value: string | null): string {
  return value ? dateFormatter.format(new Date(value)) : '–'
}

export function formatDeadline(value: string | null): string {
  if (!value) {
    return 'unbekannt'
  }
  const remainingMinutes = Math.ceil((new Date(value).getTime() - Date.now()) / 60_000)
  if (remainingMinutes <= 0) {
    return 'geschlossen'
  }
  if (remainingMinutes < 60) {
    return `in ${remainingMinutes} Min.`
  }
  const hours = Math.floor(remainingMinutes / 60)
  const minutes = remainingMinutes % 60
  if (hours < 24) {
    return `in ${hours} Std.${minutes === 0 ? '' : ` ${minutes} Min.`}`
  }
  const days = Math.floor(hours / 24)
  return `in ${days} Tag${days === 1 ? '' : 'en'}`
}

export function eventStatusLabel(status: EventStatus): string {
  return {
    OPEN: 'Offen',
    RESOLVED_YES: 'Mit JA aufgelöst',
    RESOLVED_NO: 'Mit NEIN aufgelöst',
    EXPIRED: 'Erstattet',
    ARCHIVED: 'Archiviert',
  }[status]
}

export function betStatusLabel(status: BetStatus): string {
  return {
    OPEN: 'Offen',
    WON: 'Gewonnen',
    LOST: 'Verloren',
    REFUNDED: 'Erstattet',
  }[status]
}

export function outcomeLabel(outcome: Outcome): string {
  return outcome === 'YES' ? 'JA' : 'NEIN'
}

export function liveMessage(kind: string, amount: Money): string {
  if (kind === 'PAYOUT') {
    return `Gewonnen: ${formatMoney(amount)} wurden gutgeschrieben.`
  }
  if (kind === 'LOST') {
    return 'Eine deiner Wetten wurde leider verloren.'
  }
  if (kind === 'REFUND') {
    return `Erstattung: ${formatMoney(amount)} wurden gutgeschrieben.`
  }
  return 'Deine Wetten wurden aktualisiert.'
}
