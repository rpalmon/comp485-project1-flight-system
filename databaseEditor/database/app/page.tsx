// WARNING: Public admin UI — add authentication and RLS before deploying.
import Link from "next/link";
import { Alert } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { entityLinks } from "@/lib/entities";

export default function Page() {
  return (
    <section className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle className="text-2xl">Flight Booking DB Admin Dashboard</CardTitle>
          <CardDescription>Use any module below to view rows and add data.</CardDescription>
        </CardHeader>
        <CardContent>
          <Alert className="border-destructive/40 text-destructive">
        SECURITY WARNING: This admin UI is public and intentionally insecure. Add
        authentication and strict RLS policies before any production use.
          </Alert>
        </CardContent>
      </Card>

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {entityLinks.map((item) => (
          <Card key={item.href}>
            <CardContent className="flex items-center justify-between p-4">
              <p className="font-medium">{item.label}</p>
              <Button asChild size="sm">
                <Link href={item.href}>Open</Link>
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>
    </section>
  );
}
