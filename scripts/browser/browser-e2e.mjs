#!/usr/bin/env node
// Real Chrome UI checks over the Chrome DevTools Protocol; no browser package required.
import { spawn } from 'node:child_process';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { setTimeout as delay } from 'node:timers/promises';
import { resolve } from 'node:path';
import { randomUUID } from 'node:crypto';

const base = (process.env.CLAIMS_API_BASE || 'http://127.0.0.1:8080').replace(/\/$/, '');
const evidenceRoot = resolve(process.env.CLAIMS_EVIDENCE_DIR || 'reports/verification/REQ-017-browser');
const evidence = resolve(evidenceRoot, `run-${new Date().toISOString().replaceAll(':', '').replaceAll('.', '')}`);
const envFile = resolve(process.env.CLAIMS_ENV_FILE || '.env');
const env = Object.fromEntries((await readFile(envFile, 'utf8')).split(/\r?\n/).filter(x => x && !x.startsWith('#') && x.includes('='))
  .map(x => { const i = x.indexOf('='); return [x.slice(0, i).trim(), x.slice(i + 1).trim()]; }));
if (!env.CLAIMS_DEMO_PASSWORD || env.CLAIMS_DEMO_PASSWORD.startsWith('CHANGE_ME_')) throw new Error('Configured human password is unavailable.');
await mkdir(evidence, { recursive: true });
const browser = process.env.CHROME_BIN || '/usr/bin/google-chrome';
const port = Number(process.env.CLAIMS_CHROME_DEBUG_PORT || 9227);
const profile = `/tmp/claims-chrome-${process.pid}`;
const chrome = spawn(browser, ['--headless=new', '--no-first-run', '--no-default-browser-check', '--disable-background-networking',
  '--window-size=1440,1000', `--remote-debugging-port=${port}`, `--user-data-dir=${profile}`, 'about:blank'], { stdio: ['ignore', 'ignore', 'pipe'] });
let stderr = '';
chrome.stderr.on('data', chunk => { stderr += chunk.toString(); });
const network = [];
const consoleErrors = [];
const screenshots = [];
const results = [];
let socket;
let seq = 0;
const pending = new Map();
const waitFor = async (fn, label, timeout = 10000) => {
  const until = Date.now() + timeout;
  while (Date.now() < until) { if (await fn()) return; await delay(100); }
  throw new Error(`Timed out waiting for ${label}`);
};
try {
  await waitFor(async () => { try { return (await fetch(`http://127.0.0.1:${port}/json/version`)).ok; } catch { return false; } }, 'Chrome DevTools');
  const tabs = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json();
  const tab = tabs.find(x => x.type === 'page');
  if (!tab) throw new Error('Chrome did not expose a page target.');
  socket = new WebSocket(tab.webSocketDebuggerUrl);
  await new Promise((ok, bad) => { socket.addEventListener('open', ok, { once: true }); socket.addEventListener('error', bad, { once: true }); });
  socket.addEventListener('message', event => {
    const msg = JSON.parse(event.data);
    if (msg.id && pending.has(msg.id)) { const { resolve, reject } = pending.get(msg.id); pending.delete(msg.id); msg.error ? reject(new Error(msg.error.message)) : resolve(msg.result); }
    if (msg.method === 'Network.requestWillBeSent') {
      const u = new URL(msg.params.request.url);
      if (u.pathname.startsWith('/api/')) network.push({ method: msg.params.request.method, path: u.pathname + u.search, status: null });
    }
    if (msg.method === 'Network.responseReceived') {
      const u = new URL(msg.params.response.url);
      const item = [...network].reverse().find(x => x.path === u.pathname + u.search && x.status === null);
      if (item) item.status = msg.params.response.status;
    }
    if (msg.method === 'Runtime.exceptionThrown') consoleErrors.push(msg.params.exceptionDetails.text || 'JavaScript exception');
    if (msg.method === 'Runtime.consoleAPICalled' && msg.params.type === 'error') consoleErrors.push(msg.params.args.map(x => x.value || x.description || '').join(' '));
  });
  const cdp = (method, params = {}) => new Promise((resolve, reject) => {
    const id = ++seq; pending.set(id, { resolve, reject }); socket.send(JSON.stringify({ id, method, params }));
    setTimeout(() => { if (pending.has(id)) { pending.delete(id); reject(new Error(`CDP timeout: ${method}`)); } }, 15000);
  });
  await cdp('Page.enable'); await cdp('Runtime.enable'); await cdp('Network.enable');
  await cdp('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false });
  const evaluate = async (expression, awaitPromise = true) => {
    const result = await cdp('Runtime.evaluate', { expression, awaitPromise, returnByValue: true, userGesture: true });
    if (result.exceptionDetails) throw new Error(result.exceptionDetails.text || 'Browser evaluation failed');
    return result.result?.value;
  };
  const js = async expression => evaluate(`(async()=>(${expression}))()`);
  const snapshot = async name => {
    const shot = await cdp('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false,
      clip: { x: 0, y: 0, width: 1440, height: 1000, scale: 1 } });
    const path = resolve(evidence, `${String(screenshots.length + 1).padStart(2, '0')}-${name}.png`);
    await writeFile(path, Buffer.from(shot.data, 'base64')); screenshots.push(path);
  };
  const check = async (name, fn) => {
    const n = results.length + 1; process.stdout.write(`running test case ${n}/17: ${name}\n`);
    try { const detail = await fn(); results.push({ name, result: 'PASS', detail: detail ?? null }); }
    catch (error) { results.push({ name, result: 'FAIL', detail: error.message }); throw error; }
  };
  const login = async username => {
    await js(`(()=>{document.querySelector('#username').value=${JSON.stringify(username)}; document.querySelector('#password').value=${JSON.stringify(env.CLAIMS_DEMO_PASSWORD)}; document.querySelector('#login-form button[type=submit]').click(); return true;})()`);
    await waitFor(() => js(`document.querySelector('#workspace').classList.contains('hidden')===false`), `${username} login`);
    await waitFor(() => js(`document.querySelector('#notice').textContent.includes('Signed in as')`), `${username} session`);
  };
  const logout = async () => { await js(`document.querySelector('#logout').click()`); await delay(100); };
  const selectClaim = async id => {
    await waitFor(() => js(`!![...document.querySelectorAll('#claims button')].find(b=>b.textContent.includes(${JSON.stringify(id.slice(0, 8))}))`), `claim ${id} in list`);
    await js(`[...document.querySelectorAll('#claims button')].find(b=>b.textContent.includes(${JSON.stringify(id.slice(0, 8))})).click()`);
    await waitFor(() => js(`!document.querySelector('#claim-detail').classList.contains('hidden') && document.querySelector('#claim-title').textContent===${JSON.stringify(id)}`), 'claim details');
  };
  const setField = async (selector, value) => js(`(()=>{const e=document.querySelector(${JSON.stringify(selector)}); if(!e) throw new Error('missing '+${JSON.stringify(selector)}); e.value=${JSON.stringify(value)}; e.dispatchEvent(new Event('input',{bubbles:true})); e.dispatchEvent(new Event('change',{bubbles:true}));})()`);
  const submitFacts = async (formIndex, buttonText) => {
    await js(`(()=>{const f=document.querySelectorAll('#fact-editor form')[${formIndex}]; if(!f) throw new Error('missing facts form'); f.querySelector('button[type=submit]').click();})()`);
    await waitFor(() => js(`!document.querySelector('#notice').classList.contains('error') && document.querySelector('#notice').textContent.includes('saved')`), `${buttonText} facts save`);
  };
  const transition = async (to, reason) => {
    await waitFor(async () => {
      const ready = await js(`!![...document.querySelector('#transition-form select').options].find(o=>o.value===${JSON.stringify(to)} && !o.disabled)`);
      if (ready) return true;
      if (to === 'CLOSED') {
        await js(`(()=>{document.querySelector('#claims button[aria-current=true]')?.click();return true})()`);
        await delay(600);
      }
      return false;
    }, `available ${to} transition`, to === 'CLOSED' ? 35000 : 10000);
    await setField('#transition-form select', to);
    if (['REJECTED', 'PENDING_INFO'].includes(to) && !await evaluate("document.querySelector('#transition-form textarea').required")) {
      throw new Error(`${to} transition did not require its reason in the UI`);
    }
    await setField('#transition-form textarea', reason);
    await js(`document.querySelector('#transition-form button[type=submit]').click()`);
    await waitFor(() => js(`document.querySelector('#state-badge').textContent===${JSON.stringify(to)}`), `${to} state`);
  };
  await cdp('Page.navigate', { url: base });
  await waitFor(() => js(`!!document.querySelector('#login-form')`), 'claims sign-in page');
  await check('real Chrome opens the claims sign-in UI', async () => { await snapshot('sign-in'); return (await evaluate('document.title')); });
  await check('member-alpha signs in through the UI', async () => { await login('member-alpha'); await snapshot('member-alpha-workspace'); return await evaluate("document.querySelector('#notice').textContent"); });
  await check('create form rejects missing required amounts', async () => {
    const valid = await evaluate("document.querySelector('[name=policyLimit]').checkValidity() && document.querySelector('[name=requestedAmount]').checkValidity()");
    if (valid) throw new Error('empty required amounts passed native browser validation');
    await snapshot('required-amount-validation'); return { emptyAmountsRejected: true };
  });
  await check('member-alpha creates a claim from the UI', async () => {
    await setField('[name=policyLimit]', '500'); await setField('[name=requestedAmount]', '125'); await setField('[name=currency]', 'USD');
    await js(`document.querySelector('#create-form button[type=submit]').click()`);
    await waitFor(() => js(`document.querySelector('#state-badge').textContent==='SUBMITTED'`), 'created SUBMITTED claim');
    const id = await evaluate("document.querySelector('#claim-title').textContent");
    await snapshot('member-claim-created'); return { claimId: id };
  });
  const approvedId = await evaluate("document.querySelector('#claim-title').textContent");
  await logout();
  await check('clerk signs in and records the document roster', async () => {
    await login('clerk'); await selectClaim(approvedId);
    await setField('#fact-editor form input[type=text]', `invoice-${randomUUID().slice(0, 8)}`);
    await submitFacts(0, 'document'); await snapshot('documents-verified'); return await evaluate("document.querySelector('#notice').textContent");
  });
  await check('clerk transitions the claim to DOCUMENTS_VERIFIED', async () => { await transition('DOCUMENTS_VERIFIED', 'Browser E2E documents verified'); await snapshot('documents-verified-state'); return await evaluate("document.querySelector('#state-badge').textContent"); });
  await logout();
  await check('team lead assigns the assessor', async () => { await login('lead'); await selectClaim(approvedId); await submitFacts(0, 'assessor'); await snapshot('assessor-assigned'); return await evaluate("document.querySelector('#notice').textContent"); });
  await check('team lead starts assessment', async () => { await transition('UNDER_ASSESSMENT', 'Browser E2E assessment'); await snapshot('under-assessment'); return await evaluate("document.querySelector('#state-badge').textContent"); });
  await logout();
  await check('assessor saves the completed report', async () => { await login('assessor'); await selectClaim(approvedId); await submitFacts(0, 'assessment report'); await snapshot('assessment-complete'); return await evaluate("document.querySelector('#notice').textContent"); });
  await check('assessor approves the claim', async () => { await transition('APPROVED', 'Browser E2E approved'); await snapshot('approved'); return await evaluate("document.querySelector('#state-badge').textContent"); });
  await logout();
  await check('finance initiates payment', async () => { await login('finance'); await selectClaim(approvedId); await transition('PAYMENT_INITIATED', 'Browser E2E payment'); await snapshot('payment-initiated'); return await evaluate("document.querySelector('#state-badge').textContent"); });
  await check('outbox confirmation enables payment closure in the UI', async () => {
    await waitFor(async () => {
      const ready = await js(`!![...document.querySelector('#transition-form select').options].find(o=>o.value==='CLOSED' && !o.disabled)`);
      if (ready) return true;
      await js(`(()=>{document.querySelector('#claims button[aria-current=true]')?.click();return true})()`);
      await delay(600);
      return false;
    }, 'outbox payment confirmation', 35000);
    await transition('CLOSED', 'Browser E2E payment confirmed'); await snapshot('closed'); return await evaluate("document.querySelector('#state-badge').textContent");
  });
  await logout();
  await check('member-beta cannot see member-alpha claims in the UI', async () => {
    await login('member-beta'); await delay(300); const visible = await evaluate(`![...document.querySelectorAll('#claims button')].some(b=>b.textContent.includes(${JSON.stringify(approvedId.slice(0, 8))}))`);
    if (!visible) throw new Error('member-alpha claim appeared in member-beta list'); await snapshot('member-beta-isolated-list'); return { isolated: true };
  });
  await logout();
  await check('member-alpha creates a second claim for rejection flow', async () => {
    await login('member-alpha'); await setField('[name=policyLimit]', '300'); await setField('[name=requestedAmount]', '200'); await setField('[name=currency]', 'USD');
    await js(`document.querySelector('#create-form button[type=submit]').click()`);
    await waitFor(() => js(`document.querySelector('#state-badge').textContent==='SUBMITTED'`), 'second claim creation');
    const id = await evaluate("document.querySelector('#claim-title').textContent"); await snapshot('rejection-claim-created'); return { claimId: id };
  });
  const rejectedId = await evaluate("document.querySelector('#claim-title').textContent");
  await logout();
  await check('clerk verifies the second claim documents', async () => { await login('clerk'); await selectClaim(rejectedId); await setField('#fact-editor form input[type=text]', `invoice-${randomUUID().slice(0, 8)}`); await submitFacts(0, 'document'); await transition('DOCUMENTS_VERIFIED', 'Browser E2E rejection documents'); return await evaluate("document.querySelector('#state-badge').textContent"); });
  await logout();
  await check('team lead assigns and starts second assessment', async () => { await login('lead'); await selectClaim(rejectedId); await submitFacts(0, 'assessor'); await transition('UNDER_ASSESSMENT', 'Browser E2E rejection assessment'); return await evaluate("document.querySelector('#state-badge').textContent"); });
  await logout();
  await check('assessor rejects and member acknowledges through the UI', async () => {
    await login('assessor'); await selectClaim(rejectedId); await submitFacts(0, 'assessment report'); await transition('REJECTED', 'Browser E2E rejection reason');
    await logout(); await login('member-alpha'); await selectClaim(rejectedId);
    await waitFor(() => js(`!![...document.querySelectorAll('#fact-editor button')].find(b=>b.textContent.includes('Acknowledge rejection'))`), 'rejection acknowledgement control');
    await js(`[...document.querySelectorAll('#fact-editor button')].find(b=>b.textContent.includes('Acknowledge rejection')).click()`);
    await waitFor(() => js(`document.querySelector('#notice').textContent.includes('acknowledged')`), 'rejection acknowledgement');
    await snapshot('rejection-acknowledged'); return await evaluate("document.querySelector('#notice').textContent");
  });
  const failed = results.filter(x => x.result !== 'PASS');
  const report = { status: failed.length ? 'FAIL' : 'PASS', browser: 'Google Chrome', version: (await fetch(`http://127.0.0.1:${port}/json/version`).then(x=>x.json())).Browser,
    base, startedAtUtc: new Date().toISOString(), results, screenshots: screenshots.map(x=>x.replace(`${process.cwd()}/`,'')), network, consoleErrors,
    redactions: ['Credentials, cookies and CSRF tokens were not recorded; browser network evidence contains method/path/status only.'] };
  await writeFile(resolve(evidence, 'browser-e2e.json'), JSON.stringify(report, null, 2) + '\n');
  await writeFile(resolve(evidence, 'browser-e2e.log'), results.map((x,i)=>`running test case ${i+1}/${results.length}: ${x.name} — ${x.result}`).join('\n') + '\n');
  if (consoleErrors.length) throw new Error(`Browser console/runtime errors: ${consoleErrors.join('; ')}`);
  process.stdout.write(`Chrome ${report.version}; ${results.length}/${results.length} browser cases passed; ${network.length} API requests recorded.\n`);
} catch (error) {
  const report = { status: 'FAIL', startedAtUtc: new Date().toISOString(), error: error.message, results, network, consoleErrors, chromeStderr: stderr.slice(-3000), redactions: ['No credentials, cookies, CSRF tokens, request authorization headers, or request bodies were captured.'] };
  await writeFile(resolve(evidence, 'browser-e2e-failure.json'), JSON.stringify(report, null, 2) + '\n');
  throw error;
} finally {
  socket?.close(); chrome.kill('SIGTERM');
}
