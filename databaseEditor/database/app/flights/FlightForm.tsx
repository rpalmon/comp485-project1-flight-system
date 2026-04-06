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
import { addHoursToDateTimeLocal, pick, randInt, randomDateTimeLocal, randomFlightNumber } from "@/lib/random";

type SelectOption = {
  id: number | string;
  name?: string;
  code?: string;
  model_name?: string;
};

type FlightInput = {
  airline_id: string;
  flight_number: string;
  origin_airport_id: string;
  destination_airport_id: string;
  departure_at: string;
  arrival_at: string;
  aircraft_model_id: string;
  base_price: string;
};

const initialState: FlightInput = {
  airline_id: "",
  flight_number: "",
  origin_airport_id: "",
  destination_airport_id: "",
  departure_at: "",
  arrival_at: "",
  aircraft_model_id: "",
  base_price: "",
};

function toDbId(value: string) {
  return /^\d+$/.test(value) ? Number(value) : value;
}

export default function FlightForm() {
  const router = useRouter();
  const [form, setForm] = useState<FlightInput>(initialState);
  const [airlines, setAirlines] = useState<SelectOption[]>([]);
  const [airports, setAirports] = useState<SelectOption[]>([]);
  const [aircraftModels, setAircraftModels] = useState<SelectOption[]>([]);
  const [loadingOptions, setLoadingOptions] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    async function loadOptions() {
      setLoadingOptions(true);
      const [airlinesRes, airportsRes, modelsRes] = await Promise.all([
        supabase.from("airlines").select("id,name").limit(500),
        supabase.from("airports").select("id,name,code").limit(500),
        supabase.from("aircraft_models").select("id,model_name").limit(500),
      ]);

      if (!mounted) {
        return;
      }

      if (airlinesRes.error || airportsRes.error || modelsRes.error) {
        setError(
          airlinesRes.error?.message ??
            airportsRes.error?.message ??
            modelsRes.error?.message ??
            "Failed to load options."
        );
      } else {
        setAirlines((airlinesRes.data ?? []) as SelectOption[]);
        setAirports((airportsRes.data ?? []) as SelectOption[]);
        setAircraftModels((modelsRes.data ?? []) as SelectOption[]);
      }

      setLoadingOptions(false);
    }

    void loadOptions();
    return () => {
      mounted = false;
    };
  }, []);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    const requiredFields: Array<keyof FlightInput> = [
      "airline_id",
      "flight_number",
      "origin_airport_id",
      "destination_airport_id",
      "departure_at",
      "arrival_at",
      "aircraft_model_id",
      "base_price",
    ];
    const missing = requiredFields.some((field) => !form[field]);
    if (missing) {
      setError("All required flight fields must be filled.");
      return;
    }

    const basePrice = Number(form.base_price);
    if (Number.isNaN(basePrice) || basePrice < 0) {
      setError("Base price must be a valid non-negative number.");
      return;
    }

    if (form.origin_airport_id === form.destination_airport_id) {
      setError("Origin and destination airports must be different.");
      return;
    }

    const departureDate = new Date(form.departure_at);
    const arrivalDate = new Date(form.arrival_at);
    if (Number.isNaN(departureDate.getTime()) || Number.isNaN(arrivalDate.getTime())) {
      setError("Departure and arrival dates must be valid.");
      return;
    }
    if (arrivalDate <= departureDate) {
      setError("Arrival time must be later than departure time.");
      return;
    }

    setSaving(true);
    const { error: insertError } = await supabase.from("flights").insert([
      {
        airline_id: toDbId(form.airline_id),
        flight_number: form.flight_number.trim(),
        origin_airport_id: toDbId(form.origin_airport_id),
        destination_airport_id: toDbId(form.destination_airport_id),
        departure_at: departureDate.toISOString(),
        arrival_at: arrivalDate.toISOString(),
        aircraft_model_id: toDbId(form.aircraft_model_id),
        base_price: basePrice,
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
    const airline = airlines.length > 0 ? pick(airlines) : null;

    let originAirportId = "";
    let destinationAirportId = "";

    if (airports.length >= 2) {
      const origin = pick(airports);
      const destinationCandidates = airports.filter((item) => item.id !== origin.id);
      const destination = pick(destinationCandidates);
      originAirportId = String(origin.id);
      destinationAirportId = String(destination.id);
    } else if (airports.length === 1) {
      originAirportId = String(airports[0].id);
      destinationAirportId = String(airports[0].id);
    }

    const aircraftModel = aircraftModels.length > 0 ? pick(aircraftModels) : null;
    const departureAt = randomDateTimeLocal(2, 96);
    const arrivalAt = addHoursToDateTimeLocal(departureAt, randInt(1, 16));

    setForm({
      airline_id: airline ? String(airline.id) : "",
      flight_number: randomFlightNumber(),
      origin_airport_id: originAirportId,
      destination_airport_id: destinationAirportId,
      departure_at: departureAt,
      arrival_at: arrivalAt,
      aircraft_model_id: aircraftModel ? String(aircraftModel.id) : "",
      base_price: String(randInt(80, 1400)),
    });
  }

  return (
    <form onSubmit={onSubmit} className="grid gap-3">
      <div className="grid gap-1.5">
        <Label htmlFor="flight-airline">Airline</Label>
        <NativeSelect id="flight-airline" value={form.airline_id} onChange={(e) => setForm({ ...form, airline_id: e.target.value })}>
          <option value="">Select airline</option>
          {airlines.map((airline) => (
            <option key={String(airline.id)} value={String(airline.id)}>
              {airline.name ?? airline.id}
            </option>
          ))}
        </NativeSelect>
      </div>

      <div className="grid gap-1.5">
        <Label htmlFor="flight-number">Flight Number</Label>
        <Input id="flight-number" placeholder="e.g. AA203" value={form.flight_number} onChange={(e) => setForm({ ...form, flight_number: e.target.value.toUpperCase() })} />
      </div>

      <div className="grid gap-1.5 sm:grid-cols-2">
        <div className="grid gap-1.5">
          <Label htmlFor="flight-origin">Origin Airport</Label>
          <NativeSelect id="flight-origin" value={form.origin_airport_id} onChange={(e) => setForm({ ...form, origin_airport_id: e.target.value })}>
            <option value="">Select origin airport</option>
            {airports.map((airport) => (
              <option key={String(airport.id)} value={String(airport.id)}>
                {(airport.code ? `${airport.code} - ` : "") + (airport.name ?? airport.id)}
              </option>
            ))}
          </NativeSelect>
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="flight-destination">Destination Airport</Label>
          <NativeSelect id="flight-destination" value={form.destination_airport_id} onChange={(e) => setForm({ ...form, destination_airport_id: e.target.value })}>
            <option value="">Select destination airport</option>
            {airports.map((airport) => (
              <option key={String(airport.id)} value={String(airport.id)}>
                {(airport.code ? `${airport.code} - ` : "") + (airport.name ?? airport.id)}
              </option>
            ))}
          </NativeSelect>
        </div>
      </div>

      <div className="grid gap-1.5 sm:grid-cols-2">
        <div className="grid gap-1.5">
          <Label htmlFor="flight-departure">Departure At</Label>
          <Input id="flight-departure" type="datetime-local" value={form.departure_at} onChange={(e) => setForm({ ...form, departure_at: e.target.value })} />
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="flight-arrival">Arrival At</Label>
          <Input id="flight-arrival" type="datetime-local" value={form.arrival_at} onChange={(e) => setForm({ ...form, arrival_at: e.target.value })} />
        </div>
      </div>

      <div className="grid gap-1.5 sm:grid-cols-2">
        <div className="grid gap-1.5">
          <Label htmlFor="flight-aircraft">Aircraft Model</Label>
          <NativeSelect id="flight-aircraft" value={form.aircraft_model_id} onChange={(e) => setForm({ ...form, aircraft_model_id: e.target.value })}>
            <option value="">Select aircraft model</option>
            {aircraftModels.map((model) => (
              <option key={String(model.id)} value={String(model.id)}>
                {model.model_name ?? model.id}
              </option>
            ))}
          </NativeSelect>
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="flight-base-price">Base Price</Label>
          <Input id="flight-base-price" placeholder="Base Price" type="number" min={0} step="0.01" value={form.base_price} onChange={(e) => setForm({ ...form, base_price: e.target.value })} />
        </div>
      </div>

      <div className="flex gap-2">
        <Button type="button" variant="outline" onClick={randomizeForm} disabled={loadingOptions}>Randomize</Button>
        <Button type="submit" disabled={saving || loadingOptions}>{saving ? "Saving..." : "Create"}</Button>
      </div>
      {loadingOptions ? <p className="text-sm text-muted-foreground">Loading options...</p> : null}
      {error ? <Alert className="border-destructive/40 text-destructive">{error}</Alert> : null}
    </form>
  );
}