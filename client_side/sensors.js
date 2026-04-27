(() => {
  const DEFAULT_SOURCES = [
    { key: 'source-a', label: 'Sense HAT PI A - Living Room', baseUrl: 'https://sensors.example.com/sensehat-a' },
    { key: 'source-b', label: 'Sense HAT PI B - Studio', baseUrl: 'https://sensors.example.com/sensehat-b' }
  ];

   const SOURCES = Array.isArray(window.SENSEIT_SOURCES) && window.SENSEIT_SOURCES.length === 2
    ? window.SENSEIT_SOURCES
    : DEFAULT_SOURCES;

  const REFRESH_MS = Number(window.SENSEIT_REFRESH_MS) > 0 ? Number(window.SENSEIT_REFRESH_MS) : 10000;
  const API_TIMEOUT_MS = Number(window.SENSEIT_API_TIMEOUT_MS) > 0
    ? Number(window.SENSEIT_API_TIMEOUT_MS)
    : 1000;

  const ENDPOINTS = {
    temperature: '/api/get_temperature',
    humidity: '/api/get_humidity',
    pressure: '/api/get_pressure',
    north: '/api/get_north'
  };

  const formatters = {
    temperature: (value) => `${value.toFixed(1)}°C`,
    humidity: (value) => `${value.toFixed(1)}%`,
    pressure: (value) => `${value.toFixed(1)} hPa`,
    north: (value) => `${normalizeDegrees(value).toFixed(1)}°`
  };

  const cardNodes = Array.from(document.querySelectorAll('[data-source-card]'));
  const refreshButton = document.querySelector('[data-refresh-button]');
  const refreshLabel = document.querySelector('[data-refresh-label]');
  const globalStatus = document.querySelector('[data-global-status]');
  let pollHandle = null;

  function normalizeDegrees(value) {
    const normalized = ((Number(value) % 360) + 360) % 360;
    return Number.isFinite(normalized) ? normalized : 0;
  }

  function cardinalDirection(degrees) {
    const value = normalizeDegrees(degrees);
    const labels = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
    return labels[Math.round(value / 45) % 8];
  }

  function setGlobalStatus(message) {
    if (globalStatus) {
      globalStatus.textContent = message;
    }
  }

  function buildCompassTicks(container) {
    if (!container || container.dataset.ticksBuilt === 'true') {
      return;
    }

    for (let step = 0; step < 24; step += 1) {
      const tick = document.createElement('span');
      tick.className = `compass-tick${step % 6 === 0 ? ' major' : ''}`;
      tick.style.transform = `rotate(${step * 15}deg)`;
      container.appendChild(tick);
    }

    container.dataset.ticksBuilt = 'true';
  }

  function setCardState(card, state, message) {
    const badge = card.querySelector('[data-state-badge]');
    const statusText = card.querySelector('[data-status-text]');
    if (badge) badge.dataset.state = state;
    if (statusText) statusText.textContent = message;
  }

  function updateLastUpdated(card, date = new Date()) {
    const node = card.querySelector('[data-last-updated]');
    if (!node) return;
    node.textContent = date.toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  }

  function setMetric(card, metric, value) {
    const valueNode = card.querySelector(`[data-metric-value="${metric}"]`);
    if (valueNode) {
      valueNode.textContent = formatters[metric](value);
    }
  }

  function setCompass(card, degrees) {
    const normalized = normalizeDegrees(degrees);
    const arrowWrap = card.querySelector('[data-compass-arrow]');
    const degreeNode = card.querySelector('[data-compass-degrees]');
    const directionNode = card.querySelector('[data-compass-direction]');

    if (arrowWrap) {
      arrowWrap.style.transform = `rotate(${normalized}deg)`;
    }

    if (degreeNode) {
      degreeNode.textContent = `${normalized.toFixed(1)}°`;
    }

    if (directionNode) {
      directionNode.textContent = cardinalDirection(normalized);
    }
  }

  async function fetchNumericValue(url, timeout = API_TIMEOUT_MS) {
    const controller = new AbortController();
    const timeoutHandle = window.setTimeout(() => controller.abort(), timeout);

    try {
      const response = await fetch(url, {
        headers: { Accept: 'application/json' },
        cache: 'no-store',
        signal: controller.signal
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();

      if (typeof data === 'number') {
        return data;
      }

      if (typeof data === 'string' && data.trim() !== '' && !Number.isNaN(Number(data))) {
        return Number(data);
      }

      if (data && typeof data === 'object') {
        const candidate = Object.values(data).find(
          (value) => typeof value === 'number' || (typeof value === 'string' && !Number.isNaN(Number(value)))
        );

        if (candidate !== undefined) {
          return Number(candidate);
        }
      }

      throw new Error('Invalid numeric payload');
    } catch (error) {
      if (error.name === 'AbortError') {
        throw new Error(`Timed out after ${Math.round(timeout / 1000)}s`);
      }
      throw error;
    } finally {
      window.clearTimeout(timeoutHandle);
    }
  }

  async function loadSource(card, source) {
    const sourceNameNode = card.querySelector('[data-source-name]');
    const sourceUrlNode = card.querySelector('[data-source-url]');
    const tickContainer = card.querySelector('[data-compass-ticks]');

    if (sourceNameNode) sourceNameNode.textContent = source.label;
    if (sourceUrlNode) sourceUrlNode.textContent = source.baseUrl;
    buildCompassTicks(tickContainer);
    setCardState(card, 'loading', `Waiting up to ${Math.round(API_TIMEOUT_MS / 1000)}s per API response…`);

    try {
      const [temperature, humidity, pressure, north] = await Promise.all([
        fetchNumericValue(`${source.baseUrl}${ENDPOINTS.temperature}`),
        fetchNumericValue(`${source.baseUrl}${ENDPOINTS.humidity}`),
        fetchNumericValue(`${source.baseUrl}${ENDPOINTS.pressure}`),
        fetchNumericValue(`${source.baseUrl}${ENDPOINTS.north}`)
      ]);

      setMetric(card, 'temperature', temperature);
      setMetric(card, 'humidity', humidity);
      setMetric(card, 'pressure', pressure);
      setMetric(card, 'north', north);
      setCompass(card, north);
      updateLastUpdated(card);
      setCardState(card, 'online', 'Live feed healthy');
      return true;
    } catch (error) {
      setCardState(card, 'offline', `Source unavailable: ${error.message}`);
      updateLastUpdated(card);
      ['temperature', 'humidity', 'pressure', 'north'].forEach((metric) => {
        const node = card.querySelector(`[data-metric-value="${metric}"]`);
        if (node) node.textContent = '—';
      });
      setCompass(card, 0);
      const directionNode = card.querySelector('[data-compass-direction]');
      if (directionNode) directionNode.textContent = 'Offline';
      throw error;
    }
  }

  async function refreshAll() {
    if (refreshButton) {
      refreshButton.disabled = true;
      refreshButton.textContent = 'Refreshing…';
    }

    setGlobalStatus(`Polling both Sense HAT sources. Each API call waits up to ${Math.round(API_TIMEOUT_MS / 1000)}s for a response.`);

    const settled = await Promise.allSettled(
      SOURCES.map((source, index) => loadSource(cardNodes[index], source))
    );

    const onlineCount = settled.filter((result) => result.status === 'fulfilled').length;

    if (onlineCount === SOURCES.length) {
      setGlobalStatus('Both sensor sources are online.');
    } else if (onlineCount === 0) {
      setGlobalStatus('No sensor source responded. Check API reachability or Nginx proxy settings.');
    } else {
      setGlobalStatus(`${onlineCount} of ${SOURCES.length} sensor sources responded.`);
    }

    if (refreshButton) {
      refreshButton.disabled = false;
      refreshButton.textContent = 'Refresh now';
    }
  }

  function startPolling() {
    if (pollHandle) {
      window.clearInterval(pollHandle);
    }

    refreshAll();
    pollHandle = window.setInterval(refreshAll, REFRESH_MS);
    if (refreshLabel) {
      refreshLabel.textContent = `Auto-refresh every ${Math.round(REFRESH_MS / 60000)}m · ${Math.round(API_TIMEOUT_MS / 1000)}s response timeout`;
    }
  }

  refreshButton?.addEventListener('click', refreshAll);
  startPolling();
})();
