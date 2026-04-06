// WARNING: Public admin UI — add authentication and RLS before deploying.
import AirlinesForm from "./AirlinesForm";
import { EntityPage } from "@/components/admin/entity-page";
import { createServerClient } from "@/lib/supabase/server";

type Row = Record<string, unknown> & { id?: number | string };

export const dynamic = "force-dynamic";

async function getAirlines() {
  const supabase = createServerClient();
  const ordered = await supabase
    .from("airlines")
    .select("*")
    .order("created_at", { ascending: false })
    .limit(200);

  if (!ordered.error) {
    return ordered;
  }

  return supabase.from("airlines").select("*").limit(200);
}

export default async function AirlinesPage() {
  const { data, error } = await getAirlines();

  return (
    <EntityPage
      title="Airlines"
      description="Manage airline operators and provider codes."
      rows={(data ?? []) as Row[]}
      loadError={error?.message ?? null}
      form={<AirlinesForm />}
    />
  );
}