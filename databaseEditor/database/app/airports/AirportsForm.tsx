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

type AirportInput = {
  code: string;
  name: string;
  city: string;
  country: string;
};

const initialState: AirportInput = {
  code: "",
  name: "",
  city: "",
  country: "",
};

export default function AirportsForm() {
  const router = useRouter();
  const [form, setForm] = useState<AirportInput>(initialState);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    if (!form.code || !form.name) {
      setError("Code and name are required.");
      return;
    }

    setSaving(true);
    const { error: insertError } = await supabase.from("airports").insert([
      {
        code: form.code.trim(),
        name: form.name.trim(),
        city: form.city.trim() || null,
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
    setForm({
      code: randomCode(3),
      name: `${pick(["North", "South", "East", "West", "Central"])} ${pick(["International", "Regional", "City"])} Airport`,
      city: pick(["New York", "Dallas", "Chicago", "Seattle", "Miami", "Denver"]),
      country: pick(["USA", "Canada", "UK", "France", "Japan", "UAE"]),
    });
  }

  return (
    <form onSubmit={onSubmit} className="grid gap-3">
      <div className="grid gap-1.5">
        <Label htmlFor="airport-code">Code (IATA)</Label>
        <Input id="airport-code" placeholder="e.g. JFK" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} />
      </div>
      <div className="grid gap-1.5">
        <Label htmlFor="airport-name">Name</Label>
        <Input id="airport-name" placeholder="Airport name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
      </div>
      <div className="grid gap-1.5 sm:grid-cols-2">
        <div className="grid gap-1.5">
          <Label htmlFor="airport-city">City</Label>
          <Input id="airport-city" placeholder="City" value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} />
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="airport-country">Country</Label>
          <Input id="airport-country" placeholder="Country" value={form.country} onChange={(e) => setForm({ ...form, country: e.target.value })} />
        </div>
      </div>

      <div className="flex gap-2">
        <Button type="button" variant="outline" onClick={randomizeForm}>Randomize</Button>
        <Button type="submit" disabled={saving}>{saving ? "Saving..." : "Create"}</Button>
      </div>
      {error ? <Alert className="border-destructive/40 text-destructive">{error}</Alert> : null}
    </form>
  );
}