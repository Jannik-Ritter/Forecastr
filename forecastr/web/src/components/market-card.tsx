import { ArrowDown, ArrowUp, Clock3 } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import type { Market, Outcome } from '@/core/types'
import { eventStatusLabel, formatDeadline, formatMoney } from '@/lib/format'
import { poolShare } from '@/lib/money'
import { cn } from '@/lib/utils'

interface MarketCardProps {
  image?: string
  market: Market
  onBet: (outcome: Outcome) => void
  position: number
  total: number
}

export function MarketCard({ image, market, onBet, position, total }: MarketCardProps) {
  const yesShare = poolShare(market.yesPool, market.noPool)
  const noShare = 100 - yesShare
  const isOpen = market.status === 'OPEN'

  return (
    <article
      data-market-card
      className={cn(
        'relative h-full snap-start snap-always overflow-hidden bg-[#0d0d0d] text-white',
        !image && 'brand-pattern',
      )}
    >
      {image && (
        <img
          src={image}
          alt=""
          loading={position <= 2 ? 'eager' : 'lazy'}
          className="absolute inset-0 size-full object-cover"
        />
      )}
      <div className="absolute inset-0 bg-gradient-to-b from-black/25 via-black/5 to-black/90" />
      <div
        className={cn(
          'absolute inset-x-0 bottom-0 px-5 pt-28 lg:right-28 lg:px-8 lg:pb-8',
          isOpen ? 'pb-44' : 'pb-28',
        )}
      >
        <div className="mb-3 flex items-center gap-2">
          <Badge className="border-white/20 bg-black/40 text-white backdrop-blur-md">
            {eventStatusLabel(market.status)}
          </Badge>
          <span className="text-xs text-white/65">
            {position}/{total}
          </span>
        </div>
        <h1 className="max-w-xl text-balance text-2xl leading-tight font-semibold tracking-[-0.035em] sm:text-3xl lg:text-[2rem]">
          {market.question}
        </h1>
        <div className="mt-3 flex items-center gap-2 text-xs text-white/75">
          <Clock3 className="size-3.5" aria-hidden="true" />
          <span>Wetten {formatDeadline(market.closesAt)}</span>
        </div>
        <div className="mt-5 max-w-xl">
          <div className="mb-2 flex justify-between text-xs font-medium">
            <span>JA · {formatMoney(market.yesPool)}</span>
            <span>NEIN · {formatMoney(market.noPool)}</span>
          </div>
          <div
            className="flex h-1.5 overflow-hidden rounded-full bg-outcome-no"
            aria-label={`Poolverteilung: ${yesShare.toFixed(0)} Prozent JA`}
          >
            <div className="bg-outcome-yes" style={{ width: `${yesShare}%` }} />
          </div>
        </div>
        {!isOpen && (
          <div className="mt-5 rounded-xl border border-white/15 bg-black/30 p-3 text-sm backdrop-blur-md">
            Dieser Markt ist geschlossen. {eventStatusLabel(market.status)}.
          </div>
        )}
      </div>
      {isOpen && (
        <div className="absolute inset-x-5 bottom-24 z-20 grid grid-cols-2 gap-3 lg:inset-x-auto lg:right-5 lg:bottom-8 lg:flex lg:flex-col lg:gap-3">
          <Button
            size="lg"
            aria-label="Ja"
            className="h-12 border-white/15 bg-black/65 text-base text-white shadow-xl backdrop-blur-xl hover:border-outcome-yes/70 hover:bg-black/80 lg:h-auto lg:w-20 lg:flex-col lg:rounded-2xl lg:px-2 lg:py-3"
            onClick={() => onBet('YES')}
          >
            <span className="size-2 rounded-full bg-outcome-yes lg:hidden" aria-hidden="true" />
            <span className="hidden size-8 place-items-center rounded-full bg-outcome-yes text-outcome-yes-foreground lg:grid">
              <ArrowUp className="size-4" aria-hidden="true" />
            </span>
            <span className="font-semibold">Ja</span>
            <span className="hidden text-xs leading-none font-semibold text-outcome-yes lg:block">
              {yesShare.toFixed(0)} %
            </span>
          </Button>
          <Button
            size="lg"
            aria-label="Nein"
            className="h-12 border-white/15 bg-black/65 text-base text-white shadow-xl backdrop-blur-xl hover:border-outcome-no/70 hover:bg-black/80 lg:h-auto lg:w-20 lg:flex-col lg:rounded-2xl lg:px-2 lg:py-3"
            onClick={() => onBet('NO')}
          >
            <span className="size-2 rounded-full bg-outcome-no lg:hidden" aria-hidden="true" />
            <span className="hidden size-8 place-items-center rounded-full bg-outcome-no text-outcome-no-foreground lg:grid">
              <ArrowDown className="size-4" aria-hidden="true" />
            </span>
            <span className="font-semibold">Nein</span>
            <span className="hidden text-xs leading-none font-semibold text-outcome-no lg:block">
              {noShare.toFixed(0)} %
            </span>
          </Button>
        </div>
      )}
    </article>
  )
}
