import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { BarChart3, Database, FileUp, Gavel, LoaderCircle, ShieldCheck } from 'lucide-react'
import { useState, type CSSProperties } from 'react'
import { toast } from 'sonner'

import { EmptyState, PageError, PageLoading } from '@/components/page-state'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { forecastrApi, queryKeys } from '@/core/forecastr-api'
import type { ManualResolution, Market, Outcome } from '@/core/types'
import { eventStatusLabel, formatMoney } from '@/lib/format'
import { parseMoneyInput } from '@/lib/money'
import { cn } from '@/lib/utils'
import { useSession } from '@/session/session-context'

const adminSections = [
  { value: 'overview', label: 'Übersicht', icon: BarChart3 },
  { value: 'resolve', label: 'Auflösen', icon: Gavel },
  { value: 'import', label: 'Import', icon: FileUp },
  { value: 'seed', label: 'Testdaten', icon: Database },
]

export function AdminPage() {
  const { user } = useSession()
  const [activeSection, setActiveSection] = useState('overview')
  const [isPointerSelection, setIsPointerSelection] = useState(false)
  const activeSectionIndex = adminSections.findIndex(
    (section) => section.value === activeSection,
  )
  const indicatorTransform = `translate3d(${activeSectionIndex * 100}%, 0, 0)`
  const indicatorContentTransform = `translate3d(${-activeSectionIndex * (100 / adminSections.length)}%, 0, 0)`

  return (
    <div className="ui-page-enter mx-auto max-w-2xl">
      <div className="mb-6 flex items-center gap-3">
        <div className="grid size-10 place-items-center rounded-xl bg-foreground text-background">
          <ShieldCheck className="size-5" aria-hidden="true" />
        </div>
        <div>
          <p className="text-xs font-medium tracking-widest text-muted-foreground uppercase">Administration</p>
          <h1 className="text-2xl font-semibold tracking-tight">Admin-Panel</h1>
        </div>
      </div>
      <Tabs value={activeSection} onValueChange={setActiveSection}>
        <TabsList
          className="relative grid w-full grid-cols-4 overflow-hidden rounded-xl border border-border bg-card/85 p-1 shadow-sm group-data-horizontal/tabs:h-12"
          onPointerDown={() => setIsPointerSelection(true)}
          onKeyDown={() => setIsPointerSelection(false)}
        >
          <span
            aria-hidden="true"
            className={cn(
              'admin-tabs-indicator pointer-events-none absolute inset-y-1 left-1 z-20 w-[calc((100%-0.5rem)/4)] overflow-hidden rounded-lg bg-accent text-accent-foreground shadow-sm',
              isPointerSelection && 'ui-selection-indicator',
            )}
            style={{ transform: indicatorTransform }}
          >
            <span
              className={cn(
                'admin-tabs-indicator-content absolute inset-y-0 left-0 grid w-[400%] grid-cols-4',
                isPointerSelection && 'ui-selection-indicator-content',
              )}
              style={{ transform: indicatorContentTransform }}
            >
              {adminSections.map(({ value, label, icon: Icon }) => (
                <span
                  key={value}
                  className="flex h-full min-w-0 items-center justify-center gap-1.5 px-1.5 text-sm font-medium"
                >
                  <Icon className="size-4" aria-hidden="true" />
                  <span className="hidden sm:inline">{label}</span>
                </span>
              ))}
            </span>
          </span>
          {adminSections.map(({ value, label, icon: Icon }) => (
            <TabsTrigger
              key={value}
              value={value}
              aria-label={label}
              className="relative z-10 h-full rounded-lg text-muted-foreground data-active:border-transparent data-active:bg-transparent data-active:text-muted-foreground data-active:shadow-none focus-visible:z-30 dark:data-active:border-transparent dark:data-active:bg-transparent dark:data-active:text-muted-foreground"
            >
              <Icon aria-hidden="true" />
              <span className="hidden sm:inline">{label}</span>
            </TabsTrigger>
          ))}
        </TabsList>
        <TabsContent value="overview" className="mt-5">
          <Overview userId={user!.id} />
        </TabsContent>
        <TabsContent value="resolve" className="mt-5">
          <Resolution userId={user!.id} />
        </TabsContent>
        <TabsContent value="import" className="mt-5">
          <ImportEvents userId={user!.id} />
        </TabsContent>
        <TabsContent value="seed" className="mt-5 space-y-5">
          <SeedUsers userId={user!.id} />
          <SeedEvents userId={user!.id} />
        </TabsContent>
      </Tabs>
    </div>
  )
}

function Overview({ userId }: { userId: string }) {
  const stats = useQuery({
    queryKey: queryKeys.stats,
    queryFn: () => forecastrApi.getAdminStats(userId),
  })

  if (stats.isLoading) {
    return <PageLoading label="Statistik wird geladen …" />
  }
  if (stats.error) {
    return <PageError error={stats.error} retry={() => void stats.refetch()} />
  }

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-3 gap-3">
        <StatCard label="Benutzer" value={stats.data!.users.toString()} />
        <StatCard label="Ereignisse" value={stats.data!.events.toString()} />
        <StatCard label="Wetten" value={stats.data!.bets.toString()} />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <StatCard label="Auszahlungen" value={formatMoney(stats.data!.payouts)} />
        <StatCard label="Gebühren" value={formatMoney(stats.data!.fees)} />
      </div>
      <Card>
        <CardHeader>
          <CardTitle>Ereignisstatus</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {Object.entries(stats.data!.eventsByStatus).map(([status, count]) => (
            <div key={status} className="flex items-center justify-between text-sm">
              <span className="text-muted-foreground">{eventStatusText(status)}</span>
              <span className="font-medium tabular-nums">{count}</span>
            </div>
          ))}
        </CardContent>
      </Card>
      <Card>
        <CardHeader>
          <CardTitle>Resolver</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {Object.entries(stats.data!.resolver).map(([name, count]) => (
            <div key={name} className="flex items-center justify-between text-sm">
              <span className="text-muted-foreground">{name}</span>
              <span className="font-medium tabular-nums">{count}</span>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  )
}

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <Card>
      <CardContent className="p-4">
        <p className="text-xs text-muted-foreground">{label}</p>
        <p className="mt-1 truncate text-lg font-semibold tabular-nums">{value}</p>
      </CardContent>
    </Card>
  )
}

function Resolution({ userId }: { userId: string }) {
  const queryClient = useQueryClient()
  const [selected, setSelected] = useState<Market | null>(null)
  const [pendingResolution, setPendingResolution] = useState<ManualResolution | null>(null)
  const markets = useQuery({
    queryKey: queryKeys.search('', 'OPEN'),
    queryFn: () => forecastrApi.searchEvents(userId, '', 'OPEN'),
  })
  const resolve = useMutation({
    mutationFn: (outcome: ManualResolution) =>
      forecastrApi.resolveEvent(userId, selected!.id, outcome),
    onSuccess: (result) => {
      toast.success(
        result.changed
          ? `Markt aufgelöst · ${formatMoney(result.payouts)} ausgezahlt`
          : `Markt war bereits ${eventStatusLabel(result.status).toLowerCase()}.`,
      )
      setSelected(null)
      setPendingResolution(null)
      void queryClient.invalidateQueries({ queryKey: queryKeys.stats })
      void queryClient.invalidateQueries({ queryKey: queryKeys.feed })
      void queryClient.invalidateQueries({ queryKey: ['events'] })
    },
    onError: (error: Error) => toast.error(error.message),
  })

  if (markets.isLoading) {
    return <PageLoading label="Offene Märkte werden geladen …" />
  }
  if (markets.error) {
    return <PageError error={markets.error} retry={() => void markets.refetch()} />
  }

  return (
    <>
      <div className="space-y-3">
        {markets.data?.length === 0 && (
          <EmptyState
            title="Keine offenen Ereignisse"
            description="Sobald ein Markt geöffnet ist, kannst du ihn hier auflösen."
          />
        )}
        {markets.data?.map((market, index) => (
          <Card
            key={market.id}
            className="ui-stagger-item"
            style={{ '--stagger-index': index } as CSSProperties}
          >
            <CardContent className="flex items-start justify-between gap-3 p-4">
              <div>
                <p className="font-medium leading-snug">{market.question}</p>
                <p className="mt-2 text-xs text-muted-foreground">
                  JA {formatMoney(market.yesPool)} · NEIN {formatMoney(market.noPool)}
                </p>
              </div>
              <Button
                size="sm"
                variant="outline"
                onClick={() => {
                  resolve.reset()
                  setPendingResolution(null)
                  setSelected(market)
                }}
              >
                Auflösen
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>
      <Dialog
        open={selected !== null && pendingResolution === null}
        onOpenChange={(open) => {
          if (!open && pendingResolution === null) {
            resolve.reset()
            setSelected(null)
          }
        }}
      >
        <DialogContent>
          {selected && (
            <>
              <DialogHeader>
                <DialogTitle>Markt auflösen</DialogTitle>
                <DialogDescription>{selected.question}</DialogDescription>
              </DialogHeader>
              <p className="text-sm">Das Ergebnis löst Auszahlungen oder Erstattungen aus und kann nicht zurückgenommen werden.</p>
              <div className="grid grid-cols-3 gap-2">
                <Button
                  className="bg-outcome-yes text-outcome-yes-foreground hover-fine:brightness-95"
                  onClick={() => setPendingResolution('YES')}
                >
                  JA
                </Button>
                <Button
                  className="bg-outcome-no text-outcome-no-foreground hover-fine:brightness-95"
                  onClick={() => setPendingResolution('NO')}
                >
                  NEIN
                </Button>
                <Button variant="outline" onClick={() => setPendingResolution('REFUND')}>
                  Erstatten
                </Button>
              </div>
            </>
          )}
        </DialogContent>
      </Dialog>
      <AlertDialog
        open={selected !== null && pendingResolution !== null}
        onOpenChange={(open) => {
          if (!open && !resolve.isPending) {
            setPendingResolution(null)
          }
        }}
      >
        <AlertDialogContent aria-busy={resolve.isPending}>
          {selected && pendingResolution && (
            <>
              <AlertDialogHeader>
                <AlertDialogTitle>{resolutionLabel(pendingResolution)} endgültig bestätigen?</AlertDialogTitle>
                <AlertDialogDescription>
                  Der Markt „{selected.question}“ wird {resolutionDescription(pendingResolution)}.
                  Diese Aktion löst Zahlungen aus und kann nicht rückgängig gemacht werden.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel disabled={resolve.isPending}>Zurück</AlertDialogCancel>
                <AlertDialogAction
                  variant="destructive"
                  disabled={resolve.isPending}
                  aria-busy={resolve.isPending}
                  onClick={(event) => {
                    event.preventDefault()
                    resolve.mutate(pendingResolution)
                  }}
                >
                  {resolve.isPending && <LoaderCircle className="animate-spin motion-reduce:animate-none" aria-hidden="true" />}
                  Endgültig bestätigen
                </AlertDialogAction>
              </AlertDialogFooter>
            </>
          )}
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}

function ImportEvents({ userId }: { userId: string }) {
  const queryClient = useQueryClient()
  const [path, setPath] = useState('')
  const mutation = useMutation({
    mutationFn: () => forecastrApi.importEvents(userId, path),
    onSuccess: () => {
      toast.success('Import abgeschlossen.')
      void queryClient.invalidateQueries({ queryKey: queryKeys.feed })
      void queryClient.invalidateQueries({ queryKey: ['events'] })
      void queryClient.invalidateQueries({ queryKey: queryKeys.stats })
    },
    onError: (error: Error) => toast.error(error.message),
  })

  return (
    <Card>
      <CardHeader>
        <CardTitle>CSV importieren</CardTitle>
        <CardDescription>
          Der Pfad wird auf dem Server gelesen. Ein leerer Pfad importiert die gebündelten Standarddateien.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form
          className="space-y-4"
          aria-busy={mutation.isPending}
          onSubmit={(event) => {
            event.preventDefault()
            mutation.mutate()
          }}
        >
          <div className="space-y-2">
            <Label htmlFor="import-path">Serverpfad</Label>
            <Input id="import-path" value={path} placeholder="Leer = Standards" onChange={(event) => setPath(event.target.value)} />
          </div>
          <Button type="submit" disabled={mutation.isPending} aria-busy={mutation.isPending}>
            {mutation.isPending && <LoaderCircle className="animate-spin motion-reduce:animate-none" aria-hidden="true" />}
            Importieren
          </Button>
        </form>
        {mutation.data && (
          <div
            className="admin-import-result mt-5 rounded-xl bg-muted p-4 text-sm"
            role="status"
            aria-live="polite"
          >
            <p>
              {mutation.data.accepted} übernommen · {mutation.data.skipped} übersprungen ·{' '}
              {mutation.data.rejected} abgelehnt
            </p>
            {mutation.data.errors.map((error) => (
              <p key={error} className="mt-2 text-destructive">{error}</p>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function SeedUsers({ userId }: { userId: string }) {
  const queryClient = useQueryClient()
  const [count, setCount] = useState('1')
  const [betsPerUser, setBetsPerUser] = useState('0')
  const [eventId, setEventId] = useState('')
  const [outcome, setOutcome] = useState<'RANDOM' | Outcome>('RANDOM')
  const [stake, setStake] = useState('')
  const [error, setError] = useState('')
  const mutation = useMutation({
    mutationFn: (values: {
      count: number
      betsPerUser: number
      eventId?: string
      outcome?: Outcome
      stake?: string
    }) => forecastrApi.seedTestUsers(userId, values),
    onSuccess: (result) => {
      toast.success(`${result.userIds.length} Konten und ${result.betIds.length} Wetten erzeugt.`)
      setError('')
      void queryClient.invalidateQueries({ queryKey: ['users'] })
      void queryClient.invalidateQueries({ queryKey: queryKeys.feed })
      void queryClient.invalidateQueries({ queryKey: queryKeys.stats })
    },
    onError: (mutationError: Error) => toast.error(mutationError.message),
  })

  const submit = (submitEvent: React.FormEvent) => {
    submitEvent.preventDefault()
    const parsedCount = positiveInteger(count)
    const parsedBets = nonNegativeInteger(betsPerUser)
    const parsedStake = stake ? parseMoneyInput(stake).value : undefined
    if (!parsedCount || parsedBets === null) {
      setError('Anzahl und Wetten pro Konto sind ungültig.')
      return
    }
    if (eventId && !/^[1-9]\d*$/.test(eventId)) {
      setError('Die Ereignis-ID muss positiv sein.')
      return
    }
    if (stake && !parsedStake) {
      setError('Der Einsatz ist ungültig.')
      return
    }
    mutation.mutate({
      count: parsedCount,
      betsPerUser: parsedBets,
      ...(parsedBets > 0 && eventId ? { eventId } : {}),
      ...(parsedBets > 0 && outcome !== 'RANDOM' ? { outcome } : {}),
      ...(parsedBets > 0 && parsedStake ? { stake: parsedStake } : {}),
    })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Testkonten erstellen</CardTitle>
        <CardDescription>Optional werden pro Konto direkt Wetten erzeugt.</CardDescription>
      </CardHeader>
      <CardContent>
        <form
          className="space-y-4"
          onSubmit={submit}
          aria-busy={mutation.isPending}
          aria-describedby={error ? 'seed-users-error' : undefined}
        >
          <div className="grid grid-cols-2 gap-3">
            <Field label="Anzahl Konten" id="seed-user-count" value={count} onChange={setCount} />
            <Field label="Wetten pro Konto" id="seed-user-bets" value={betsPerUser} onChange={setBetsPerUser} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="seed-event-id">Ereignis-ID (optional)</Label>
            <Input id="seed-event-id" inputMode="numeric" value={eventId} disabled={betsPerUser === '0'} onChange={(event) => setEventId(event.target.value.trim())} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label htmlFor="seed-user-outcome">Ergebnis</Label>
              <Select value={outcome} onValueChange={(value) => setOutcome(value as 'RANDOM' | Outcome)} disabled={betsPerUser === '0'}>
                <SelectTrigger id="seed-user-outcome"><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="RANDOM">Zufällig</SelectItem>
                  <SelectItem value="YES">JA</SelectItem>
                  <SelectItem value="NO">NEIN</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <Field label="Einsatz (optional)" id="seed-stake" value={stake} onChange={setStake} disabled={betsPerUser === '0'} decimal />
          </div>
          {error && (
            <p id="seed-users-error" className="text-xs text-destructive" role="alert">
              {error}
            </p>
          )}
          <Button type="submit" disabled={mutation.isPending} aria-busy={mutation.isPending}>
            {mutation.isPending && <LoaderCircle className="animate-spin motion-reduce:animate-none" aria-hidden="true" />}
            Konten erzeugen
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}

function SeedEvents({ userId }: { userId: string }) {
  const queryClient = useQueryClient()
  const [count, setCount] = useState('1')
  const [lifetime, setLifetime] = useState('10')
  const [error, setError] = useState('')
  const mutation = useMutation({
    mutationFn: ({ amount, minutes }: { amount: number; minutes?: number }) =>
      forecastrApi.seedTestEvents(userId, amount, minutes),
    onSuccess: (result) => {
      toast.success(`${result.eventIds.length} Ereignisse erzeugt.`)
      setError('')
      void queryClient.invalidateQueries({ queryKey: queryKeys.feed })
      void queryClient.invalidateQueries({ queryKey: ['events'] })
      void queryClient.invalidateQueries({ queryKey: queryKeys.stats })
    },
    onError: (mutationError: Error) => toast.error(mutationError.message),
  })

  return (
    <Card>
      <CardHeader>
        <CardTitle>Testereignisse erstellen</CardTitle>
        <CardDescription>Die maximale Laufzeit beträgt 1.440 Minuten.</CardDescription>
      </CardHeader>
      <CardContent>
        <form
          className="space-y-4"
          aria-busy={mutation.isPending}
          aria-describedby={error ? 'seed-events-error' : undefined}
          onSubmit={(event) => {
            event.preventDefault()
            const parsedCount = positiveInteger(count)
            const parsedLifetime = lifetime ? positiveInteger(lifetime) : 10
            if (!parsedCount || !parsedLifetime || parsedLifetime > 1440) {
              setError('Anzahl oder Laufzeit ist ungültig.')
              return
            }
            mutation.mutate({ amount: parsedCount, minutes: parsedLifetime })
          }}
        >
          <div className="grid grid-cols-2 gap-3">
            <Field label="Anzahl Ereignisse" id="seed-event-count" value={count} onChange={setCount} />
            <Field label="Laufzeit (Min.)" id="seed-event-lifetime" value={lifetime} onChange={setLifetime} />
          </div>
          {error && (
            <p id="seed-events-error" className="text-xs text-destructive" role="alert">
              {error}
            </p>
          )}
          <Button type="submit" disabled={mutation.isPending} aria-busy={mutation.isPending}>
            {mutation.isPending && <LoaderCircle className="animate-spin motion-reduce:animate-none" aria-hidden="true" />}
            Ereignisse erzeugen
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}

function Field({
  label,
  id,
  value,
  onChange,
  disabled = false,
  decimal = false,
}: {
  label: string
  id: string
  value: string
  onChange: (value: string) => void
  disabled?: boolean
  decimal?: boolean
}) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <Input
        id={id}
        inputMode={decimal ? 'decimal' : 'numeric'}
        value={value}
        disabled={disabled}
        onChange={(event) => onChange(event.target.value.trim())}
      />
    </div>
  )
}

function positiveInteger(value: string): number | null {
  if (!/^[1-9]\d*$/.test(value)) {
    return null
  }
  const parsed = Number(value)
  return Number.isSafeInteger(parsed) ? parsed : null
}

function nonNegativeInteger(value: string): number | null {
  if (!/^\d+$/.test(value)) {
    return null
  }
  const parsed = Number(value)
  return Number.isSafeInteger(parsed) ? parsed : null
}

function eventStatusText(status: string): string {
  const labels: Record<string, string> = {
    OPEN: 'Offen',
    RESOLVED_YES: 'Mit JA aufgelöst',
    RESOLVED_NO: 'Mit NEIN aufgelöst',
    EXPIRED: 'Erstattet',
    ARCHIVED: 'Archiviert',
  }
  return labels[status] ?? status
}

function resolutionLabel(resolution: ManualResolution): string {
  if (resolution === 'REFUND') {
    return 'Erstattung'
  }
  return resolution === 'YES' ? 'JA' : 'NEIN'
}

function resolutionDescription(resolution: ManualResolution): string {
  if (resolution === 'REFUND') {
    return 'endgültig erstattet'
  }
  return `endgültig mit ${resolutionLabel(resolution)} aufgelöst`
}
