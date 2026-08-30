import { useQuery } from '@tanstack/react-query'
import { ChevronDown, ChevronUp } from 'lucide-react'
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
  const [activeIndex, setActiveIndex] = useState(0)

  const scrollToMarket = (index: number) => {
    if (!feedRef.current || !feed.data?.length) {
      return
    }
    const nextIndex = Math.max(0, Math.min(feed.data.length - 1, index))
    const cards = feedRef.current.querySelectorAll<HTMLElement>('[data-market-card]')
    cards.item(nextIndex)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  const handleKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    if (!feedRef.current || !['ArrowDown', 'ArrowUp', 'PageDown', 'PageUp'].includes(event.key)) {
      return
    }
    event.preventDefault()
    const direction = event.key === 'ArrowDown' || event.key === 'PageDown' ? 1 : -1
    scrollToMarket(activeIndex + direction)
  }

  const handleScroll = (event: React.UIEvent<HTMLDivElement>) => {
    const feedElement = event.currentTarget
    if (feedElement.clientHeight === 0) {
      return
    }
    const nextIndex = Math.round(feedElement.scrollTop / feedElement.clientHeight)
    setActiveIndex(Math.max(0, Math.min((feed.data?.length ?? 1) - 1, nextIndex)))
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
      <div className="flex h-full justify-center bg-[#0d0d0d] lg:gap-6 lg:px-6">
        <div
          ref={feedRef}
          tabIndex={0}
          aria-label="Markt-Feed"
          onKeyDown={handleKeyDown}
          onScroll={handleScroll}
          className="h-full w-full snap-y snap-mandatory overflow-y-auto overscroll-contain scroll-smooth focus:outline-none lg:max-w-[640px] lg:border-x lg:border-white/10 motion-reduce:scroll-auto"
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
        <nav aria-label="Feed-Navigation" className="hidden w-12 flex-col justify-center gap-3 lg:flex">
          <button
            type="button"
            aria-label="Vorheriger Markt"
            disabled={activeIndex === 0}
            onClick={() => scrollToMarket(activeIndex - 1)}
            className="grid size-12 place-items-center rounded-full bg-white/10 text-white transition-colors hover:bg-white/20 disabled:opacity-30"
          >
            <ChevronUp className="size-5" aria-hidden="true" />
          </button>
          <button
            type="button"
            aria-label="Nächster Markt"
            disabled={activeIndex + 1 >= feed.data.length}
            onClick={() => scrollToMarket(activeIndex + 1)}
            className="grid size-12 place-items-center rounded-full bg-white/10 text-white transition-colors hover:bg-white/20 disabled:opacity-30"
          >
            <ChevronDown className="size-5" aria-hidden="true" />
          </button>
        </nav>
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
