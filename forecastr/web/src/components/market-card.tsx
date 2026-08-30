import { ArrowDown, ArrowUp, Clock3 } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
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
      className="relative h-full snap-start snap-always overflow-hidden bg-[#0d0d0d] text-white"
    >
      {image && (
        <img
          src={image}
          alt=""
          loading={position <= 2 ? 'eager' : 'lazy'}
          className="absolute inset-0 size-full object-cover"
        />
      )}
      <div className="relative flex h-full flex-col px-5 pt-20 pb-24 [text-shadow:0_1px_12px_rgb(0_0_0/0.45)] lg:px-8 lg:pb-8">
        <div className="flex items-center gap-3 text-xs">
          <Badge className="gap-1.5 border-white/10 bg-white/8 text-white/80 backdrop-blur-md">
            {isOpen && (
              <span className="size-1.5 rounded-full bg-outcome-yes" aria-hidden="true" />
            )}
            {eventStatusLabel(market.status)}
          </Badge>
          <span className="ml-auto text-white/45 tabular-nums">
            {position}/{total}
          </span>
        </div>
        <div className="my-auto max-w-xl py-8">
          <h1 className="text-balance text-2xl leading-tight font-semibold tracking-[-0.03em] sm:text-3xl lg:text-4xl">
            {market.question}
          </h1>
          <div className="mt-3 flex items-center gap-2 text-sm text-white/60">
            <Clock3 className="size-4" aria-hidden="true" />
            <span>Wetten {formatDeadline(market.closesAt)}</span>
          </div>
        </div>
        <div>
          <div className="mb-1.5 flex justify-between text-xs font-medium">
            <span className="text-outcome-yes">JA · {formatMoney(market.yesPool)}</span>
            <span className="text-outcome-no">NEIN · {formatMoney(market.noPool)}</span>
          </div>
          <div
            className="flex h-1.5 overflow-hidden rounded-full bg-white/15"
            aria-label={`Poolverteilung: ${yesShare.toFixed(0)} Prozent JA`}
          >
            <div className="bg-outcome-yes" style={{ width: `${yesShare}%` }} />
            <div className="flex-1 bg-outcome-no" />
          </div>
          {isOpen ? (
            <div className="mt-3 grid grid-cols-2 gap-2">
              <OutcomeButton outcome="YES" share={yesShare} onBet={onBet} />
              <OutcomeButton outcome="NO" share={noShare} onBet={onBet} />
            </div>
          ) : (
            <p className="mt-3 text-sm text-white/55">
              Dieser Markt ist geschlossen. {eventStatusLabel(market.status)}.
            </p>
          )}
        </div>
      </div>
    </article>
  )
}

function OutcomeButton({
  outcome,
  share,
  onBet,
}: {
  outcome: Outcome
  share: number
  onBet: (outcome: Outcome) => void
}) {
  const isYes = outcome === 'YES'
  const Icon = isYes ? ArrowUp : ArrowDown
  const label = isYes ? 'Ja' : 'Nein'

  return (
    <button
      type="button"
      aria-label={`${label}, ${share.toFixed(0)} Prozent`}
      onClick={() => onBet(outcome)}
      className={cn(
        'flex h-12 items-center justify-center gap-2 rounded-xl text-sm font-semibold transition [text-shadow:none] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/70 active:scale-[0.98]',
        isYes
          ? 'bg-outcome-yes text-outcome-yes-foreground hover:bg-outcome-yes/90'
          : 'bg-outcome-no text-outcome-no-foreground hover:bg-outcome-no/90',
      )}
    >
      <Icon className="size-4" aria-hidden="true" />
      {label}
      <span className="text-xs font-medium opacity-70 tabular-nums">{share.toFixed(0)} %</span>
    </button>
  )
}
