import { useCallback, useRef } from 'react'

const imageModules = import.meta.glob<string>(
  '/src/assets/pets/*.{avif,webp,png,jpg,jpeg}',
  { eager: true, import: 'default', query: '?url' },
)
const petImages = Object.values(imageModules)

export function usePetImages(): (marketId: string) => string | undefined {
  const assignments = useRef(new Map<string, string>())
  const bag = useRef<string[]>([])

  return useCallback((marketId: string) => {
    const assigned = assignments.current.get(marketId)
    if (assigned) {
      return assigned
    }
    if (petImages.length === 0) {
      return undefined
    }
    if (bag.current.length === 0) {
      bag.current = shuffle(petImages)
    }
    const image = bag.current.pop()
    if (image) {
      assignments.current.set(marketId, image)
    }
    return image
  }, [])
}

function shuffle(values: string[]): string[] {
  const shuffled = [...values]
  for (let index = shuffled.length - 1; index > 0; index--) {
    const target = Math.floor(Math.random() * (index + 1))
    ;[shuffled[index], shuffled[target]] = [shuffled[target], shuffled[index]]
  }
  return shuffled
}
