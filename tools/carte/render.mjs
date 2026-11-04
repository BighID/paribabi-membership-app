// Renders a face of the card from its SVG: Outfit embedded, some layers hidden, positions moved, the logo placed,
// the view box cut. Usage: node render.mjs <config.json>
import { createRequire } from 'node:module';
import { readFileSync } from 'node:fs';
const require = createRequire('/Users/macpro/Documents/Paribabi/paribabi-frontend-app/recettes/lot-2-adhesions/05-whatsapp-et-email/');
const puppeteer = require('puppeteer-core');
const config = JSON.parse(readFileSync(process.argv[2], 'utf8'));
const fonts = '/Users/macpro/StudioProjects/AdherentApp/assets';
const b64 = (path) => readFileSync(path).toString('base64');
const face = (weight, file) => `@font-face{font-family:Outfit;font-weight:${weight};src:url(data:font/ttf;base64,${b64(`${fonts}/${file}`)}) format('truetype')}`;
let svg = readFileSync(config.svg, 'utf8').replace(/<\?xml[^>]*>/, '');
for (const [from, to] of config.edits ?? []) {
  if (!svg.includes(from)) throw new Error(`Introuvable : ${from}`);
  svg = svg.split(from).join(to);
}
const [vx, vy, vw, vh] = config.viewBox;
svg = svg.replace(/viewBox="[^"]*"/, `viewBox="${vx} ${vy} ${vw} ${vh}"`).replace(/width="85.6mm" height="54mm"/, `width="${vw}" height="${vh}"`);
if (config.logo) {
  const [x, y, w, h] = config.logo.box;
  const pad = config.logo.pad ?? 8;
  const image = `<image href="data:image/png;base64,${b64(config.logo.file)}" x="${x + pad}" y="${y + pad}" width="${w - 2 * pad}" height="${h - 2 * pad}" preserveAspectRatio="xMidYMid meet"/>`;
  svg = svg.replace('</svg>', `${image}</svg>`);
}
const hidden = (config.hide ?? []).map((id) => `#${id}`).join(',');
const html = `<!doctype html><html><head><style>
${face(400, 'Outfit-Regular.ttf')}${face(500, 'Outfit-Medium.ttf')}${face(600, 'Outfit-SemiBold.ttf')}${face(700, 'Outfit-Bold.ttf')}
html,body{margin:0;background:transparent}svg{display:block;width:${vw}px;height:${vh}px}
${hidden ? `${hidden}{display:none!important}` : ''}</style></head><body>${svg}</body></html>`;
const browser = await puppeteer.launch({
  executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  headless: true,
  defaultViewport: { width: vw, height: vh, deviceScaleFactor: config.scale ?? 2 },
});
const page = await browser.newPage();
await page.setContent(html, { waitUntil: 'load' });
await page.evaluate(async () => {
  await Promise.all(['400', '500', '600', '700'].map((w) => document.fonts.load(`${w} 20px Outfit`)));
  await document.fonts.ready;
});
await page.screenshot({ path: config.out, omitBackground: true, clip: { x: 0, y: 0, width: vw, height: vh } });
await browser.close();
