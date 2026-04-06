// WARNING: Public admin UI — add authentication and RLS before deploying.
import PassengersForm from "./PassengersForm";
import { EntityPage } from "@/components/admin/entity-page";
import { createServerClient } from "@/lib/supabase/server";

type Row = Record<string, unknown> & { id?: number | string };

export const dynamic = "force-dynamic";

async function getPassengers() {
  const supabase = createServerClient();
  const ordered = await supabase
    .from("passengers")
    .select("*")
    .order("created_at", { ascending: false })
    .limit(200);

  if (!ordered.error) {
    return ordered;
  }

  return supabase.from("passengers").select("*").limit(200);
}

export default async function PassengersPage() {
  const { data, error } = await getPassengers();

  return (
    <EntityPage
      title="Passengers"
      description="Manage passenger details, preferences, and seat assignments."
      rows={(data ?? []) as Row[]}
      loadError={error?.message ?? null}
      form={<PassengersForm />}
    />
  );
}