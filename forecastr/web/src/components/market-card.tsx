import { Clock3 } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import type { Market, Outcome } from '@/core/types'
import { eventStatusLabel, formatDeadline, formatMoney } from '@/lib/format'
import { poolShare } from '@/lib/money'
import { fallbackColor } from '@/lib/pet-images'

interface MarketCardProps {
  image?: string
  market: Market
  onBet: (outcome: Outcome) => void
  position: number
  total: number
}

export function MarketCard({ image, market, onBet, position, total }: MarketCardProps) {
  const yesShare = poolShare(market.yesPool, market.noPool)

  return (
    <article
      data-market-card
      className="relative h-full snap-start snap-always overflow-hidden bg-zinc-950 text-white"
      style={{ backgroundColor: image ? undefined : fallbackColor(market.id) }}
    >
      {image && (
        <img
          src={image}
          alt=""
          loading={position <= 2 ? 'eager' : 'lazy'}
          className="absolute inset-0 size-full object-cover"
        />
      )}
      <div className="absolute inset-0 bg-black/30" />
      <div className="absolute inset-x-0 bottom-0 border-t border-white/10 bg-black/60 px-5 pb-28 pt-8 backdrop-blur-sm">
        <div className="mb-3 flex items-center gap-2">
          <Badge className="border-white/20 bg-black/35 text-white backdrop-blur-md">
            {eventStatusLabel(market.status)}
          </Badge>
          <span className="text-xs text-white/65">
            {position}/{total}
          </span>
        </div>
        <h1 className="text-balance text-2xl leading-tight font-semibold tracking-tight sm:text-3xl">
          {market.question}
        </h1>
        <div className="mt-3 flex items-center gap-2 text-xs text-white/75">
          <Clock3 className="size-3.5" aria-hidden="true" />
          <span>Wetten {formatDeadline(market.closesAt)}</span>
        </div>
        <div className="mt-5">
          <div className="mb-2 flex justify-between text-xs font-medium">
            <span>JA · {formatMoney(market.yesPool)}</span>
            <span>NEIN · {formatMoney(market.noPool)}</span>
          </div>
          <div
            className="flex h-1.5 overflow-hidden rounded-full bg-rose-400"
            aria-label={`Poolverteilung: ${yesShare.toFixed(0)} Prozent JA`}
          >
            <div className="bg-emerald-400" style={{ width: `${yesShare}%` }} />
          </div>
        </div>
        {market.status === 'OPEN' ? (
          <div className="mt-5 grid grid-cols-2 gap-3">
            <Button
              size="lg"
              className="h-12 bg-emerald-500 text-base text-white shadow-lg hover:bg-emerald-600"
              onClick={() => onBet('YES')}
            >
              JA
            </Button>
            <Button
              size="lg"
              className="h-12 bg-rose-500 text-base text-white shadow-lg hover:bg-rose-600"
              onClick={() => onBet('NO')}
            >
              NEIN
            </Button>
          </div>
        ) : (
          <div className="mt-5 rounded-xl border border-white/15 bg-black/30 p-3 text-sm backdrop-blur-md">
            Dieser Markt ist geschlossen. {eventStatusLabel(market.status)}.
          </div>
        )}
      </div>
    </article>
  )
}
