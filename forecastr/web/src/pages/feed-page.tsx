import { useQuery } from '@tanstack/react-query'
import { ChevronDown, ChevronUp } from 'lucide-react'
import { useEffect, useLayoutEffect, useRef, useState } from 'react'

import { BetDrawer } from '@/components/bet-drawer'
import { MarketCard } from '@/components/market-card'
import { EmptyState, PageError, PageLoading } from '@/components/page-state'
import { forecastrApi, queryKeys } from '@/core/forecastr-api'
import type { Market, Outcome } from '@/core/types'
import { usePetImages } from '@/lib/pet-images'
import { useSession } from '@/session/session-context'

interface MarketTransition {
  departingMarket: Market
  enteringMarketId: string
  position: number
  total: number
}

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
  const [marketTransition, setMarketTransition] = useState<MarketTransition | null>(null)
  const previousMarketsRef = useRef<Market[]>([])

  useLayoutEffect(() => {
    const nextMarkets = feed.data ?? []
    const previousMarkets = previousMarketsRef.current
    const activeMarket = previousMarkets[activeIndex]

    if (!activeMarket || nextMarkets.length === 0) {
      previousMarketsRef.current = nextMarkets
      return
    }

    const preservedIndex = nextMarkets.findIndex((market) => market.id === activeMarket.id)
    if (preservedIndex >= 0) {
      if (preservedIndex !== activeIndex) {
        alignFeedToMarket(preservedIndex)
      }
    } else {
      const enteringIndex = Math.min(activeIndex, nextMarkets.length - 1)
      setMarketTransition({
        departingMarket: activeMarket,
        enteringMarketId: nextMarkets[enteringIndex].id,
        position: activeIndex + 1,
        total: previousMarkets.length,
      })
      alignFeedToMarket(enteringIndex)
    }

    previousMarketsRef.current = nextMarkets

    function alignFeedToMarket(index: number) {
      setActiveIndex(index)
      if (feedRef.current) {
        feedRef.current.scrollTop = index * feedRef.current.clientHeight
      }
    }
  }, [activeIndex, feed.data])

  useEffect(() => {
    if (!marketTransition) {
      return
    }
    const fallbackTimer = window.setTimeout(() => setMarketTransition(null), 300)
    return () => window.clearTimeout(fallbackTimer)
  }, [marketTransition])

  const scrollToMarket = (index: number, behavior: ScrollBehavior) => {
    if (!feedRef.current || !feed.data?.length) {
      return
    }
    const nextIndex = Math.max(0, Math.min(feed.data.length - 1, index))
    const cards = feedRef.current.querySelectorAll<HTMLElement>('[data-market-card]')
    const shouldReduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    cards.item(nextIndex)?.scrollIntoView({
      behavior: shouldReduceMotion ? 'auto' : behavior,
      block: 'start',
    })
  }

  const handleKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    if (!feedRef.current || !['ArrowDown', 'ArrowUp', 'PageDown', 'PageUp'].includes(event.key)) {
      return
    }
    event.preventDefault()
    const direction = event.key === 'ArrowDown' || event.key === 'PageDown' ? 1 : -1
    scrollToMarket(activeIndex + direction, 'auto')
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
      <div className="h-full bg-background lg:px-6">
        <div className="relative mx-auto h-full w-full lg:max-w-160">
          <div
            ref={feedRef}
            tabIndex={0}
            aria-label="Markt-Feed"
            onKeyDown={handleKeyDown}
            onScroll={handleScroll}
            className="h-full w-full snap-y snap-mandatory overflow-y-auto overscroll-contain focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring lg:border-x lg:border-border"
          >
            {feed.data.map((market, index) => (
              <MarketCard
                key={market.id}
                market={market}
                image={imageFor(market.id)}
                position={index + 1}
                total={feed.data.length}
                onBet={(outcome) => setBet({ market, outcome })}
                className={
                  marketTransition?.enteringMarketId === market.id
                    ? 'feed-market-entering'
                    : undefined
                }
                onAnimationEnd={(event) => {
                  if (
                    event.currentTarget === event.target &&
                    marketTransition?.enteringMarketId === market.id
                  ) {
                    setMarketTransition(null)
                  }
                }}
              />
            ))}
          </div>
          {marketTransition && (
            <div
              className="feed-market-leaving pointer-events-none absolute inset-0 z-10"
              aria-hidden="true"
              inert
            >
              <MarketCard
                market={marketTransition.departingMarket}
                image={imageFor(marketTransition.departingMarket.id)}
                position={marketTransition.position}
                total={marketTransition.total}
                onBet={() => undefined}
              />
            </div>
          )}
          <nav
            aria-label="Feed-Navigation"
            className="absolute top-1/2 left-full ml-6 hidden w-12 -translate-y-1/2 flex-col gap-3 lg:flex"
          >
            <button
              type="button"
              aria-label="Vorheriger Markt"
              disabled={activeIndex === 0}
              onClick={() => scrollToMarket(activeIndex - 1, 'smooth')}
              className="ui-pressable grid size-12 place-items-center rounded-full bg-foreground/10 text-foreground hover-fine:bg-foreground/20 focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-30"
            >
              <ChevronUp className="size-5" aria-hidden="true" />
            </button>
            <button
              type="button"
              aria-label="Nächster Markt"
              disabled={activeIndex + 1 >= feed.data.length}
              onClick={() => scrollToMarket(activeIndex + 1, 'smooth')}
              className="ui-pressable grid size-12 place-items-center rounded-full bg-foreground/10 text-foreground hover-fine:bg-foreground/20 focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-30"
            >
              <ChevronDown className="size-5" aria-hidden="true" />
            </button>
          </nav>
        </div>
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
