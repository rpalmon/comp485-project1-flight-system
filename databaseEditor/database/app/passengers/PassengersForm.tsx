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
import { pick, randInt } from "@/lib/random";

type PassengerInput = {
  booking_id: string;
  full_name: string;
  passenger_type: string;
  seat_label: string;
  seat_preferences: string;
  meal_preferences: string;
  beverages: string;
};

function parseCsvToTextArray(value: string): string[] {
  return value
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

const initialState: PassengerInput = {
  booking_id: "",
  full_name: "",
  passenger_type: "adult",
  seat_label: "",
  seat_preferences: "",
  meal_preferences: "",
  beverages: "",
};

type BookingOption = { id: number | string; booking_reference?: string };

function toDbId(value: string) {
  return /^\d+$/.test(value) ? Number(value) : value;
}

export default function PassengersForm() {
  const router = useRouter();
  const [form, setForm] = useState<PassengerInput>(initialState);
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

    if (!form.booking_id || !form.full_name || !form.passenger_type) {
      setError("Booking ID, full name, and passenger type are required.");
      return;
    }

    setSaving(true);
    const { error: insertError } = await supabase.from("passengers").insert([
      {
        booking_id: toDbId(form.booking_id),
        full_name: form.full_name.trim(),
        passenger_type: form.passenger_type.trim(),
        seat_label: form.seat_label.trim() || null,
        seat_preferences: parseCsvToTextArray(form.seat_preferences),
        meal_preferences: parseCsvToTextArray(form.meal_preferences),
        beverages: parseCsvToTextArray(form.beverages),
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
    const fullName = `${pick(["Alex", "Jamie", "Taylor", "Morgan", "Jordan", "Casey"])} ${pick(["Smith", "Johnson", "Lee", "Brown", "Garcia", "Khan"])}`;
    setForm({
      booking_id: bookings.length > 0 ? String(pick(bookings).id) : "",
      full_name: fullName,
      passenger_type: pick(["adult", "child", "infant"]),
      seat_label: `${randInt(1, 45)}${pick(["A", "B", "C", "D", "E", "F"])}`,
      seat_preferences: pick(["window", "aisle", "extra_legroom", "front_row"]),
      meal_preferences: pick(["vegetarian", "kosher", "halal", "standard"]),
      beverages: pick(["water", "coffee", "tea", "juice"]),
    });
  }

  return (
    <form onSubmit={onSubmit} className="grid gap-3">
      <div className="grid gap-1.5 sm:grid-cols-2">
        <div className="grid gap-1.5">
          <Label htmlFor="passenger-booking-id">Booking ID</Label>
          <NativeSelect id="passenger-booking-id" value={form.booking_id} onChange={(e) => setForm({ ...form, booking_id: e.target.value })}>
            <option value="">Select booking</option>
            {bookings.map((booking) => (
              <option key={String(booking.id)} value={String(booking.id)}>
                {booking.booking_reference ? `${booking.booking_reference} (${String(booking.id)})` : String(booking.id)}
              </option>
            ))}
          </NativeSelect>
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="passenger-name">Full Name</Label>
          <Input id="passenger-name" placeholder="Full Name" value={form.full_name} onChange={(e) => setForm({ ...form, full_name: e.target.value })} />
        </div>
      </div>

      <div className="grid gap-1.5 sm:grid-cols-2">
        <div className="grid gap-1.5">
          <Label htmlFor="passenger-type">Passenger Type</Label>
          <Input id="passenger-type" placeholder="Passenger Type" value={form.passenger_type} onChange={(e) => setForm({ ...form, passenger_type: e.target.value })} />
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="passenger-seat-label">Seat Label</Label>
          <Input id="passenger-seat-label" placeholder="Seat Label" value={form.seat_label} onChange={(e) => setForm({ ...form, seat_label: e.target.value.toUpperCase() })} />
        </div>
      </div>

      <div className="grid gap-1.5">
        <Label htmlFor="passenger-seat-preferences">Seat Preferences (comma-separated)</Label>
        <Input id="passenger-seat-preferences" placeholder="window, extra_legroom" value={form.seat_preferences} onChange={(e) => setForm({ ...form, seat_preferences: e.target.value })} />
      </div>
      <div className="grid gap-1.5 sm:grid-cols-2">
        <div className="grid gap-1.5">
          <Label htmlFor="passenger-meal-preferences">Meal Preferences</Label>
          <Input id="passenger-meal-preferences" placeholder="vegetarian, low_sodium" value={form.meal_preferences} onChange={(e) => setForm({ ...form, meal_preferences: e.target.value })} />
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="passenger-beverages">Beverages</Label>
          <Input id="passenger-beverages" placeholder="water, coffee" value={form.beverages} onChange={(e) => setForm({ ...form, beverages: e.target.value })} />
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