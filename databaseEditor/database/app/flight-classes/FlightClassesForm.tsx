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

type FlightClassInput = {
  flight_id: string;
  class_name: string;
  seats_total: string;
  price_modifier: string;
};

const initialState: FlightClassInput = {
  flight_id: "",
  class_name: "",
  seats_total: "",
  price_modifier: "1",
};

type FlightOption = { id: number | string; flight_number?: string };

function toDbId(value: string) {
  return /^\d+$/.test(value) ? Number(value) : value;
}

export default function FlightClassesForm() {
  const router = useRouter();
  const [form, setForm] = useState<FlightClassInput>(initialState);
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

    if (!form.flight_id || !form.class_name || !form.seats_total || !form.price_modifier) {
      setError("All fields are required.");
      return;
    }

    const seatsTotal = Number(form.seats_total);
    const priceModifier = Number(form.price_modifier);
    if (Number.isNaN(seatsTotal) || seatsTotal <= 0) {
      setError("Seats total must be a positive number.");
      return;
    }
    if (Number.isNaN(priceModifier) || priceModifier < 0) {
      setError("Price modifier must be a valid non-negative number.");
      return;
    }

    setSaving(true);
    const { error: insertError } = await supabase.from("flight_classes").insert([
      {
        flight_id: toDbId(form.flight_id),
        class_name: form.class_name.trim(),
        seats_total: seatsTotal,
        price_modifier: priceModifier,
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
    setForm({
      flight_id: flights.length > 0 ? String(pick(flights).id) : "",
      class_name: pick(["economy", "premium", "business", "first"]),
      seats_total: String(randInt(8, 160)),
      price_modifier: String((randInt(90, 260) / 100).toFixed(2)),
    });
  }

  return (
    <form onSubmit={onSubmit} className="grid gap-3">
      <div className="grid gap-1.5 sm:grid-cols-2">
        <div className="grid gap-1.5">
          <Label htmlFor="flight-class-flight-id">Flight ID</Label>
          <NativeSelect id="flight-class-flight-id" value={form.flight_id} onChange={(e) => setForm({ ...form, flight_id: e.target.value })}>
            <option value="">Select flight</option>
            {flights.map((flight) => (
              <option key={String(flight.id)} value={String(flight.id)}>
                {flight.flight_number ? `${flight.flight_number} (${String(flight.id)})` : String(flight.id)}
              </option>
            ))}
          </NativeSelect>
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="flight-class-name">Class Name</Label>
          <Input id="flight-class-name" placeholder="Class Name" value={form.class_name} onChange={(e) => setForm({ ...form, class_name: e.target.value })} />
        </div>
      </div>
      <div className="grid gap-1.5 sm:grid-cols-2">
        <div className="grid gap-1.5">
          <Label htmlFor="flight-class-seats">Seats Total</Label>
          <Input id="flight-class-seats" placeholder="Seats Total" type="number" min={1} value={form.seats_total} onChange={(e) => setForm({ ...form, seats_total: e.target.value })} />
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="flight-class-modifier">Price Modifier</Label>
          <Input id="flight-class-modifier" placeholder="Price Modifier" type="number" min={0} step="0.01" value={form.price_modifier} onChange={(e) => setForm({ ...form, price_modifier: e.target.value })} />
        </div>
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