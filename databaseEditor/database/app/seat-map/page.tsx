// WARNING: Public admin UI — add authentication and RLS before deploying.
import SeatMapForm from "./SeatMapForm";
import { EntityPage } from "@/components/admin/entity-page";
import { createServerClient } from "@/lib/supabase/server";

type Row = Record<string, unknown> & { id?: number | string };

export const dynamic = "force-dynamic";

async function getSeatMap() {
  const supabase = createServerClient();
  const ordered = await supabase
    .from("seat_map")
    .select("*")
    .order("created_at", { ascending: false })
    .limit(200);

  if (!ordered.error) {
    return ordered;
  }

  return supabase.from("seat_map").select("*").limit(200);
}

export default async function SeatMapPage() {
  const { data, error } = await getSeatMap();

  return (
    <EntityPage
      title="Seat Map"
      description="Manage per-flight seat availability and seat-type flags."
      rows={(data ?? []) as Row[]}
      loadError={error?.message ?? null}
      form={<SeatMapForm />}
    />
  );
}