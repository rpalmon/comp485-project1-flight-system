"use client";
// WARNING: Public admin UI — add authentication and RLS before deploying.
import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Alert } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { NativeSelect } from "@/components/ui/native-select";
import { supabase } from "@/lib/supabase/client";
import { pick, randInt, randomCode } from "@/lib/random";

type PaymentInput = {
  booking_id: string;
  provider: string;
  provider_charge_id: string;
  amount: string;
  currency: string;
  status: string;
};

const initialState: PaymentInput = {
  booking_id: "",
  provider: "",
  provider_charge_id: "",
  amount: "",
  currency: "USD",
  status: "pending",
};

type BookingOption = { id: number | string; booking_reference?: string };

function toDbId(value: string) {
  return /^\d+$/.test(value) ? Number(value) : value;
}

export default function PaymentsForm() {
  const router = useRouter();
  const [form, setForm] = useState<PaymentInput>(initialState);
  const [bookings, setBookings] = useState<BookingOption[]>([]);
  const [saving, setSaving] = useState(false);
  const [loadingOptions, setLoadingOptions] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    async function loadBookings() {
      setLoadingOptions(true);
      const { data, error: loadError } = await supabase
        .from("bookings")
        .select("id,booking_reference")
        .limit(500);

      if (!mounted) return;

      if (loadError) {
        setError(loadError.message);
      } else {
        setBookings((data ?? []) as BookingOption[]);
      }

      setLoadingOptions(false);
    }

    void loadBookings();
    return () => {
      mounted = false;
    };
  }, []);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    if (!form.booking_id || !form.provider || !form.provider_charge_id || !form.amount || !form.currency || !form.status) {
      setError("All payment fields are required.");
      return;
    }

    const amount = Number(form.amount);
    if (Number.isNaN(amount) || amount < 0) {
      setError("Amount must be a valid non-negative number.");
      return;
    }

    setSaving(true);
    const { error: insertError } = await supabase.from("payments").insert([
      {
        booking_id: toDbId(form.booking_id),
        provider: form.provider.trim(),
        provider_charge_id: form.provider_charge_id.trim(),
        amount,
        currency: form.currency.trim().toUpperCase(),
        status: form.status.trim(),
      },
    ]);
    setSaving(false);

    if (insertError) {
      setError(insertError.message);
      return;
    }

    setForm(initialState);
    router.refresh();
  }

  function randomizeForm() {
    const provider = pick(["stripe", "adyen", "paypal"]);

    setForm({
      booking_id: bookings.length > 0 ? String(pick(bookings).id) : "",
      provider,
      provider_charge_id: `${provider}_${randomCode(10)}`,
      amount: String(randInt(80, 3000)),
      currency: pick(["USD", "EUR", "GBP", "CAD"]),
      status: pick(["pending", "succeeded", "failed"]),
    });
  }

  return (
    <form onSubmit={onSubmit} className="grid gap-3">
      <div className="grid gap-1.5 sm:grid-cols-2">
        <div className="grid gap-1.5">
          <Label htmlFor="payment-booking-id">Booking ID</Label>
          <NativeSelect id="payment-booking-id" value={form.booking_id} onChange={(e) => setForm({ ...form, booking_id: e.target.value })}>
            <option value="">Select booking</option>
            {bookings.map((booking) => (
              <option key={String(booking.id)} value={String(booking.id)}>
                {booking.booking_reference ? `${booking.booking_reference} (${String(booking.id)})` : String(booking.id)}
              </option>
            ))}
          </NativeSelect>
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="payment-provider">Provider</Label>
          <Input id="payment-provider" placeholder="Provider" value={form.provider} onChange={(e) => setForm({ ...form, provider: e.target.value })} />
        </div>
      </div>
      <div className="grid gap-1.5">
        <Label htmlFor="payment-provider-charge-id">Provider Charge ID</Label>
        <Input id="payment-provider-charge-id" placeholder="Provider Charge ID" value={form.provider_charge_id} onChange={(e) => setForm({ ...form, provider_charge_id: e.target.value })} />
      </div>
      <div className="grid gap-1.5 sm:grid-cols-3">
        <div className="grid gap-1.5">
          <Label htmlFor="payment-amount">Amount</Label>
          <Input id="payment-amount" placeholder="Amount" type="number" min={0} step="0.01" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} />
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="payment-currency">Currency</Label>
          <Input id="payment-currency" placeholder="Currency" value={form.currency} onChange={(e) => setForm({ ...form, currency: e.target.value.toUpperCase() })} />
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="payment-status">Status</Label>
          <Input id="payment-status" placeholder="Status" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })} />
        </div>
      </div>
      <div className="flex gap-2">
        <Button type="button" variant="outline" onClick={randomizeForm} disabled={loadingOptions}>Randomize</Button>
        <Button type="submit" disabled={saving || loadingOptions}>{saving ? "Saving..." : "Create"}</Button>
      </div>
      {loadingOptions ? <p className="text-sm text-muted-foreground">Loading bookings...</p> : null}
      {error ? <Alert className="border-destructive/40 text-destructive">{error}</Alert> : null}
    </form>
  );
}