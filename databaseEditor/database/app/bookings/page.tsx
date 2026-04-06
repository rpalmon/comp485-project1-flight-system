// WARNING: Public admin UI — add authentication and RLS before deploying.
import BookingsForm from "./BookingsForm";
import { EntityPage } from "@/components/admin/entity-page";
import { createServerClient } from "@/lib/supabase/server";

type Row = Record<string, unknown> & { id?: number | string };

export const dynamic = "force-dynamic";

async function getBookings() {
  const supabase = createServerClient();
  const ordered = await supabase
    .from("bookings")
    .select("*")
    .order("created_at", { ascending: false })
    .limit(200);

  if (!ordered.error) {
    return ordered;
  }

  return supabase.from("bookings").select("*").limit(200);
}

export default async function BookingsPage() {
  const { data, error } = await getBookings();

  return (
    <EntityPage
      title="Bookings"
      description="Capture booking records, status, and customer contact details."
      rows={(data ?? []) as Row[]}
      loadError={error?.message ?? null}
      form={<BookingsForm />}
    />
  );
}