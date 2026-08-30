import { House, Search, TicketCheck, UserRound, WalletCards } from 'lucide-react'
import { NavLink } from 'react-router-dom'

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
      className="absolute inset-x-2 bottom-[max(0.5rem,env(safe-area-inset-bottom))] z-40 flex h-16 items-center justify-around rounded-2xl border border-white/10 bg-black/70 px-1 text-white shadow-2xl backdrop-blur-xl lg:hidden"
    >
      {destinations.map((destination) => (
        <NavigationLink key={destination.to} {...destination} compact />
      ))}
    </nav>
  )
}

export function DesktopNavigation() {
  return (
    <aside className="hidden h-[calc(100dvh-2rem)] flex-col py-8 lg:flex">
      <div className="mb-10 px-4">
        <p className="text-xl font-semibold tracking-tight">FORECASTR</p>
        <p className="mt-1 text-xs text-muted-foreground">Frage. Wette. Wissen.</p>
      </div>
      <nav aria-label="Hauptnavigation" className="space-y-1">
        {destinations.map((destination) => (
          <NavigationLink key={destination.to} {...destination} />
        ))}
      </nav>
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
          'flex items-center rounded-xl transition-colors',
          compact
            ? 'h-14 min-w-14 flex-col justify-center gap-1 px-2 text-[0.65rem]'
            : 'h-11 gap-3 px-4 text-sm font-medium',
          isActive
            ? compact
              ? 'text-white'
              : 'bg-foreground text-background'
            : compact
              ? 'text-white/60 hover:text-white'
              : 'text-muted-foreground hover:bg-muted hover:text-foreground',
        )
      }
    >
      <Icon className={compact ? 'size-5' : 'size-4'} aria-hidden="true" />
      <span>{label}</span>
    </NavLink>
  )
}
