// WARNING: Public admin UI — add authentication and RLS before deploying.
import { ReactNode } from "react";
import { Alert } from "@/components/ui/alert";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

type Row = Record<string, unknown> & { id?: number | string };

type EntityPageProps = {
  title: string;
  description: string;
  rows: Row[];
  loadError?: string | null;
  form: ReactNode;
};

export function EntityPage({ title, description, rows, loadError, form }: EntityPageProps) {
  return (
    <section className="space-y-4">
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
        <p className="text-sm text-muted-foreground">{description}</p>
      </div>

      {loadError ? (
        <Alert className="border-destructive/40 text-destructive">Load error: {loadError}</Alert>
      ) : null}

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Create</CardTitle>
            <CardDescription>Use Randomize + Create for quick test data entry.</CardDescription>
          </CardHeader>
          <CardContent>{form}</CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Rows ({rows.length})</CardTitle>
            <CardDescription>Latest rows from Supabase (up to 200 records).</CardDescription>
          </CardHeader>
          <CardContent>
            {rows.length === 0 ? (
              <p className="text-sm text-muted-foreground">No rows found.</p>
            ) : (
              <div className="max-h-[520px] space-y-2 overflow-auto pr-1">
                {rows.map((row, index) => (
                  <details key={String(row.id ?? index)} className="rounded-md border p-3 text-sm">
                    <summary className="cursor-pointer font-medium">
                      Row {index + 1}
                      {row.id !== undefined ? ` • ID: ${String(row.id)}` : ""}
                    </summary>
                    <pre className="mt-2 overflow-x-auto rounded-md bg-muted p-3 text-xs">
                      {JSON.stringify(row, null, 2)}
                    </pre>
                  </details>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </section>
  );
}