import * as React from "react"

import { cn } from "@/lib/utils"

function OverlayFrame({
  className,
  ...props
}: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="overlay-frame"
      className={cn(
        "app-overlay-frame pointer-events-none fixed inset-y-0 right-0 z-50",
        className
      )}
      {...props}
    />
  )
}

export { OverlayFrame }
