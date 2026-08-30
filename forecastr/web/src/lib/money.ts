import Decimal from 'decimal.js'

import type { Market, Money, Outcome } from '@/core/types'

const MoneyDecimal = Decimal.clone({
  precision: 40,
  rounding: Decimal.ROUND_HALF_EVEN,
  toExpNeg: -40,
  toExpPos: 40,
})

export interface MoneyInputResult {
  error?: string
  value?: Money
}

export function parseMoneyInput(rawValue: string): MoneyInputResult {
  const value = rawValue.trim().replace(',', '.')
  if (!/^\d{1,17}(?:\.\d{1,2})?$/.test(value)) {
    return { error: 'Bitte gib einen positiven Betrag mit höchstens zwei Nachkommastellen ein.' }
  }
  const amount = new MoneyDecimal(value)
  if (!amount.greaterThan(0)) {
    return { error: 'Der Betrag muss größer als 0 sein.' }
  }
  return { value: amount.toDecimalPlaces(2, Decimal.ROUND_HALF_EVEN).toFixed(2) }
}

export function compareMoney(left: Money, right: Money): number {
  return new MoneyDecimal(left).comparedTo(new MoneyDecimal(right))
}

export function potentialProfit(market: Market, outcome: Outcome, stake: Money): Money {
  const selectedPool = new MoneyDecimal(outcome === 'YES' ? market.yesPool : market.noPool)
  const otherPool = new MoneyDecimal(outcome === 'YES' ? market.noPool : market.yesPool)
  const stakeValue = new MoneyDecimal(stake)
  const winningPool = selectedPool.plus(stakeValue)
  const totalPool = winningPool.plus(otherPool)
  const gross = stakeValue
    .times(totalPool)
    .dividedBy(winningPool)
    .toDecimalPlaces(2, Decimal.ROUND_HALF_EVEN)
  const grossProfit = MoneyDecimal.max(gross.minus(stakeValue), 0)
  const fee = grossProfit.times('0.05').toDecimalPlaces(2, Decimal.ROUND_HALF_EVEN)
  return grossProfit.minus(fee).toDecimalPlaces(2, Decimal.ROUND_HALF_EVEN).toFixed(2)
}

export function poolShare(yesPool: Money, noPool: Money): number {
  const yes = new MoneyDecimal(yesPool)
  const total = yes.plus(noPool)
  if (total.isZero()) {
    return 50
  }
  return yes.times(100).dividedBy(total).toNumber()
}

export function canonicalMoney(value: Money | null | undefined): Money {
  return new MoneyDecimal(value ?? 0).toDecimalPlaces(2, Decimal.ROUND_HALF_EVEN).toFixed(2)
}
