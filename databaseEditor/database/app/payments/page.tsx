// WARNING: Public admin UI — add authentication and RLS before deploying.
import PaymentsForm from "./PaymentsForm";
import { EntityPage } from "@/components/admin/entity-page";
import { createServerClient } from "@/lib/supabase/server";

type Row = Record<string, unknown> & { id?: number | string };

export const dynamic = "force-dynamic";

async function getPayments() {
  const supabase = createServerClient();
  const ordered = await supabase
    .from("payments")
    .select("*")
    .order("created_at", { ascending: false })
    .limit(200);

  if (!ordered.error) {
    return ordered;
  }

  return supabase.from("payments").select("*").limit(200);
}

export default async function PaymentsPage() {
  const { data, error } = await getPayments();

  return (
    <EntityPage
      title="Payments"
      description="Track booking payments and provider charge references."
      rows={(data ?? []) as Row[]}
      loadError={error?.message ?? null}
      form={<PaymentsForm />}
    />
  );
}