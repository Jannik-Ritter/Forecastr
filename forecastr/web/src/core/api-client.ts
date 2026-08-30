import { LosslessNumber, parse, stringify } from 'lossless-json'
import type { z } from 'zod'

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

interface RequestOptions {
  actorUserId?: string
  body?: unknown
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
}

export async function request<T>(
  path: string,
  schema: z.ZodType<T>,
  options: RequestOptions = {},
): Promise<T> {
  const headers = requestHeaders(options)
  let response: Response
  try {
    response = await fetch(path, {
      method: options.method ?? 'GET',
      headers,
      body: options.body === undefined ? undefined : stringify(options.body),
    })
  } catch {
    throw new ApiError(-1, 'Der Forecastr-Server ist nicht erreichbar.')
  }

  const text = await response.text()
  const payload = text ? safeParse(text) : null
  if (!response.ok) {
    throw new ApiError(response.status, germanError(response.status, readMessage(payload)))
  }

  const result = schema.safeParse(payload)
  if (!result.success) {
    throw new ApiError(-1, 'Der Server hat eine unverständliche Antwort gesendet.')
  }
  return result.data
}

export async function requestWithoutResponse(
  path: string,
  options: RequestOptions,
): Promise<void> {
  const headers = requestHeaders(options)
  let response: Response
  try {
    response = await fetch(path, {
      method: options.method ?? 'DELETE',
      headers,
      body: options.body === undefined ? undefined : stringify(options.body),
    })
  } catch {
    throw new ApiError(-1, 'Der Forecastr-Server ist nicht erreichbar.')
  }
  if (!response.ok) {
    const text = await response.text()
    throw new ApiError(response.status, germanError(response.status, readMessage(safeParse(text))))
  }
}

export function exactNumber(value: string): LosslessNumber {
  return new LosslessNumber(value)
}

export function parseJson(value: string): unknown {
  return safeParse(value)
}

function requestHeaders(options: RequestOptions): Headers {
  const headers = new Headers({ Accept: 'application/json' })
  if (options.actorUserId) {
    headers.set('X-Forecastr-User-Id', options.actorUserId)
  }
  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }
  return headers
}

function safeParse(value: string): unknown {
  if (!value) {
    return null
  }
  try {
    return parse(value)
  } catch {
    return null
  }
}

function readMessage(value: unknown): string {
  if (typeof value !== 'object' || value === null || !('message' in value)) {
    return ''
  }
  const message = value.message
  return typeof message === 'string' ? message : ''
}

function germanError(status: number, message: string): string {
  const messages: Record<string, string> = {
    'Username already exists': 'Dieser Benutzername ist bereits vergeben.',
    'Account has open bets':
      'Das Konto kann erst gelöscht werden, wenn alle offenen Wetten abgeschlossen sind.',
    'Insufficient balance': 'Das Guthaben reicht für diesen Betrag nicht aus.',
    'Event is not open for betting': 'Dieser Markt ist nicht mehr für Wetten geöffnet.',
    'Stake must be positive': 'Der Betrag muss größer als 0 sein.',
    'Amount must be positive': 'Der Betrag muss größer als 0 sein.',
    'User not found': 'Das Benutzerkonto wurde nicht gefunden.',
    'Event not found': 'Der Markt wurde nicht gefunden.',
    'Admin account required': 'Für diese Aktion ist ein Administratorkonto erforderlich.',
    'Server is busy; retry the request':
      'Der Server ist gerade ausgelastet. Bitte versuche es erneut.',
  }
  if (messages[message]) {
    return messages[message]
  }
  if (message.toLowerCase().includes('username')) {
    return 'Der Benutzername ist ungültig.'
  }
  if (message.toLowerCase().includes('amount') || message.toLowerCase().includes('stake')) {
    return 'Bitte gib einen gültigen Betrag mit höchstens zwei Nachkommastellen ein.'
  }
  return `Die Anfrage konnte nicht ausgeführt werden (Fehler ${status}).`
}
