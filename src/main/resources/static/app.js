(() => {
  const $ = (id) => document.getElementById(id);
  const state = { auth: '', csrfHeader: '', csrfToken: '', claims: [], selected: null, options: [], audit: [], actor: '' };
  const API = '/api/v1';
  const commandInputGuards = new Set(['rejection_reason_present', 'information_description_present']);
  const notice = (message, error = false) => { $('notice').textContent = message; $('notice').classList.toggle('error', error); };
  const request = async (path, options = {}) => {
    const headers = new Headers(options.headers || {});
    headers.set('Authorization', state.auth);
    headers.set('X-Correlation-ID', crypto.randomUUID());
    if (options.mutate) {
      headers.set('Content-Type', 'application/json');
      headers.set('Idempotency-Key', crypto.randomUUID());
      if (state.csrfHeader && state.csrfToken) headers.set(state.csrfHeader, state.csrfToken);
    }
    const response = await fetch(`${API}${path}`, { ...options, headers, credentials: 'same-origin' });
    const type = response.headers.get('content-type') || '';
    const body = type.includes('json') ? await response.json() : null;
    if (!response.ok) throw new Error(body?.message || `Request failed (${response.status})`);
    return body;
  };
  const el = (tag, text, className) => { const node = document.createElement(tag); if (text !== undefined) node.textContent = text; if (className) node.className = className; return node; };
  const addMeta = (term, value) => { const box = document.createElement('div'); box.append(el('dt', term), el('dd', value)); $('claim-meta').append(box); };
  const showClaim = async (claim) => {
    state.selected = claim;
    const [detail, options, audit] = await Promise.all([
      request(`/claims/${encodeURIComponent(claim.id)}`),
      request(`/claims/${encodeURIComponent(claim.id)}/transitions`),
      request(`/claims/${encodeURIComponent(claim.id)}/audit`)
    ]);
    state.selected = detail; state.options = options.items || options; state.audit = audit.items || [];
    renderDetail(); renderList();
  };
  const renderList = () => {
    const list = $('claims'); list.replaceChildren();
    for (const claim of state.claims) {
      const item = document.createElement('li'); const button = document.createElement('button'); button.type = 'button';
      button.setAttribute('aria-current', state.selected?.id === claim.id ? 'true' : 'false');
      button.append(el('span', claim.id.slice(0, 8)), el('span', claim.state));
      button.addEventListener('click', () => withBusy(() => showClaim(claim)));
      item.append(button); list.append(item);
    }
  };
  const renderFacts = () => {
    const root = $('fact-editor'); root.replaceChildren();
    const role = state.actor.startsWith('member-') ? 'member' : ({ clerk: 'document_clerk', lead: 'team_lead', assessor: 'assessor' }[state.actor] || '');
    const addForm = (title, fields, submitText, onSubmit) => {
      const form = document.createElement('form'); form.className = 'fact-card form-stack'; form.append(el('h3', title));
      for (const field of fields) {
        const label = el('label', field.label); let input;
        if (field.type === 'checkbox') { input = document.createElement('input'); input.type = 'checkbox'; input.checked = field.value; label.className = 'check-row'; label.append(input); }
        else { input = document.createElement('input'); input.type = field.type || 'text'; input.value = field.value || ''; input.required = field.required !== false; input.step = field.step || ''; input.maxLength = 120; label.append(input); }
        field.input = input; form.append(label);
      }
      const button = el('button', submitText); button.type = 'submit'; form.append(button);
      form.addEventListener('submit', (event) => { event.preventDefault(); onSubmit(fields.map((field) => field.input)); }); root.append(form);
    };
    if (role === 'document_clerk' && ['SUBMITTED', 'PENDING_INFO'].includes(state.selected.state)) {
      addForm('Document roster', [
        { label: 'Stable document ID', value: '' },
        { label: 'Required', type: 'checkbox', value: true },
        { label: 'Present', type: 'checkbox', value: true },
        { label: 'Valid', type: 'checkbox', value: true }
      ], 'Save document', ([id, required, present, valid]) => withBusy(() => updateFacts({ documents: { [id.value]: { required: required.checked, present: present.checked, valid: valid.checked } } })));
    }
    if (role === 'member' && state.selected.state === 'PENDING_INFO') {
      const docs = state.selected.facts?.documents || {}; const ids = Object.keys(docs);
      if (ids.length) addForm('Mark a requested document present', [
        { label: 'Document ID', value: ids[0] }, { label: 'Present', type: 'checkbox', value: true }
      ], 'Send information', ([id, present]) => withBusy(() => updateFacts({ documents: { [id.value]: { present: present.checked } } })));
    }
    if (role === 'team_lead' && ['SUBMITTED', 'DOCUMENTS_VERIFIED'].includes(state.selected.state)) {
      addForm('Assign assessor', [{ label: 'Assessor ID', value: 'assessor' }], 'Assign', ([id]) => withBusy(() => updateFacts({ assessorId: id.value })));
    }
    if (role === 'assessor' && state.selected.state === 'UNDER_ASSESSMENT') {
      addForm('Assessment report', [{ label: 'Complete', type: 'checkbox', value: true }], 'Save report', ([complete]) => withBusy(() => updateFacts({ assessmentReport: { complete: complete.checked } })));
    }
    if (role === 'member' && state.selected.state === 'REJECTED') {
      const button = el('button', 'Acknowledge rejection'); button.type = 'button';
      button.addEventListener('click', () => withBusy(async () => {
        state.selected = await request(`/claims/${state.selected.id}/rejection-acknowledgement`, {
          method: 'POST', mutate: true, body: JSON.stringify({ expectedVersion: state.selected.version })
        }); await refreshSelected(); notice('Rejection acknowledged.');
      })); root.append(button);
    }
  };
  const updateFacts = async (facts) => {
    state.selected = await request(`/claims/${state.selected.id}/facts`, {
      method: 'PUT', mutate: true, body: JSON.stringify({ expectedVersion: state.selected.version, facts })
    }); await refreshSelected(); notice('Claim information saved.');
  };
  const renderDetail = () => {
    const claim = state.selected; $('empty-state').classList.add('hidden'); $('claim-detail').classList.remove('hidden');
    $('claim-title').textContent = claim.id; $('state-badge').textContent = claim.state;
    const meta = $('claim-meta'); meta.replaceChildren(); addMeta('Policy limit', `${claim.policyLimit} ${claim.currency}`);
    addMeta('Workflow version', String(claim.version)); addMeta('Created', new Date(claim.createdAt).toLocaleString());
    const optionList = $('options'); optionList.replaceChildren(); const select = $('transition-form').elements.targetState; select.replaceChildren();
    const placeholder = el('option', 'Choose a configured transition'); placeholder.value = ''; placeholder.selected = true; select.append(placeholder);
    for (const option of state.options) {
      const item = document.createElement('li'); item.className = 'option';
      item.append(el('strong', `${option.from} → ${option.to}`), el('small', `${option.transitionId} · role: ${option.authorizedRoles.join(', ')}`));
      const commandInputOnly = option.failures?.length > 0 && option.failures.every(failure => commandInputGuards.has(failure));
      const canSelect = option.available || commandInputOnly;
      const status = el('small', option.available ? 'Ready' : commandInputOnly ? 'Enter a reason to continue' : option.failures.join(', '), canSelect ? '' : 'failure'); item.append(status); optionList.append(item);
      const choice = el('option', `${option.to} — ${option.transitionId}`); choice.value = option.to; choice.disabled = !canSelect; select.append(choice);
    }
    const audit = $('audit'); audit.replaceChildren();
    for (const entry of state.audit) {
      const item = document.createElement('li'); const title = el('strong', `${entry.fromState || 'Created'} → ${entry.toState}`);
      const detail = el('p', `${entry.actorId} (${entry.role}) · ${entry.reason || 'No note'}`);
      const time = document.createElement('time'); time.dateTime = entry.timestamp; time.textContent = new Date(entry.timestamp).toLocaleString();
      item.append(title, detail, time); audit.append(item);
    }
    renderFacts();
  };
  $('transition-form').elements.targetState.addEventListener('change', (event) => {
    const requiresReason = ['REJECTED', 'PENDING_INFO'].includes(event.currentTarget.value);
    const reason = $('transition-form').elements.reason;
    reason.required = requiresReason;
    reason.placeholder = requiresReason ? 'A reason is required for this transition.' : '';
  });
  const refreshSelected = async () => {
    const refreshed = await request(`/claims/${state.selected.id}`); state.selected = refreshed;
    state.claims = await request('/claims?page=0&size=20').then((r) => r.items); await showClaim(refreshed);
  };
  const loadClaims = async () => { const page = await request('/claims?page=0&size=20'); state.claims = page.items; renderList(); };
  const withBusy = async (work) => { try { notice(''); await work(); } catch (error) { notice(error.message || 'The request failed.', true); } };
  $('login-form').addEventListener('submit', (event) => {
    event.preventDefault(); withBusy(async () => {
      const username = $('username').value.trim(); const password = $('password').value;
      state.auth = `Basic ${btoa(`${username}:${password}`)}`; state.actor = username; $('password').value = '';
      const session = await request('/session'); state.csrfHeader = session.csrfHeaderName || ''; state.csrfToken = session.csrfToken || '';
      $('workspace').classList.remove('hidden'); $('logout').classList.remove('hidden'); await loadClaims(); notice(`Signed in as ${username}.`);
    });
  });
  $('logout').addEventListener('click', () => { state.auth = ''; state.csrfHeader = ''; state.csrfToken = ''; state.actor = ''; state.claims = []; state.selected = null; $('workspace').classList.add('hidden'); $('logout').classList.add('hidden'); $('claims').replaceChildren(); $('claim-detail').classList.add('hidden'); $('empty-state').classList.remove('hidden'); notice('Signed out.'); });
  $('refresh').addEventListener('click', () => withBusy(loadClaims));
  $('create-form').addEventListener('submit', (event) => { event.preventDefault(); withBusy(async () => {
    const form = new FormData(event.currentTarget); const policyLimit = String(form.get('policyLimit')).trim(); const requestedAmount = String(form.get('requestedAmount')).trim();
    const decimal = /^(?:0|[1-9][0-9]{0,15})(?:\.[0-9]{1,2})?$/;
    if (!decimal.test(policyLimit) || !decimal.test(requestedAmount)) throw new Error('Amounts must be nonnegative decimals with at most 16 integer and 2 fractional digits.');
    const currency = String(form.get('currency')).toUpperCase();
    const facts = `{"requestedAmount":${requestedAmount}}`;
    const body = `{"policyLimit":${policyLimit},"currency":${JSON.stringify(currency)},"facts":${facts}}`;
    const claim = await request('/claims', { method: 'POST', mutate: true, body });
    state.selected = claim; await loadClaims(); await showClaim(claim); notice('Claim created.');
  }); });
  $('transition-form').addEventListener('submit', (event) => { event.preventDefault(); withBusy(async () => {
    const form = new FormData(event.currentTarget); const role = state.actor === 'system';
    const route = role ? `/claims/${state.selected.id}/system-transitions` : `/claims/${state.selected.id}/transitions`;
    await request(route, { method: 'POST', mutate: true, body: JSON.stringify({ targetState: form.get('targetState'), expectedVersion: state.selected.version, reason: form.get('reason'), facts: {} }) });
    await refreshSelected(); notice('Transition recorded.');
  }); });
})();
