import { PartyPopper } from 'lucide-react'
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type CSSProperties,
  type ReactNode,
} from 'react'

import type { Money } from '@/core/types'
import { formatMoney } from '@/lib/format'

interface Celebration {
  amount: Money
  id: number
}

type Celebrate = (amount: Money) => void

const CelebrationContext = createContext<Celebrate | null>(null)

const confettiColors = [
  'var(--outcome-yes)',
  'var(--brand)',
  'color-mix(in oklch, var(--outcome-yes) 55%, white)',
  'color-mix(in oklch, var(--brand) 55%, white)',
  'var(--primary)',
]

const confetti = Array.from({ length: 32 }, (_, index) => ({
  color: confettiColors[index % confettiColors.length],
  delay: `${(index % 8) * 0.08}s`,
  drift: `${((index * 37) % 180) - 90}px`,
  duration: `${1.8 + (index % 5) * 0.18}s`,
  left: `${3 + ((index * 29) % 94)}%`,
  rotation: `${360 + (index % 4) * 180}deg`,
}))

export function CelebrationProvider({ children }: { children: ReactNode }) {
  const [celebration, setCelebration] = useState<Celebration | null>(null)
  const nextId = useRef(0)
  const dismissTimer = useRef<number | undefined>(undefined)

  const celebrate = useCallback((amount: Money) => {
    nextId.current += 1
    setCelebration({ amount, id: nextId.current })
    if (dismissTimer.current !== undefined) {
      window.clearTimeout(dismissTimer.current)
    }
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    dismissTimer.current = window.setTimeout(
      () => setCelebration(null),
      reduceMotion ? 2_000 : 3_200,
    )
  }, [])

  useEffect(
    () => () => {
      if (dismissTimer.current !== undefined) {
        window.clearTimeout(dismissTimer.current)
      }
    },
    [],
  )

  return (
    <CelebrationContext.Provider value={celebrate}>
      {children}
      {celebration && <WinCelebration key={celebration.id} amount={celebration.amount} />}
    </CelebrationContext.Provider>
  )
}

export function useCelebration(): Celebrate {
  const celebrate = useContext(CelebrationContext)
  if (!celebrate) {
    throw new Error('useCelebration must be used within CelebrationProvider')
  }
  return celebrate
}

function WinCelebration({ amount }: { amount: Money }) {
  return (
      <div className="pointer-events-none fixed inset-0 z-50 overflow-hidden">
      <div className="absolute inset-0 motion-reduce:hidden" aria-hidden="true">
        {confetti.map((piece, index) => (
          <span
            key={index}
            className="win-confetti-piece"
            style={
              {
                '--confetti-color': piece.color,
                '--confetti-delay': piece.delay,
                '--confetti-drift': piece.drift,
                '--confetti-duration': piece.duration,
                '--confetti-left': piece.left,
                '--confetti-rotation': piece.rotation,
              } as CSSProperties
            }
          />
        ))}
      </div>
      <div className="app-overlay-frame absolute inset-y-0 right-0 grid place-items-center px-4">
        <div
          className="win-celebration-card rounded-3xl border border-outcome-yes/40 bg-foreground px-8 py-7 text-center text-background shadow-2xl"
          role="status"
          aria-live="polite"
        >
          <span className="mx-auto grid size-14 place-items-center rounded-full bg-outcome-yes text-outcome-yes-foreground">
            <PartyPopper className="size-7" aria-hidden="true" />
          </span>
          <p className="mt-4 text-sm font-medium tracking-widest text-outcome-yes uppercase">
            Gewonnen
          </p>
          <p className="mt-1 text-3xl font-semibold tracking-tight tabular-nums">{formatMoney(amount)}</p>
          <p className="mt-2 text-sm text-background/65">Dein Gewinn wurde gutgeschrieben.</p>
        </div>
      </div>
    </div>
  )
}
