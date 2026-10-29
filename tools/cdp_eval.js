// Выполнить выражение в сцене через DevTools: node cdp_eval.js "<js>"
(async () => {
  const list = await (await fetch('http://127.0.0.1:9222/json/list')).json();
  const pg = list.find(p => p.type === 'page') || list[0];
  const ws = new WebSocket(pg.webSocketDebuggerUrl);
  await new Promise(r => ws.onopen = r);
  ws.onmessage = m => { console.log(String(m.data).slice(0, 2000)); ws.close(); };
  ws.send(JSON.stringify({ id: 1, method: 'Runtime.evaluate', params: { expression: process.argv[2], returnByValue: true } }));
})();
