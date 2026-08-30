import { isLosslessNumber } from 'lossless-json'
import { z } from 'zod'

const numericText = z.unknown().transform((value, context) => {
  if (isLosslessNumber(value)) {
    return value.toString()
  }
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value.toString()
  }
  if (typeof value === 'string' && value.trim() !== '') {
    return value
  }
  context.addIssue({ code: 'custom', message: 'Expected a JSON number' })
  return z.NEVER
})

const safeInteger = numericText.transform((value, context) => {
  const parsed = Number(value)
  if (!Number.isSafeInteger(parsed)) {
    context.addIssue({ code: 'custom', message: 'Expected a safe integer' })
    return z.NEVER
  }
  return parsed
})

const identifier = numericText.refine((value) => /^\d+$/.test(value), 'Expected an identifier')
const money = numericText.refine(
  (value) => /^-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?$/.test(value),
  'Expected a decimal amount',
)
const optionalInstant = z.string().nullable()

export const userSchema = z.object({
  id: identifier,
  username: z.string(),
  isAdmin: z.boolean(),
  createdAt: z.string(),
  deletedAt: optionalInstant,
})

export const userPageSchema = z.object({
  content: z.array(userSchema),
  page: safeInteger,
  totalPages: safeInteger,
  totalElements: safeInteger,
})

export const balanceSchema = z.object({
  userId: identifier,
  balance: money,
  currency: z.string(),
  version: identifier,
})

export const marketSchema = z.object({
  id: identifier,
  question: z.string(),
  createdAt: z.string(),
  closesAt: optionalInstant,
  settlementAt: optionalInstant,
  status: z.enum(['OPEN', 'RESOLVED_YES', 'RESOLVED_NO', 'EXPIRED', 'ARCHIVED']),
  resolvedAt: optionalInstant,
  yesPool: money,
  noPool: money,
})

export const betSchema = z.object({
  id: identifier,
  userId: identifier,
  eventId: identifier,
  outcome: z.enum(['YES', 'NO']),
  stake: money,
  placedAt: z.string(),
  status: z.enum(['OPEN', 'WON', 'LOST', 'REFUNDED']),
  payoutAmount: money,
  feeAmount: money,
})

export const adminStatsSchema = z.object({
  users: safeInteger,
  events: safeInteger,
  eventsByStatus: z.record(z.string(), safeInteger),
  bets: safeInteger,
  payouts: money,
  fees: money,
  perUserBetCount: z.record(z.string(), safeInteger),
  resolver: z.record(z.string(), safeInteger),
})

export const importReportSchema = z.object({
  accepted: safeInteger,
  rejected: safeInteger,
  skipped: safeInteger,
  errors: z.array(z.string()),
})

export const testUsersResultSchema = z.object({
  userIds: z.array(identifier),
  betIds: z.array(identifier),
})

export const testEventsResultSchema = z.object({
  eventIds: z.array(identifier),
})

export const resolutionResultSchema = z.object({
  eventId: identifier,
  status: z.enum(['OPEN', 'RESOLVED_YES', 'RESOLVED_NO', 'EXPIRED', 'ARCHIVED']),
  winners: safeInteger,
  losers: safeInteger,
  payouts: money,
  fees: money,
  changed: z.boolean(),
})

export const liveMessageSchema = z.discriminatedUnion('type', [
  z.object({
    type: z.literal('FEED'),
    eventId: identifier,
    action: z.string(),
  }),
  z.object({
    type: z.literal('NOTIFICATION'),
    userId: identifier,
    eventId: identifier,
    kind: z.string(),
    amount: money,
  }),
  z.object({ type: z.literal('ERROR'), message: z.string() }),
  z.object({ type: z.literal('CONNECTED'), message: z.string() }),
  z.object({ type: z.literal('SUBSCRIBED'), topic: z.string() }),
])
