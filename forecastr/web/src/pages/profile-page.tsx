import { useMutation, useQueryClient } from '@tanstack/react-query'
import { LoaderCircle, LogOut, Moon, Pencil, ShieldCheck, Sun, Trash2 } from 'lucide-react'
import { useTheme } from 'next-themes'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { forecastrApi, queryKeys } from '@/core/forecastr-api'
import { useSession } from '@/session/session-context'

export function ProfilePage() {
  const { user, selectUser, logout } = useSession()
  const { resolvedTheme, setTheme } = useTheme()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [isRenaming, setIsRenaming] = useState(false)
  const [username, setUsername] = useState(user!.username)
  const rename = useMutation({
    mutationFn: () => forecastrApi.updateUser(user!.id, username.trim()),
    onSuccess: (updatedUser) => {
      queryClient.setQueryData(queryKeys.user(user!.id), updatedUser)
      selectUser(updatedUser)
      setIsRenaming(false)
      toast.success('Der Benutzername wurde geändert.')
    },
    onError: (error: Error) => toast.error(error.message),
  })
  const deleteAccount = useMutation({
    mutationFn: () => forecastrApi.deleteUser(user!.id),
    onSuccess: () => {
      queryClient.clear()
      logout()
      navigate('/select')
      toast.success('Das Konto wurde gelöscht.')
    },
    onError: (error: Error) => toast.error(error.message),
  })

  const leave = () => {
    queryClient.clear()
    logout()
    navigate('/select')
  }

  return (
    <div className="mx-auto max-w-lg space-y-5">
      <div>
        <p className="text-xs font-medium tracking-widest text-muted-foreground uppercase">Konto</p>
        <h1 className="mt-1 text-2xl font-semibold tracking-tight">Profil</h1>
      </div>
      <Card>
        <CardHeader>
          <div className="flex items-center gap-4">
            <div className="grid size-14 place-items-center rounded-full bg-foreground text-lg font-bold text-background">
              {user!.username.slice(0, 2).toUpperCase()}
            </div>
            <div>
              <CardTitle>{user!.username}</CardTitle>
              <CardDescription className="mt-1">Demo-Konto · ID {user!.id}</CardDescription>
            </div>
            {user!.isAdmin && <Badge className="ml-auto">Admin</Badge>}
          </div>
        </CardHeader>
        <CardContent className="space-y-3">
          <Button variant="outline" className="w-full justify-start" onClick={() => setIsRenaming(true)}>
            <Pencil aria-hidden="true" />
            Benutzername ändern
          </Button>
          {user!.isAdmin && (
            <Button asChild variant="outline" className="w-full justify-start">
              <Link to="/admin">
                <ShieldCheck aria-hidden="true" />
                Admin-Panel
              </Link>
            </Button>
          )}
          <Button variant="outline" className="w-full justify-start" onClick={leave}>
            <LogOut aria-hidden="true" />
            Ausloggen
          </Button>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Darstellung</CardTitle>
          <CardDescription>Die Auswahl wird auf diesem Gerät gespeichert.</CardDescription>
        </CardHeader>
        <CardContent className="flex items-center justify-between">
          <Label htmlFor="theme-switch" className="flex items-center gap-2">
            {resolvedTheme === 'dark' ? <Moon aria-hidden="true" /> : <Sun aria-hidden="true" />}
            Dunkles Design
          </Label>
          <Switch
            id="theme-switch"
            checked={resolvedTheme === 'dark'}
            onCheckedChange={(checked) => setTheme(checked ? 'dark' : 'light')}
          />
        </CardContent>
      </Card>

      <Card className="border-destructive/25">
        <CardHeader>
          <CardTitle>Konto löschen</CardTitle>
          <CardDescription>Konten mit offenen Wetten können nicht gelöscht werden.</CardDescription>
        </CardHeader>
        <CardContent>
          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button variant="destructive" className="w-full">
                <Trash2 aria-hidden="true" />
                Konto endgültig löschen
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>Konto wirklich löschen?</AlertDialogTitle>
                <AlertDialogDescription>
                  Das Konto „{user!.username}“ wird dauerhaft deaktiviert. Diese Aktion kann nicht rückgängig gemacht werden.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>Abbrechen</AlertDialogCancel>
                <AlertDialogAction
                  onClick={() => deleteAccount.mutate()}
                  disabled={deleteAccount.isPending}
                >
                  {deleteAccount.isPending && <LoaderCircle className="animate-spin" />}
                  Löschen
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </CardContent>
      </Card>

      <Dialog open={isRenaming} onOpenChange={setIsRenaming}>
        <DialogContent>
          <form
            onSubmit={(event) => {
              event.preventDefault()
              if (username.trim()) {
                rename.mutate()
              }
            }}
          >
            <DialogHeader>
              <DialogTitle>Benutzername ändern</DialogTitle>
              <DialogDescription>Der Name darf höchstens 80 Zeichen enthalten.</DialogDescription>
            </DialogHeader>
            <div className="my-5 space-y-2">
              <Label htmlFor="profile-username">Benutzername</Label>
              <Input
                id="profile-username"
                autoFocus
                maxLength={80}
                value={username}
                onChange={(event) => setUsername(event.target.value)}
              />
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setIsRenaming(false)}>
                Abbrechen
              </Button>
              <Button type="submit" disabled={!username.trim() || rename.isPending}>
                {rename.isPending && <LoaderCircle className="animate-spin" />}
                Speichern
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}
