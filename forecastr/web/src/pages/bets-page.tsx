import { useQuery } from '@tanstack/react-query'
import { RefreshCw } from 'lucide-react'

import { EmptyState, PageError, PageLoading } from '@/components/page-state'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { forecastrApi, queryKeys } from '@/core/forecastr-api'
import type { Bet, BetStatus, Market } from '@/core/types'
import { betStatusLabel, formatDate, formatDeadline, formatMoney, outcomeLabel } from '@/lib/format'
import { useSession } from '@/session/session-context'

interface BetWithMarket {
  bet: Bet
  market: Market | null
}

const statuses: BetStatus[] = ['OPEN', 'WON', 'LOST', 'REFUNDED']

export function BetsPage() {
  const { user } = useSession()
  const bets = useQuery({
    queryKey: queryKeys.bets(user!.id),
    queryFn: async (): Promise<BetWithMarket[]> => {
      const values = await forecastrApi.getBets(user!.id)
      const events = new Map<string, Market | null>()
      await Promise.all(
        [...new Set(values.map((bet) => bet.eventId))].map(async (eventId) => {
          try {
            events.set(eventId, await forecastrApi.getEvent(user!.id, eventId))
          } catch {
            events.set(eventId, null)
          }
        }),
      )
      return values
        .map((bet) => ({ bet, market: events.get(bet.eventId) ?? null }))
        .sort((left, right) => {
          const statusOrder = statuses.indexOf(left.bet.status) - statuses.indexOf(right.bet.status)
          return statusOrder || right.bet.placedAt.localeCompare(left.bet.placedAt)
        })
    },
  })

  return (
    <div className="mx-auto max-w-2xl">
      <div className="mb-6 flex items-end justify-between">
        <div>
          <p className="text-xs font-medium tracking-widest text-muted-foreground uppercase">Portfolio</p>
          <h1 className="mt-1 text-2xl font-semibold tracking-tight">Meine Wetten</h1>
        </div>
        <Button size="icon" variant="outline" aria-label="Wetten aktualisieren" onClick={() => void bets.refetch()}>
          <RefreshCw className={bets.isFetching ? 'animate-spin' : ''} aria-hidden="true" />
        </Button>
      </div>

      {bets.isLoading && <PageLoading label="Wetten werden geladen …" />}
      {bets.error && <PageError error={bets.error} retry={() => void bets.refetch()} />}
      {bets.data?.length === 0 && (
        <EmptyState title="Noch keine Wetten" description="Wähle im Feed JA oder NEIN, um deine erste Prognose abzugeben." />
      )}
      {bets.data && (
        <div className="space-y-7">
          {statuses.map((status) => {
            const grouped = bets.data.filter(({ bet }) => bet.status === status)
            if (grouped.length === 0) {
              return null
            }
            return (
              <section key={status}>
                <h2 className="mb-3 text-sm font-semibold tracking-wide text-muted-foreground uppercase">
                  {betStatusLabel(status)} · {grouped.length}
                </h2>
                <div className="space-y-3">
                  {grouped.map(({ bet, market }) => (
                    <Card key={bet.id}>
                      <CardContent className="space-y-3 p-4">
                        <div className="flex items-start justify-between gap-3">
                          <p className="font-medium leading-snug">
                            {market?.question ?? `Markt ${bet.eventId}`}
                          </p>
                          <Badge
                            className={
                              bet.outcome === 'YES'
                                ? 'bg-outcome-yes/15 text-green-700 dark:text-outcome-yes'
                                : 'bg-outcome-no/15 text-red-700 dark:text-outcome-no'
                            }
                          >
                            {outcomeLabel(bet.outcome)}
                          </Badge>
                        </div>
                        <div className="grid grid-cols-2 gap-3 text-sm">
                          <div>
                            <p className="text-xs text-muted-foreground">Einsatz</p>
                            <p className="mt-1 font-medium">{formatMoney(bet.stake)}</p>
                          </div>
                          <div>
                            <p className="text-xs text-muted-foreground">Platziert</p>
                            <p className="mt-1 font-medium">{formatDate(bet.placedAt)}</p>
                          </div>
                        </div>
                        {bet.status === 'OPEN' && (
                          <p className="rounded-lg bg-muted px-3 py-2 text-xs text-muted-foreground">
                            Abrechnung {formatDeadline(market?.settlementAt ?? null)}
                          </p>
                        )}
                        {bet.status === 'WON' && (
                          <p className="text-sm text-green-700 dark:text-outcome-yes">
                            Ausgezahlt {formatMoney(bet.payoutAmount)} · Gebühr {formatMoney(bet.feeAmount)}
                          </p>
                        )}
                        {bet.status === 'LOST' && (
                          <p className="text-sm text-muted-foreground">Auszahlung {formatMoney('0.00')}</p>
                        )}
                        {bet.status === 'REFUNDED' && (
                          <p className="text-sm text-muted-foreground">Erstattet {formatMoney(bet.payoutAmount)}</p>
                        )}
                      </CardContent>
                    </Card>
                  ))}
                </div>
              </section>
            )
          })}
        </div>
      )}
    </div>
  )
}
