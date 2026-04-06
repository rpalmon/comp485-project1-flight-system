// WARNING: Public admin UI — add authentication and RLS before deploying.
import FlightClassesForm from "./FlightClassesForm";
import { EntityPage } from "@/components/admin/entity-page";
import { createServerClient } from "@/lib/supabase/server";

type Row = Record<string, unknown> & { id?: number | string };

export const dynamic = "force-dynamic";

async function getFlightClasses() {
  const supabase = createServerClient();
  const ordered = await supabase
    .from("flight_classes")
    .select("*")
    .order("created_at", { ascending: false })
    .limit(200);

  if (!ordered.error) {
    return ordered;
  }

  return supabase.from("flight_classes").select("*").limit(200);
}

export default async function FlightClassesPage() {
  const { data, error } = await getFlightClasses();

  return (
    <EntityPage
      title="Flight Classes"
      description="Configure class inventory and fare multipliers by flight."
      rows={(data ?? []) as Row[]}
      loadError={error?.message ?? null}
      form={<FlightClassesForm />}
    />
  );
}