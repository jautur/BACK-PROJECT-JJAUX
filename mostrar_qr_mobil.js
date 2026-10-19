const os = require('os');
const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');

let QRCode;
try {
  QRCode = require('qrcode');
} catch (e) {
  try {
    QRCode = require(path.join(__dirname, '..', 'FRONT-PROJECT-JJAUX', 'node_modules', 'qrcode'));
  } catch (e2) {
    QRCode = null;
  }
}

// 1. Detectar la IP local a la xarxa Wi-Fi / Ethernet
function getLocalIp() {
  const interfaces = os.networkInterfaces();
  const candidates = [];

  for (const name in interfaces) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        const addr = iface.address;
        // Filtrar adaptadors virtuals coneguts
        if (
          addr.startsWith('127.') ||
          addr.startsWith('169.254.') ||
          addr.startsWith('192.168.56.') || // VirtualBox
          addr.startsWith('172.17.') || // Docker
          addr.startsWith('172.28.') || // WSL
          addr.startsWith('172.29.') || // WSL
          addr.startsWith('172.30.') || // WSL
          addr.startsWith('172.31.')    // Hyper-V
        ) {
          continue;
        }
        // Donar màxima prioritat a xarxes locals domèstiques habituals
        if (addr.startsWith('192.168.')) {
          return addr;
        }
        candidates.push(addr);
      }
    }
  }

  return candidates[0] || 'localhost';
}

const localIp = getLocalIp();
const port = 4300;
const urlWeb = `http://${localIp}:${port}/inici`;
const urlApp = `http://${localIp}:${port}/app`;

async function main() {
  console.log('\n==========================================================');
  console.log('       AVISA - CONNEXIO MOBIL PER WI-FI LOCAL');
  console.log('==========================================================');
  console.log(` IP local detectada:   ${localIp}`);
  console.log(` Port del Frontend:    ${port}`);
  console.log(` Web principal:        ${urlWeb}`);
  console.log(` App PWA operativa:    ${urlApp}`);
  console.log('==========================================================\n');

  let qrDataUrlWeb = '';
  let qrDataUrlApp = '';

  if (QRCode) {
    try {
      // Mostrar QR al terminal
      console.log(' Escaneja aquest codi QR amb la camara del mobil (Web):');
      const terminalQr = await QRCode.toString(urlWeb, { type: 'terminal', small: true });
      console.log(terminalQr);

      // Generar DataURL per a la pagina HTML
      qrDataUrlWeb = await QRCode.toDataURL(urlWeb, { width: 300, margin: 2 });
      qrDataUrlApp = await QRCode.toDataURL(urlApp, { width: 300, margin: 2 });
    } catch (err) {
      console.log(' (No s\'ha pogut renderitzar el codi QR al terminal)');
    }
  } else {
    console.log(' (Consell: instal·la "qrcode" per veure el codi QR directament a la consola)');
  }

  // Generar fitxer HTML visual per obrir al navegador del PC
  const htmlPath = path.join(__dirname, 'mobil_connectar.html');
  const htmlContent = `<!DOCTYPE html>
<html lang="ca">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>AVISA - Connectar Dispositius Mobils</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@500;700;800;900&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg: #09090c;
      --card-bg: #121218;
      --card-border: #232330;
      --neon: #ff1744;
      --bone: #f7f3ed;
      --muted: #94a3b8;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: 'Plus Jakarta Sans', system-ui, sans-serif;
      background-color: var(--bg);
      color: var(--bone);
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 2rem 1rem;
    }
    .container {
      max-width: 820px;
      width: 100%;
      text-align: center;
    }
    .badge {
      display: inline-block;
      background: rgba(255, 23, 68, 0.15);
      color: var(--neon);
      border: 1px solid rgba(255, 23, 68, 0.35);
      padding: 0.35rem 0.85rem;
      border-radius: 9999px;
      font-size: 0.82rem;
      font-weight: 800;
      letter-spacing: 0.05em;
      text-transform: uppercase;
      margin-bottom: 1rem;
    }
    h1 {
      font-size: 2.4rem;
      font-weight: 900;
      letter-spacing: 0.04em;
      margin-bottom: 0.5rem;
      color: #ffffff;
    }
    h1 span {
      color: var(--neon);
      text-shadow: 0 0 20px rgba(255, 23, 68, 0.5);
    }
    .subtitle {
      font-size: 1.05rem;
      color: var(--muted);
      margin-bottom: 2rem;
      line-height: 1.5;
    }
    .wifi-notice {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.65rem;
      background: #181822;
      border: 1px solid #2e2e42;
      border-radius: 12px;
      padding: 0.85rem 1.25rem;
      margin-bottom: 2rem;
      color: #e2e8f0;
      font-weight: 700;
      font-size: 0.92rem;
    }
    .wifi-icon {
      font-size: 1.3rem;
    }
    .cards-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: 1.5rem;
      margin-bottom: 2.5rem;
      text-align: left;
    }
    .card {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 18px;
      padding: 1.75rem;
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
      transition: all 0.25s ease;
    }
    .card:hover {
      border-color: rgba(255, 23, 68, 0.5);
      transform: translateY(-4px);
      box-shadow: 0 16px 40px rgba(0, 0, 0, 0.7), 0 0 20px rgba(255, 23, 68, 0.15);
    }
    .card-title {
      font-size: 1.25rem;
      font-weight: 800;
      color: #ffffff;
      margin-bottom: 0.35rem;
    }
    .card-desc {
      font-size: 0.85rem;
      color: var(--muted);
      margin-bottom: 1.25rem;
    }
    .qr-wrapper {
      background: #ffffff;
      padding: 12px;
      border-radius: 14px;
      display: inline-flex;
      margin-bottom: 1.25rem;
      box-shadow: 0 4px 18px rgba(0, 0, 0, 0.3);
    }
    .qr-wrapper img {
      width: 220px;
      height: 220px;
      display: block;
    }
    .url-badge {
      display: block;
      background: #09090c;
      border: 1px solid #282836;
      color: #38bdf8;
      font-family: monospace;
      font-size: 0.95rem;
      font-weight: 700;
      padding: 0.65rem 1rem;
      border-radius: 8px;
      text-decoration: none;
      word-break: break-all;
      width: 100%;
      transition: all 0.2s ease;
    }
    .url-badge:hover {
      background: rgba(255, 23, 68, 0.12);
      border-color: var(--neon);
      color: #ffffff;
    }
    .instructions-box {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 16px;
      padding: 1.75rem;
      text-align: left;
      margin-top: 1rem;
    }
    .instructions-title {
      font-size: 1.15rem;
      font-weight: 800;
      color: #ffffff;
      margin-bottom: 1rem;
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }
    .steps-list {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
      gap: 1.25rem;
    }
    .step-item {
      background: #161620;
      border: 1px solid #272738;
      border-radius: 12px;
      padding: 1.15rem;
    }
    .step-header {
      font-weight: 800;
      font-size: 0.95rem;
      color: var(--neon);
      margin-bottom: 0.5rem;
      display: flex;
      align-items: center;
      gap: 0.4rem;
    }
    .step-text {
      font-size: 0.85rem;
      color: #cbd5e1;
      line-height: 1.5;
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="badge">Connexió Local Mòbil</div>
    <h1>Desplegament Mòbil <span>AVISA</span></h1>
    <p class="subtitle">Connecta qualsevol telèfon o tauleta a la teva xarxa Wi-Fi local per provar l'aplicació en temps real.</p>

    <div class="wifi-notice">
      <span class="wifi-icon">📶</span>
      <span>Important: El telèfon mòbil ha d'estar connectat a la <strong>mateixa xarxa Wi-Fi</strong> que aquest ordinador.</span>
    </div>

    <div class="cards-grid">
      <!-- TARGETA 1: WEB PRINCIPAL -->
      <div class="card">
        <h2 class="card-title">1. Versió Web (Inici)</h2>
        <p class="card-desc">Portada pública d'informació, captació i registre</p>
        <div class="qr-wrapper">
          <img src="${qrDataUrlWeb}" alt="Codi QR Web">
        </div>
        <a href="${urlWeb}" target="_blank" class="url-badge">${urlWeb}</a>
      </div>

      <!-- TARGETA 2: APP PWA STANDALONE -->
      <div class="card">
        <h2 class="card-title">2. Versió App (PWA)</h2>
        <p class="card-desc">Panell operatiu directe de tasques i encàrrecs</p>
        <div class="qr-wrapper">
          <img src="${qrDataUrlApp}" alt="Codi QR App">
        </div>
        <a href="${urlApp}" target="_blank" class="url-badge">${urlApp}</a>
      </div>
    </div>

    <div class="instructions-box">
      <h3 class="instructions-title">📲 Com instal·lar AVISA al telèfon com a App Nativa</h3>
      <div class="steps-list">
        <div class="step-item">
          <div class="step-header">🍏 iPhone (iOS / Safari)</div>
          <p class="step-text">
            1. Obre la càmera i escaneja el codi QR.<br>
            2. Obre l'enllaç amb <strong>Safari</strong>.<br>
            3. Prem el botó <strong>Compartir</strong> (icona quadrada amb fletxa cap amunt).<br>
            4. Tria <strong>"Afegeix a la pantalla d'inici"</strong>.
          </p>
        </div>
        <div class="step-item">
          <div class="step-header">🤖 Android (Google Chrome)</div>
          <p class="step-text">
            1. Obre la càmera i escaneja el codi QR.<br>
            2. Obre l'enllaç amb <strong>Chrome</strong>.<br>
            3. Prem el botó de la barra superior o els <strong>3 punts</strong> de la dreta.<br>
            4. Tria <strong>"Instal·la l'aplicació"</strong> o <strong>"Afegeix a pantalla d'inici"</strong>.
          </p>
        </div>
      </div>
    </div>
  </div>
</body>
</html>`;

  fs.writeFileSync(htmlPath, htmlContent, 'utf-8');
  console.log(` S'ha generat la guia visual interactiva a: ${htmlPath}`);

  // Obrir la guia al navegador per facilitar l'escaneig amb el mòbil
  const urlToOpen = `http://localhost:${port}/mobil`;
  if (process.platform === 'win32') {
    exec(`cmd.exe /c start "" "${urlToOpen}"`, () => {
      process.exit(0);
    });
  } else if (process.platform === 'darwin') {
    exec(`open "${urlToOpen}"`, () => {
      process.exit(0);
    });
  } else {
    exec(`xdg-open "${urlToOpen}"`, () => {
      process.exit(0);
    });
  }
}

main().catch((err) => {
  console.error('Error executant mostrar_qr_mobil:', err);
  process.exit(1);
});

