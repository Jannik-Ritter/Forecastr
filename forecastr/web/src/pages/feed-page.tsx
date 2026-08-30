import { useQuery } from '@tanstack/react-query'
import { useRef, useState } from 'react'

import { BetDrawer } from '@/components/bet-drawer'
import { MarketCard } from '@/components/market-card'
import { EmptyState, PageError, PageLoading } from '@/components/page-state'
import { forecastrApi, queryKeys } from '@/core/forecastr-api'
import type { Market, Outcome } from '@/core/types'
import { usePetImages } from '@/lib/pet-images'
import { useSession } from '@/session/session-context'

export function FeedPage() {
  const { user } = useSession()
  const feed = useQuery({
    queryKey: queryKeys.feed,
    queryFn: () => forecastrApi.getFeed(user!.id),
  })
  const feedRef = useRef<HTMLDivElement>(null)
  const imageFor = usePetImages()
  const [bet, setBet] = useState<{ market: Market; outcome: Outcome } | null>(null)

  const handleKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    if (!feedRef.current || !['ArrowDown', 'ArrowUp', 'PageDown', 'PageUp'].includes(event.key)) {
      return
    }
    event.preventDefault()
    const direction = event.key === 'ArrowDown' || event.key === 'PageDown' ? 1 : -1
    const current = Math.round(feedRef.current.scrollTop / feedRef.current.clientHeight)
    const next = Math.max(0, Math.min((feed.data?.length ?? 1) - 1, current + direction))
    const cards = feedRef.current.querySelectorAll<HTMLElement>('[data-market-card]')
    cards.item(next)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  if (feed.isLoading) {
    return <PageLoading label="Feed wird geladen …" />
  }
  if (feed.error) {
    return <PageError error={feed.error} retry={() => void feed.refetch()} />
  }
  if (!feed.data?.length) {
    return (
      <EmptyState
        title="Keine offenen Märkte"
        description="Im Moment gibt es nichts zu bewerten. Live-Updates informieren dich über neue Märkte."
      />
    )
  }

  return (
    <>
      <div
        ref={feedRef}
        tabIndex={0}
        aria-label="Markt-Feed"
        onKeyDown={handleKeyDown}
        className="h-full snap-y snap-mandatory overflow-y-auto overscroll-contain scroll-smooth focus:outline-none motion-reduce:scroll-auto"
      >
        {feed.data.map((market, index) => (
          <MarketCard
            key={market.id}
            market={market}
            image={imageFor(market.id)}
            position={index + 1}
            total={feed.data.length}
            onBet={(outcome) => setBet({ market, outcome })}
          />
        ))}
      </div>
      <BetDrawer
        market={bet?.market ?? null}
        outcome={bet?.outcome ?? null}
        open={bet !== null}
        onOpenChange={(isOpen) => !isOpen && setBet(null)}
      />
    </>
  )
}
