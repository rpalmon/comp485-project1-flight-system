// WARNING: Public admin UI — add authentication and RLS before deploying.
import * as React from "react";
import { cn } from "@/lib/utils";

function Alert({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      role="alert"
      data-slot="alert"
      className={cn("w-full rounded-md border px-4 py-3 text-sm", className)}
      {...props}
    />
  );
}

export { Alert };