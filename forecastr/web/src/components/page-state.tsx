import { AlertCircle, LoaderCircle } from 'lucide-react'

import { Button } from '@/components/ui/button'

export function PageLoading({ label = 'Wird geladen …' }: { label?: string }) {
  return (
    <div
      className="flex min-h-64 flex-col items-center justify-center gap-3 text-muted-foreground"
      role="status"
      aria-live="polite"
      aria-busy="true"
    >
      <LoaderCircle className="size-6 animate-spin motion-reduce:animate-none" aria-hidden="true" />
      <p>{label}</p>
    </div>
  )
}

export function PageError({ error, retry }: { error: Error; retry: () => void }) {
  return (
    <div
      className="flex min-h-64 flex-col items-center justify-center gap-3 px-6 text-center"
      role="alert"
    >
      <AlertCircle className="size-7 text-destructive" aria-hidden="true" />
      <div>
        <p className="font-medium">Das hat nicht funktioniert.</p>
        <p className="mt-1 text-sm text-muted-foreground">{error.message}</p>
      </div>
      <Button variant="outline" onClick={retry}>
        Erneut versuchen
      </Button>
    </div>
  )
}

export function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <div className="flex min-h-64 flex-col items-center justify-center px-6 text-center">
      <p className="font-medium">{title}</p>
      <p className="mt-1 max-w-sm text-sm text-muted-foreground">{description}</p>
    </div>
  )
}
