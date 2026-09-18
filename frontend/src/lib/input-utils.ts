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
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return [year, month, day].join("-");
}
