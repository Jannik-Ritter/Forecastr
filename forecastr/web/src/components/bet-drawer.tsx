import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { LoaderCircle } from 'lucide-react'
import { useMemo, useState } from 'react'
import { toast } from 'sonner'

import { Button } from '@/components/ui/button'
import {
  Drawer,
  DrawerClose,
  DrawerContent,
  DrawerDescription,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
} from '@/components/ui/drawer'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { forecastrApi, queryKeys } from '@/core/forecastr-api'
import type { Market, Outcome } from '@/core/types'
import { formatMoney, outcomeLabel } from '@/lib/format'
import { compareMoney, parseMoneyInput, potentialProfit } from '@/lib/money'
import { useSession } from '@/session/session-context'

interface BetDrawerProps {
  market: Market | null
  onOpenChange: (isOpen: boolean) => void
  outcome: Outcome | null
  open: boolean
}

export function BetDrawer({ market, outcome, open, onOpenChange }: BetDrawerProps) {
  const { user } = useSession()
  const queryClient = useQueryClient()
  const [amount, setAmount] = useState('')
  const parsedAmount = useMemo(() => parseMoneyInput(amount), [amount])
  const balanceQuery = useQuery({
    queryKey: queryKeys.balance(user!.id),
    queryFn: () => forecastrApi.getBalance(user!.id),
    enabled: open,
  })
  const mutation = useMutation({
    mutationFn: (stake: string) =>
      forecastrApi.placeBet(user!.id, market!.id, outcome!, stake),
    onSuccess: () => {
      toast.success('Wette erfolgreich platziert.')
      void queryClient.invalidateQueries({ queryKey: queryKeys.balance(user!.id) })
      void queryClient.invalidateQueries({ queryKey: queryKeys.bets(user!.id) })
      void queryClient.invalidateQueries({ queryKey: queryKeys.feed })
      void queryClient.invalidateQueries({ queryKey: queryKeys.event(market!.id) })
      closeDrawer()
    },
    onError: (error: Error) => {
      toast.error(error.message)
      void queryClient.invalidateQueries({ queryKey: queryKeys.balance(user!.id) })
      void queryClient.invalidateQueries({ queryKey: queryKeys.feed })
    },
  })

  if (!market || !outcome) {
    return null
  }

  const isOverBalance =
    parsedAmount.value && balanceQuery.data
      ? compareMoney(parsedAmount.value, balanceQuery.data.balance) > 0
      : false
  const error = amount
    ? isOverBalance
      ? 'Das Guthaben reicht für diesen Einsatz nicht aus.'
      : parsedAmount.error
    : undefined
  const profit = parsedAmount.value
    ? potentialProfit(market, outcome, parsedAmount.value)
    : '0.00'

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    if (parsedAmount.value && !isOverBalance) {
      mutation.mutate(parsedAmount.value)
    }
  }

  return (
    <Drawer
      open={open}
      onOpenChange={(isOpen) => {
        if (!isOpen) {
          closeDrawer()
        }
      }}
    >
      <DrawerContent className="lg:left-[calc(50%+7.5rem)] lg:right-auto lg:w-[480px] lg:-translate-x-1/2">
        <form onSubmit={submit} className="mx-auto w-full max-w-md">
          <DrawerHeader>
            <DrawerTitle>
              {outcomeLabel(outcome)} auf „{market.question}“
            </DrawerTitle>
            <DrawerDescription>
              Verfügbar: {formatMoney(balanceQuery.data?.balance)}
            </DrawerDescription>
          </DrawerHeader>
          <div className="space-y-4 px-4">
            <div className="space-y-2">
              <Label htmlFor="bet-amount">Einsatz in EUR</Label>
              <Input
                id="bet-amount"
                autoFocus
                inputMode="decimal"
                placeholder="10,00"
                value={amount}
                aria-invalid={Boolean(error)}
                onChange={(event) => setAmount(event.target.value)}
              />
              {error && <p className="text-xs text-destructive">{error}</p>}
            </div>
            <div className="rounded-xl bg-muted p-3">
              <p className="text-xs text-muted-foreground">Möglicher Nettogewinn</p>
              <p className="mt-1 text-xl font-semibold">{formatMoney(profit)}</p>
              <p className="mt-1 text-xs text-muted-foreground">
                Nach 5 % Gebühr auf den Gewinn. Dein Einsatz ist nicht enthalten.
              </p>
            </div>
          </div>
          <DrawerFooter>
            <Button
              type="submit"
              size="lg"
              disabled={!parsedAmount.value || isOverBalance || mutation.isPending}
              className={
                outcome === 'YES'
                  ? 'bg-outcome-yes text-outcome-yes-foreground hover:brightness-95'
                  : 'bg-outcome-no text-outcome-no-foreground hover:brightness-95'
              }
            >
              {mutation.isPending && <LoaderCircle className="animate-spin" aria-hidden="true" />}
              {formatMoney(parsedAmount.value)} auf {outcomeLabel(outcome)} setzen
            </Button>
            <DrawerClose asChild>
              <Button type="button" variant="outline">
                Abbrechen
              </Button>
            </DrawerClose>
          </DrawerFooter>
        </form>
      </DrawerContent>
    </Drawer>
  )

  function closeDrawer() {
    setAmount('')
    mutation.reset()
    onOpenChange(false)
  }
}
