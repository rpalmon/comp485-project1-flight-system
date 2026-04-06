"use client";
// WARNING: Public admin UI — add authentication and RLS before deploying.
import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Alert } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { NativeSelect } from "@/components/ui/native-select";
import { supabase } from "@/lib/supabase/client";
import { pick, randInt } from "@/lib/random";

type SeatMapInput = {
  flight_id: string;
  seat_label: string;
  class_name: string;
  is_window: boolean;
  is_aisle: boolean;
  is_middle: boolean;
  is_available: boolean;
};

const initialState: SeatMapInput = {
  flight_id: "",
  seat_label: "",
  class_name: "",
  is_window: false,
  is_aisle: false,
  is_middle: false,
  is_available: true,
};

type FlightOption = { id: number | string; flight_number?: string };

function toDbId(value: string) {
  return /^\d+$/.test(value) ? Number(value) : value;
}

export default function SeatMapForm() {
  const router = useRouter();
  const [form, setForm] = useState<SeatMapInput>(initialState);
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

    if (!form.flight_id || !form.seat_label || !form.class_name) {
      setError("Flight ID, seat label, and class name are required.");
      return;
    }

    setSaving(true);
    const { error: insertError } = await supabase.from("seat_map").insert([
      {
        flight_id: toDbId(form.flight_id),
        seat_label: form.seat_label.trim(),
        class_name: form.class_name.trim(),
        is_window: form.is_window,
        is_aisle: form.is_aisle,
        is_middle: form.is_middle,
        is_available: form.is_available,
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
    const seatRow = randInt(1, 45);
    const seatCol = pick(["A", "B", "C", "D", "E", "F"]);
    const seatType = pick(["window", "aisle", "middle"]);

    setForm({
      flight_id: flights.length > 0 ? String(pick(flights).id) : "",
      seat_label: `${seatRow}${seatCol}`,
      class_name: pick(["economy", "premium", "business", "first"]),
      is_window: seatType === "window",
      is_aisle: seatType === "aisle",
      is_middle: seatType === "middle",
      is_available: pick([true, true, true, false]),
    });
  }

  return (
    <form onSubmit={onSubmit} className="grid gap-3">
      <div className="grid gap-1.5 sm:grid-cols-2">
        <div className="grid gap-1.5">
          <Label htmlFor="seat-map-flight-id">Flight ID</Label>
          <NativeSelect id="seat-map-flight-id" value={form.flight_id} onChange={(e) => setForm({ ...form, flight_id: e.target.value })}>
            <option value="">Select flight</option>
            {flights.map((flight) => (
              <option key={String(flight.id)} value={String(flight.id)}>
                {flight.flight_number ? `${flight.flight_number} (${String(flight.id)})` : String(flight.id)}
              </option>
            ))}
          </NativeSelect>
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="seat-map-seat-label">Seat Label</Label>
          <Input id="seat-map-seat-label" placeholder="e.g. 12A" value={form.seat_label} onChange={(e) => setForm({ ...form, seat_label: e.target.value.toUpperCase() })} />
        </div>
      </div>
      <div className="grid gap-1.5">
        <Label htmlFor="seat-map-class">Class Name</Label>
        <Input id="seat-map-class" placeholder="Class Name" value={form.class_name} onChange={(e) => setForm({ ...form, class_name: e.target.value })} />
      </div>

      <div className="grid gap-2 sm:grid-cols-2">
        <Label className="flex items-center gap-2"><Checkbox checked={form.is_window} onChange={(e) => setForm({ ...form, is_window: e.target.checked })} /> Is Window</Label>
        <Label className="flex items-center gap-2"><Checkbox checked={form.is_aisle} onChange={(e) => setForm({ ...form, is_aisle: e.target.checked })} /> Is Aisle</Label>
        <Label className="flex items-center gap-2"><Checkbox checked={form.is_middle} onChange={(e) => setForm({ ...form, is_middle: e.target.checked })} /> Is Middle</Label>
        <Label className="flex items-center gap-2"><Checkbox checked={form.is_available} onChange={(e) => setForm({ ...form, is_available: e.target.checked })} /> Is Available</Label>
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