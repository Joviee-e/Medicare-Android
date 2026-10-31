/**
 * MediCare+ Master Web Application Controller
 * Handles navigation, adherence tracking, audio synthesis, accessibility,
 * AI assistance, simulated pharmacy maps, and backend synchronization.
 */

(function () {
  'use strict';

  // --- 1. State Initialization & Persistence ---
  const STORAGE_KEYS = {
    MEDICINES: 'medicare_medicines_v1',
    PROFILE: 'medicare_profile_v1',
    THEME: 'medicare_theme_v1',
    ACCESSIBILITY: 'medicare_access_v1',
    VIEW_MODE: 'medicare_view_mode_v1'
  };

  let medicines = [];
  let profile = {};
  let currentFilter = 'all';
  let activeTab = 'tabDashboard';
  let selectedMedicineType = 'tablet';
  let voiceEnabled = false;

  function loadState() {
    try {
      const storedMeds = localStorage.getItem(STORAGE_KEYS.MEDICINES);
      medicines = storedMeds ? JSON.parse(storedMeds) : [...INITIAL_MEDICINES];

      const storedProfile = localStorage.getItem(STORAGE_KEYS.PROFILE);
      profile = storedProfile ? JSON.parse(storedProfile) : { ...INITIAL_PROFILE };

      const storedTheme = localStorage.getItem(STORAGE_KEYS.THEME) || 'light';
      if (storedTheme === 'dark') document.body.classList.add('dark-theme');

      const storedAccess = localStorage.getItem(STORAGE_KEYS.ACCESSIBILITY);
      if (storedAccess) {
        const acc = JSON.parse(storedAccess);
        if (acc.largeFont) document.body.classList.add('large-font');
        if (acc.highContrast) document.body.classList.add('high-contrast');
      }

      const storedViewMode = localStorage.getItem(STORAGE_KEYS.VIEW_MODE) || 'mobile';
      if (storedViewMode === 'desktop') {
        setDesktopMode(true);
      }
    } catch (e) {
      console.warn('Error reading from localStorage, using defaults', e);
      medicines = [...INITIAL_MEDICINES];
      profile = { ...INITIAL_PROFILE };
    }
  }

  function saveMedicines() {
    try {
      localStorage.setItem(STORAGE_KEYS.MEDICINES, JSON.stringify(medicines));
    } catch (e) {
      console.error('Failed to save medicines to localStorage', e);
    }
  }

  function saveAccessibility() {
    try {
      const acc = {
        largeFont: document.body.classList.contains('large-font'),
        highContrast: document.body.classList.contains('high-contrast')
      };
      localStorage.setItem(STORAGE_KEYS.ACCESSIBILITY, JSON.stringify(acc));
    } catch (e) {
      console.error('Failed to save accessibility state', e);
    }
  }

  // --- 2. Web Audio Harmonic Chime Synthesizer ---
  function playChime(type = 'success') {
    try {
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      if (!AudioContext) return;
      const ctx = new AudioContext();

      if (type === 'alarm') {
        // High alert pleasant two-tone chime
        [523.25, 659.25, 783.99].forEach((freq, i) => {
          const osc = ctx.createOscillator();
          const gain = ctx.createGain();
          osc.type = 'sine';
          osc.frequency.setValueAtTime(freq, ctx.currentTime + i * 0.15);
          gain.gain.setValueAtTime(0.18, ctx.currentTime + i * 0.15);
          gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + i * 0.15 + 0.4);
          osc.connect(gain);
          gain.connect(ctx.destination);
          osc.start(ctx.currentTime + i * 0.15);
          osc.stop(ctx.currentTime + i * 0.15 + 0.45);
        });
      } else {
        // Gentle congratulatory chime
        [440, 554.37, 659.25].forEach((freq, i) => {
          const osc = ctx.createOscillator();
          const gain = ctx.createGain();
          osc.type = 'triangle';
          osc.frequency.setValueAtTime(freq, ctx.currentTime + i * 0.12);
          gain.gain.setValueAtTime(0.15, ctx.currentTime + i * 0.12);
          gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + i * 0.12 + 0.5);
          osc.connect(gain);
          gain.connect(ctx.destination);
          osc.start(ctx.currentTime + i * 0.12);
          osc.stop(ctx.currentTime + i * 0.12 + 0.55);
        });
      }
    } catch (e) {
      // AudioContext might require user gesture in some browsers
    }
  }

  // --- 3. Speech Synthesis for Senior Accessibility ---
  function speakText(text) {
    if (!voiceEnabled || !('speechSynthesis' in window)) return;
    try {
      window.speechSynthesis.cancel();
      const cleanText = text.replace(/[*#_•]/g, '');
      const utterance = new SpeechSynthesisUtterance(cleanText);
      utterance.rate = 0.95; // Slightly slower for senior legibility
      utterance.pitch = 1.0;
      window.speechSynthesis.speak(utterance);
    } catch (e) {
      console.warn('Speech synthesis error', e);
    }
  }

  // --- 4. Toast Notification Utility ---
  function showToast(message, icon = '✓') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = 'toast-msg';
    toast.innerHTML = `<span style="color: #9ff2e0; font-size: 1.1rem;">${icon}</span> <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(10px) scale(0.95)';
      toast.style.transition = 'all 0.25s ease';
      setTimeout(() => toast.remove(), 250);
    }, 3200);
  }

  // --- 5. Adherence Ring & Progress Engine ---
  function updateAdherenceMetrics() {
    const totalDoses = medicines.length;
    const takenDoses = medicines.filter(m => m.status === 'taken').length;
    const percent = totalDoses > 0 ? Math.round((takenDoses / totalDoses) * 100) : 0;

    // Update circular SVG path
    const progressPath = document.getElementById('adherenceProgressPath');
    const percentText = document.getElementById('adherencePercentText');
    const heroText = document.getElementById('adherenceHeroText');

    if (progressPath) {
      progressPath.setAttribute('stroke-dasharray', `${percent}, 100`);
      // Color change depending on compliance
      if (percent >= 75) {
        progressPath.style.stroke = '#9ff2e0';
      } else if (percent >= 50) {
        progressPath.style.stroke = '#fde68a';
      } else {
        progressPath.style.stroke = '#fca5a5';
      }
    }

    if (percentText) percentText.textContent = `${percent}%`;
    if (heroText) heroText.textContent = `${takenDoses} of ${totalDoses} Doses Taken`;
  }

  // --- 6. Render Dashboard Timeline ---
  function renderDashboardTimeline() {
    const feed = document.getElementById('todayDoseFeed');
    if (!feed) return;
    feed.innerHTML = '';

    medicines.forEach(med => {
      const card = document.createElement('article');
      card.className = 'dose-card';

      let statusColor = '#00677f';
      let tagClass = 'tag-scheduled';
      let tagLabel = 'Scheduled';

      if (med.status === 'taken') {
        statusColor = 'var(--status-taken)';
        tagClass = 'tag-taken';
        tagLabel = 'Taken';
      } else if (med.status === 'upcoming') {
        statusColor = 'var(--status-upcoming)';
        tagClass = 'tag-upcoming';
        tagLabel = 'Upcoming';
      }

      card.style.setProperty('--card-status-color', statusColor);

      const typeIcon = getTypeIcon(med.type);
      const isTaken = med.status === 'taken';

      card.innerHTML = `
        <div class="med-icon-box" aria-hidden="true">${typeIcon}</div>
        <div class="dose-info-col">
          <div class="dose-name">${escapeHtml(med.name)} <span style="font-weight: 500; font-size: 0.85rem; color: var(--text-secondary);">${escapeHtml(med.dosage)}</span></div>
          <div class="dose-meta">
            <span class="dose-tag ${tagClass}">${tagLabel}</span>
            <span>⏰ ${escapeHtml(med.scheduleTimes ? med.scheduleTimes[0] : 'Daily')}</span>
            <span>🍽️ ${formatMealTiming(med.mealTiming)}</span>
          </div>
        </div>
        <div>
          ${isTaken
            ? `<button class="take-action-btn btn-taken-state" disabled title="Dose already logged today">✓ Done</button>`
            : `<button class="take-action-btn" data-med-id="${med.id}">Take</button>`
          }
        </div>
      `;

      feed.appendChild(card);
    });

    // Attach click events for Take button
    feed.querySelectorAll('.take-action-btn:not([disabled])').forEach(btn => {
      btn.addEventListener('click', () => {
        const medId = btn.getAttribute('data-med-id');
        markMedicationTaken(medId);
      });
    });
  }

  // --- 7. Render Prescriptions Feed ---
  function renderPrescriptionsFeed() {
    const container = document.getElementById('allMedicinesContainer');
    if (!container) return;
    container.innerHTML = '';

    const searchTerm = (document.getElementById('searchMedicinesInput')?.value || '').toLowerCase().trim();

    const filtered = medicines.filter(m => {
      const matchesSearch = !searchTerm ||
        m.name.toLowerCase().includes(searchTerm) ||
        (m.prescribedFor && m.prescribedFor.toLowerCase().includes(searchTerm)) ||
        m.dosage.toLowerCase().includes(searchTerm);

      if (!matchesSearch) return false;

      if (currentFilter === 'all') return true;
      if (currentFilter === 'morning') return (m.scheduleTimes || []).some(t => t.includes('AM'));
      if (currentFilter === 'evening') return (m.scheduleTimes || []).some(t => t.includes('PM'));
      if (currentFilter === 'as_needed') return m.frequency === 'as_needed';
      return true;
    });

    if (filtered.length === 0) {
      container.innerHTML = `
        <div style="grid-column: 1/-1; text-align: center; padding: 40px 20px; color: var(--text-muted);">
          <div style="font-size: 2.2rem; margin-bottom: 8px;">🔍</div>
          <div style="font-weight: 600;">No medications match your query.</div>
          <p style="font-size: 0.85rem; margin-top: 4px;">Try clearing filters or search terms.</p>
        </div>
      `;
      return;
    }

    filtered.forEach(med => {
      const card = document.createElement('article');
      card.className = 'medication-full-card';

      const typeIcon = getTypeIcon(med.type);
      const isLowStock = med.inventoryRemaining <= 15;

      card.innerHTML = `
        <div class="med-header-row">
          <div class="med-title-group">
            <div class="med-icon-box" style="width: 44px; height: 44px; font-size: 1.2rem;">${typeIcon}</div>
            <div>
              <div style="font-size: 1.15rem; font-weight: 700; color: var(--text-primary);">${escapeHtml(med.name)}</div>
              <div style="font-size: 0.82rem; color: var(--text-secondary);">${escapeHtml(med.dosage)} • ${formatFrequency(med.frequency)}</div>
            </div>
          </div>
          <div class="pill-inventory-badge ${isLowStock ? 'low-stock' : ''}">
            ${med.inventoryRemaining} pills left
          </div>
        </div>

        <div class="med-schedule-tags">
          ${(med.scheduleTimes || ['Daily']).map(t => `<span class="time-chip-badge">⏰ ${escapeHtml(t)}</span>`).join('')}
          <span class="time-chip-badge" style="background: var(--bg-surface-subtle); color: var(--text-secondary);">
            🍽️ ${formatMealTiming(med.mealTiming)}
          </span>
        </div>

        <div class="med-instructions-text">
          <strong>Instructions:</strong> ${escapeHtml(med.instructions || med.precautions || 'Take as prescribed by doctor.')}
        </div>

        <div class="card-actions-row">
          <button class="btn-danger-action" data-del-id="${med.id}">Delete</button>
          <button class="btn-secondary-action" data-log-id="${med.id}">
            ${med.status === 'taken' ? '✓ Taken Today' : 'Log Dose'}
          </button>
        </div>
      `;

      container.appendChild(card);
    });

    // Delete medication listener
    container.querySelectorAll('[data-del-id]').forEach(btn => {
      btn.addEventListener('click', () => {
        const id = btn.getAttribute('data-del-id');
        deleteMedication(id);
      });
    });

    // Log dose listener
    container.querySelectorAll('[data-log-id]').forEach(btn => {
      btn.addEventListener('click', () => {
        const id = btn.getAttribute('data-log-id');
        markMedicationTaken(id);
      });
    });
  }

  // --- 8. Mark Taken & State Modifiers ---
  function markMedicationTaken(id) {
    const med = medicines.find(m => m.id === id);
    if (!med) return;

    med.status = 'taken';
    if (med.inventoryRemaining > 0) med.inventoryRemaining -= 1;
    med.history = med.history || [];
    med.history.push({
      date: 'Today',
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      status: 'taken'
    });

    saveMedicines();
    updateAdherenceMetrics();
    renderDashboardTimeline();
    renderPrescriptionsFeed();
    playChime('success');
    showToast(`Logged dose: ${med.name} (${med.dosage}) taken!`, '🎉');
  }

  function deleteMedication(id) {
    const med = medicines.find(m => m.id === id);
    if (!med) return;

    if (confirm(`Are you sure you want to remove ${med.name} from your prescriptions?`)) {
      medicines = medicines.filter(m => m.id !== id);
      saveMedicines();
      updateAdherenceMetrics();
      renderDashboardTimeline();
      renderPrescriptionsFeed();
      showToast(`Removed ${med.name} from regimen.`, '🗑️');
    }
  }

  // --- 9. Add Medicine Modal & Form Validation ---
  function setupAddMedicineModal() {
    const modal = document.getElementById('addMedicineModal');
    const openBtn = document.getElementById('btnOpenAddMedModal');
    const quickAddBtn = document.getElementById('btnQuickAddMed');
    const qaAddMed = document.getElementById('qaAddMed');
    const closeBtn = document.getElementById('btnCloseAddMedModal');
    const cancelBtn = document.getElementById('btnCancelAddMed');
    const form = document.getElementById('addMedicationForm');
    const typeButtons = document.querySelectorAll('.type-chip-btn');
    const nameInput = document.getElementById('medNameInput');
    const warningBox = document.getElementById('safetyWarningBox');

    function openModal() {
      modal.classList.add('open');
      if (nameInput) nameInput.focus();
    }

    function closeModal() {
      modal.classList.remove('open');
      form.reset();
      warningBox.style.display = 'none';
      typeButtons.forEach(b => b.classList.remove('selected'));
      typeButtons[0]?.classList.add('selected');
      selectedMedicineType = 'tablet';
    }

    if (openBtn) openBtn.addEventListener('click', openModal);
    if (quickAddBtn) quickAddBtn.addEventListener('click', openModal);
    if (qaAddMed) qaAddMed.addEventListener('click', openModal);
    if (closeBtn) closeBtn.addEventListener('click', closeModal);
    if (cancelBtn) cancelBtn.addEventListener('click', closeModal);

    // Type selection
    typeButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        typeButtons.forEach(b => b.classList.remove('selected'));
        btn.classList.add('selected');
        selectedMedicineType = btn.getAttribute('data-type') || 'tablet';
      });
    });

    // Realtime allergy & conflict check while typing
    if (nameInput) {
      nameInput.addEventListener('input', () => {
        const val = nameInput.value.toLowerCase().trim();
        if (val.includes('penicillin') || val.includes('amoxicillin') || val.includes('ampicillin')) {
          warningBox.innerHTML = `⚠️ <strong>Allergy Alert:</strong> Eleanor Vance has a documented severe allergy to Penicillin-class antibiotics. Consult Dr. Robert Chen before adding this medication.`;
          warningBox.style.display = 'block';
        } else if (val.includes('sulfa') || val.includes('bactrim')) {
          warningBox.innerHTML = `⚠️ <strong>Allergy Alert:</strong> Documented Sulfa allergy in medical record.`;
          warningBox.style.display = 'block';
        } else {
          warningBox.style.display = 'none';
        }
      });
    }

    // Submit handler
    if (form) {
      form.addEventListener('submit', (e) => {
        e.preventDefault();
        const name = document.getElementById('medNameInput').value.trim();
        const dosage = document.getElementById('medDosageInput').value.trim();
        const frequency = document.getElementById('medFrequencySelect').value;
        const timeVal = document.getElementById('medTimeInput').value;
        const mealTiming = document.getElementById('medMealSelect').value;
        const instructions = document.getElementById('medInstructionsInput').value.trim();

        // Format 24h to 12h AM/PM
        let formattedTime = '08:00 AM';
        if (timeVal) {
          const [hours, minutes] = timeVal.split(':');
          const h = parseInt(hours, 10);
          const ampm = h >= 12 ? 'PM' : 'AM';
          const h12 = h % 12 || 12;
          formattedTime = `${String(h12).padStart(2, '0')}:${minutes} ${ampm}`;
        }

        const newMed = {
          id: 'med_' + Date.now(),
          name: name,
          type: selectedMedicineType,
          dosage: dosage,
          instructions: instructions || 'Take according to physician instructions.',
          frequency: frequency,
          scheduleTimes: [formattedTime],
          mealTiming: mealTiming,
          startDate: new Date().toISOString().split('T')[0],
          endDate: '2026-12-31',
          inventoryRemaining: 30,
          totalPills: 30,
          status: 'upcoming',
          history: []
        };

        medicines.unshift(newMed);
        saveMedicines();
        updateAdherenceMetrics();
        renderDashboardTimeline();
        renderPrescriptionsFeed();
        closeModal();
        playChime('success');
        showToast(`Added ${name} (${dosage}) to daily regimen!`, '💊');
      });
    }
  }

  // --- 10. AI Health Assistant Engine ---
  function setupAiChatbot() {
    const chatForm = document.getElementById('chatMessageForm');
    const inputField = document.getElementById('chatInputMessage');
    const feed = document.getElementById('chatMessagesFeed');
    const suggestions = document.querySelectorAll('.prompt-chip-btn');
    const voiceToggle = document.getElementById('btnVoiceReadoutToggle');

    if (voiceToggle) {
      voiceToggle.addEventListener('click', () => {
        voiceEnabled = !voiceEnabled;
        voiceToggle.style.color = voiceEnabled ? 'var(--status-taken)' : 'inherit';
        showToast(voiceEnabled ? 'Voice readout assistance enabled 🔊' : 'Voice readout muted 🔇', '🔊');
      });
    }

    function appendMessage(text, sender = 'bot') {
      const bubble = document.createElement('div');
      bubble.className = `chat-bubble bubble-${sender}`;
      
      // Support basic bold & bullet formatting
      const formatted = text
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\n\n/g, '<br><br>')
        .replace(/\n•/g, '<br>•');

      const timeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      bubble.innerHTML = `${formatted}<div class="chat-time-stamp">${timeStr}</div>`;
      feed.appendChild(bubble);
      feed.scrollTop = feed.scrollHeight;

      if (sender === 'bot') {
        speakText(text);
      }
    }

    async function handleUserQuery(query) {
      if (!query.trim()) return;
      appendMessage(query, 'user');
      if (inputField) inputField.value = '';

      // Typing indicator
      const typingIndicator = document.createElement('div');
      typingIndicator.className = 'chat-bubble bubble-bot';
      typingIndicator.id = 'botTypingBubble';
      typingIndicator.innerHTML = `<em>MediCare+ AI is analyzing medication records...</em>`;
      feed.appendChild(typingIndicator);
      feed.scrollTop = feed.scrollHeight;

      // Try contacting the Flask Backend API at /api/ai/chat
      let backendSuccess = false;
      try {
        const response = await fetch('http://127.0.0.1:5000/api/ai/chat', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer demo_token'
          },
          body: JSON.stringify({ message: query, context: { patient_id: profile.id } }),
          signal: AbortSignal.timeout(3000)
        });

        if (response.ok) {
          const resData = await response.json();
          if (resData.reply) {
            typingIndicator.remove();
            appendMessage(resData.reply, 'bot');
            backendSuccess = true;
          }
        }
      } catch (err) {
        // Fallback to local intelligent knowledge base
      }

      if (!backendSuccess) {
        setTimeout(() => {
          typingIndicator.remove();
          const localReply = matchLocalAiResponse(query);
          appendMessage(localReply, 'bot');
        }, 650);
      }
    }

    function matchLocalAiResponse(query) {
      const q = query.toLowerCase();

      // Check knowledge base
      for (const item of AI_KNOWLEDGE_BASE) {
        if (item.keywords.some(k => q.includes(k))) {
          return item.response;
        }
      }

      // Default safe assistant response
      return `**MediCare+ Health Assistant Guidance:**\n\nI have reviewed your active profile. You are currently scheduled for:\n• **Metformin 500mg** (take with meals)\n• **Lisinopril 10mg** (morning with water)\n• **Atorvastatin 20mg** (evening)\n• **Vitamin D3** (morning fatty meal)\n\nFor questions about symptoms, prescription adjustments, or urgent health concerns, please consult **Dr. Robert Chen** at (555) 890-1234 or dial 911 for emergencies.`;
    }

    if (chatForm) {
      chatForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const msg = inputField.value;
        handleUserQuery(msg);
      });
    }

    suggestions.forEach(btn => {
      btn.addEventListener('click', () => {
        const prompt = btn.getAttribute('data-prompt') || btn.textContent;
        handleUserQuery(prompt);
      });
    });
  }

  // --- 11. Nearby Pharmacies & Street Map Interaction ---
  function setupPharmacies() {
    const listFeed = document.getElementById('pharmacyListFeed');
    const pinsGroup = document.getElementById('mapPinsGroup');
    if (!listFeed || !pinsGroup) return;

    listFeed.innerHTML = '';
    pinsGroup.innerHTML = '';

    INITIAL_PHARMACIES.forEach((pharm, index) => {
      // Map Pin SVG
      const pin = document.createElementNS('http://www.w3.org/2000/svg', 'g');
      pin.setAttribute('class', 'pharmacy-map-pin');
      pin.setAttribute('data-id', pharm.id);
      pin.style.cursor = 'pointer';

      pin.innerHTML = `
        <circle cx="${pharm.coordinates.x}" cy="${pharm.coordinates.y}" r="4" fill="#00677f" stroke="#ffffff" stroke-width="1.2" />
        <text x="${pharm.coordinates.x - 2}" y="${pharm.coordinates.y - 5}" font-size="3" fill="#004d60" font-weight="bold">${index + 1}</text>
      `;

      pinsGroup.appendChild(pin);

      // Pharmacy Feed Card
      const card = document.createElement('article');
      card.className = 'pharmacy-card-item';
      card.id = `card_${pharm.id}`;

      card.innerHTML = `
        <div class="pharmacy-meta-row">
          <div class="pharmacy-name-title">#${index + 1} ${escapeHtml(pharm.name)}</div>
          <span class="pharmacy-distance-tag">${escapeHtml(pharm.distance)}</span>
        </div>
        <div style="font-size: 0.84rem; color: var(--text-secondary);">${escapeHtml(pharm.address)}</div>
        <div style="display: flex; gap: 8px; font-size: 0.8rem; align-items: center;">
          <span style="color: #f59e0b; font-weight: 700;">★ ${pharm.rating}</span>
          <span style="color: var(--text-muted);">(${pharm.reviewsCount} reviews)</span>
          <span class="dose-tag tag-taken">${escapeHtml(pharm.status)}</span>
        </div>
        <div class="pharmacy-features-row">
          ${pharm.features.map(f => `<span class="dose-tag" style="background: var(--bg-surface-subtle); color: var(--text-secondary); font-size: 0.7rem;">${escapeHtml(f)}</span>`).join('')}
        </div>
        <div class="pharmacy-btn-row">
          <button class="btn-secondary-action btn-call-pharm" data-phone="${pharm.phone}">📞 Call</button>
          <button class="btn-primary-action btn-directions-pharm" data-name="${pharm.name}">🧭 Directions</button>
        </div>
      `;

      listFeed.appendChild(card);

      // Card click highlights pin
      card.addEventListener('click', (e) => {
        if (e.target.closest('button')) return;
        highlightPharmacy(pharm.id);
      });

      pin.addEventListener('click', () => {
        highlightPharmacy(pharm.id);
        card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      });
    });

    // Call and Directions actions
    listFeed.querySelectorAll('.btn-call-pharm').forEach(btn => {
      btn.addEventListener('click', () => {
        const phone = btn.getAttribute('data-phone');
        showToast(`Connecting telephone call to ${phone}...`, '📞');
      });
    });

    listFeed.querySelectorAll('.btn-directions-pharm').forEach(btn => {
      btn.addEventListener('click', () => {
        const name = btn.getAttribute('data-name');
        showToast(`Turn-by-turn navigation started to ${name}`, '🧭');
      });
    });

    function highlightPharmacy(id) {
      document.querySelectorAll('.pharmacy-card-item').forEach(c => c.classList.remove('selected-pharmacy'));
      const activeCard = document.getElementById(`card_${id}`);
      if (activeCard) activeCard.classList.add('selected-pharmacy');

      // Highlight pin
      pinsGroup.querySelectorAll('circle').forEach(c => {
        c.setAttribute('r', '4');
        c.setAttribute('fill', '#00677f');
      });
      const activePin = pinsGroup.querySelector(`[data-id="${id}"] circle`);
      if (activePin) {
        activePin.setAttribute('r', '6');
        activePin.setAttribute('fill', '#f59e0b');
      }
    }
  }

  // --- 12. Simulated Dose Reminder Alarm ---
  function setupAlarmSimulator() {
    const modal = document.getElementById('alarmModalOverlay');
    const triggerBtn = document.getElementById('btnTriggerAlarm');
    const qaAlarmBtn = document.getElementById('qaAlarm');
    const takeBtn = document.getElementById('btnAlarmTakeNow');
    const snoozeBtn = document.getElementById('btnAlarmSnooze');
    const skipBtn = document.getElementById('btnAlarmSkip');

    function openAlarm() {
      modal.classList.add('open');
      playChime('alarm');
      speakText('Attention Eleanor. Scheduled dose for Metformin 500mg is due now.');
    }

    function closeAlarm() {
      modal.classList.remove('open');
    }

    if (triggerBtn) triggerBtn.addEventListener('click', openAlarm);
    if (qaAlarmBtn) qaAlarmBtn.addEventListener('click', openAlarm);

    if (takeBtn) {
      takeBtn.addEventListener('click', () => {
        closeAlarm();
        const metformin = medicines.find(m => m.name.toLowerCase().includes('metformin'));
        if (metformin) markMedicationTaken(metformin.id);
        else showToast('Dose recorded as taken!', '✓');
      });
    }

    if (snoozeBtn) {
      snoozeBtn.addEventListener('click', () => {
        closeAlarm();
        showToast('Alarm snoozed for 10 minutes. Reminder will ring at 08:55 AM.', '⏳');
      });
    }

    if (skipBtn) {
      skipBtn.addEventListener('click', () => {
        closeAlarm();
        showToast('Dose skipped. Reason recorded in doctor compliance logs.', 'ℹ️');
      });
    }
  }

  // --- 13. Accessibility Suite & Theme Controls ---
  function setupAccessibilityAndTheme() {
    const toggleDark = document.getElementById('toggleDarkMode');
    const toggleLarge = document.getElementById('toggleLargeFont');
    const toggleContrast = document.getElementById('toggleHighContrast');
    const quickThemeBtn = document.getElementById('btnQuickTheme');
    const quickContrastBtn = document.getElementById('btnQuickContrast');

    function syncTheme(isDark) {
      if (isDark) {
        document.body.classList.add('dark-theme');
        localStorage.setItem(STORAGE_KEYS.THEME, 'dark');
      } else {
        document.body.classList.remove('dark-theme');
        localStorage.setItem(STORAGE_KEYS.THEME, 'light');
      }
      if (toggleDark) toggleDark.checked = isDark;
    }

    if (toggleDark) {
      toggleDark.checked = document.body.classList.contains('dark-theme');
      toggleDark.addEventListener('change', () => syncTheme(toggleDark.checked));
    }

    if (quickThemeBtn) {
      quickThemeBtn.addEventListener('click', () => {
        const isDarkNow = document.body.classList.contains('dark-theme');
        syncTheme(!isDarkNow);
      });
    }

    if (toggleLarge) {
      toggleLarge.checked = document.body.classList.contains('large-font');
      toggleLarge.addEventListener('change', () => {
        if (toggleLarge.checked) document.body.classList.add('large-font');
        else document.body.classList.remove('large-font');
        saveAccessibility();
      });
    }

    if (toggleContrast) {
      toggleContrast.checked = document.body.classList.contains('high-contrast');
      toggleContrast.addEventListener('change', () => {
        if (toggleContrast.checked) document.body.classList.add('high-contrast');
        else document.body.classList.remove('high-contrast');
        saveAccessibility();
      });
    }

    if (quickContrastBtn) {
      quickContrastBtn.addEventListener('click', () => {
        const isHigh = document.body.classList.toggle('high-contrast');
        if (toggleContrast) toggleContrast.checked = isHigh;
        saveAccessibility();
      });
    }
  }

  // --- 14. Viewport Switching (Mobile Frame vs Desktop) ---
  function setDesktopMode(isDesktop) {
    const mainContainer = document.getElementById('appMainContainer');
    const btnMobile = document.getElementById('btnViewMobile');
    const btnDesktop = document.getElementById('btnViewDesktop');

    if (isDesktop) {
      mainContainer.classList.add('desktop-mode');
      btnDesktop?.classList.add('active');
      btnMobile?.classList.remove('active');
      localStorage.setItem(STORAGE_KEYS.VIEW_MODE, 'desktop');
    } else {
      mainContainer.classList.remove('desktop-mode');
      btnMobile?.classList.add('active');
      btnDesktop?.classList.remove('active');
      localStorage.setItem(STORAGE_KEYS.VIEW_MODE, 'mobile');
    }
  }

  function setupViewportModeToggle() {
    const btnMobile = document.getElementById('btnViewMobile');
    const btnDesktop = document.getElementById('btnViewDesktop');

    if (btnMobile) btnMobile.addEventListener('click', () => setDesktopMode(false));
    if (btnDesktop) btnDesktop.addEventListener('click', () => setDesktopMode(true));
  }

  // --- 15. Tab Navigation ---
  function switchTab(tabId) {
    activeTab = tabId;

    // Update screen tab visibility
    document.querySelectorAll('.screen-tab').forEach(tab => {
      tab.classList.remove('active-tab');
    });
    const target = document.getElementById(tabId);
    if (target) target.classList.add('active-tab');

    // Update nav buttons
    document.querySelectorAll('.nav-item-btn').forEach(btn => {
      if (btn.getAttribute('data-tab') === tabId) {
        btn.classList.add('active');
      } else {
        btn.classList.remove('active');
      }
    });

    // Scroll to top
    const scroller = document.querySelector('.screen-scroll-container');
    if (scroller) scroller.scrollTop = 0;
  }

  function setupNavigation() {
    document.querySelectorAll('.nav-item-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const tab = btn.getAttribute('data-tab');
        if (tab) switchTab(tab);
      });
    });

    // Quick links from Dashboard
    document.getElementById('qaAskAi')?.addEventListener('click', () => switchTab('tabChatbot'));
    document.getElementById('qaPharmacies')?.addEventListener('click', () => switchTab('tabPharmacies'));
    document.getElementById('linkViewAllMeds')?.addEventListener('click', () => switchTab('tabPrescriptions'));

    // Prescriptions filter chips
    document.querySelectorAll('.filter-chip').forEach(chip => {
      chip.addEventListener('click', () => {
        document.querySelectorAll('.filter-chip').forEach(c => c.classList.remove('active'));
        chip.classList.add('active');
        currentFilter = chip.getAttribute('data-filter') || 'all';
        renderPrescriptionsFeed();
      });
    });

    // Search input
    const searchInput = document.getElementById('searchMedicinesInput');
    if (searchInput) {
      searchInput.addEventListener('input', renderPrescriptionsFeed);
    }
  }

  // --- 16. Helpers ---
  function getTypeIcon(type) {
    switch (type) {
      case 'capsule': return '💊';
      case 'syrup': return '🧪';
      case 'injection': return '💉';
      default: return '💊';
    }
  }

  function formatMealTiming(timing) {
    switch (timing) {
      case 'with_food': return 'With food';
      case 'after_food': return 'After food';
      case 'before_food': return 'Before food';
      default: return 'No food requirement';
    }
  }

  function formatFrequency(freq) {
    switch (freq) {
      case 'twice_daily': return 'Twice daily';
      case 'weekly': return 'Weekly';
      case 'as_needed': return 'As needed';
      default: return 'Once daily';
    }
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function updateLiveClock() {
    const clock = document.getElementById('statusBarClock');
    if (clock) {
      const now = new Date();
      clock.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
  }

  // --- 17. App Lifecycle Boot ---
  window.addEventListener('DOMContentLoaded', () => {
    loadState();
    setupNavigation();
    setupViewportModeToggle();
    setupAccessibilityAndTheme();
    setupAddMedicineModal();
    setupAiChatbot();
    setupPharmacies();
    setupAlarmSimulator();

    updateAdherenceMetrics();
    renderDashboardTimeline();
    renderPrescriptionsFeed();

    updateLiveClock();
    setInterval(updateLiveClock, 30000);
  });

})();
