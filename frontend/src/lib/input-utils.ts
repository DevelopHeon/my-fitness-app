export function sanitizeText(value: string, maxLength = 100) {
  return value.replace(/[0-9]/g, "").slice(0, maxLength);
}

export function sanitizeInteger(value: string) {
  return value.replace(/\D/g, "");
}

export function sanitizeDecimal(value: string, decimalPlaces = 2) {
  const cleaned = value.replace(/[^\d.]/g, "");
  const [whole = "", ...rest] = cleaned.split(".");
  if (rest.length === 0) return whole;
  return whole + "." + rest.join("").slice(0, decimalPlaces);
}

export function todayString() {
  const now = new Date();
  return localDateString(now);
}

export function currentTimeString() {
  const now = new Date();
  return [
    String(now.getHours()).padStart(2, "0"),
    String(now.getMinutes()).padStart(2, "0"),
  ].join(":");
}

export function localDateString(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return [year, month, day].join("-");
}

export function localTimeString(date: Date) {
  return [
    String(date.getHours()).padStart(2, "0"),
    String(date.getMinutes()).padStart(2, "0"),
  ].join(":");
}
