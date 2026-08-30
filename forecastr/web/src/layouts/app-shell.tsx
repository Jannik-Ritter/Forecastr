import { useQuery, useQueryClient } from '@tanstack/react-query'
import { ShieldCheck } from 'lucide-react'
import { useEffect } from 'react'
import { Link, Outlet, useLocation } from 'react-router-dom'

import { DesktopNavigation, MobileNavigation } from '@/components/navigation'
import { ApiError } from '@/core/api-client'
import { forecastrApi, queryKeys } from '@/core/forecastr-api'
import { useLiveUpdates } from '@/hooks/use-live-updates'
import { formatMoney } from '@/lib/format'
import { cn } from '@/lib/utils'
import { useSession } from '@/session/session-context'

export function AppShell() {
  const { user, selectUser, logout } = useSession()
  const location = useLocation()
  const queryClient = useQueryClient()
  const isFeed = location.pathname === '/feed'
  const userId = user!.id
  const userQuery = useQuery({
    queryKey: queryKeys.user(userId),
    queryFn: () => forecastrApi.getUser(userId),
  })
  const balanceQuery = useQuery({
    queryKey: queryKeys.balance(userId),
    queryFn: () => forecastrApi.getBalance(userId),
  })

  useLiveUpdates(userId)

  useEffect(() => {
    if (userQuery.data) {
      selectUser(userQuery.data)
    }
  }, [selectUser, userQuery.data])

  useEffect(() => {
    if (userQuery.error instanceof ApiError && userQuery.error.status === 404) {
      queryClient.clear()
      logout()
    }
  }, [logout, queryClient, userQuery.error])

  return (
    <div className="h-dvh overflow-hidden bg-muted/35 lg:grid lg:grid-cols-[220px_480px_220px] lg:justify-center lg:gap-6 lg:p-4">
      <DesktopNavigation />
      <main className="relative h-dvh overflow-hidden bg-background shadow-2xl lg:h-[calc(100dvh-2rem)] lg:rounded-[2rem] lg:border">
        <header className="pointer-events-none absolute inset-x-0 top-0 z-40 flex h-16 items-center justify-between bg-black/55 px-4 text-white backdrop-blur-md">
          <Link to="/feed" className="pointer-events-auto font-semibold tracking-tight">
            FORECASTR
          </Link>
          <div className="pointer-events-auto flex items-center gap-2">
            <Link
              to="/wallet"
              className="rounded-full border border-white/15 bg-black/35 px-3 py-1.5 text-xs font-medium backdrop-blur-md"
            >
              {balanceQuery.data ? formatMoney(balanceQuery.data.balance) : '–'}
            </Link>
            <Link
              to="/profile"
              aria-label={`Profil von ${user!.username}`}
              className="grid size-8 place-items-center rounded-full bg-white text-xs font-bold text-black"
            >
              {user!.username.slice(0, 2).toUpperCase()}
            </Link>
          </div>
        </header>
        <div
          className={cn(
            'h-full',
            isFeed ? 'overflow-hidden' : 'overflow-y-auto bg-background px-4 pb-24 pt-20',
          )}
        >
          <Outlet />
        </div>
        <MobileNavigation />
      </main>
      <aside className="hidden h-[calc(100dvh-2rem)] flex-col py-8 lg:flex">
        <div className="space-y-4 rounded-2xl border bg-card p-4 shadow-sm">
          <div>
            <p className="text-sm font-medium">{user!.username}</p>
            <p className="text-xs text-muted-foreground">Demo-Konto</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Verfügbar</p>
            <p className="mt-1 text-lg font-semibold">
              {balanceQuery.data ? formatMoney(balanceQuery.data.balance) : '–'}
            </p>
          </div>
          {user!.isAdmin && (
            <Link to="/admin" className="flex items-center gap-2 text-sm font-medium">
              <ShieldCheck className="size-4" aria-hidden="true" />
              Admin-Panel
            </Link>
          )}
        </div>
      </aside>
    </div>
  )
}
