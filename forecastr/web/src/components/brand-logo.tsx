import type { ComponentPropsWithoutRef } from 'react'

import { cn } from '@/lib/utils'

export function BrandLogo({ className, ...props }: ComponentPropsWithoutRef<'span'>) {
  return (
    <span
      className={cn('inline-flex items-center gap-2.5 text-current', className)}
      {...props}
    >
      <BrandMark className="h-[1em] w-auto shrink-0" />
      <span className="font-semibold tracking-[-0.04em]">Forecastr</span>
    </span>
  )
}

export function BrandMark({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 64 48"
      aria-hidden="true"
      className={className}
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d="M27.6 4H39.1C42 4 43.7 7.2 42.2 9.7L22.1 43.9H8.4C5.4 43.9 3.7 40.6 5.3 38L24.5 5.8C25.2 4.7 26.3 4 27.6 4Z"
        fill="currentColor"
      />
      <path
        d="M41.4 20H55.5C58.4 20 60.1 23.2 58.6 25.7L47.9 43.9H31C28 43.9 26.3 40.6 27.9 38L38.3 21.8C39 20.7 40.1 20 41.4 20Z"
        fill="currentColor"
      />
    </svg>
  )
}
