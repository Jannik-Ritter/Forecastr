import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from 'next-themes'
import { lazy, Suspense, type ReactNode } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'

import { PageLoading } from '@/components/page-state'
import { Toaster } from '@/components/ui/sonner'
import { AppShell } from '@/layouts/app-shell'
import { CelebrationProvider } from '@/notifications/celebration-context'
import { SessionProvider, useSession } from '@/session/session-context'

const AccountPage = lazy(() =>
  import('@/pages/account-page').then((module) => ({ default: module.AccountPage })),
)
const AdminPage = lazy(() =>
  import('@/pages/admin-page').then((module) => ({ default: module.AdminPage })),
)
const BetsPage = lazy(() =>
  import('@/pages/bets-page').then((module) => ({ default: module.BetsPage })),
)
const FeedPage = lazy(() =>
  import('@/pages/feed-page').then((module) => ({ default: module.FeedPage })),
)
const ProfilePage = lazy(() =>
  import('@/pages/profile-page').then((module) => ({ default: module.ProfilePage })),
)
const SearchPage = lazy(() =>
  import('@/pages/search-page').then((module) => ({ default: module.SearchPage })),
)
const WalletPage = lazy(() =>
  import('@/pages/wallet-page').then((module) => ({ default: module.WalletPage })),
)

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 10_000,
    },
    mutations: {
      retry: false,
    },
  },
})

function App() {
  return (
    <ThemeProvider
      attribute="class"
      defaultTheme="dark"
      enableSystem={false}
      storageKey="forecastr-theme"
    >
      <QueryClientProvider client={queryClient}>
        <SessionProvider>
          <CelebrationProvider>
            <BrowserRouter>
              <AppRoutes />
            </BrowserRouter>
            <Toaster
              position="bottom-center"
              offset={24}
              mobileOffset={{ bottom: 88, left: 16, right: 16 }}
              richColors
            />
          </CelebrationProvider>
        </SessionProvider>
      </QueryClientProvider>
    </ThemeProvider>
  )
}

function AppRoutes() {
  const { user } = useSession()

  return (
    <Routes>
      <Route path="/select" element={<Suspended><AccountPage /></Suspended>} />
      <Route element={user ? <AppShell /> : <Navigate to="/select" replace />}>
        <Route path="/feed" element={<Suspended><FeedPage /></Suspended>} />
        <Route path="/search" element={<Suspended><SearchPage /></Suspended>} />
        <Route path="/bets" element={<Suspended><BetsPage /></Suspended>} />
        <Route path="/wallet" element={<Suspended><WalletPage /></Suspended>} />
        <Route path="/profile" element={<Suspended><ProfilePage /></Suspended>} />
        <Route
          path="/admin"
          element={
            user?.isAdmin ? (
              <Suspended><AdminPage /></Suspended>
            ) : (
              <Navigate to="/feed" replace />
            )
          }
        />
      </Route>
      <Route path="*" element={<Navigate to={user ? '/feed' : '/select'} replace />} />
    </Routes>
  )
}

function Suspended({ children }: { children: ReactNode }) {
  return <Suspense fallback={<PageLoading />}>{children}</Suspense>
}

export default App
