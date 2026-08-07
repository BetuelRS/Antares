
import { execFile } from 'node:child_process';

const ADB = 'C:/Users/Betuel/AppData/Local/Android/Sdk/platform-tools/adb.exe';
const seconds = Number(process.argv[2] ?? 420);
const speed = Number(process.argv[3] ?? 3.0);

const M_PER_DEG_LAT = 111_320;
let lat = 41.157900;
let lon = -8.629100;
const alt = 90;

const geoFix = (lon, lat, alt) =>
  new Promise((res) => execFile(ADB, ['emu', 'geo', 'fix', String(lon), String(lat), String(alt)], () => res()));

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

(async () => {
  console.log(`corrida simulada: ${seconds}s a ${speed} m/s → ~${((seconds * speed) / 1000).toFixed(2)} km`);

  const t0 = Date.now();
  for (let t = 0; t < seconds; t++) {

    const heading = (t * 0.6 * Math.PI) / 180;
    const dLat = (speed * Math.cos(heading)) / M_PER_DEG_LAT;
    const dLon = (speed * Math.sin(heading)) / (M_PER_DEG_LAT * Math.cos((lat * Math.PI) / 180));
    lat += dLat;
    lon += dLon;
    await geoFix(lon.toFixed(6), lat.toFixed(6), alt);
    if (t % 30 === 0) console.log(`  t=${t}s  ${lat.toFixed(5)},${lon.toFixed(5)}  (~${((t * speed) / 1000).toFixed(2)} km)`);
    const nextAt = t0 + (t + 1) * 1000;
    await sleep(Math.max(0, nextAt - Date.now()));
  }
  console.log('fim da rota simulada');
})();
