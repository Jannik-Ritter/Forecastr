import { useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { toast } from 'sonner'

import { parseJson } from '@/core/api-client'
import { queryKeys } from '@/core/forecastr-api'
import { liveMessageSchema } from '@/core/schemas'
import { liveMessage } from '@/lib/format'
import { useCelebration } from '@/notifications/celebration-context'

export function useLiveUpdates(userId: string) {
  const queryClient = useQueryClient()
  const celebrate = useCelebration()

  useEffect(() => {
    let socket: WebSocket | null = null
    let reconnectTimer: number | undefined
    let reconnectAttempt = 0
    let isClosed = false
    let hasShownConnectionWarning = false

    const scheduleReconnect = () => {
      if (isClosed) {
        return
      }
      if (!hasShownConnectionWarning) {
        toast.warning('Live-Aktualisierungen sind unterbrochen. Die App bleibt nutzbar.')
        hasShownConnectionWarning = true
      }
      const delay = Math.min(1_000 * 2 ** reconnectAttempt, 15_000)
      reconnectAttempt += 1
      reconnectTimer = window.setTimeout(connect, delay)
    }

    const connect = () => {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      socket = new WebSocket(`${protocol}//${window.location.host}/ws`)
      socket.addEventListener('open', () => {
        reconnectAttempt = 0
        hasShownConnectionWarning = false
        socket?.send(JSON.stringify({ action: 'subscribe', topic: '/topic/feed' }))
        socket?.send(
          JSON.stringify({ action: 'subscribe', topic: `/topic/users/${userId}` }),
        )
      })
      socket.addEventListener('message', (event) => {
        if (typeof event.data !== 'string') {
          return
        }
        const result = liveMessageSchema.safeParse(parseJson(event.data))
        if (!result.success) {
          return
        }
        const message = result.data
        if (message.type === 'FEED') {
          void queryClient.invalidateQueries({ queryKey: queryKeys.feed })
          void queryClient.invalidateQueries({ queryKey: ['events'] })
          void queryClient.invalidateQueries({ queryKey: queryKeys.event(message.eventId) })
        }
        if (message.type === 'NOTIFICATION') {
          void queryClient.invalidateQueries({ queryKey: queryKeys.balance(userId) })
          void queryClient.invalidateQueries({ queryKey: queryKeys.bets(userId) })
          if (message.kind === 'PAYOUT') {
            celebrate(message.amount)
          } else {
            toast.info(liveMessage(message.kind, message.amount))
          }
        }
        if (message.type === 'ERROR') {
          toast.error('Live-Aktualisierungen sind momentan nicht verfügbar.')
        }
      })
      socket.addEventListener('close', scheduleReconnect)
      socket.addEventListener('error', () => socket?.close())
    }

    connect()
    return () => {
      isClosed = true
      if (reconnectTimer !== undefined) {
        window.clearTimeout(reconnectTimer)
      }
      socket?.close(1000, 'Session beendet')
    }
  }, [celebrate, queryClient, userId])
}
