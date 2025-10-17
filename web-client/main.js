(() => {
  const hostInput = document.getElementById('hostUrl');
  const nameInput = document.getElementById('playerName');
  const connectBtn = document.getElementById('connectBtn');
  const disconnectBtn = document.getElementById('disconnectBtn');
  const readyBtn = document.getElementById('readyBtn');
  const guessBtn = document.getElementById('guessBtn');
  const guessInput = document.getElementById('guessInput');
  const status = document.getElementById('status');
  const playersList = document.getElementById('players');
  const messages = document.getElementById('messages');
  const canvas = document.getElementById('canvas');
  const ctx = canvas.getContext('2d');

  let socket = null;
  let playerId = `web-${Math.floor(Math.random() * 100000)}`;
  let isReady = false;
  let currentStrokeId = null;

  const jsonEncode = (obj) => JSON.stringify(obj) + '\n';

  const connect = () => {
    const url = hostInput.value.trim();
    if (!url) {
      alert('Enter host websocket URL');
      return;
    }
    socket = new WebSocket(url);
    status.textContent = 'Connecting...';
    socket.onopen = () => {
      status.textContent = 'Connected';
      connectBtn.disabled = true;
      disconnectBtn.disabled = false;
      readyBtn.disabled = false;
      guessBtn.disabled = false;
      socket.send(jsonEncode({
        type: 'join',
        playerId,
        name: nameInput.value.trim() || 'Web Player'
      }));
    };
    socket.onclose = () => {
      status.textContent = 'Disconnected';
      connectBtn.disabled = false;
      disconnectBtn.disabled = true;
      readyBtn.disabled = true;
      guessBtn.disabled = true;
    };
    socket.onerror = (err) => {
      console.error(err);
      status.textContent = 'Error';
    };
    socket.onmessage = (event) => {
      const line = event.data.trim();
      if (!line) return;
      try {
        const message = JSON.parse(line);
        handleMessage(message);
      } catch (err) {
        console.error('Failed to parse message', err);
      }
    };
  };

  const disconnect = () => {
    if (socket) {
      socket.close();
      socket = null;
    }
  };

  const toggleReady = () => {
    isReady = !isReady;
    sendMessage({ type: 'ready', playerId, ready: isReady });
  };

  const sendGuess = () => {
    const guess = guessInput.value.trim();
    if (!guess) return;
    sendMessage({ type: 'guess', playerId, text: guess });
    guessInput.value = '';
  };

  const sendMessage = (msg) => {
    if (!socket || socket.readyState !== WebSocket.OPEN) return;
    socket.send(jsonEncode(msg));
  };

  const handleMessage = (message) => {
    switch (message.type) {
      case 'player_joined':
        addPlayer(message.player);
        break;
      case 'lobby_update':
        renderPlayers(message.players);
        break;
      case 'stroke_start':
        startStroke(message);
        break;
      case 'stroke_point':
        extendStroke(message);
        break;
      case 'stroke_end':
        currentStrokeId = null;
        break;
      case 'guess':
        addGuess(message.playerId, message.text, false);
        break;
      case 'correct_guess':
        addGuess(message.playerId, message.word, true);
        break;
      default:
        break;
    }
  };

  const addPlayer = (player) => {
    const li = document.createElement('li');
    li.textContent = `${player.name} (${player.playerId})`;
    li.id = `player-${player.playerId}`;
    playersList.appendChild(li);
  };

  const renderPlayers = (players) => {
    playersList.innerHTML = '';
    players.forEach(addPlayer);
  };

  const addGuess = (player, text, correct) => {
    const div = document.createElement('div');
    div.className = correct ? 'correct' : '';
    div.textContent = `${player}: ${text}`;
    messages.appendChild(div);
    messages.scrollTop = messages.scrollHeight;
  };

  const startStroke = (msg) => {
    ctx.beginPath();
    ctx.strokeStyle = msg.color;
    ctx.lineWidth = msg.thickness;
    ctx.lineJoin = 'round';
    ctx.lineCap = 'round';
    ctx.moveTo(msg.x, msg.y);
    currentStrokeId = msg.strokeId;
  };

  const extendStroke = (msg) => {
    if (currentStrokeId !== msg.strokeId) {
      ctx.beginPath();
      ctx.moveTo(msg.x, msg.y);
      currentStrokeId = msg.strokeId;
    } else {
      ctx.lineTo(msg.x, msg.y);
      ctx.stroke();
    }
  };

  const handleCanvasPointer = () => {
    let drawing = false;
    canvas.addEventListener('pointerdown', (event) => {
      drawing = true;
      const rect = canvas.getBoundingClientRect();
      const x = event.clientX - rect.left;
      const y = event.clientY - rect.top;
      const strokeId = `ws-${Date.now()}`;
      currentStrokeId = strokeId;
      sendMessage({
        type: 'stroke_start',
        strokeId,
        playerId,
        color: '#000000',
        thickness: 4,
        x,
        y,
        timestamp: Date.now()
      });
    });
    canvas.addEventListener('pointermove', (event) => {
      if (!drawing) return;
      const rect = canvas.getBoundingClientRect();
      const x = event.clientX - rect.left;
      const y = event.clientY - rect.top;
      sendMessage({
        type: 'stroke_point',
        strokeId: currentStrokeId,
        x,
        y,
        timestamp: Date.now()
      });
    });
    window.addEventListener('pointerup', () => {
      if (!drawing) return;
      drawing = false;
      sendMessage({ type: 'stroke_end', strokeId: currentStrokeId });
      currentStrokeId = null;
    });
  };

  connectBtn.addEventListener('click', connect);
  disconnectBtn.addEventListener('click', disconnect);
  readyBtn.addEventListener('click', toggleReady);
  guessBtn.addEventListener('click', sendGuess);
  handleCanvasPointer();
})();
