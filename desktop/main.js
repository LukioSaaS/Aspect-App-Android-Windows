// Aspect para Windows (Electron)
const { app, BrowserWindow, Menu, session, shell } = require("electron");
const path = require("path");

// Decodificação de HEVC (quando o Windows/placa de vídeo suportar), vídeo sem exigir clique e rede local liberada
app.commandLine.appendSwitch("enable-features", "PlatformHEVCDecoderSupport");
app.commandLine.appendSwitch("disable-features", "BlockInsecurePrivateNetworkRequests,PrivateNetworkAccessSendPreflights");
app.commandLine.appendSwitch("autoplay-policy", "no-user-gesture-required");
// Mostra as faixas de áudio dos arquivos MKV (a troca de faixa fica disponível no menu "Áudio e legendas")
app.commandLine.appendSwitch("enable-blink-features", "AudioVideoTracks");

if (!app.requestSingleInstanceLock()) {
  app.quit();
}

let win = null;

function createWindow() {
  Menu.setApplicationMenu(null);
  win = new BrowserWindow({
    width: 1280,
    height: 780,
    minWidth: 900,
    minHeight: 560,
    backgroundColor: "#050505",
    title: "Aspect",
    icon: path.join(__dirname, "build", "icon.png"),
    autoHideMenuBar: true,
    show: false,
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      backgroundThrottling: false,
    },
  });
  win.once("ready-to-show", () => win.show());
  win.loadFile(path.join(__dirname, "app", "index.html"));

  // links externos abrem no navegador, nunca dentro do app
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https?:/i.test(url)) shell.openExternal(url);
    return { action: "deny" };
  });
  win.webContents.on("will-navigate", (e, url) => {
    if (!url.startsWith("file://")) {
      e.preventDefault();
      if (/^https?:/i.test(url)) shell.openExternal(url);
    }
  });
  win.on("closed", () => {
    win = null;
  });
}

app.on("second-instance", () => {
  if (win) {
    if (win.isMinimized()) win.restore();
    win.focus();
  }
});

app.whenReady().then(() => {
  // Servidores IPTV normalmente não enviam CORS: o app aceita as respostas deles
  session.defaultSession.webRequest.onHeadersReceived((details, callback) => {
    const h = details.responseHeaders || {};
    Object.keys(h).forEach((k) => {
      if (k.toLowerCase().indexOf("access-control-allow-") === 0) delete h[k];
    });
    h["Access-Control-Allow-Origin"] = ["*"];
    h["Access-Control-Allow-Headers"] = ["*"];
    h["Access-Control-Allow-Methods"] = ["GET, HEAD, OPTIONS"];
    callback({ responseHeaders: h });
  });
  createWindow();
  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on("window-all-closed", () => {
  app.quit();
});
