(() => {
    const notice = document.getElementById('orderLiveNotice');
    if (!notice || !window.WebSocket) return;
    let socket, stopped = false, retry = 2000, pending = '';
    const connect = () => {
        if (stopped) return;
        socket = new WebSocket(`${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws`);
        socket.onopen = () => { pending = ''; socket.send('CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n\0'); };
        socket.onmessage = event => {
            pending += event.data;
            let end;
            while ((end = pending.indexOf('\0')) >= 0) {
                const frame = pending.slice(0, end).replace(/^\n+/, '');
                pending = pending.slice(end + 1);
                if (frame.startsWith('CONNECTED')) {
                    retry = 2000;
                    socket.send('SUBSCRIBE\nid:orders\ndestination:/user/queue/notifications\nack:auto\n\n\0');
                } else if (frame.startsWith('MESSAGE')) {
                    try {
                        const payload = JSON.parse(frame.slice(frame.indexOf('\n\n') + 2));
                        if (payload.type !== 'NEW_ORDER') continue;
                        notice.replaceChildren(document.createTextNode(`${payload.title}: ${payload.message} `));
                        const link = document.createElement('a');
                        link.href = location.pathname.startsWith('/admin/') ? '/admin/orders' : '/staff/orders';
                        link.textContent = 'Xem danh sách đơn';
                        notice.append(link); notice.hidden = false;
                    } catch (_) { /* Ignore unrelated broker frames. */ }
                }
            }
        };
        socket.onclose = () => { if (!stopped) { setTimeout(connect, retry); retry = Math.min(retry * 2, 60000); } };
        socket.onerror = () => socket.close();
    };
    window.addEventListener('pagehide', () => { stopped = true; if (socket) socket.close(); });
    connect();
})();
