import { readFile } from "node:fs/promises";
import path from "node:path";

const outDir = path.resolve("out");

const requiredPngs = new Map([
  ["icon-192.png", [192, 192]],
  ["icon-512.png", [512, 512]],
  ["icon-maskable-512.png", [512, 512]],
  ["apple-touch-icon.png", [180, 180]],
]);

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

async function pngDimensions(fileName) {
  const buffer = await readFile(path.join(outDir, fileName));
  const signature = buffer.subarray(0, 8).toString("hex");
  assert(
    signature === "89504e470d0a1a0a",
    `${fileName} is not a valid PNG file`,
  );
  return [buffer.readUInt32BE(16), buffer.readUInt32BE(20)];
}

const manifest = JSON.parse(
  await readFile(path.join(outDir, "manifest.webmanifest"), "utf8"),
);

assert(manifest.id === "/", "manifest id must be /");
assert(manifest.start_url === "/", "manifest start_url must be /");
assert(manifest.scope === "/", "manifest scope must be /");
assert(manifest.display === "standalone", "manifest display must be standalone");

const iconKey = (icon) =>
  `${icon.src}|${icon.sizes}|${icon.type}|${icon.purpose}`;
const icons = new Set(manifest.icons.map(iconKey));

assert(
  icons.has("/icon-192.png|192x192|image/png|any"),
  "manifest is missing the 192x192 app icon",
);
assert(
  icons.has("/icon-512.png|512x512|image/png|any"),
  "manifest is missing the 512x512 app icon",
);
assert(
  icons.has("/icon-maskable-512.png|512x512|image/png|maskable"),
  "manifest is missing the maskable icon",
);

for (const [fileName, expected] of requiredPngs) {
  const actual = await pngDimensions(fileName);
  assert(
    actual[0] === expected[0] && actual[1] === expected[1],
    `${fileName} must be ${expected[0]}x${expected[1]}, got ${actual[0]}x${actual[1]}`,
  );
}

const indexHtml = await readFile(path.join(outDir, "index.html"), "utf8");

assert(
  indexHtml.includes('name="apple-mobile-web-app-capable" content="yes"'),
  "index.html must enable iOS standalone web app mode",
);
assert(
  indexHtml.includes('rel="apple-touch-icon" href="/apple-touch-icon.png"'),
  "index.html must reference the Apple touch icon",
);

const serviceWorker = await readFile(path.join(outDir, "sw.js"), "utf8");

for (const pathPrefix of ["/api/", "/oauth2/", "/login/", "/logout", "/actuator/"]) {
  assert(
    serviceWorker.includes(`"${pathPrefix}"`),
    `service worker must bypass ${pathPrefix}`,
  );
}

assert(
  serviceWorker.includes('request.mode === "navigate"'),
  "service worker must not cache navigation responses",
);

console.log("PWA assets verified");
