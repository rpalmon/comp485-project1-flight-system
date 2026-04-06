"use client";
// WARNING: Public admin UI — add authentication and RLS before deploying.
import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { Alert } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { supabase } from "@/lib/supabase/client";
import { pick, randomCode } from "@/lib/random";

type AirlineInput = {
  name: string;
  iata_code: string;
  icao_code: string;
  country: string;
};

const initialState: AirlineInput = {
  name: "",
  iata_code: "",
  icao_code: "",
  country: "",
};

export default function AirlinesForm() {
  const router = useRouter();
  const [form, setForm] = useState<AirlineInput>(initialState);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    if (!form.name) {
      setError("Name is required.");
      return;
    }

    setSaving(true);
    const { error: insertError } = await supabase.from("airlines").insert([
      {
        name: form.name.trim(),
        iata_code: form.iata_code.trim() || null,
        icao_code: form.icao_code.trim() || null,
        country: form.country.trim() || null,
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
    const name = `${pick(["Global", "Sky", "Pacific", "Atlantic", "Aero", "Nimbus"])} ${pick(["Air", "Airways", "Flights", "Express"])}`;
    setForm({
      name,
      iata_code: randomCode(2),
      icao_code: randomCode(3),
      country: pick(["USA", "Canada", "UK", "Germany", "Japan", "Australia"]),
    });
  }

  return (
    <form onSubmit={onSubmit} className="grid gap-3">
      <div className="grid gap-1.5">
        <Label htmlFor="airline-name">Name</Label>
        <Input id="airline-name" placeholder="Airline name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
      </div>
      <div className="grid gap-1.5 sm:grid-cols-2">
        <div className="grid gap-1.5">
          <Label htmlFor="airline-iata">IATA Code</Label>
          <Input id="airline-iata" placeholder="AA" value={form.iata_code} onChange={(e) => setForm({ ...form, iata_code: e.target.value.toUpperCase() })} />
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="airline-icao">ICAO Code</Label>
          <Input id="airline-icao" placeholder="AAL" value={form.icao_code} onChange={(e) => setForm({ ...form, icao_code: e.target.value.toUpperCase() })} />
        </div>
      </div>
      <div className="grid gap-1.5">
        <Label htmlFor="airline-country">Country</Label>
        <Input id="airline-country" placeholder="Country" value={form.country} onChange={(e) => setForm({ ...form, country: e.target.value })} />
      </div>
      <div className="flex gap-2">
        <Button type="button" variant="outline" onClick={randomizeForm}>Randomize</Button>
        <Button type="submit" disabled={saving}>{saving ? "Saving..." : "Create"}</Button>
      </div>
      {error ? <Alert className="border-destructive/40 text-destructive">{error}</Alert> : null}
    </form>
  );
}
