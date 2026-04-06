// WARNING: Public admin UI — add authentication and RLS before deploying.
const alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

export function randInt(min: number, max: number) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

export function pick<T>(values: T[]): T {
  return values[randInt(0, values.length - 1)];
}

export function randomCode(length: number) {
  return Array.from({ length }, () => alphabet[randInt(0, alphabet.length - 1)]).join("");
}

export function randomBookingReference() {
  return Math.random().toString(36).slice(2, 8).toUpperCase();
}

export function randomFlightNumber() {
  return `${pick(["AA", "DL", "UA", "WN", "BA", "AF"])}${randInt(100, 9999)}`;
}

function toLocalDateTimeInputValue(value: Date) {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, "0");
  const day = String(value.getDate()).padStart(2, "0");
  const hour = String(value.getHours()).padStart(2, "0");
  const minute = String(value.getMinutes()).padStart(2, "0");
  return `${year}-${month}-${day}T${hour}:${minute}`;
}

export function addHoursToDateTimeLocal(input: string, hoursToAdd: number) {
  const parsed = new Date(input);
  if (Number.isNaN(parsed.getTime())) {
    return input;
  }

  return toLocalDateTimeInputValue(new Date(parsed.getTime() + hoursToAdd * 60 * 60 * 1000));
}

export function randomDateTimeLocal(minHoursFromNow: number, maxHoursFromNow: number) {
  const now = Date.now();
  const hours = randInt(minHoursFromNow, maxHoursFromNow);
  return toLocalDateTimeInputValue(new Date(now + hours * 60 * 60 * 1000));
}