/**
 * Hotspot Skribble — Web Client v1.0
 *
 * Connects to an Android host via WebSocket.
 * Supports: joining lobby, drawing, guessing, live canvas sync, scoring.
 */
(() => {
  'use strict';

  // ─── DOM References ───
  const $ = (id) => document.getElementById(id);
  const screens = {
    connect: $('screen-connect'),
    lobby: $('screen-lobby'),
    game: $('screen-game'),
    scores: $('screen-scores'),
  };

  const dom = {
    hostUrl: $('hostUrl'),
    playerName: $('playerName'),
    connectBtn: $('connectBtn'),
    disconnectBtn: $('disconnectBtn'),
    status: $('status'),
    roomTitle: $('roomTitle'),
    players: $('players'),
    readyBtn: $('readyBtn'),
    canvas: $('canvas'),
    drawerLabel: $('drawerLabel'),
    roundLabel: $('roundLabel'),
    timerDisplay: $('timerDisplay'),
    secretWordBanner: $('secretWordBanner'),
    secretWord: $('secretWord'),
    drawingTools: $('drawingTools'),
    colorPalette: $('colorPalette'),
    thicknessSlider: $('thicknessSlider'),
    thicknessValue: $('thicknessValue'),
    undoBtn: $('undoBtn'),
    clearBtn: $('clearBtn'),
    guessSection: $('guessSection'),
    messages: $('messages'),
    guessInput: $('guessInput'),
    guessBtn: $('guessBtn'),
    scoreboard: $('scoreboard'),
    scoresTitle: $('scoresTitle'),
    revealedWord: $('revealedWord'),
    finalScores: $('finalScores'),
  };

  // ─── State ───
  let socket = null;
  let playerId = `web-${Date.now()}-${Math.floor(Math.random() * 10000)}`;
  let isReady = false;
  let isDrawer = false;
  let currentStrokeId = null;
  let selectedColor = '#000000';
  let thickness = 12;
  let drawing = false;
  let players = [];
  let currentRoundNumber = '?';
  let totalRoundsCount = '?';

  // Canvas context
  const ctx = dom.canvas.getContext('2d');

  // Stroke history for redraws
  let strokeHistory = [];

  // ─── Color Palette ───
  const COLORS = [
    '#000000', '#808080', '#FFFFFF', '#FF5722', '#E91E63', '#FFC107',
    '#FF9800', '#4CAF50', '#2196F3', '#9C27B0', '#795548', '#00BCD4',
  ];

  // ─── Screen Management ───
  function showScreen(name) {
    Object.values(screens).forEach((s) => s.classList.remove('active'));
    screens[name].classList.add('active');
  }

  // ─── Canvas Setup ───
  function resizeCanvas() {
    const wrapper = dom.canvas.parentElement;
    const rect = wrapper.getBoundingClientRect();
    const dpr = window.devicePixelRatio || 1;
    dom.canvas.width = rect.width * dpr;
    dom.canvas.height = rect.height * dpr;
    ctx.scale(dpr, dpr);
    dom.canvas._cssWidth = rect.width;
    dom.canvas._cssHeight = rect.height;
    redrawAllStrokes();
  }

  function redrawAllStrokes() {
    ctx.clearRect(0, 0, dom.canvas._cssWidth || 600, dom.canvas._cssHeight || 400);
    strokeHistory.forEach((stroke) => drawStrokePath(stroke));
  }

  function drawStrokePath(stroke) {
    if (!stroke.points || stroke.points.length === 0) return;
    const w = dom.canvas._cssWidth || 600;
    const h = dom.canvas._cssHeight || 400;
    ctx.beginPath();
    ctx.strokeStyle = stroke.color || '#000000';
    ctx.lineWidth = stroke.thickness || 4;
    ctx.lineJoin = 'round';
    ctx.lineCap = 'round';

    const first = stroke.points[0];
    ctx.moveTo(first.x * w, first.y * h);

    if (stroke.points.length === 1) {
      ctx.lineTo(first.x * w + 0.1, first.y * h + 0.1);
    } else if (stroke.points.length === 2) {
      ctx.lineTo(stroke.points[1].x * w, stroke.points[1].y * h);
    } else {
      for (let i = 1; i < stroke.points.length; i++) {
        const prev = stroke.points[i - 1];
        const curr = stroke.points[i];
        const midX = (prev.x + curr.x) / 2 * w;
        const midY = (prev.y + curr.y) / 2 * h;
        ctx.quadraticCurveTo(prev.x * w, prev.y * h, midX, midY);
      }
      const last = stroke.points[stroke.points.length - 1];
      ctx.lineTo(last.x * w, last.y * h);
    }
    ctx.stroke();
  }

  // ─── WebSocket ───
  function connect() {
    const url = dom.hostUrl.value.trim();
    const name = dom.playerName.value.trim() || 'Web Player';
    if (!url) {
      dom.status.textContent = 'Enter host address';
      dom.status.className = 'status-text error';
      return;
    }

    dom.status.textContent = 'Connecting…';
    dom.status.className = 'status-text';
    dom.connectBtn.disabled = true;

    try {
      socket = new WebSocket(url);
    } catch (e) {
      dom.status.textContent = 'Invalid URL';
      dom.status.className = 'status-text error';
      dom.connectBtn.disabled = false;
      return;
    }

    socket.onopen = () => {
      dom.status.textContent = 'Connected ✓';
      dom.status.className = 'status-text connected';
      send({ type: 'join', playerId, name });
      showScreen('lobby');
    };

    socket.onclose = () => {
      dom.status.textContent = 'Disconnected';
      dom.status.className = 'status-text';
      dom.connectBtn.disabled = false;
      showScreen('connect');
      resetGameState();
    };

    socket.onerror = () => {
      dom.status.textContent = 'Connection failed';
      dom.status.className = 'status-text error';
      dom.connectBtn.disabled = false;
    };

    socket.onmessage = (event) => {
      const line = event.data.trim();
      if (!line) return;
      try {
        handleMessage(JSON.parse(line));
      } catch (e) {
        console.warn('Parse error:', e);
      }
    };
  }

  function disconnect() {
    if (socket) {
      socket.close();
      socket = null;
    }
  }

  function send(msg) {
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(msg) + '\n');
    }
  }

  function resetGameState() {
    isReady = false;
    isDrawer = false;
    currentStrokeId = null;
    drawing = false;
    strokeHistory = [];
    players = [];
    currentRoundNumber = '?';
    totalRoundsCount = '?';
  }

  // ─── Message Handler ───
  function handleMessage(msg) {
    switch (msg.type) {
      case 'player_joined': {
        const joinedName = msg.player?.name || 'Someone';
        // Show a transient notification in the lobby player list area
        const note = document.createElement('div');
        note.className = 'msg system';
        note.textContent = `${joinedName} joined`;
        note.style.cssText = 'text-align:center;padding:4px;font-size:0.85rem;color:var(--teal);';
        dom.players.parentElement?.appendChild(note);
        setTimeout(() => note.remove(), 3000);
        break;
      }

      case 'lobby_update':
        players = msg.players || [];
        renderPlayers(players);
        break;

      case 'round_start':
        showScreen('game');
        strokeHistory = [];
        resizeCanvas();
        isDrawer = msg.drawerId === playerId;
        dom.drawerLabel.textContent = `✏️ Drawing: ${getPlayerName(msg.drawerId)}`;
        // roundNumber/totalRounds aren't in the message; track locally
        if (currentRoundNumber === '?') currentRoundNumber = 1; else currentRoundNumber++;
        dom.roundLabel.textContent = `Round ${currentRoundNumber}`;
        dom.secretWordBanner.classList.add('hidden');
        dom.drawingTools.classList.toggle('hidden', !isDrawer);
        dom.guessSection.classList.toggle('hidden', isDrawer);
        dom.messages.innerHTML = '';
        updateScoreboard();
        break;

      case 'secret_word_assigned':
        if (msg.drawerId === playerId) {
          dom.secretWord.textContent = `🖊️ Draw: ${msg.hash || '???'}`;
          dom.secretWordBanner.classList.remove('hidden');
        }
        break;

      case 'timer_update': {
        const t = msg.secondsRemaining ?? 0;
        dom.timerDisplay.textContent = `⏱ ${t}s`;
        dom.timerDisplay.classList.toggle('urgent', t <= 10);
        break;
      }

      case 'stroke_start':
        handleStrokeStart(msg);
        break;

      case 'stroke_point':
        handleStrokePoint(msg);
        break;

      case 'stroke_end':
        break;

      case 'stroke_remove':
        strokeHistory = strokeHistory.filter((s) => s.strokeId !== msg.strokeId);
        redrawAllStrokes();
        break;

      case 'stroke_points':
        // Batch stroke update — Android sends pts as List<List<Float>>, e.g. [[x,y], [x,y,t]]
        if (msg.strokeId) {
          let stroke = strokeHistory.find((s) => s.strokeId === msg.strokeId);
          if (!stroke) {
            // Batch creates stroke only if we missed the stroke_start; inherit from existing or default
            const ref = strokeHistory.length > 0 ? strokeHistory[strokeHistory.length - 1] : null;
            stroke = { strokeId: msg.strokeId, color: ref?.color || '#000', thickness: ref?.thickness || 4, points: [] };
            strokeHistory.push(stroke);
          }
          const rawPts = msg.pts || msg.points || [];
          rawPts.forEach((p) => {
            if (Array.isArray(p)) {
              // Array format: [x, y] or [x, y, timestamp]
              stroke.points.push(normalizePoint({ x: p[0], y: p[1] }));
            } else {
              // Object format: {x, y}
              stroke.points.push(normalizePoint(p));
            }
          });
          redrawAllStrokes();
        }
        break;

      case 'canvas_clear':
        strokeHistory = [];
        redrawAllStrokes();
        break;

      case 'guess':
        addGuessMessage(msg.playerId, msg.text, false);
        break;

      case 'correct_guess':
        addGuessMessage(msg.playerId, `Guessed correctly! 🎉`, true);
        // Update the guesser's score using msg.points (total score for that player)
        if (typeof msg.points === 'number') {
          players = players.map((p) => {
            if (p.playerId === msg.playerId) return { ...p, score: msg.points };
            return p;
          });
          updateScoreboard();
        }
        break;

      case 'round_end':
        showScreen('scores');
        dom.scoresTitle.textContent = '📊 Round Scores';
        if (msg.word) {
          dom.revealedWord.textContent = `💡 The word was: ${msg.word}`;
          dom.revealedWord.classList.remove('hidden');
        }
        renderFinalScores(msg.scores);
        break;

      case 'game_end':
        showScreen('scores');
        dom.scoresTitle.textContent = '🏆 Game Over!';
        dom.revealedWord.classList.add('hidden');
        currentRoundNumber = '?';
        renderFinalScores(msg.scores);
        break;

      case 'player_left':
        addSystemMessage(`${msg.name || 'Someone'} disconnected`);
        break;

      case 'heartbeat':
        break;

      default:
        console.log('Unknown message type:', msg.type);
    }
  }

  // ─── Stroke Handling (receiving) ───
  function normalizePoint(msg) {
    // Points from Android use absolute pixel coords; normalize to 0..1
    // If values > 1, assume absolute coords based on typical canvas dimensions
    const w = 600; // Reference width from Android canvas
    const h = 428; // 600 / 1.4 aspect ratio
    if (msg.x > 1 || msg.y > 1) {
      return { x: msg.x / w, y: msg.y / h };
    }
    return { x: msg.x, y: msg.y };
  }

  function handleStrokeStart(msg) {
    const stroke = {
      strokeId: msg.strokeId,
      color: msg.color || '#000000',
      thickness: msg.thickness || 4,
      points: [normalizePoint(msg)],
    };
    strokeHistory.push(stroke);
    drawStrokePath(stroke);
  }

  function handleStrokePoint(msg) {
    const stroke = strokeHistory.find((s) => s.strokeId === msg.strokeId);
    if (!stroke) return;
    stroke.points.push(normalizePoint(msg));
    // Incremental draw for performance
    redrawAllStrokes();
  }

  // ─── Drawing (sending) ───
  function getCanvasPos(event) {
    const rect = dom.canvas.getBoundingClientRect();
    const x = (event.clientX ?? event.touches?.[0]?.clientX ?? 0) - rect.left;
    const y = (event.clientY ?? event.touches?.[0]?.clientY ?? 0) - rect.top;
    return { x, y };
  }

  function startDrawing(event) {
    if (!isDrawer) return;
    event.preventDefault();
    drawing = true;
    const pos = getCanvasPos(event);
    currentStrokeId = `ws-${Date.now()}-${Math.floor(Math.random() * 10000)}`;
    send({
      type: 'stroke_start',
      strokeId: currentStrokeId,
      playerId,
      color: selectedColor,
      thickness,
      x: pos.x,
      y: pos.y,
      timestamp: Date.now(),
    });
  }

  function moveDrawing(event) {
    if (!drawing || !isDrawer) return;
    event.preventDefault();
    const pos = getCanvasPos(event);
    send({
      type: 'stroke_point',
      strokeId: currentStrokeId,
      x: pos.x,
      y: pos.y,
      timestamp: Date.now(),
    });
  }

  function stopDrawing() {
    if (!drawing) return;
    drawing = false;
    if (currentStrokeId) {
      send({ type: 'stroke_end', strokeId: currentStrokeId });
      currentStrokeId = null;
    }
  }

  // ─── UI Rendering ───
  function renderPlayers(playerList) {
    dom.players.innerHTML = '';
    playerList.forEach((p) => {
      const li = document.createElement('li');
      const name = document.createElement('span');
      name.className = 'player-name';
      name.textContent = p.playerId === playerId ? `👉 ${p.name}` : p.name;
      li.appendChild(name);

      const badges = document.createElement('span');
      if (p.isHost) {
        const badge = document.createElement('span');
        badge.className = 'player-badge';
        badge.textContent = '👑 Host';
        badges.appendChild(badge);
      }
      if (p.isReady) {
        const badge = document.createElement('span');
        badge.className = 'player-badge';
        badge.textContent = '✅';
        badge.style.marginLeft = '4px';
        badges.appendChild(badge);
      }
      li.appendChild(badges);

      const score = document.createElement('span');
      score.className = 'player-score';
      score.textContent = `${p.score ?? 0} pts`;
      li.appendChild(score);

      dom.players.appendChild(li);
    });
  }

  function updateScoreboard() {
    const sorted = [...players].sort((a, b) => (b.score ?? 0) - (a.score ?? 0)).slice(0, 5);
    dom.scoreboard.innerHTML = '';
    sorted.forEach((p) => {
      const item = document.createElement('div');
      item.className = 'score-item';
      item.innerHTML = `<div class="score-name">${escapeHtml(p.name)}</div><div class="score-value">${p.score ?? 0}</div>`;
      dom.scoreboard.appendChild(item);
    });
  }

  function renderFinalScores(scores) {
    dom.finalScores.innerHTML = '';
    let sorted;
    if (Array.isArray(scores)) {
      sorted = [...scores].sort((a, b) => (b.score ?? 0) - (a.score ?? 0));
    } else {
      sorted = [...players].sort((a, b) => (b.score ?? 0) - (a.score ?? 0));
    }

    sorted.forEach((p, i) => {
      const row = document.createElement('div');
      row.className = 'score-row';
      row.style.animationDelay = `${i * 80}ms`;
      const medal = i === 0 ? '🥇' : i === 1 ? '🥈' : i === 2 ? '🥉' : `${i + 1}.`;
      row.innerHTML = `
        <span class="rank">${medal}</span>
        <span class="name">${escapeHtml(p.name)}</span>
        <span class="pts">${p.score ?? 0} pts</span>
      `;
      dom.finalScores.appendChild(row);
    });
  }

  function addGuessMessage(pid, text, correct) {
    const div = document.createElement('div');
    div.className = `msg${correct ? ' correct' : ''}`;
    div.innerHTML = `<span class="sender">${escapeHtml(getPlayerName(pid))}</span>${escapeHtml(text)}`;
    dom.messages.appendChild(div);
    dom.messages.scrollTop = dom.messages.scrollHeight;
  }

  function addSystemMessage(text) {
    const div = document.createElement('div');
    div.className = 'msg system';
    div.textContent = text;
    dom.messages.appendChild(div);
    dom.messages.scrollTop = dom.messages.scrollHeight;
  }

  function getPlayerName(pid) {
    const p = players.find((p) => p.playerId === pid);
    return p?.name || pid || 'Unknown';
  }

  function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  // ─── Color Palette Setup ───
  function setupColorPalette() {
    COLORS.forEach((color) => {
      const swatch = document.createElement('div');
      swatch.className = `color-swatch${color === selectedColor ? ' active' : ''}`;
      swatch.style.background = color;
      if (color === '#FFFFFF') {
        swatch.style.border = '2px solid #ccc';
      }
      swatch.addEventListener('click', () => {
        selectedColor = color;
        document.querySelectorAll('.color-swatch').forEach((s) => s.classList.remove('active'));
        swatch.classList.add('active');
      });
      dom.colorPalette.appendChild(swatch);
    });
  }

  // ─── Event Listeners ───
  dom.connectBtn.addEventListener('click', connect);
  dom.disconnectBtn.addEventListener('click', disconnect);

  dom.readyBtn.addEventListener('click', () => {
    isReady = !isReady;
    send({ type: 'ready', playerId, ready: isReady });
    dom.readyBtn.textContent = isReady ? '🔄 Unready' : '✅ Toggle Ready';
  });

  dom.guessBtn.addEventListener('click', () => {
    const text = dom.guessInput.value.trim();
    if (!text) return;
    send({ type: 'guess', playerId, text, timestamp: Date.now() });
    dom.guessInput.value = '';
  });

  dom.guessInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') dom.guessBtn.click();
  });

  dom.hostUrl.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') dom.connectBtn.click();
  });

  dom.thicknessSlider.addEventListener('input', () => {
    thickness = parseInt(dom.thicknessSlider.value, 10);
    dom.thicknessValue.textContent = `${thickness}px`;
  });

  dom.undoBtn.addEventListener('click', () => {
    if (strokeHistory.length > 0) {
      const last = strokeHistory.pop();
      send({ type: 'stroke_remove', strokeId: last.strokeId });
      redrawAllStrokes();
    }
  });

  dom.clearBtn.addEventListener('click', () => {
    strokeHistory = [];
    send({ type: 'canvas_clear' });
    redrawAllStrokes();
  });

  // Canvas pointer events (mouse + touch)
  dom.canvas.addEventListener('pointerdown', startDrawing);
  dom.canvas.addEventListener('pointermove', moveDrawing);
  dom.canvas.addEventListener('pointerup', stopDrawing);
  dom.canvas.addEventListener('pointerleave', stopDrawing);
  dom.canvas.addEventListener('pointercancel', stopDrawing);

  // Prevent scroll while drawing
  dom.canvas.addEventListener('touchmove', (e) => {
    if (drawing) e.preventDefault();
  }, { passive: false });

  // Resize canvas on window resize
  let resizeTimeout;
  window.addEventListener('resize', () => {
    clearTimeout(resizeTimeout);
    resizeTimeout = setTimeout(resizeCanvas, 100);
  });

  // ─── Initialize ───
  setupColorPalette();

  // Load saved values from sessionStorage
  const savedUrl = sessionStorage.getItem('skidl_url');
  const savedName = sessionStorage.getItem('skidl_name');
  if (savedUrl) dom.hostUrl.value = savedUrl;
  if (savedName) dom.playerName.value = savedName;

  // Save on change
  dom.hostUrl.addEventListener('input', () => sessionStorage.setItem('skidl_url', dom.hostUrl.value));
  dom.playerName.addEventListener('input', () => sessionStorage.setItem('skidl_name', dom.playerName.value));
})();
