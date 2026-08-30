import { z } from 'zod'

import { exactNumber, request, requestWithoutResponse } from '@/core/api-client'
import {
  adminStatsSchema,
  balanceSchema,
  betSchema,
  importReportSchema,
  marketSchema,
  resolutionResultSchema,
  testEventsResultSchema,
  testUsersResultSchema,
  userPageSchema,
  userSchema,
} from '@/core/schemas'
import type { ManualResolution, Outcome } from '@/core/types'

export const queryKeys = {
  users: (page: number) => ['users', page] as const,
  user: (userId: string) => ['user', userId] as const,
  balance: (userId: string) => ['balance', userId] as const,
  feed: ['feed'] as const,
  search: (text: string, status = '') => ['events', 'search', text, status] as const,
  event: (eventId: string) => ['event', eventId] as const,
  bets: (userId: string) => ['bets', userId] as const,
  stats: ['admin', 'stats'] as const,
}

export const forecastrApi = {
  getUserPage(page: number) {
    return request(`/users/page?page=${Math.max(0, page)}&size=9`, userPageSchema)
  },

  getUser(userId: string) {
    return request(`/users/${encodeURIComponent(userId)}`, userSchema, { actorUserId: userId })
  },

  createUser(username: string) {
    return request('/users', userSchema, {
      method: 'POST',
      body: { username, initialBalance: exactNumber('100.00') },
    })
  },

  updateUser(userId: string, username: string) {
    return request(`/users/${encodeURIComponent(userId)}`, userSchema, {
      actorUserId: userId,
      method: 'PUT',
      body: { username },
    })
  },

  deleteUser(userId: string) {
    return requestWithoutResponse(`/users/${encodeURIComponent(userId)}`, {
      actorUserId: userId,
      method: 'DELETE',
    })
  },

  getBalance(userId: string) {
    return request(`/users/${encodeURIComponent(userId)}/balance`, balanceSchema, {
      actorUserId: userId,
    })
  },

  updateBalance(userId: string, operation: 'deposit' | 'withdraw', amount: string) {
    return request(`/users/${encodeURIComponent(userId)}/${operation}`, balanceSchema, {
      actorUserId: userId,
      method: 'POST',
      body: { amount: exactNumber(amount) },
    })
  },

  getFeed(userId: string) {
    return request('/feed?limit=200', z.array(marketSchema), { actorUserId: userId })
  },

  searchEvents(userId: string, text: string, status = '') {
    const parameters = new URLSearchParams()
    if (text.trim()) {
      parameters.set('name', text.trim())
    }
    if (status) {
      parameters.set('status', status)
    }
    const query = parameters.size === 0 ? '' : `?${parameters}`
    return request(`/events${query}`, z.array(marketSchema), { actorUserId: userId })
  },

  getEvent(userId: string, eventId: string) {
    return request(`/events/${encodeURIComponent(eventId)}`, marketSchema, {
      actorUserId: userId,
    })
  },

  getBets(userId: string) {
    return request(`/users/${encodeURIComponent(userId)}/bets`, z.array(betSchema), {
      actorUserId: userId,
    })
  },

  placeBet(userId: string, eventId: string, outcome: Outcome, stake: string) {
    return request(`/events/${encodeURIComponent(eventId)}/bets`, betSchema, {
      actorUserId: userId,
      method: 'POST',
      body: {
        userId: exactNumber(userId),
        outcome,
        stake: exactNumber(stake),
      },
    })
  },

  getAdminStats(userId: string) {
    return request('/stats', adminStatsSchema, { actorUserId: userId })
  },

  importEvents(userId: string, path: string) {
    const query = path.trim() ? `?path=${encodeURIComponent(path.trim())}` : ''
    return request(`/admin/import${query}`, importReportSchema, {
      actorUserId: userId,
      method: 'POST',
      body: {},
    })
  },

  seedTestUsers(
    userId: string,
    values: {
      count: number
      betsPerUser: number
      eventId?: string
      outcome?: Outcome
      stake?: string
    },
  ) {
    return request('/admin/test-data/users', testUsersResultSchema, {
      actorUserId: userId,
      method: 'POST',
      body: {
        count: values.count,
        betsPerUser: values.betsPerUser,
        ...(values.eventId ? { eventId: exactNumber(values.eventId) } : {}),
        ...(values.outcome ? { outcome: values.outcome } : {}),
        ...(values.stake ? { stake: exactNumber(values.stake) } : {}),
      },
    })
  },

  seedTestEvents(userId: string, count: number, expiresInMinutes?: number) {
    return request('/admin/test-data/events', testEventsResultSchema, {
      actorUserId: userId,
      method: 'POST',
      body: {
        count,
        ...(expiresInMinutes ? { expiresInMinutes } : {}),
      },
    })
  },

  resolveEvent(userId: string, eventId: string, outcome: ManualResolution) {
    return request(
      `/admin/events/${encodeURIComponent(eventId)}/resolve`,
      resolutionResultSchema,
      {
        actorUserId: userId,
        method: 'POST',
        body: { outcome },
      },
    )
  },
}
