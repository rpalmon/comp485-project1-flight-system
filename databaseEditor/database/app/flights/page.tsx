// WARNING: Public admin UI — add authentication and RLS before deploying.
import FlightForm from "./FlightForm";
import { EntityPage } from "@/components/admin/entity-page";
import { createServerClient } from "@/lib/supabase/server";

type Row = Record<string, unknown> & { id?: number | string };

export const dynamic = "force-dynamic";

async function getFlights() {
  const supabase = createServerClient();
  const ordered = await supabase
    .from("flights")
    .select("*")
    .order("created_at", { ascending: false })
    .limit(200);

  if (!ordered.error) {
    return ordered;
  }

  return supabase.from("flights").select("*").limit(200);
}

export default async function FlightsPage() {
  const { data, error } = await getFlights();

  return (
    <EntityPage
      title="Flights"
      description="Manage route instances, schedule windows, and base fares."
      rows={(data ?? []) as Row[]}
      loadError={error?.message ?? null}
      form={<FlightForm />}
    />
  );
}