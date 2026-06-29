// State Management
let API_URL = '';
let REFRESH_INTERVAL = 5; // in seconds
let refreshTimer = null;
let countdownInterval = null;
let currentCountdown = 5;
let allJobs = [];

// DOM Elements
const elements = {
  jobsTbody: document.getElementById('jobs-tbody'),
  statTotal: document.getElementById('stat-total'),
  statRunning: document.getElementById('stat-running'),
  statPaused: document.getElementById('stat-paused'),
  statFailed: document.getElementById('stat-failed'),
  
  // Search & Filters
  searchInput: document.getElementById('search-input'),
  statusFilter: document.getElementById('status-filter'),
  typeFilter: document.getElementById('type-filter'),
  refreshIndicator: document.getElementById('refresh-indicator'),
  connectedEndpoint: document.getElementById('connected-endpoint'),
  
  // Buttons
  btnRefresh: document.getElementById('btn-refresh'),
  btnNewJob: document.getElementById('btn-new-job'),
  btnSettings: document.getElementById('btn-settings'),
  btnSaveSettings: document.getElementById('btn-save-settings'),
  btnSetNow: document.getElementById('btn-set-now'),
  
  // Modals
  modalJobForm: document.getElementById('modal-job-form'),
  modalHistory: document.getElementById('modal-history'),
  modalSettings: document.getElementById('modal-settings'),
  
  // Forms & Inputs
  jobForm: document.getElementById('job-form'),
  jobFormTitle: document.getElementById('job-form-title'),
  formJobId: document.getElementById('form-job-id'),
  jobName: document.getElementById('job-name'),
  jobDescription: document.getElementById('job-description'),
  jobScheduleType: document.getElementById('job-schedule-type'),
  jobCronExpression: document.getElementById('job-cron-expression'),
  jobPriority: document.getElementById('job-priority'),
  jobMaxRetries: document.getElementById('job-max-retries'),
  jobExecutionTime: document.getElementById('job-execution-time'),
  jobPayload: document.getElementById('job-payload'),
  payloadError: document.getElementById('payload-error'),
  
  // History Modal
  historyJobName: document.getElementById('history-job-name'),
  detailJobId: document.getElementById('detail-job-id'),
  detailSchedule: document.getElementById('detail-schedule'),
  detailPayload: document.getElementById('detail-payload'),
  historyTbody: document.getElementById('history-tbody'),
  
  // Settings Inputs
  settingsApiUrl: document.getElementById('settings-api-url'),
  settingsRefreshRate: document.getElementById('settings-refresh-rate'),
  toastContainer: document.getElementById('toast-container'),
  
  fieldCronContainer: document.getElementById('field-cron-container'),
  fieldExecutionTimeContainer: document.getElementById('field-execution-time-container'),
  fieldCronHelp: document.getElementById('field-cron-help'),
};

// Initialize Application
document.addEventListener('DOMContentLoaded', () => {
  loadConfig();
  setupEventListeners();
  fetchJobs();
  startAutoRefresh();
});

// Config loader (from localStorage or Auto-detect)
function loadConfig() {
  const savedUrl = localStorage.getItem('scheduler_api_url');
  if (savedUrl) {
    API_URL = savedUrl;
  } else {
    // If running inside same origin, use relative origin, else default to localhost:8080
    if (window.location.origin && window.location.origin.includes('localhost')) {
      API_URL = window.location.origin;
    } else {
      API_URL = 'http://localhost:8080';
    }
  }
  
  const savedInterval = localStorage.getItem('scheduler_refresh_interval');
  if (savedInterval !== null) {
    REFRESH_INTERVAL = parseInt(savedInterval, 10);
  } else {
    REFRESH_INTERVAL = 5;
  }

  elements.connectedEndpoint.textContent = API_URL;
  elements.settingsApiUrl.value = API_URL;
  elements.settingsRefreshRate.value = REFRESH_INTERVAL;
}

// Save config handler
function saveConfig() {
  let url = elements.settingsApiUrl.value.trim();
  if (url.endsWith('/')) {
    url = url.slice(0, -1);
  }
  
  const interval = parseInt(elements.settingsRefreshRate.value, 10);
  
  if (url) {
    localStorage.setItem('scheduler_api_url', url);
  } else {
    localStorage.removeItem('scheduler_api_url');
  }
  
  localStorage.setItem('scheduler_refresh_interval', isNaN(interval) ? 5 : interval);
  
  showToast('Configuration updated! Reloading...', 'success');
  setTimeout(() => {
    window.location.reload();
  }, 1000);
}

// Setup all DOM actions and button triggers
function setupEventListeners() {
  // Modal toggling
  elements.btnNewJob.addEventListener('click', () => openJobFormModal());
  elements.btnSettings.addEventListener('click', () => openModal(elements.modalSettings));
  elements.btnRefresh.addEventListener('click', () => {
    fetchJobs();
    resetCountdown();
  });
  
  // Close buttons
  document.querySelectorAll('.close-modal-btn').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const targetId = e.target.getAttribute('data-target');
      closeModal(document.getElementById(targetId));
    });
  });
  
  // Click outside to close modals
  window.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal')) {
      closeModal(e.target);
    }
  });

  // Schedule Type Change visibility
  elements.jobScheduleType.addEventListener('change', handleScheduleTypeChange);

  // Set next execution time to current moment helper
  elements.btnSetNow.addEventListener('click', () => {
    const now = new Date();
    // Format to local ISO (YYYY-MM-DDTHH:mm)
    const offset = now.getTimezoneOffset() * 60000;
    const localISOTime = (new Date(now - offset)).toISOString().slice(0, 16);
    elements.jobExecutionTime.value = localISOTime;
  });

  // Settings Save
  elements.btnSaveSettings.addEventListener('click', saveConfig);

  // Form Submit
  elements.jobForm.addEventListener('submit', handleFormSubmit);

  // Filters change listener
  elements.searchInput.addEventListener('input', renderJobsTable);
  elements.statusFilter.addEventListener('change', renderJobsTable);
  elements.typeFilter.addEventListener('change', renderJobsTable);
}

// Open modals
function openModal(modal) {
  modal.style.display = 'block';
  document.body.classList.add('modal-open');
}

function closeModal(modal) {
  modal.style.display = 'none';
  if (modal === elements.modalJobForm) {
    elements.jobForm.reset();
    elements.formJobId.value = '';
    elements.payloadError.textContent = '';
  }
  
  // Check if any other modals are open
  const openModals = document.querySelectorAll('.modal');
  let anyOpen = false;
  openModals.forEach(m => {
    if (m.style.display === 'block') {
      anyOpen = true;
    }
  });
  if (!anyOpen) {
    document.body.classList.remove('modal-open');
  }
}

// Form reset + Modal open
function openJobFormModal(job = null) {
  elements.jobForm.reset();
  elements.formJobId.value = '';
  elements.payloadError.textContent = '';
  
  if (job) {
    elements.jobFormTitle.textContent = 'Edit Job';
    elements.formJobId.value = job.id;
    elements.jobName.value = job.name;
    elements.jobDescription.value = job.description || '';
    elements.jobScheduleType.value = job.scheduleType;
    elements.jobCronExpression.value = job.cronExpression || '';
    elements.jobPriority.value = job.priority;
    elements.jobMaxRetries.value = job.maxRetries;
    
    if (job.payload) {
      try {
        // Pretty print JSON payload
        const parsed = JSON.parse(job.payload);
        elements.jobPayload.value = JSON.stringify(parsed, null, 2);
      } catch (e) {
        elements.jobPayload.value = job.payload;
      }
    }
    
	if (job.nextExecutionTime &&job.nextExecutionTime !== 0) {
	    const date = new Date(job.nextExecutionTime);

	    if (!isNaN(date.getTime())) {
	        const offset = date.getTimezoneOffset() * 60000;
	        elements.jobExecutionTime.value = new Date(date.getTime() - offset)
	                							.toISOString()
	                							.slice(0, 16);
	    }
	}
  } else {
    elements.jobFormTitle.textContent = 'Create New Job';
    // Defaults for new job
    elements.jobPriority.value = 5;
    elements.jobMaxRetries.value = 3;
    elements.jobScheduleType.value = 'ONE_TIME';
    elements.jobPayload.value = '{\n  "type": "http",\n  "url": "https://httpbin.org/post",\n  "method": "POST"\n}';
    
    // Set default next exec time to 5 minutes from now
    const fiveMinutesNow = new Date(Date.now() + 5 * 60000);
    const offset = fiveMinutesNow.getTimezoneOffset() * 60000;
    const formatted = new Date(fiveMinutesNow - offset).toISOString().slice(0, 16);
    elements.jobExecutionTime.value = formatted;
  }
  
  handleScheduleTypeChange();
  openModal(elements.modalJobForm);
}

// Handle schedule fields visibility toggle
function handleScheduleTypeChange() {
  const type = elements.jobScheduleType.value;
  if (type === 'ONE_TIME') {
    elements.fieldCronContainer.style.display = 'none';
    elements.jobCronExpression.required = false;
    
    elements.fieldExecutionTimeContainer.style.display = 'block';
    elements.jobExecutionTime.required = true;
  } else {
    elements.fieldCronContainer.style.display = 'block';
    elements.jobCronExpression.required = true;
    
    if (type === 'CRON') {
      elements.fieldCronHelp.textContent = 'Standard 5-field cron: minute, hour, day, month, day-of-week (e.g., */5 * * * *)';
      elements.jobCronExpression.placeholder = '*/5 * * * *';
      
      // Cron executions set time dynamically, nextExecutionTime is optional
      elements.fieldExecutionTimeContainer.style.display = 'none';
      elements.jobExecutionTime.required = false;
    } else {
      elements.fieldCronHelp.textContent = 'ISO-8601 Duration (e.g. PT30S) or integer seconds (e.g. 30)';
      elements.jobCronExpression.placeholder = '30';
      
      // Fixed rates/delays need a start time
      elements.fieldExecutionTimeContainer.style.display = 'block';
      elements.jobExecutionTime.required = true;
    }
  }
}

// Start Auto refresh timers
function startAutoRefresh() {
  if (refreshTimer) clearInterval(refreshTimer);
  if (countdownInterval) clearInterval(countdownInterval);
  
  if (REFRESH_INTERVAL <= 0) {
    elements.refreshIndicator.textContent = 'Auto-refresh disabled';
    return;
  }
  
  currentCountdown = REFRESH_INTERVAL;
  updateCountdownText();
  
  countdownInterval = setInterval(() => {
    currentCountdown--;
    if (currentCountdown <= 0) {
      currentCountdown = REFRESH_INTERVAL;
    }
    updateCountdownText();
  }, 1000);
  
  refreshTimer = setInterval(() => {
    fetchJobs();
  }, REFRESH_INTERVAL * 1000);
}

function resetCountdown() {
  if (REFRESH_INTERVAL > 0) {
    currentCountdown = REFRESH_INTERVAL;
    updateCountdownText();
  }
}

function updateCountdownText() {
  elements.refreshIndicator.textContent = `Auto-refreshing in ${currentCountdown}s...`;
}

// Fetch all jobs
async function fetchJobs() {
  try {
    const response = await fetch(`${API_URL}/jobs?limit=100&offset=0`);
    if (!response.ok) {
      throw new Error(`Server returned ${response.status} ${response.statusText}`);
    }
    allJobs = await response.json();
    updateStatsSummary();
    renderJobsTable();
  } catch (error) {
    console.error('Failed to fetch jobs:', error);
    elements.jobsTbody.innerHTML = `
      <tr>
        <td colspan="7" class="empty-state">
          <i class="fa-solid fa-triangle-exclamation" style="color: var(--danger);"></i>
          <p>Failed to connect to the Scheduler API server.</p>
          <span style="font-size: 0.8rem; color: var(--text-dim);">${error.message}</span>
          <div style="margin-top: 1rem;">
            <button onclick="fetchJobs()" class="btn btn-secondary btn-small">Try Again</button>
            <button onclick="elements.btnSettings.click()" class="btn btn-primary btn-small">Configure API</button>
          </div>
        </td>
      </tr>
    `;
  }
}

// Aggregate and update statistics widgets
function updateStatsSummary() {
  const stats = {
    total: allJobs.length,
    running: 0,
    paused: 0,
    failed: 0
  };
  
  allJobs.forEach(job => {
    if (job.status === 'RUNNING') stats.running++;
    else if (job.status === 'PAUSED') stats.paused++;
    else if (job.status === 'FAILED' || job.status === 'DEAD') stats.failed++;
  });
  
  elements.statTotal.textContent = stats.total;
  elements.statRunning.textContent = stats.running;
  elements.statPaused.textContent = stats.paused;
  elements.statFailed.textContent = stats.failed;
}

// Render jobs to visual table
function renderJobsTable() {
  const query = elements.searchInput.value.toLowerCase().trim();
  const statusFilterVal = elements.statusFilter.value;
  const typeFilterVal = elements.typeFilter.value;
  
  const filteredJobs = allJobs.filter(job => {
    // Search filter
    const matchesSearch = 
      job.name.toLowerCase().includes(query) || 
      (job.description && job.description.toLowerCase().includes(query)) ||
      job.id.toLowerCase().includes(query);
      
    // Status filter
    const matchesStatus = statusFilterVal === 'ALL' || job.status === statusFilterVal;
    
    // Type filter
    const matchesType = typeFilterVal === 'ALL' || job.scheduleType === typeFilterVal;
    
    return matchesSearch && matchesStatus && matchesType;
  });
  
  if (filteredJobs.length === 0) {
    elements.jobsTbody.innerHTML = `
      <tr>
        <td colspan="7" class="empty-state">
          <i class="fa-solid fa-box-open"></i>
          <p>No jobs match the active filters or search terms.</p>
        </td>
      </tr>
    `;
    return;
  }
  
  elements.jobsTbody.innerHTML = '';
  
  filteredJobs.forEach(job => {
    const row = document.createElement('tr');
    
    // Job details cell
    const detailsCell = document.createElement('td');
    detailsCell.className = 'job-title-cell';
    detailsCell.innerHTML = `
      <span class="job-name-txt">${escapeHtml(job.name)}</span>
      <span class="job-desc-txt">${escapeHtml(job.description || 'No description')}</span>
      <span class="job-id-txt">${job.id}</span>
    `;
    row.appendChild(detailsCell);
    
    // Type & Schedule cell
    const scheduleCell = document.createElement('td');
    let scheduleText = '';
    if (job.scheduleType === 'ONE_TIME') {
      scheduleText = '<span class="badge badge-secondary">One-Time</span>';
    } else {
      const typeBadge = job.scheduleType === 'CRON' ? 'badge-info' : 'badge-paused';
      const typeName = job.scheduleType.replace('_', ' ');
      scheduleText = `
        <div style="display: flex; flex-direction: column; gap: 0.25rem;">
          <span class="badge ${typeBadge}">${typeName}</span>
          <span class="cron-badge">${escapeHtml(job.cronExpression || '')}</span>
        </div>
      `;
    }
    scheduleCell.innerHTML = scheduleText;
    row.appendChild(scheduleCell);
    
    // Priority cell
    const priorityCell = document.createElement('td');
    priorityCell.innerHTML = `<span class="priority-txt">${job.priority}</span>`;
    row.appendChild(priorityCell);
    
    // Status cell
    const statusCell = document.createElement('td');
    let statusClass = 'badge-secondary';
    switch (job.status) {
      case 'RUNNING': statusClass = 'badge-success'; break;
      case 'SCHEDULED': statusClass = 'badge-info'; break;
      case 'PAUSED': statusClass = 'badge-paused'; break;
      case 'FAILED': 
      case 'DEAD': statusClass = 'badge-danger'; break;
      case 'CREATED': statusClass = 'badge-secondary'; break;
      case 'CANCELLED': statusClass = 'badge-secondary'; break;
      case 'COMPLETED': statusClass = 'badge-success'; break;
    }
    statusCell.innerHTML = `<span class="badge ${statusClass}">${job.status}</span>`;
    row.appendChild(statusCell);
    
    // Executions stats cell
    const executionsCell = document.createElement('td');
    executionsCell.innerHTML = `
      <div style="display:flex; flex-direction:column; font-size:0.8rem; gap:0.15rem;">
        <span>Retries: <strong>${job.retryCount} / ${job.maxRetries}</strong></span>
      </div>
    `;
    row.appendChild(executionsCell);

    // Timeline cell
    const timelineCell = document.createElement('td');
    timelineCell.className = 'timeline-cell';
    timelineCell.innerHTML = `
      <div><span class="time-label">Last:</span> ${formatTime(job.lastExecutionTime)}</div>
      <div><span class="time-label">Next:</span> ${formatTime(job.nextExecutionTime)}</div>
    `;
    row.appendChild(timelineCell);
    
    // Action buttons cell
    const actionsCell = document.createElement('td');
    actionsCell.className = 'actions-col';
    
    // Determine operations based on current status
    const isPaused = job.status === 'PAUSED';
    const isTerminal = job.status === 'COMPLETED' || job.status === 'CANCELLED' || job.status === 'DEAD';
    const isRunning = job.status === 'RUNNING';
    
    const playPauseIcon = isPaused ? 'fa-play' : 'fa-pause';
    const playPauseTitle = isPaused ? 'Resume Job' : 'Pause Job';
    
    const actionGroup = document.createElement('div');
    actionGroup.className = 'action-btn-group';
    
    // Edit action
    const editBtn = createActionButton('fa-pen-to-square', 'Edit Configuration', () => openJobFormModal(job));
    actionGroup.appendChild(editBtn);

    // Play/Pause action
    const playPauseBtn = createActionButton(playPauseIcon, playPauseTitle, () => togglePlayPause(job));
    playPauseBtn.className += ' btn-play-pause';
    if (isTerminal) playPauseBtn.disabled = true;
    actionGroup.appendChild(playPauseBtn);
    
    // Trigger immediate execution
    const triggerBtn = createActionButton('fa-bolt', 'Trigger execution now', () => triggerJobImmediate(job.id));
    triggerBtn.className += ' btn-trigger-now';
    if (isTerminal || isPaused) triggerBtn.disabled = true;
    actionGroup.appendChild(triggerBtn);
    
    // View execution history logs
    const logsBtn = createActionButton('fa-clock-rotate-left', 'Execution History Logs', () => viewJobHistory(job));
    logsBtn.className += ' btn-view-logs';
    actionGroup.appendChild(logsBtn);
    
    // Delete action
    const deleteBtn = createActionButton('fa-trash-can', 'Delete Job', () => deleteJob(job.id, job.name));
    deleteBtn.className += ' btn-delete';
    actionGroup.appendChild(deleteBtn);
    
    actionsCell.appendChild(actionGroup);
    row.appendChild(actionsCell);
    
    elements.jobsTbody.appendChild(row);
  });
}

// Action Helper
function createActionButton(iconClass, tooltip, clickHandler) {
  const btn = document.createElement('button');
  btn.className = 'action-btn';
  btn.title = tooltip;
  btn.innerHTML = `<i class="fa-solid ${iconClass}"></i>`;
  btn.addEventListener('click', (e) => {
    e.stopPropagation();
    clickHandler();
  });
  return btn;
}

// Format date time helper
function formatTime(value) {
  if (value == null || value === 0) {
    return '<span class="text-dim">-</span>';
  }

  let date;

  if (typeof value === "number") {
    // Backend sends Unix seconds
    date = new Date(value * 1000);
  } else if (!isNaN(value) && value.toString().indexOf('-') === -1) {
    // Numeric string
    date = new Date(parseFloat(value) * 1000);
  } else {
    // ISO string
    date = new Date(value);
  }

  if (isNaN(date.getTime())) {
    return '<span class="text-dim">-</span>';
  }

  const pad = n => String(n).padStart(2, '0');

  return `
    <span class="code-text">
      ${date.getFullYear()}-${pad(date.getMonth()+1)}-${pad(date.getDate())}
      ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}
    </span>
  `;
}

// Play/Pause Action
async function togglePlayPause(job) {
  const action = job.status === 'PAUSED' ? 'resume' : 'pause';
  try {
    const response = await fetch(`${API_URL}/jobs/${job.id}/${action}`, {
      method: 'POST'
    });
    
    if (!response.ok) throw new Error(`Status ${response.status}`);
    
    showToast(`Job "${job.name}" has been ${action}d successfully.`, 'success');
    fetchJobs();
  } catch (error) {
    showToast(`Failed to ${action} job: ${error.message}`, 'error');
  }
}

// Trigger Execution
async function triggerJobImmediate(id) {
  try {
    const response = await fetch(`${API_URL}/jobs/${id}/trigger`, {
      method: 'POST'
    });
    
    if (!response.ok) throw new Error(`Status ${response.status}`);
    
    showToast(`Execution triggered successfully.`, 'success');
    fetchJobs();
  } catch (error) {
    showToast(`Failed to trigger execution: ${error.message}`, 'error');
  }
}

// Delete Job Action
async function deleteJob(id, name) {
  if (!confirm(`Are you sure you want to delete job "${name}"?\nThis cannot be undone.`)) {
    return;
  }
  
  try {
    const response = await fetch(`${API_URL}/jobs/${id}`, {
      method: 'DELETE'
    });
    
    if (!response.ok) throw new Error(`Status ${response.status}`);
    
    showToast(`Job "${name}" deleted.`, 'success');
    fetchJobs();
  } catch (error) {
    showToast(`Failed to delete job: ${error.message}`, 'error');
  }
}

// Form Validation and submission
async function handleFormSubmit(e) {
  e.preventDefault();
  
  const id = elements.formJobId.value;
  const name = elements.jobName.value.trim();
  const description = elements.jobDescription.value.trim();
  const scheduleType = elements.jobScheduleType.value;
  const cronExpression = elements.jobCronExpression.value.trim();
  const priority = parseInt(elements.jobPriority.value, 10);
  const maxRetries = parseInt(elements.jobMaxRetries.value, 10);
  const executionTimeVal = elements.jobExecutionTime.value;
  const payloadVal = elements.jobPayload.value.trim();
  
  // Clean validation errors
  elements.payloadError.textContent = '';
  
  // Validate JSON payload
  try {
    JSON.parse(payloadVal);
  } catch (err) {
    elements.payloadError.textContent = `Invalid JSON: ${err.message}`;
    return;
  }
  
  // Process Next Execution Time to ISO UTC
  let nextExecutionTime = null;
  if (scheduleType !== 'CRON' || executionTimeVal) {
    if (executionTimeVal) {
      nextExecutionTime = new Date(executionTimeVal).toISOString();
    } else if (scheduleType !== 'CRON') {
      elements.payloadError.textContent = 'Next execution time is required for this schedule type.';
      return;
    }
  }

  const payload = {
    name,
    description: description || null,
    payload: payloadVal,
    scheduleType,
    cronExpression: (scheduleType === 'ONE_TIME') ? null : cronExpression,
    priority,
    maxRetries,
    nextExecutionTime
  };
  
  try {
    const url = id ? `${API_URL}/jobs/${id}` : `${API_URL}/jobs`;
    const method = id ? 'PUT' : 'POST';
    
    const response = await fetch(url, {
      method,
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    });
    
    const responseData = await response.json();
    
    if (!response.ok) {
      if (responseData.code === 'validation_failed' && responseData.errors) {
        throw new Error(`Validation failed: ${responseData.errors.join(', ')}`);
      } else {
        throw new Error(responseData.message || `Status ${response.status}`);
      }
    }
    
    showToast(`Job saved successfully!`, 'success');
    closeModal(elements.modalJobForm);
    fetchJobs();
  } catch (error) {
    showToast(`Failed to save job: ${error.message}`, 'error');
  }
}

// View Job History list
async function viewJobHistory(job) {
  elements.historyJobName.textContent = job.name;
  elements.detailJobId.textContent = job.id;
  elements.detailSchedule.innerHTML = `
    Type: <strong style="color:var(--text-main); font-size:0.8rem;">${job.scheduleType}</strong> 
    ${job.cronExpression ? `| Expression: <span class="cron-badge">${job.cronExpression}</span>` : ''}
  `;
  elements.detailPayload.textContent = job.payload;
  
  // Loading state
  elements.historyTbody.innerHTML = `
    <tr>
      <td colspan="6" class="loading-state">
        <i class="fa-solid fa-spinner fa-spin"></i> Loading history records...
      </td>
    </tr>
  `;
  
  openModal(elements.modalHistory);
  
  try {
    const response = await fetch(`${API_URL}/jobs/${job.id}/history?limit=100&offset=0`);
    if (!response.ok) throw new Error(`Status ${response.status}`);
    
    const executions = await response.json();
    renderHistoryTable(executions);
  } catch (error) {
    elements.historyTbody.innerHTML = `
      <tr>
        <td colspan="6" class="empty-state">
          <i class="fa-solid fa-triangle-exclamation" style="color: var(--danger);"></i>
          <p>Failed to retrieve execution records.</p>
          <span style="font-size:0.8rem; color:var(--text-dim);">${error.message}</span>
        </td>
      </tr>
    `;
  }
}

function renderHistoryTable(executions) {
  if (executions.length === 0) {
    elements.historyTbody.innerHTML = `
      <tr>
        <td colspan="6" class="empty-state">
          <i class="fa-solid fa-clock"></i>
          <p>No executions have run for this job yet.</p>
        </td>
      </tr>
    `;
    return;
  }
  
  elements.historyTbody.innerHTML = '';
  
  executions.forEach(exec => {
    const row = document.createElement('tr');
    
    // Execution ID
    const idCell = document.createElement('td');
    idCell.innerHTML = `<span class="code-text" style="font-size:0.75rem;" title="${exec.id}">${exec.id.substring(0,8)}...</span>`;
    row.appendChild(idCell);
    
    // Status
    const statusCell = document.createElement('td');
    let badgeClass = 'badge-secondary';
    switch (exec.status) {
      case 'SUCCEEDED': badgeClass = 'badge-success'; break;
      case 'RUNNING': badgeClass = 'badge-info'; break;
      case 'QUEUED': badgeClass = 'badge-secondary'; break;
      case 'CANCELLED': badgeClass = 'badge-secondary'; break;
      case 'FAILED': badgeClass = 'badge-danger'; break;
      case 'DEAD_LETTERED': badgeClass = 'badge-danger'; break;
    }
    statusCell.innerHTML = `<span class="badge ${badgeClass}">${exec.status}</span>`;
    row.appendChild(statusCell);
    
    // Run Timeline & Duration
    const timelineCell = document.createElement('td');
    const duration = calculateDuration(exec.startedAt, exec.completedAt);
    timelineCell.innerHTML = `
      <div style="font-size:0.8rem; display:flex; flex-direction:column; gap:0.15rem;">
        <span>Start: ${formatTime(exec.startedAt)}</span>
        <span>End: ${formatTime(exec.completedAt)}</span>
        ${duration ? `<span style="color:var(--primary); font-weight:600;">Duration: ${duration}</span>` : ''}
      </div>
    `;
    row.appendChild(timelineCell);
    
    // Executor info
    const executorCell = document.createElement('td');
    executorCell.innerHTML = `<span class="code-text">${escapeHtml(exec.executorId || '-')}</span>`;
    row.appendChild(executorCell);
    
    // Retry count
    const retryCell = document.createElement('td');
    retryCell.innerHTML = `<strong style="font-size:0.9rem;">${exec.retryNumber}</strong>`;
    row.appendChild(retryCell);
    
    // Error / Log message
    const errorCell = document.createElement('td');
    if (exec.errorMessage) {
      errorCell.innerHTML = `<div class="history-error">${escapeHtml(exec.errorMessage)}</div>`;
    } else if (exec.logs) {
      errorCell.innerHTML = `<div class="history-logs">${escapeHtml(exec.logs)}</div>`;
    } else {
      errorCell.innerHTML = `<span class="text-dim">-</span>`;
    }
    row.appendChild(errorCell);
    
    elements.historyTbody.appendChild(row);
  });
}

// Duration helper
function calculateDuration(startStr, endStr) {
  if (!startStr || !endStr) return null;
  const start = new Date(startStr);
  const end = new Date(endStr);
  if (isNaN(start.getTime()) || isNaN(end.getTime())) return null;
  
  const diffMs = end - start;
  if (diffMs < 0) return '0ms';
  
  if (diffMs < 1000) return `${diffMs}ms`;
  
  const diffSec = Math.floor(diffMs / 1000);
  if (diffSec < 60) return `${diffSec}s`;
  
  const diffMin = Math.floor(diffSec / 60);
  const remSec = diffSec % 60;
  return `${diffMin}m ${remSec}s`;
}

// Toast notification helper
function showToast(message, type = 'info') {
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  
  let iconClass = 'fa-circle-info';
  if (type === 'success') iconClass = 'fa-circle-check';
  else if (type === 'warning') iconClass = 'fa-circle-exclamation';
  else if (type === 'error') iconClass = 'fa-circle-xmark';
  
  toast.innerHTML = `
    <i class="fa-solid ${iconClass} toast-icon"></i>
    <span class="toast-message">${escapeHtml(message)}</span>
    <button class="toast-close">&times;</button>
  `;
  
  // Close triggers
  const closeBtn = toast.querySelector('.toast-close');
  closeBtn.addEventListener('click', () => {
    toast.style.animation = 'fadeIn 0.2s ease-out reverse';
    setTimeout(() => toast.remove(), 200);
  });
  
  elements.toastContainer.appendChild(toast);
  
  // Auto-remove after 4 seconds
  setTimeout(() => {
    if (toast.parentElement) {
      toast.style.animation = 'fadeIn 0.2s ease-out reverse';
      setTimeout(() => {
        if (toast.parentElement) toast.remove();
      }, 200);
    }
  }, 4000);
}

// HTML XSS escaping helper
function escapeHtml(unsafe) {
  if (unsafe === null || unsafe === undefined) return '';
  return String(unsafe)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
