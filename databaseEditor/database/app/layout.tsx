// WARNING: Public admin UI — add authentication and RLS before deploying.
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { entityLinks } from "@/lib/entities";
import "./globals.css";

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en">
      <body className="min-h-screen bg-background text-foreground">
        <header className="border-b bg-card/50 backdrop-blur">
          <div className="mx-auto flex w-full max-w-7xl flex-wrap items-center gap-2 px-4 py-3">
            <Button asChild variant="secondary" size="sm">
              <Link href="/">Flight Booking Admin (Public)</Link>
            </Button>
            {entityLinks.map((item) => (
              <Button key={item.href} asChild variant="ghost" size="sm">
                <Link href={item.href}>{item.label}</Link>
              </Button>
            ))}
          </div>
        </header>
        <main className="mx-auto w-full max-w-7xl px-4 py-6">{children}</main>
      </body>
    </html>
  );
}
