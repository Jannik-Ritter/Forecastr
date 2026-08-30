import { House, LogOut, Search, TicketCheck, UserRound, WalletCards } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'

import { BrandLogo } from '@/components/brand-logo'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'

const destinations = [
  { to: '/feed', label: 'Feed', icon: House },
  { to: '/search', label: 'Suche', icon: Search },
  { to: '/bets', label: 'Wetten', icon: TicketCheck },
  { to: '/wallet', label: 'Wallet', icon: WalletCards },
  { to: '/profile', label: 'Profil', icon: UserRound },
]

const desktopDestinations = destinations.filter((destination) => destination.to !== '/search')

export function MobileNavigation() {
  const location = useLocation()
  const activeDestinationIndex = destinations.findIndex(
    (destination) => location.pathname === destination.to,
  )
  const visibleDestinationIndex = Math.max(activeDestinationIndex, 0)
  const indicatorTransform = `translate3d(${visibleDestinationIndex * 100}%, 0, 0)`
  const indicatorContentTransform = `translate3d(${-visibleDestinationIndex * (100 / destinations.length)}%, 0, 0)`

  return (
    <nav
      aria-label="Hauptnavigation"
      className="absolute inset-x-2 bottom-0 z-40 grid h-[calc(4rem+env(safe-area-inset-bottom))] grid-cols-5 items-center rounded-2xl border border-border bg-card/85 px-1 pb-[env(safe-area-inset-bottom)] text-foreground shadow-2xl backdrop-blur-xl lg:hidden"
    >
      <span
        aria-hidden="true"
        className={cn(
          'mobile-navigation-indicator ui-selection-indicator pointer-events-none absolute bottom-[calc(0.25rem+env(safe-area-inset-bottom))] left-1 top-1 z-20 w-[calc((100%-0.5rem)/5)] overflow-hidden rounded-xl bg-accent text-accent-foreground shadow-sm',
          activeDestinationIndex >= 0 ? 'opacity-100' : 'opacity-0',
        )}
        style={{ transform: indicatorTransform }}
      >
        <span
          className="mobile-navigation-indicator-content ui-selection-indicator-content absolute inset-y-0 left-0 grid w-[500%] grid-cols-5"
          style={{ transform: indicatorContentTransform }}
        >
          {destinations.map(({ to, label, icon: Icon }) => (
            <span
              key={to}
              className="flex h-full min-w-0 flex-col items-center justify-center gap-1 px-1 text-xs"
            >
              <Icon className="size-5" aria-hidden="true" />
              <span>{label}</span>
            </span>
          ))}
        </span>
      </span>
      {destinations.map((destination) => (
        <NavigationLink key={destination.to} {...destination} compact />
      ))}
    </nav>
  )
}

export function DesktopNavigation({ onLogout }: { onLogout: () => void }) {
  const location = useLocation()
  const navigate = useNavigate()
  const [search, setSearch] = useState(() => readSearchTerm(location.pathname, location.search))

  useEffect(() => {
    setSearch(readSearchTerm(location.pathname, location.search))
  }, [location.pathname, location.search])

  return (
    <aside className="hidden h-dvh w-(--app-sidebar-width) shrink-0 flex-col overflow-y-auto border-r border-sidebar-border bg-sidebar px-4 py-5 text-sidebar-foreground lg:flex">
      <Link to="/feed" aria-label="Forecastr Feed" className="mb-4 px-1 py-1 text-[1.75rem]">
        <BrandLogo />
      </Link>

      <form
        role="search"
        aria-label="Märkte suchen"
        className="relative mb-5"
        onSubmit={(event) => {
          event.preventDefault()
          const parameters = new URLSearchParams()
          parameters.set('q', search.trim())
          navigate(`/search?${parameters.toString()}`)
        }}
      >
        <Search
          className="pointer-events-none absolute left-3 top-1/2 z-10 size-5 -translate-y-1/2 text-muted-foreground"
          aria-hidden="true"
        />
        <Input
          type="search"
          value={search}
          aria-label="Märkte suchen"
          placeholder="Suchen"
          autoComplete="off"
          className="h-11 rounded-xl border-transparent bg-sidebar-accent pl-10 pr-3 text-sm font-medium text-sidebar-foreground shadow-none placeholder:text-muted-foreground focus-visible:border-sidebar-ring focus-visible:ring-sidebar-ring/40 dark:bg-sidebar-accent"
          onChange={(event) => setSearch(event.target.value)}
        />
      </form>

      <nav aria-label="Hauptnavigation" className="space-y-1">
        {desktopDestinations.map((destination) => (
          <NavigationLink key={destination.to} {...destination} />
        ))}
      </nav>

      <div className="mt-auto pt-8">
        <Button className="h-11 w-full text-sm font-semibold" onClick={onLogout}>
          <LogOut aria-hidden="true" />
          Ausloggen
        </Button>
        <div className="mt-6 border-t border-sidebar-border pt-4">
          <p className="text-xs leading-relaxed text-muted-foreground">
            Kurze Aufmerksamkeitsspanne.
            <br />
            Langfristige Konsequenzen.
          </p>
          <p className="mt-3 text-[0.7rem] text-muted-foreground">© 2026 Forecastr</p>
        </div>
      </div>
    </aside>
  )
}

function NavigationLink({
  to,
  label,
  icon: Icon,
  compact = false,
}: (typeof destinations)[number] & { compact?: boolean }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        cn(
          'ui-pressable flex items-center rounded-xl',
          compact
            ? 'relative z-10 h-14 min-w-0 flex-col justify-center gap-1 px-1 text-xs'
            : 'h-12 gap-3 bg-transparent px-2 text-base font-semibold outline-none hover-fine:bg-sidebar-foreground/8 focus-visible:ring-2 focus-visible:ring-sidebar-ring focus-visible:ring-offset-2 focus-visible:ring-offset-sidebar',
          isActive
            ? compact
              ? 'text-muted-foreground'
              : 'text-brand'
            : compact
              ? 'text-muted-foreground hover-fine:text-foreground'
              : 'text-sidebar-foreground',
        )
      }
    >
      <Icon className={compact ? 'size-5' : 'size-6'} aria-hidden="true" />
      <span>{label}</span>
    </NavLink>
  )
}

function readSearchTerm(pathname: string, search: string): string {
  if (pathname !== '/search') {
    return ''
  }
  return new URLSearchParams(search).get('q') ?? ''
}
