import { useQuery, useQueryClient } from '@tanstack/react-query'
import { ShieldCheck } from 'lucide-react'
import { useEffect } from 'react'
import { Link, Outlet, useLocation } from 'react-router-dom'

import { BrandLogo } from '@/components/brand-logo'
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
    document.body.dataset.appShell = 'true'
    return () => {
      delete document.body.dataset.appShell
    }
  }, [])

  useEffect(() => {
    if (userQuery.error instanceof ApiError && userQuery.error.status === 404) {
      queryClient.clear()
      logout()
    }
  }, [logout, queryClient, userQuery.error])

  return (
    <div className="h-dvh overflow-hidden bg-background lg:flex">
      <DesktopNavigation />
      <main className="relative h-dvh min-w-0 flex-1 overflow-hidden bg-background">
        <header
          className={cn(
            'pointer-events-none absolute inset-x-0 top-0 z-40 flex h-16 items-center justify-between px-4 backdrop-blur-md lg:hidden',
            isFeed
              ? 'bg-background/45 text-foreground'
              : 'border-b bg-background/90 text-foreground',
          )}
        >
          <Link
            to="/feed"
            aria-label="Forecastr Feed"
            className="pointer-events-auto text-xl"
          >
            <BrandLogo />
          </Link>
          <div className="pointer-events-auto flex items-center gap-2">
            <Link
              to="/wallet"
              className={cn(
                'ui-pressable ui-touch-target rounded-full border px-3 py-1.5 text-xs font-medium tabular-nums backdrop-blur-md',
                isFeed ? 'border-foreground/15 bg-background/35' : 'border-border bg-card',
              )}
            >
              {balanceQuery.data ? formatMoney(balanceQuery.data.balance) : '–'}
            </Link>
            <Link
              to="/profile"
              aria-label={`Profil von ${user!.username}`}
              className="ui-pressable ui-touch-target grid size-8 place-items-center rounded-full bg-primary text-xs font-bold text-primary-foreground"
            >
              {user!.username.slice(0, 2).toUpperCase()}
            </Link>
          </div>
        </header>
        <header className="pointer-events-none absolute right-5 top-4 z-50 hidden items-center justify-end lg:flex">
          <div className="pointer-events-auto flex items-center gap-1 rounded-full border bg-card/90 p-1 shadow-xl backdrop-blur-xl">
            {user!.isAdmin && (
              <Link
                to="/admin"
                aria-label="Admin-Panel"
                className="ui-pressable ui-touch-target grid size-9 place-items-center rounded-full text-muted-foreground hover-fine:bg-muted hover-fine:text-foreground"
              >
                <ShieldCheck className="size-4" aria-hidden="true" />
              </Link>
            )}
            <Link
              to="/wallet"
              className="ui-pressable ui-touch-target rounded-full px-3 py-2 text-xs font-semibold tabular-nums hover-fine:bg-muted"
            >
              {balanceQuery.data ? formatMoney(balanceQuery.data.balance) : '–'}
            </Link>
            <Link
              to="/profile"
              aria-label={`Profil von ${user!.username}`}
              className="ui-pressable ui-touch-target grid size-9 place-items-center rounded-full bg-primary text-xs font-bold text-primary-foreground"
            >
              {user!.username.slice(0, 2).toUpperCase()}
            </Link>
          </div>
        </header>
        <div
          className={cn(
            'h-full',
            isFeed
              ? 'overflow-hidden bg-background'
              : 'overflow-y-auto bg-background px-4 pb-24 pt-20 lg:px-10 lg:pb-12 lg:pt-24 xl:px-16',
          )}
        >
          <Outlet />
        </div>
        <MobileNavigation />
      </main>
    </div>
  )
}
