"use client";
// WARNING: Public admin UI — add authentication and RLS before deploying.
import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { Alert } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { supabase } from "@/lib/supabase/client";
import { pick, randInt } from "@/lib/random";

type AircraftModelInput = {
  manufacturer: string;
  model_name: string;
  seat_capacity: string;
};

const initialState: AircraftModelInput = {
  manufacturer: "",
  model_name: "",
  seat_capacity: "",
};

export default function AircraftModelsForm() {
  const router = useRouter();
  const [form, setForm] = useState<AircraftModelInput>(initialState);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    if (!form.manufacturer || !form.model_name || !form.seat_capacity) {
      setError("Manufacturer, model name, and seat capacity are required.");
      return;
    }

    const seatCapacity = Number(form.seat_capacity);
    if (Number.isNaN(seatCapacity) || seatCapacity <= 0) {
      setError("Seat capacity must be a positive number.");
      return;
    }

    setSaving(true);
    const { error: insertError } = await supabase.from("aircraft_models").insert([
      {
        manufacturer: form.manufacturer.trim(),
        model_name: form.model_name.trim(),
        seat_capacity: seatCapacity,
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
    const manufacturer = pick(["Boeing", "Airbus", "Embraer", "Bombardier"]);
    const model_name = pick(["737-800", "A320neo", "E190", "A350-900", "787-9"]);

    setForm({
      manufacturer,
      model_name,
      seat_capacity: String(randInt(70, 360)),
    });
  }

  return (
    <form onSubmit={onSubmit} className="grid gap-3">
      <div className="grid gap-1.5">
        <Label htmlFor="aircraft-manufacturer">Manufacturer</Label>
        <Input id="aircraft-manufacturer" placeholder="Manufacturer" value={form.manufacturer} onChange={(e) => setForm({ ...form, manufacturer: e.target.value })} />
      </div>
      <div className="grid gap-1.5">
        <Label htmlFor="aircraft-model">Model Name</Label>
        <Input id="aircraft-model" placeholder="Model Name" value={form.model_name} onChange={(e) => setForm({ ...form, model_name: e.target.value })} />
      </div>
      <div className="grid gap-1.5">
        <Label htmlFor="aircraft-capacity">Seat Capacity</Label>
        <Input id="aircraft-capacity" placeholder="Seat Capacity" type="number" min={1} value={form.seat_capacity} onChange={(e) => setForm({ ...form, seat_capacity: e.target.value })} />
      </div>
      <div className="flex gap-2">
        <Button type="button" variant="outline" onClick={randomizeForm}>Randomize</Button>
        <Button type="submit" disabled={saving}>{saving ? "Saving..." : "Create"}</Button>
      </div>
      {error ? <Alert className="border-destructive/40 text-destructive">{error}</Alert> : null}
    </form>
  );
}