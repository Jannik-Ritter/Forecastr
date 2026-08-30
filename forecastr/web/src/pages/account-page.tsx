import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight, LoaderCircle, Plus } from 'lucide-react'
import { useState, type CSSProperties } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'

import { BrandLogo } from '@/components/brand-logo'
import { EmptyState, PageError, PageLoading } from '@/components/page-state'
import { Badge } from '@/components/ui/badge'
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
import type { User } from '@/core/types'
import { useSession } from '@/session/session-context'

export function AccountPage() {
  const { user, selectUser } = useSession()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [isCreating, setIsCreating] = useState(false)
  const [username, setUsername] = useState('')
  const users = useQuery({
    queryKey: queryKeys.users(page),
    queryFn: () => forecastrApi.getUserPage(page),
  })
  const createUser = useMutation({
    mutationFn: () => forecastrApi.createUser(username.trim()),
    onSuccess: (createdUser) => {
      queryClient.clear()
      selectUser(createdUser)
      navigate('/feed')
    },
    onError: (error: Error) => toast.error(error.message),
  })

  if (user) {
    return <Navigate to="/feed" replace />
  }

  const choose = (selectedUser: User) => {
    queryClient.clear()
    selectUser(selectedUser)
    navigate('/feed')
  }

  return (
    <main className="dark relative min-h-dvh overflow-hidden bg-background px-4 py-10 text-foreground">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_50%_30%,color-mix(in_oklch,var(--brand)_20%,transparent),transparent_38%),linear-gradient(to_bottom,color-mix(in_oklch,var(--background)_20%,transparent),var(--background)_86%)]" />
      <div className="relative mx-auto flex min-h-[calc(100dvh-5rem)] max-w-md flex-col justify-center">
        <div className="account-chooser-enter mb-7 text-center">
          <h1 className="flex justify-center text-4xl">
            <BrandLogo />
          </h1>
          <p className="mt-2 text-sm text-foreground/60">Wähle ein Demo-Konto und starte den Feed.</p>
        </div>
        <Card className="account-chooser-enter account-chooser-card-enter border-foreground/10 bg-card/95 text-card-foreground shadow-2xl backdrop-blur-xl">
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle>Konto auswählen</CardTitle>
              <Badge variant="secondary">Demo-Modus</Badge>
            </div>
            <CardDescription>Wähle ein Konto aus, um fortzufahren.</CardDescription>
          </CardHeader>
          <CardContent>
            {users.isLoading && <PageLoading label="Konten werden geladen …" />}
            {users.error && (
              <PageError error={users.error} retry={() => void users.refetch()} />
            )}
            {users.data && (
              <div className="space-y-2">
                {users.data.content.length === 0 && (
                  <EmptyState
                    title="Keine Konten"
                    description="Erstelle ein neues Konto, um zu starten."
                  />
                )}
                {users.data.content.map((listedUser, index) => (
                  <button
                    type="button"
                    key={listedUser.id}
                    onClick={() => choose(listedUser)}
                    className="ui-stagger-item ui-pressable flex w-full items-center justify-between rounded-xl border bg-background px-3 py-3 text-left hover-fine:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    style={{ '--stagger-index': index } as CSSProperties}
                  >
                    <span className="flex items-center gap-3">
                      <span className="grid size-9 place-items-center rounded-full bg-primary text-xs font-bold text-primary-foreground">
                        {listedUser.username.slice(0, 2).toUpperCase()}
                      </span>
                      <span className="font-medium">{listedUser.username}</span>
                    </span>
                    {listedUser.isAdmin && <Badge variant="outline">Admin</Badge>}
                  </button>
                ))}
                <Button className="mt-3 w-full" variant="outline" onClick={() => setIsCreating(true)}>
                  <Plus aria-hidden="true" />
                  Neues Konto
                </Button>
                <div className="flex items-center justify-between pt-3">
                  <Button
                    size="icon"
                    variant="ghost"
                    aria-label="Vorherige Seite"
                    disabled={page === 0}
                    onClick={() => setPage((current) => Math.max(0, current - 1))}
                  >
                    <ChevronLeft aria-hidden="true" />
                  </Button>
                  <p className="text-xs text-muted-foreground">
                    Seite {users.data.page + 1}/{Math.max(1, users.data.totalPages)} ·{' '}
                    {users.data.totalElements} Konten
                  </p>
                  <Button
                    size="icon"
                    variant="ghost"
                    aria-label="Nächste Seite"
                    disabled={page + 1 >= users.data.totalPages}
                    onClick={() => setPage((current) => current + 1)}
                  >
                    <ChevronRight aria-hidden="true" />
                  </Button>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
      <Dialog open={isCreating} onOpenChange={setIsCreating}>
        <DialogContent>
          <form
            aria-busy={createUser.isPending}
            onSubmit={(event) => {
              event.preventDefault()
              if (username.trim()) {
                createUser.mutate()
              }
            }}
          >
            <DialogHeader>
              <DialogTitle>Neues Konto</DialogTitle>
              <DialogDescription>Das Startguthaben beträgt 100,00 €.</DialogDescription>
            </DialogHeader>
            <div className="my-5 space-y-2">
              <Label htmlFor="new-username">Benutzername</Label>
              <Input
                id="new-username"
                autoFocus
                maxLength={80}
                value={username}
                onChange={(event) => setUsername(event.target.value)}
              />
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setIsCreating(false)}>
                Abbrechen
              </Button>
              <Button
                type="submit"
                disabled={!username.trim() || createUser.isPending}
                aria-busy={createUser.isPending}
              >
                {createUser.isPending && <LoaderCircle className="animate-spin motion-reduce:animate-none" aria-hidden="true" />}
                Konto erstellen
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </main>
  )
}
