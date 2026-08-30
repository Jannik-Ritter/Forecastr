import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowDownToLine, ArrowUpFromLine, LoaderCircle, WalletCards } from 'lucide-react'
import { useMemo, useState } from 'react'
import { toast } from 'sonner'

import { PageError, PageLoading } from '@/components/page-state'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { forecastrApi, queryKeys } from '@/core/forecastr-api'
import { formatMoney } from '@/lib/format'
import { compareMoney, parseMoneyInput } from '@/lib/money'
import { useSession } from '@/session/session-context'

type WalletOperation = 'deposit' | 'withdraw'

export function WalletPage() {
  const { user } = useSession()
  const queryClient = useQueryClient()
  const [operation, setOperation] = useState<WalletOperation | null>(null)
  const [amount, setAmount] = useState('')
  const parsedAmount = useMemo(() => parseMoneyInput(amount), [amount])
  const balance = useQuery({
    queryKey: queryKeys.balance(user!.id),
    queryFn: () => forecastrApi.getBalance(user!.id),
  })
  const updateBalance = useMutation({
    mutationFn: (value: string) => forecastrApi.updateBalance(user!.id, operation!, value),
    onSuccess: (result) => {
      queryClient.setQueryData(queryKeys.balance(user!.id), result)
      toast.success(`Neues Guthaben: ${formatMoney(result.balance)}`)
      closeDialog()
    },
    onError: (error: Error) => {
      toast.error(error.message)
      void queryClient.invalidateQueries({ queryKey: queryKeys.balance(user!.id) })
    },
  })

  if (balance.isLoading) {
    return <PageLoading label="Wallet wird geladen …" />
  }
  if (balance.error) {
    return <PageError error={balance.error} retry={() => void balance.refetch()} />
  }

  const isOverBalance =
    operation === 'withdraw' && parsedAmount.value && balance.data
      ? compareMoney(parsedAmount.value, balance.data.balance) > 0
      : false

  function openDialog(nextOperation: WalletOperation) {
    setAmount('')
    updateBalance.reset()
    setOperation(nextOperation)
  }

  function closeDialog() {
    setOperation(null)
    setAmount('')
  }

  return (
    <div className="mx-auto max-w-lg">
      <div className="mb-6">
        <p className="text-xs font-medium tracking-widest text-muted-foreground uppercase">Guthaben</p>
        <h1 className="mt-1 text-2xl font-semibold tracking-tight">Wallet</h1>
      </div>
      <Card className="overflow-hidden border-0 bg-foreground text-background shadow-xl">
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardDescription className="text-background/60">Verfügbares Guthaben</CardDescription>
              <CardTitle className="mt-2 text-4xl tracking-tight">{formatMoney(balance.data?.balance)}</CardTitle>
            </div>
            <WalletCards className="size-8 opacity-60" aria-hidden="true" />
          </div>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-3">
          <Button variant="secondary" size="lg" onClick={() => openDialog('deposit')}>
            <ArrowDownToLine aria-hidden="true" />
            Einzahlen
          </Button>
          <Button variant="secondary" size="lg" onClick={() => openDialog('withdraw')}>
            <ArrowUpFromLine aria-hidden="true" />
            Auszahlen
          </Button>
        </CardContent>
      </Card>
      <p className="mt-4 text-sm text-muted-foreground">
        Ein- und Auszahlungen gelten sofort. Offene Wetten sind bereits vom verfügbaren Guthaben abgezogen.
      </p>

      <Dialog open={operation !== null} onOpenChange={(open) => !open && closeDialog()}>
        <DialogContent>
          <form
            onSubmit={(event) => {
              event.preventDefault()
              if (parsedAmount.value && !isOverBalance) {
                updateBalance.mutate(parsedAmount.value)
              }
            }}
          >
            <DialogHeader>
              <DialogTitle>{operation === 'deposit' ? 'Geld einzahlen' : 'Geld auszahlen'}</DialogTitle>
              <DialogDescription>
                Aktuell verfügbar: {formatMoney(balance.data?.balance)}
              </DialogDescription>
            </DialogHeader>
            <div className="my-5 space-y-2">
              <Label htmlFor="wallet-amount">Betrag in EUR</Label>
              <Input
                id="wallet-amount"
                autoFocus
                inputMode="decimal"
                placeholder="25,00"
                value={amount}
                aria-invalid={Boolean(amount && (parsedAmount.error || isOverBalance))}
                onChange={(event) => setAmount(event.target.value)}
              />
              {amount && parsedAmount.error && <p className="text-xs text-destructive">{parsedAmount.error}</p>}
              {isOverBalance && <p className="text-xs text-destructive">Das Guthaben reicht für diesen Betrag nicht aus.</p>}
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={closeDialog}>
                Abbrechen
              </Button>
              <Button type="submit" disabled={!parsedAmount.value || isOverBalance || updateBalance.isPending}>
                {updateBalance.isPending && <LoaderCircle className="animate-spin" />}
                Bestätigen
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}
