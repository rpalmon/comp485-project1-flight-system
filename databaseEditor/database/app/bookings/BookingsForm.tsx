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
import { addHoursToDateTimeLocal, pick, randInt, randomBookingReference, randomDateTimeLocal } from "@/lib/random";

type BookingInput = {
  booking_reference: string;
  flight_id: string;
  class_name: string;
  num_passengers: string;
  total_price: string;
  status: string;
  depart_at: string;
  arrive_at: string;
  contact_email: string;
};

function generateBookingReference() {
  return randomBookingReference();
}

const initialState: BookingInput = {
  booking_reference: generateBookingReference(),
  flight_id: "",
  class_name: "",
  num_passengers: "1",
  total_price: "",
  status: "pending",
  depart_at: "",
  arrive_at: "",
  contact_email: "",
};

type FlightOption = { id: number | string; flight_number?: string };

function toDbId(value: string) {
  return /^\d+$/.test(value) ? Number(value) : value;
}

export default function BookingsForm() {
  const router = useRouter();
  const [form, setForm] = useState<BookingInput>(initialState);
  const [flights, setFlights] = useState<FlightOption[]>([]);
  const [saving, setSaving] = useState(false);
  const [loadingOptions, setLoadingOptions] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    async function loadFlights() {
      setLoadingOptions(true);
      const { data, error: loadError } = await supabase
        .from("flights")
        .select("id,flight_number")
        .limit(500);

      if (!mounted) return;

      if (loadError) {
        setError(loadError.message);
      } else {
        setFlights((data ?? []) as FlightOption[]);
      }

      setLoadingOptions(false);
    }

    void loadFlights();
    return () => {
      mounted = false;
    };
  }, []);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    if (
      !form.booking_reference ||
      !form.flight_id ||
      !form.class_name ||
      !form.num_passengers ||
      !form.total_price ||
      !form.status ||
      !form.depart_at ||
      !form.arrive_at ||
      !form.contact_email
    ) {
      setError("All booking fields are required.");
      return;
    }

    const numPassengers = Number(form.num_passengers);
    const totalPrice = Number(form.total_price);
    if (Number.isNaN(numPassengers) || numPassengers <= 0) {
      setError("Number of passengers must be a positive number.");
      return;
    }
    if (Number.isNaN(totalPrice) || totalPrice < 0) {
      setError("Total price must be a valid non-negative number.");
      return;
    }

    const departDate = new Date(form.depart_at);
    const arriveDate = new Date(form.arrive_at);
    if (Number.isNaN(departDate.getTime()) || Number.isNaN(arriveDate.getTime())) {
      setError("Depart and arrive times must be valid.");
      return;
    }
    if (arriveDate <= departDate) {
      setError("Arrive time must be later than depart time.");
      return;
    }

    if (!form.contact_email.includes("@")) {
      setError("Please enter a valid contact email.");
      return;
    }

    setSaving(true);
    const { error: insertError } = await supabase.from("bookings").insert([
      {
        booking_reference: form.booking_reference.trim(),
        flight_id: toDbId(form.flight_id),
        class_name: form.class_name.trim(),
        num_passengers: numPassengers,
        total_price: totalPrice,
        status: form.status.trim() || "pending",
        depart_at: departDate.toISOString(),
        arrive_at: arriveDate.toISOString(),
        contact_email: form.contact_email.trim(),
      },
    ]);
    setSaving(false);

    if (insertError) {
      setError(insertError.message);
      return;
    }

    setForm({ ...initialState, booking_reference: generateBookingReference() });
    router.refresh();
  }

  function randomizeForm() {
    const departAt = randomDateTimeLocal(4, 120);
    const arriveAt = addHoursToDateTimeLocal(departAt, randInt(1, 20));

    setForm({
      booking_reference: generateBookingReference(),
      flight_id: flights.length > 0 ? String(pick(flights).id) : "",
      class_name: pick(["economy", "premium", "business", "first"]),
      num_passengers: String(randInt(1, 6)),
      total_price: String(randInt(120, 4500)),
      status: pick(["pending", "confirmed", "ticketed"]),
      depart_at: departAt,
      arrive_at: arriveAt,
      contact_email: `${pick(["alex", "sam", "casey", "morgan", "jamie"])}${randInt(10, 99)}@example.com`,
    });
  }

  return (
    <form onSubmit={onSubmit} className="grid gap-3">
      <div className="grid gap-1.5 sm:grid-cols-2">
        <div className="grid gap-1.5">
          <Label htmlFor="booking-reference">Booking Reference</Label>
          <Input id="booking-reference" placeholder="Booking Reference" value={form.booking_reference} onChange={(e) => setForm({ ...form, booking_reference: e.target.value.toUpperCase() })} />
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="booking-flight-id">Flight ID</Label>
          <NativeSelect id="booking-flight-id" value={form.flight_id} onChange={(e) => setForm({ ...form, flight_id: e.target.value })}>
            <option value="">Select flight</option>
            {flights.map((flight) => (
              <option key={String(flight.id)} value={String(flight.id)}>
                {flight.flight_number ? `${flight.flight_number} (${String(flight.id)})` : String(flight.id)}
              </option>
            ))}
          </NativeSelect>
        </div>
      </div>

      <div className="grid gap-1.5 sm:grid-cols-3">
        <div className="grid gap-1.5">
          <Label htmlFor="booking-class">Class Name</Label>
          <Input id="booking-class" placeholder="Class Name" value={form.class_name} onChange={(e) => setForm({ ...form, class_name: e.target.value })} />
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="booking-passengers">Passengers</Label>
          <Input id="booking-passengers" placeholder="Passengers" type="number" min={1} value={form.num_passengers} onChange={(e) => setForm({ ...form, num_passengers: e.target.value })} />
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="booking-total-price">Total Price</Label>
          <Input id="booking-total-price" placeholder="Total Price" type="number" min={0} step="0.01" value={form.total_price} onChange={(e) => setForm({ ...form, total_price: e.target.value })} />
        </div>
      </div>

      <div className="grid gap-1.5 sm:grid-cols-3">
        <div className="grid gap-1.5">
          <Label htmlFor="booking-status">Status</Label>
          <Input id="booking-status" placeholder="Status" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })} />
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="booking-depart-at">Depart At</Label>
          <Input id="booking-depart-at" type="datetime-local" value={form.depart_at} onChange={(e) => setForm({ ...form, depart_at: e.target.value })} />
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="booking-arrive-at">Arrive At</Label>
          <Input id="booking-arrive-at" type="datetime-local" value={form.arrive_at} onChange={(e) => setForm({ ...form, arrive_at: e.target.value })} />
        </div>
      </div>

      <div className="grid gap-1.5">
        <Label htmlFor="booking-email">Contact Email</Label>
        <Input id="booking-email" placeholder="Contact Email" type="email" value={form.contact_email} onChange={(e) => setForm({ ...form, contact_email: e.target.value })} />
      </div>

      <div className="flex gap-2">
        <Button type="button" variant="outline" onClick={randomizeForm} disabled={loadingOptions}>Randomize</Button>
        <Button type="submit" disabled={saving || loadingOptions}>{saving ? "Saving..." : "Create"}</Button>
      </div>
      {loadingOptions ? <p className="text-sm text-muted-foreground">Loading flights...</p> : null}
      {error ? <Alert className="border-destructive/40 text-destructive">{error}</Alert> : null}
    </form>
  );
}