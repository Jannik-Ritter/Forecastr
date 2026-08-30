import { useQuery } from '@tanstack/react-query'
import { Search } from 'lucide-react'
import { useState } from 'react'

import { BetDrawer } from '@/components/bet-drawer'
import { EmptyState, PageError, PageLoading } from '@/components/page-state'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { forecastrApi, queryKeys } from '@/core/forecastr-api'
import type { Market, Outcome } from '@/core/types'
import { eventStatusLabel, formatDeadline, formatMoney } from '@/lib/format'
import { usePetImages } from '@/lib/pet-images'
import { cn } from '@/lib/utils'
import { useSession } from '@/session/session-context'

export function SearchPage() {
  const { user } = useSession()
  const [input, setInput] = useState('')
  const [term, setTerm] = useState<string | null>(null)
  const [selected, setSelected] = useState<Market | null>(null)
  const [bet, setBet] = useState<{ market: Market; outcome: Outcome } | null>(null)
  const imageFor = usePetImages()
  const results = useQuery({
    queryKey: queryKeys.search(term ?? ''),
    queryFn: () => forecastrApi.searchEvents(user!.id, term ?? ''),
    enabled: term !== null,
  })

  return (
    <div className="mx-auto max-w-2xl">
      <div className="mb-6">
        <p className="text-xs font-medium tracking-widest text-muted-foreground uppercase">Märkte</p>
        <h1 className="mt-1 text-2xl font-semibold tracking-tight">Suchen</h1>
      </div>
      <form
        className="flex gap-2"
        onSubmit={(event) => {
          event.preventDefault()
          setTerm(input.trim())
        }}
      >
        <Input
          type="search"
          value={input}
          placeholder="Thema oder Frage"
          aria-label="Suchbegriff"
          onChange={(event) => setInput(event.target.value)}
        />
        <Button type="submit" aria-label="Suchen">
          <Search aria-hidden="true" />
        </Button>
      </form>
      <p className="mt-2 text-xs text-muted-foreground">
        Eine leere Suche zeigt alle Märkte.
      </p>

      <div className="mt-6 space-y-3">
        {term === null && (
          <EmptyState title="Wonach suchst du?" description="Suche nach einem Thema oder zeige alle Märkte." />
        )}
        {results.isLoading && <PageLoading label="Märkte werden gesucht …" />}
        {results.error && (
          <PageError error={results.error} retry={() => void results.refetch()} />
        )}
        {results.data?.length === 0 && (
          <EmptyState title="Keine Treffer" description="Für diesen Suchbegriff gibt es keine Märkte." />
        )}
        {results.data?.map((market) => {
          const image = imageFor(market.id)
          return (
            <button key={market.id} type="button" onClick={() => setSelected(market)} className="w-full text-left">
              <Card className="overflow-hidden p-0 transition-transform hover:-translate-y-0.5">
                <CardContent className="flex gap-3 p-3">
                  <div
                    className={cn(
                      'size-20 shrink-0 rounded-xl bg-cover bg-center',
                      !image && 'brand-pattern',
                    )}
                    style={image ? { backgroundImage: `url(${image})` } : undefined}
                    aria-hidden="true"
                  />
                  <div className="min-w-0 flex-1 py-1">
                    <Badge variant="secondary" className="mb-2">
                      {eventStatusLabel(market.status)}
                    </Badge>
                    <p className="line-clamp-2 font-medium leading-snug">{market.question}</p>
                    <p className="mt-2 text-xs text-muted-foreground">
                      JA {formatMoney(market.yesPool)} · NEIN {formatMoney(market.noPool)}
                    </p>
                  </div>
                </CardContent>
              </Card>
            </button>
          )
        })}
      </div>

      <Dialog open={selected !== null} onOpenChange={(open) => !open && setSelected(null)}>
        <DialogContent>
          {selected && (
            <>
              <DialogHeader>
                <Badge variant="secondary" className="w-fit">
                  {eventStatusLabel(selected.status)}
                </Badge>
                <DialogTitle className="text-xl leading-snug">{selected.question}</DialogTitle>
                <DialogDescription>Wetten {formatDeadline(selected.closesAt)}</DialogDescription>
              </DialogHeader>
              <div className="grid grid-cols-2 gap-3 rounded-xl bg-muted p-3 text-sm">
                <div>
                  <p className="text-xs text-muted-foreground">JA-Pool</p>
                  <p className="mt-1 font-medium">{formatMoney(selected.yesPool)}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">NEIN-Pool</p>
                  <p className="mt-1 font-medium">{formatMoney(selected.noPool)}</p>
                </div>
              </div>
              {selected.status === 'OPEN' && (
                <div className="grid grid-cols-2 gap-3">
                  <Button
                    className="bg-outcome-yes text-outcome-yes-foreground hover:brightness-95"
                    onClick={() => {
                      setBet({ market: selected, outcome: 'YES' })
                      setSelected(null)
                    }}
                  >
                    JA wetten
                  </Button>
                  <Button
                    className="bg-outcome-no text-outcome-no-foreground hover:brightness-95"
                    onClick={() => {
                      setBet({ market: selected, outcome: 'NO' })
                      setSelected(null)
                    }}
                  >
                    NEIN wetten
                  </Button>
                </div>
              )}
            </>
          )}
        </DialogContent>
      </Dialog>
      <BetDrawer
        market={bet?.market ?? null}
        outcome={bet?.outcome ?? null}
        open={bet !== null}
        onOpenChange={(open) => !open && setBet(null)}
      />
    </div>
  )
}
