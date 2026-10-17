(() => {
  const $ = (selector, root = document) => root.querySelector(selector);
  const els = { login: $('#login-panel'), loginForm: $('#login-form'), workspace: $('#workspace'), account: $('#account'), list: $('#claim-list'), empty: $('#claims-empty'), detail: $('#claim-detail'), notice: $('#notice'), create: $('#create-panel'), createForm: $('#create-form') };
  const state = { credentials: null, session: null, claims: [], selected: null, commandKeys: new Map(), busy: false };
  const knownHumanRoles = ['document_clerk', 'team_lead', 'assessor', 'finance', 'member'];

  function notify(message, error = false) {
    els.notice.textContent = message;
    els.notice.classList.toggle('error', error);
    els.notice.hidden = false;
    window.clearTimeout(notify.timer);
    notify.timer = window.setTimeout(() => { els.notice.hidden = true; }, 6500);
  }
  function basicHeader() { return `Basic ${state.credentials}`; }
  async function request(path, options = {}) {
    const headers = new Headers(options.headers || {});
    headers.set('Authorization', basicHeader());
    if (options.body !== undefined) headers.set('Content-Type', 'application/json');
    if (options.mutate && state.session) headers.set(state.session.csrfHeader, state.session.csrfToken);
    const response = await fetch(path, { ...options, headers, credentials: 'same-origin' });
    if (response.status === 401) throw new Error('Sign-in expired. Please sign in again.');
    const contentType = response.headers.get('content-type') || '';
    const body = contentType.includes('application/json') ? await response.json() : null;
    if (!response.ok) {
      const suffix = body && body.traceId ? ` (trace ${body.traceId})` : '';
      throw new Error((body && body.message ? body.message : `Request failed with status ${response.status}`) + suffix);
    }
    return body;
  }
  async function signedIn(username, password) {
    state.credentials = btoa(`${username}:${password}`);
    try {
      state.session = await request('/api/session');
      $('#password').value = '';
      els.login.hidden = true; els.workspace.hidden = false;
      els.account.hidden = false;
      $('#account-name').textContent = `${state.session.userId} · ${state.session.role.replaceAll('_', ' ')}`;
      await loadClaims();
    } catch (error) {
      state.credentials = null; state.session = null; $('#password').value = '';
      notify(error.message, true);
    }
  }
  function signOut() {
    state.credentials = null; state.session = null; state.claims = []; state.selected = null; state.commandKeys.clear();
    els.workspace.hidden = true; els.login.hidden = false; els.account.hidden = true; els.loginForm.reset(); els.list.replaceChildren(); els.detail.replaceChildren();
    $('#username').focus();
  }
  function element(tag, text, className) {
    const node = document.createElement(tag);
    if (text !== undefined && text !== null) node.textContent = String(text);
    if (className) node.className = className;
    return node;
  }
  function renderClaims() {
    els.list.replaceChildren();
    els.empty.hidden = state.claims.length > 0;
    for (const claim of state.claims) {
      const button = element('button', undefined, 'claim-item'); button.type = 'button';
      button.setAttribute('aria-current', String(state.selected?.id === claim.id));
      button.append(element('span', claim.id, 'claim-id'), element('span', claim.state, 'claim-state'), element('span', `${claim.currency} ${claim.policyLimit} · v${claim.version}`, 'claim-meta'));
      button.addEventListener('click', () => selectClaim(claim.id));
      els.list.append(button);
    }
  }
  async function loadClaims() {
    els.list.replaceChildren(element('p', 'Loading claims…', 'loading'));
    try {
      state.claims = await request('/api/claims');
      renderClaims();
      if (state.selected && state.claims.some(item => item.id === state.selected.id)) await selectClaim(state.selected.id, false);
      else if (state.claims.length) await selectClaim(state.claims[0].id, false);
      else renderEmptyDetail();
    } catch (error) { notify(error.message, true); }
  }
  function renderEmptyDetail() {
    const wrap = element('div', undefined, 'empty-state'); wrap.append(element('span', '↗', 'empty-symbol'), element('h2', 'Choose a claim'), element('p', 'Claim status, workflow actions, and audit history will appear here.'));
    els.detail.replaceChildren(wrap);
  }
  async function selectClaim(id, reload = true) {
    els.detail.setAttribute('aria-busy', 'true'); els.detail.replaceChildren(element('p', 'Loading claim…', 'loading'));
    try {
      const [claim, options, audit] = await Promise.all([request(`/api/claims/${encodeURIComponent(id)}`), request(`/api/claims/${encodeURIComponent(id)}/transitions`), request(`/api/claims/${encodeURIComponent(id)}/audit`)]);
      state.selected = claim;
      renderClaims(); renderDetail(claim, options, audit);
    } catch (error) { notify(error.message, true); }
    finally { els.detail.removeAttribute('aria-busy'); }
  }
  function metric(label, value) { const item = element('div', undefined, 'metric'); item.append(element('span', label), element('strong', value ?? '—')); return item; }
  function section(title) { const part = element('section', undefined, 'subsection'); part.append(element('h3', title)); return part; }
  function renderDetail(claim, options, audit) {
    const fragment = document.createDocumentFragment();
    const header = element('div', undefined, 'claim-title');
    const titleBlock = element('div'); titleBlock.append(element('div', 'Claim details', 'eyebrow'), element('h2', `Claim ${claim.id.slice(0, 8)}`), element('span', `Member ${claim.memberId}`, 'claim-meta'));
    header.append(titleBlock, element('span', claim.state, 'pill')); fragment.append(header);
    const facts = element('div', undefined, 'facts-grid');
    facts.append(metric('Current state', claim.state), metric('Version', claim.version), metric('Policy limit', `${claim.currency} ${claim.policyLimit}`), metric('Information requests', claim.informationRequests), metric('Assessment started', claim.assessmentStartedAt || 'Not started'), metric('Last request', claim.lastInformationRequestedAt || '—'));
    fragment.append(facts);
    fragment.append(buildFactSection(claim));
    const available = section('Configured actions');
    if (!options.length) available.append(element('p', 'No transitions are configured for this state.', 'empty'));
    const cards = element('div', undefined, 'transition-list');
    for (const option of options) {
      const card = element('article', undefined, 'transition-card');
      card.append(element('strong', option.target), element('p', `Required role: ${option.role.replaceAll('_', ' ')}`));
      if (option.blockers.length) card.append(element('p', option.blockers.join(' · ')));
      const button = element('button', option.allowed ? `Move to ${option.target}` : 'Unavailable', 'button primary'); button.type = 'button'; button.disabled = !option.allowed || state.busy;
      button.addEventListener('click', () => executeTransition(option.target)); card.append(button); cards.append(card);
    }
    available.append(cards); fragment.append(available);
    const history = section('Audit timeline');
    if (!audit.length) history.append(element('p', 'No transitions have been recorded yet.', 'empty'));
    const timeline = element('div', undefined, 'audit-list');
    for (const entry of [...audit].reverse()) {
      const row = element('div', undefined, 'audit-row'); const time = element('time', new Date(entry.timestamp).toLocaleString(), 'audit-time'); time.dateTime = entry.timestamp;
      const detail = element('div', undefined, 'audit-detail'); detail.append(element('strong', `${entry.fromState} → ${entry.toState}`), element('span', `${entry.userId} · ${entry.role.replaceAll('_', ' ')} · ${entry.configVersion}`));
      if (entry.notes) detail.append(element('span', entry.notes));
      row.append(time, detail); timeline.append(row);
    }
    history.append(timeline); fragment.append(history);
    els.detail.replaceChildren(fragment);
  }
  function inputFor(name, value) {
    const input = document.createElement('input'); input.className = 'fact-control'; input.name = name;
    if (['reportComplete', 'paymentConfirmed', 'memberAcknowledged'].includes(name)) { input.type = 'checkbox'; input.checked = value === true; }
    else if (name === 'assessmentAmount') { input.type = 'number'; input.step = '0.01'; input.min = '0'; input.value = value ?? ''; }
    else { input.type = 'text'; input.maxLength = name === 'assessorId' ? 120 : 2000; input.value = value ?? ''; }
    return input;
  }
  function buildFactSection(claim) {
    const area = section('Role-specific information');
    const role = state.session.role; const form = element('form', undefined, 'fact-form form-grid');
    const add = (label, name, value, type) => {
      const wrapper = element('label', label); let control;
      if (type === 'documents') {
        const current = claim.facts.documents || {};
        for (const doc of claim.facts.requiredDocuments || []) {
          const documentLabel = element('label', doc); const select = document.createElement('select'); select.name = `document:${doc}`; select.className = 'fact-control';
          for (const [text, val] of [['Needs review', 'false'], ['Accepted', 'true']]) { const option = document.createElement('option'); option.textContent = text; option.value = val; select.append(option); }
          select.value = current[doc] === true ? 'true' : 'false'; documentLabel.append(select); form.append(documentLabel);
        }
        return;
      } else if (type === 'select') {
        control = document.createElement('select'); control.name = name; control.className = 'fact-control';
        for (const [text, val] of [['Not complete','false'],['Complete','true']]) { const option = document.createElement('option'); option.textContent = text; option.value = val; control.append(option); }
        control.value = value === true ? 'true' : 'false';
      } else control = inputFor(name, value);
      wrapper.append(control); form.append(wrapper);
    };
    let fields = [];
    if (role === 'document_clerk' && ['SUBMITTED','PENDING_INFO'].includes(claim.state)) fields = [['Documents', 'documents', claim.facts.documents, 'documents'], ['New information', 'newInformation', '', 'text']];
    if (role === 'team_lead' && claim.state === 'DOCUMENTS_VERIFIED') fields = [['Assessor ID', 'assessorId', claim.facts.assessorId, 'text']];
    if (role === 'assessor' && claim.state === 'UNDER_ASSESSMENT') fields = [['Report status', 'reportComplete', claim.facts.reportComplete, 'select'], ['Assessment amount', 'assessmentAmount', claim.facts.assessmentAmount, 'text'], ['Assessment currency', 'assessmentCurrency', claim.facts.assessmentCurrency || claim.currency, 'text'], ['Rejection reason', 'rejectionReason', claim.facts.rejectionReason, 'text'], ['Missing information', 'missingInfoDescription', claim.facts.missingInfoDescription, 'text']];
    if (role === 'finance' && claim.state === 'PAYMENT_INITIATED') fields = [['Payment confirmed', 'paymentConfirmed', claim.facts.paymentConfirmed, 'select'], ['Payment reference', 'paymentReference', claim.facts.paymentReference, 'text']];
    if (role === 'member' && claim.state === 'REJECTED' && claim.memberId === state.session.userId) fields = [['Acknowledgment', 'memberAcknowledged', claim.facts.memberAcknowledged, 'select']];
    for (const [label, name, value, type] of fields) add(label, name, value, type);
    if (!fields.length) { area.append(element('p', 'No editable facts for this role in the current state.', 'empty')); return area; }
    const version = document.createElement('input'); version.type = 'hidden'; version.name = 'expectedVersion'; version.value = claim.version; form.append(version);
    const actions = element('div', undefined, 'form-actions'); const save = element('button', 'Save information', 'button secondary'); save.type = 'submit'; actions.append(save); form.append(actions);
    form.addEventListener('submit', event => { event.preventDefault(); saveFacts(form); });
    area.append(form); return area;
  }
  async function saveFacts(form) {
    const formData = new FormData(form); const updates = {};
    for (const [key, raw] of formData.entries()) {
      if (key === 'expectedVersion') continue;
      if (key.startsWith('document:')) { updates.documents ||= {}; updates.documents[key.slice(9)] = raw === 'true'; continue; }
      if (['reportComplete','paymentConfirmed','memberAcknowledged'].includes(key)) updates[key] = raw === 'true';
      else if (key === 'assessmentAmount') updates[key] = Number(raw);
      else updates[key] = raw;
    }
    if (!Object.keys(updates).length) return;
    await withBusy(async () => { await request(`/api/claims/${state.selected.id}/facts`, { method: 'PATCH', mutate: true, body: JSON.stringify({ expectedVersion: Number(formData.get('expectedVersion')), updates }) }); notify('Information saved.'); await loadClaims(); });
  }
  async function executeTransition(target) {
    const selected = state.selected; if (!selected) return;
    const keyId = `${selected.id}:${selected.version}:${target}`;
    let idempotencyKey = state.commandKeys.get(keyId);
    if (!idempotencyKey) { idempotencyKey = crypto.randomUUID(); state.commandKeys.set(keyId, idempotencyKey); }
    await withBusy(async () => {
      const path = state.session.role === 'system' ? `/api/system/claims/${selected.id}/transitions` : `/api/claims/${selected.id}/transitions`;
      await request(path, { method: 'POST', mutate: true, body: JSON.stringify({ target, expectedVersion: selected.version, idempotencyKey, notes: '' }) });
      state.commandKeys.delete(keyId); notify(`Claim moved to ${target}.`); await loadClaims();
    });
  }
  async function withBusy(action) {
    if (state.busy) return;
    state.busy = true;
    try { await action(); } catch (error) { notify(error.message, true); }
    finally { state.busy = false; if (state.selected) await selectClaim(state.selected.id); }
  }
  function showCreate(show) { els.create.hidden = !show; if (show) $('input[name="memberId"]', els.create).focus(); }
  els.loginForm.addEventListener('submit', event => { event.preventDefault(); const data = new FormData(els.loginForm); signedIn(String(data.get('username')).trim(), String(data.get('password'))); });
  $('#new-claim-toggle').addEventListener('click', () => showCreate(els.create.hidden));
  $('#cancel-create').addEventListener('click', () => showCreate(false));
  $('#refresh').addEventListener('click', loadClaims);
  els.createForm.addEventListener('submit', async event => {
    event.preventDefault(); const form = new FormData(els.createForm);
    const payload = { memberId: String(form.get('memberId')).trim(), policyLimit: Number(form.get('policyLimit')), currency: String(form.get('currency')).trim().toUpperCase(), requiredDocuments: String(form.get('requiredDocuments')).split(',').map(item => item.trim()).filter(Boolean) };
    await withBusy(async () => { const claim = await request('/api/claims', { method: 'POST', mutate: true, body: JSON.stringify(payload) }); state.selected = claim; showCreate(false); els.createForm.reset(); notify('Claim created.'); await loadClaims(); });
  });
  window.addEventListener('pagehide', () => { state.credentials = null; state.session = null; });
  // The sign-out control is created only after login and contains no credential data.
  const account = els.account; const accountName = element('span', undefined); accountName.id = 'account-name'; account.append(accountName); const signOutButton = element('button', 'Sign out', 'button secondary'); signOutButton.type = 'button'; signOutButton.addEventListener('click', signOutHandler);
  function signOutHandler() { signOut(); }
  // Keep the account action accessible while rendering the principal as plain text.
  account.append(signOutButton);
})();
