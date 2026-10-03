/*
Alexander Baldree
Max Jankowski
Aftabur Rahman
Jordan Dardar

Moffat Bay Marina Project
The Green Team
CSD460
*/

(function () {
  const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  function el(id) { return document.getElementById(id); }

  function setSessionUI() {
    const pill = el('sessionPill');
    if (!pill) return;
    const active = sessionStorage.getItem('moffatPrototypeSession') === 'active';
    const name = sessionStorage.getItem('moffatPrototypeName') || '';
    const firstName = name.trim().split(/\s+/)[0] || '';
    pill.textContent = active ? (firstName ? `Welcome, ${firstName}` : 'Prototype session: signed in') : 'Prototype session: signed out';
  }

  function showError(input, message) {
    if (input) input.classList.add('invalid');
    const error = input ? el(`${input.id}Error`) : null;
    if (error) error.textContent = message;
  }

  function clearError(input) {
    if (input) input.classList.remove('invalid');
    const error = input ? el(`${input.id}Error`) : null;
    if (error) error.textContent = '';
  }

  function passwordMeetsProjectRules(value) {
    return value.length >= 8 && /[A-Z]/.test(value) && /[a-z]/.test(value);
  }

  function updatePasswordRules() {
    const input = el('password');
    if (!input) return;
    const value = input.value;
    const lengthRule = el('ruleLength');
    const upperRule = el('ruleUpper');
    const lowerRule = el('ruleLower');
    if (lengthRule) lengthRule.classList.toggle('met', value.length >= 8);
    if (upperRule) upperRule.classList.toggle('met', /[A-Z]/.test(value));
    if (lowerRule) lowerRule.classList.toggle('met', /[a-z]/.test(value));
  }

  function setupRegistration() {
    const form = el('registrationForm');
    if (!form) return;

    const email = el('email');
    const password = el('password');
    const confirmPassword = el('confirmPassword');
    password.addEventListener('input', updatePasswordRules);

    form.addEventListener('submit', (event) => {
      event.preventDefault();
      let valid = true;
      form.querySelectorAll('input').forEach(clearError);

      const required = ['firstName', 'lastName', 'email', 'telephone', 'boatName', 'boatLength', 'password', 'confirmPassword'];
      required.forEach((id) => {
        const input = el(id);
        if (!input.value.trim()) {
          showError(input, 'This field is required for the prototype flow.');
          valid = false;
        }
      });

      if (email.value && !EMAIL_RE.test(email.value.trim())) {
        showError(email, 'Enter a standard email format, such as name@example.com.');
        valid = false;
      }

      const boatLength = Number(el('boatLength').value);
      if (el('boatLength').value && (!Number.isFinite(boatLength) || boatLength <= 0)) {
        showError(el('boatLength'), 'Enter a boat length greater than 0 feet.');
        valid = false;
      }

      if (password.value && !passwordMeetsProjectRules(password.value)) {
        showError(password, 'Password must be at least 8 characters and include an uppercase and lowercase letter.');
        valid = false;
      }

      if (confirmPassword.value && confirmPassword.value !== password.value) {
        showError(confirmPassword, 'Passwords do not match.');
        valid = false;
      }

      if (!valid) return;

      sessionStorage.setItem('moffatPrototypeEmail', email.value.trim());
      sessionStorage.setItem('moffatPrototypeName', `${el('firstName').value.trim()} ${el('lastName').value.trim()}`);
      const customerId = `MB-${Math.floor(10000 + Math.random() * 90000)}`;
      sessionStorage.setItem('moffatPrototypeCustomerId', customerId);
      window.location.href = 'verification.html';
    });
  }

  function setupVerification() {
    const emailText = el('verificationEmail');
    const customerIdText = el('customerId');
    const verifyBtn = el('verifyBtn');
    const notice = el('verificationNotice');

    if (!verifyBtn) return;

    const storedEmail = sessionStorage.getItem('moffatPrototypeEmail');
    const storedCustomerId = sessionStorage.getItem('moffatPrototypeCustomerId');
    if (emailText) emailText.textContent = storedEmail || 'Email address from your registration';
    if (customerIdText) customerIdText.textContent = storedCustomerId || 'Account pending verification';

    const token = new URLSearchParams(window.location.search).get('token');

    async function verifyToken() {
      if (!token) {
        if (notice) {
          notice.textContent = 'No verification token was found in this link. Please use the verification link from your email.';
        }
        return;
      }

      verifyBtn.disabled = true;
      verifyBtn.textContent = 'Verifying...';
      if (notice) notice.textContent = 'Checking your verification token with the server...';

      try {
        const response = await fetch('verification', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          },
          body: JSON.stringify({ token: token })
        });

        const body = await response.json();

        if (!response.ok || !body.ok) {
          throw new Error(body.message || 'Email verification could not be completed.');
        }

        sessionStorage.setItem('moffatPrototypeVerified', 'true');
        el('verificationPending').classList.add('hidden');
        el('verificationComplete').classList.remove('hidden');
      } catch (error) {
        if (notice) {
          notice.textContent = error.message || 'Email verification could not be completed.';
        }
        verifyBtn.disabled = false;
        verifyBtn.textContent = 'Try Verification Again';
      }
    }

    verifyBtn.addEventListener('click', verifyToken);

    // Opening the link from EmailService automatically verifies the token.
    if (token) verifyToken();
  }

  function setupLogin() {
    const form = el('loginForm');
    if (!form) return;
    const email = el('loginEmail');
    const password = el('loginPassword');
    const storedEmail = sessionStorage.getItem('moffatPrototypeEmail');
    if (storedEmail) email.value = storedEmail;

    form.addEventListener('submit', (event) => {
      event.preventDefault();
      clearError(email);
      clearError(password);
      let valid = true;
      if (!EMAIL_RE.test(email.value.trim())) {
        showError(email, 'Enter a valid email address.');
        valid = false;
      }
      if (password.value.length < 8) {
        showError(password, 'Enter a password with at least 8 characters for this prototype demonstration.');
        valid = false;
      }
      if (!valid) return;

      sessionStorage.setItem('moffatPrototypeEmail', email.value.trim());
      sessionStorage.setItem('moffatPrototypeSession', 'active');
      setSessionUI();
      const welcomeName = el('welcomeName');
      if (welcomeName) {
        const storedName = sessionStorage.getItem('moffatPrototypeName') || '';
        welcomeName.textContent = storedName.trim().split(/\s+/)[0] || 'Customer';
      }
      el('loginCard').classList.add('hidden');
      el('loginSuccess').classList.remove('hidden');
    });

    const logoutBtn = el('logoutBtn');
    if (logoutBtn) logoutBtn.addEventListener('click', () => {
      sessionStorage.removeItem('moffatPrototypeSession');
      setSessionUI();
      el('loginSuccess').classList.add('hidden');
      el('loginCard').classList.remove('hidden');
      password.value = '';
    });
  }

  function setupWaitlist() {
    const select = el('slipCategory');
    if (!select) return;

    const defaults = { '26': 2, '40': 4, '50': 1 };
    let counts;
    try {
      counts = JSON.parse(sessionStorage.getItem('moffatWaitlistCounts')) || defaults;
    } catch (_) { counts = defaults; }

    function renderCounts() {
      ['26', '40', '50'].forEach((size) => {
        const countEl = el(`count${size}`);
        if (countEl) countEl.textContent = counts[size];
      });
      const size = select.value;
      el('aheadCount').textContent = counts[size];
      el('positionNumber').textContent = `#${counts[size] + 1}`;
      el('selectedCategoryText').textContent = `${size} ft`;
    }

    select.addEventListener('change', () => {
      el('joinConfirmation').classList.add('hidden');
      el('joinPrompt').classList.remove('hidden');
      renderCounts();
    });

    const joinBtn = el('joinWaitlistBtn');
    joinBtn.addEventListener('click', () => {
      const active = sessionStorage.getItem('moffatPrototypeSession') === 'active';
      if (!active) {
        el('loginRequired').classList.remove('hidden');
        return;
      }
      el('loginRequired').classList.add('hidden');
      el('confirmPanel').classList.remove('hidden');
      el('confirmCategory').textContent = `${select.value} ft`;
      el('confirmAhead').textContent = counts[select.value];
      el('confirmPosition').textContent = `#${counts[select.value] + 1}`;
    });

    const confirmBtn = el('confirmJoinBtn');
    confirmBtn.addEventListener('click', () => {
      const size = select.value;
      counts[size] += 1;
      sessionStorage.setItem('moffatWaitlistCounts', JSON.stringify(counts));
      el('finalCategory').textContent = `${size} ft`;
      el('finalPosition').textContent = `#${counts[size]}`;
      el('confirmPanel').classList.add('hidden');
      el('joinPrompt').classList.add('hidden');
      el('joinConfirmation').classList.remove('hidden');
      renderCounts();
    });

    const cancelBtn = el('cancelJoinBtn');
    cancelBtn.addEventListener('click', () => el('confirmPanel').classList.add('hidden'));

    const resetBtn = el('resetDemoBtn');
    if (resetBtn) resetBtn.addEventListener('click', () => {
      counts = { ...defaults };
      sessionStorage.setItem('moffatWaitlistCounts', JSON.stringify(counts));
      el('joinConfirmation').classList.add('hidden');
      el('confirmPanel').classList.add('hidden');
      el('loginRequired').classList.add('hidden');
      el('joinPrompt').classList.remove('hidden');
      renderCounts();
    });

    renderCounts();
  }

  document.addEventListener('DOMContentLoaded', () => {
    setSessionUI();
    setupRegistration();
    setupVerification();
    setupLogin();
    setupWaitlist();
  });
})();
