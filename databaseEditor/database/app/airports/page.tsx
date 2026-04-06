// WARNING: Public admin UI — add authentication and RLS before deploying.
import AirportsForm from "./AirportsForm";
import { EntityPage } from "@/components/admin/entity-page";
import { createServerClient } from "@/lib/supabase/server";

type Row = Record<string, unknown> & { id?: number | string };

export const dynamic = "force-dynamic";

async function getAirports() {
  const supabase = createServerClient();
  const ordered = await supabase
    .from("airports")
    .select("*")
    .order("created_at", { ascending: false })
    .limit(200);

  if (!ordered.error) {
    return ordered;
  }

  return supabase.from("airports").select("*").limit(200);
}

export default async function AirportsPage() {
  const { data, error } = await getAirports();

  return (
    <EntityPage
      title="Airports"
      description="Manage airport records used by flights and route planning."
      rows={(data ?? []) as Row[]}
      loadError={error?.message ?? null}
      form={<AirportsForm />}
    />
  );
}