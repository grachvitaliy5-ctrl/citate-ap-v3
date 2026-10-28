// Снимок содержимого WebView через DevTools: размеры, состояние сцены, ошибки, скриншот страницы.
const fs = require('fs');
(async () => {
  const out = process.argv[2] || 'results';
  let list;
  try { list = await (await fetch('http://127.0.0.1:9222/json/list')).json(); } catch (e) { console.log('no devtools', e.message); return; }
  fs.writeFileSync(out + '/devtools_list.json', JSON.stringify(list, null, 1));
  const pg = list.find(p => p.type === 'page') || list[0];
  if (!pg) { console.log('no page'); return; }
  const ws = new WebSocket(pg.webSocketDebuggerUrl);
  let id = 0; const wait = {};
  ws.onmessage = m => { const d = JSON.parse(m.data); if (d.id && wait[d.id]) { wait[d.id](d); delete wait[d.id]; } };
  const send = (method, params) => new Promise(r => { const i = ++id; wait[i] = r; ws.send(JSON.stringify({ id: i, method, params })); });
  await new Promise(r => ws.onopen = r);
  const ev = await send('Runtime.evaluate', { returnByValue: true, expression: `JSON.stringify({w:innerWidth,h:innerHeight,dpr:devicePixelRatio,hidden:document.hidden,vis:document.visibilityState,errs:window.__errs,state:window.__p&&window.__p.state&&window.__p.state(),canvas:(function(){var c=document.getElementById('c');return c?[c.width,c.height,c.style.width,c.style.height]:null})(),title:(document.getElementById('title')||{}).className,tap:(document.getElementById('tap')||{}).className,err:!(document.getElementById('err')||{}).hidden,fonts:document.fonts&&document.fonts.status})` });
  console.log('EVAL', JSON.stringify(ev.result));
  const sc = await send('Page.captureScreenshot', { format: 'png' });
  if (sc.result && sc.result.data) fs.writeFileSync(out + '/webview_cdp.png', Buffer.from(sc.result.data, 'base64'));
  else console.log('screenshot failed', JSON.stringify(sc));
  ws.close();
})();
