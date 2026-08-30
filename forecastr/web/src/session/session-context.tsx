import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'

import type { User } from '@/core/types'

const STORAGE_KEY = 'forecastr-user'

interface SessionContextValue {
  logout: () => void
  selectUser: (user: User) => void
  user: User | null
}

const SessionContext = createContext<SessionContextValue | null>(null)

export function SessionProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(readStoredUser)
  const value = useMemo(
    () => ({
      user,
      selectUser(selectedUser: User) {
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(selectedUser))
        setUser(selectedUser)
      },
      logout() {
        sessionStorage.removeItem(STORAGE_KEY)
        setUser(null)
      },
    }),
    [user],
  )

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
}

export function useSession(): SessionContextValue {
  const context = useContext(SessionContext)
  if (!context) {
    throw new Error('useSession must be used inside SessionProvider')
  }
  return context
}

function readStoredUser(): User | null {
  const value = sessionStorage.getItem(STORAGE_KEY)
  if (!value) {
    return null
  }
  try {
    const user = JSON.parse(value) as Partial<User>
    if (typeof user.id !== 'string' || typeof user.username !== 'string') {
      return null
    }
    return user as User
  } catch {
    return null
  }
}
