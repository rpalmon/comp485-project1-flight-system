// WARNING: Public admin UI — add authentication and RLS before deploying.
import AircraftModelsForm from "./AircraftModelsForm";
import { EntityPage } from "@/components/admin/entity-page";
import { createServerClient } from "@/lib/supabase/server";

type Row = Record<string, unknown> & { id?: number | string };

export const dynamic = "force-dynamic";

async function getAircraftModels() {
  const supabase = createServerClient();
  const ordered = await supabase
    .from("aircraft_models")
    .select("*")
    .order("created_at", { ascending: false })
    .limit(200);

  if (!ordered.error) {
    return ordered;
  }

  return supabase.from("aircraft_models").select("*").limit(200);
}

export default async function AircraftModelsPage() {
  const { data, error } = await getAircraftModels();

  return (
    <EntityPage
      title="Aircraft Models"
      description="Define available aircraft models and capacities."
      rows={(data ?? []) as Row[]}
      loadError={error?.message ?? null}
      form={<AircraftModelsForm />}
    />
  );
}