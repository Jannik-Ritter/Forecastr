import { House, Search, TicketCheck, UserRound, WalletCards } from 'lucide-react'
import { Link, NavLink } from 'react-router-dom'

import { BrandLogo, BrandMark } from '@/components/brand-logo'
import { cn } from '@/lib/utils'

const destinations = [
  { to: '/feed', label: 'Feed', icon: House },
  { to: '/search', label: 'Suche', icon: Search },
  { to: '/bets', label: 'Wetten', icon: TicketCheck },
  { to: '/wallet', label: 'Wallet', icon: WalletCards },
  { to: '/profile', label: 'Profil', icon: UserRound },
]

export function MobileNavigation() {
  return (
    <nav
      aria-label="Hauptnavigation"
      className="absolute inset-x-2 bottom-[max(0.5rem,env(safe-area-inset-bottom))] z-40 flex h-16 items-center justify-around rounded-2xl border border-border bg-card/85 px-1 text-foreground shadow-2xl backdrop-blur-xl lg:hidden"
    >
      {destinations.map((destination) => (
        <NavigationLink key={destination.to} {...destination} compact />
      ))}
    </nav>
  )
}

export function DesktopNavigation() {
  return (
    <aside className="hidden h-dvh w-(--app-sidebar-width) shrink-0 flex-col border-r border-sidebar-border bg-sidebar px-3 py-5 text-sidebar-foreground lg:flex">
      <Link to="/feed" aria-label="Forecastr Feed" className="mb-8 px-3 py-1 text-[1.65rem]">
        <BrandLogo />
      </Link>
      <nav aria-label="Hauptnavigation" className="space-y-1.5">
        {destinations.map((destination) => (
          <NavigationLink key={destination.to} {...destination} />
        ))}
      </nav>
      <div className="mt-auto px-3 pb-2">
        <div className="border-t border-sidebar-border pt-4">
          <BrandMark className="mb-3 h-3.5 w-auto text-brand" />
          <p className="text-xs leading-relaxed text-muted-foreground">
            Kurze Aufmerksamkeitsspanne.
            <br />
            Langfristige Konsequenzen.
          </p>
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
            ? 'h-14 min-w-14 flex-col justify-center gap-1 px-2 text-xs'
            : 'h-11 gap-3 px-4 text-sm font-medium',
          isActive
            ? compact
              ? 'text-accent-foreground'
              : 'bg-sidebar-accent text-sidebar-accent-foreground'
            : compact
              ? 'text-muted-foreground hover-fine:text-foreground'
              : 'text-muted-foreground hover-fine:bg-sidebar-accent hover-fine:text-sidebar-foreground',
        )
      }
    >
      <Icon className="size-5" aria-hidden="true" />
      <span>{label}</span>
    </NavLink>
  )
}
