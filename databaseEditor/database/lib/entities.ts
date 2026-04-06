// WARNING: Public admin UI — add authentication and RLS before deploying.
export const entityLinks = [
  { href: "/airports", label: "Airports" },
  { href: "/airlines", label: "Airlines" },
  { href: "/aircraft-models", label: "Aircraft Models" },
  { href: "/flights", label: "Flights" },
  { href: "/flight-classes", label: "Flight Classes" },
  { href: "/seat-map", label: "Seat Map" },
  { href: "/bookings", label: "Bookings" },
  { href: "/passengers", label: "Passengers" },
  { href: "/payments", label: "Payments" },
] as const;