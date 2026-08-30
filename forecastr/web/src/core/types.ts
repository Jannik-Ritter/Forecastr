export type Identifier = string
export type Money = string

export type EventStatus =
  | 'OPEN'
  | 'RESOLVED_YES'
  | 'RESOLVED_NO'
  | 'EXPIRED'
  | 'ARCHIVED'

export type BetStatus = 'OPEN' | 'WON' | 'LOST' | 'REFUNDED'
export type Outcome = 'YES' | 'NO'
export type ManualResolution = Outcome | 'REFUND'

export interface User {
  id: Identifier
  username: string
  isAdmin: boolean
  createdAt: string
  deletedAt: string | null
}

export interface UserPage {
  content: User[]
  page: number
  totalPages: number
  totalElements: number
}

export interface Balance {
  userId: Identifier
  balance: Money
  currency: string
  version: Identifier
}

export interface Market {
  id: Identifier
  question: string
  createdAt: string
  closesAt: string | null
  settlementAt: string | null
  status: EventStatus
  resolvedAt: string | null
  yesPool: Money
  noPool: Money
}

export interface Bet {
  id: Identifier
  userId: Identifier
  eventId: Identifier
  outcome: Outcome
  stake: Money
  placedAt: string
  status: BetStatus
  payoutAmount: Money
  feeAmount: Money
}

export interface AdminStats {
  users: number
  events: number
  eventsByStatus: Record<string, number>
  bets: number
  payouts: Money
  fees: Money
  perUserBetCount: Record<string, number>
  resolver: Record<string, number>
}

export interface ImportReport {
  accepted: number
  rejected: number
  skipped: number
  errors: string[]
}

export interface TestUsersResult {
  userIds: Identifier[]
  betIds: Identifier[]
}

export interface TestEventsResult {
  eventIds: Identifier[]
}

export interface ResolutionResult {
  eventId: Identifier
  status: EventStatus
  winners: number
  losers: number
  payouts: Money
  fees: Money
  changed: boolean
}
