'use strict';

const METODOS = ['PIX', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'TED', 'BOLETO'];
const METODO_LABEL = { PIX: 'Pix', CARTAO_CREDITO: 'Crédito', CARTAO_DEBITO: 'Débito', TED: 'TED', BOLETO: 'Boleto' };
const STATUS_LABEL = { APROVADA: 'Aprovada', SUSPEITA: 'Suspeita', NEGADA: 'Negada' };
const STATUS_CLASS = { APROVADA: 'aprovada', SUSPEITA: 'suspeita', NEGADA: 'negada' };
const STATUSES = ['APROVADA', 'SUSPEITA', 'NEGADA'];
const UFS = ['SP', 'RJ', 'MG', 'RS', 'PR', 'SC', 'BA', 'PE', 'CE', 'DF'];
const MAX_LINHAS = 14;

const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 });
const brlExato = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: 2 });
const inteiro = new Intl.NumberFormat('pt-BR');

const estado = {
    total: 0, volumeTotal: 0, aprovadas: 0, suspeitas: 0, negadas: 0,
    contagemStatus: { APROVADA: 0, SUSPEITA: 0, NEGADA: 0 },
    volumePorMetodo: Object.fromEntries(METODOS.map(m => [m, 0]))
};

const barras = {}, valores = {};
const statusRefs = {};

// ---------------------------------------------------------------- Renderizacao

function fmtHora(ms) { return new Date(ms).toLocaleTimeString('pt-BR', { hour12: false }); }

function renderKpis() {
    document.getElementById('stat-volume').textContent = brl.format(estado.volumeTotal);
    const ticket = estado.total ? estado.volumeTotal / estado.total : 0;
    document.getElementById('foot-volume').textContent = 'ticket médio ' + brl.format(ticket);

    document.getElementById('stat-total').textContent = inteiro.format(estado.total);

    const taxa = estado.total ? (estado.aprovadas / estado.total) * 100 : 0;
    document.getElementById('stat-taxa').textContent = taxa.toFixed(1) + '%';
    document.getElementById('foot-taxa').textContent =
        inteiro.format(estado.aprovadas) + ' de ' + inteiro.format(estado.total) + ' aprovadas';

    document.getElementById('stat-bloqueadas').textContent = inteiro.format(estado.suspeitas + estado.negadas);
    document.getElementById('foot-bloqueadas').textContent =
        inteiro.format(estado.suspeitas) + ' suspeitas · ' + inteiro.format(estado.negadas) + ' negadas';
}

function montarStatus() {
    const lista = document.getElementById('status-list');
    lista.innerHTML = '';
    STATUSES.forEach(key => {
        const cls = STATUS_CLASS[key];
        const row = document.createElement('div');
        row.className = 'status-row';
        row.innerHTML = `
            <div class="status-name"><span class="dot ${cls}"></span>${STATUS_LABEL[key]}</div>
            <div class="status-count"><span data-count>0</span><small data-pct>0%</small></div>
            <div class="status-track"><div class="status-fill ${cls}" data-fill></div></div>`;
        lista.appendChild(row);
        statusRefs[key] = {
            count: row.querySelector('[data-count]'),
            pct: row.querySelector('[data-pct]'),
            fill: row.querySelector('[data-fill]')
        };
    });
}

function renderStatus() {
    STATUSES.forEach(key => {
        const n = estado.contagemStatus[key];
        const pct = estado.total ? (n / estado.total) * 100 : 0;
        statusRefs[key].count.textContent = inteiro.format(n);
        statusRefs[key].pct.textContent = pct.toFixed(0) + '%';
        statusRefs[key].fill.style.width = pct + '%';
    });
}

function montarBarras() {
    const viz = document.getElementById('viz');
    viz.innerHTML = '';
    METODOS.forEach(metodo => {
        const col = document.createElement('div');
        col.className = 'metodo';
        col.innerHTML = `
            <div class="metodo-track"><div class="metodo-bar"></div></div>
            <span class="metodo-val">${brl.format(0)}</span>
            <span class="metodo-lbl">${METODO_LABEL[metodo]}</span>`;
        viz.appendChild(col);
        barras[metodo] = col.querySelector('.metodo-bar');
        valores[metodo] = col.querySelector('.metodo-val');
    });
}

function renderBarras() {
    const max = Math.max(1, ...METODOS.map(m => estado.volumePorMetodo[m]));
    METODOS.forEach(m => {
        barras[m].style.height = (estado.volumePorMetodo[m] / max) * 100 + '%';
        valores[m].textContent = brl.format(estado.volumePorMetodo[m]);
    });
}

function criarLinha(v) {
    const row = document.createElement('div');
    row.className = 'txn';
    row.innerHTML = `
        <div class="txn-status"><span class="pill ${STATUS_CLASS[v.status]}" title="${v.motivo || ''}">${STATUS_LABEL[v.status]}</span></div>
        <div class="txn-method">${METODO_LABEL[v.metodo] || v.metodo}</div>
        <div class="txn-uf">${v.uf || ''}</div>
        <div class="txn-id mono">${v.id}</div>
        <div class="txn-amount mono">${brlExato.format(v.valor)}</div>
        <div class="txn-time mono">${fmtHora(v.momentoEpochMs)}</div>`;
    return row;
}

function adicionarAoVivo(v) {
    const feed = document.getElementById('feed');
    const vazio = feed.querySelector('.feed-empty');
    if (vazio) feed.innerHTML = '';
    const row = criarLinha(v);
    row.classList.add('nova');
    feed.prepend(row);
    while (feed.children.length > MAX_LINHAS) feed.removeChild(feed.lastChild);
}

// ---------------------------------------------------------------- Eventos

function contabilizar(v) {
    estado.total++;
    estado.volumeTotal += v.valor;
    estado.contagemStatus[v.status] = (estado.contagemStatus[v.status] || 0) + 1;
    if (v.status === 'APROVADA') estado.aprovadas++;
    else if (v.status === 'SUSPEITA') estado.suspeitas++;
    else estado.negadas++;
    if (estado.volumePorMetodo[v.metodo] !== undefined) estado.volumePorMetodo[v.metodo] += v.valor;
}

function onTransacao(v) {
    contabilizar(v);
    renderKpis();
    renderStatus();
    renderBarras();
    adicionarAoVivo(v);
}

// ---------------------------------------------------------------- Carga e stream

let liveRecebido = false;

async function bootstrap() {
    try {
        const est = await fetch('/api/estatisticas').then(r => r.json());
        estado.total = est.total; estado.volumeTotal = est.volumeTotal;
        estado.aprovadas = est.aprovadas; estado.suspeitas = est.suspeitas; estado.negadas = est.negadas;
        estado.contagemStatus = { APROVADA: est.aprovadas, SUSPEITA: est.suspeitas, NEGADA: est.negadas };
        METODOS.forEach(m => { estado.volumePorMetodo[m] = (est.volumePorMetodo && est.volumePorMetodo[m]) || 0; });
        renderKpis(); renderStatus(); renderBarras();
        const recentes = await fetch('/api/transacoes/recentes').then(r => r.json());
        const feed = document.getElementById('feed');
        if (recentes.length) { feed.innerHTML = ''; recentes.forEach(v => feed.appendChild(criarLinha(v))); }
    } catch (e) { /* backend indisponivel: cai no modo demo */ }
}

function conectarStream() {
    try {
        const fonte = new EventSource('/api/stream');
        fonte.addEventListener('transacao', ev => {
            if (!liveRecebido) { liveRecebido = true; pararDemo(); }
            onTransacao(JSON.parse(ev.data));
        });
    } catch (e) { /* ignore */ }
}

// ---------------------------------------------------------------- Modo demonstracao

let demoTimer = null;
const janelaPorConta = {};

function iniciarDemo() {
    if (liveRecebido || demoTimer) return;
    document.getElementById('demo-chip').hidden = false;
    demoTimer = setInterval(() => {
        const n = 1 + Math.floor(Math.random() * 3);
        for (let i = 0; i < n; i++) onTransacao(gerarTransacao());
    }, 900);
}

function pararDemo() {
    if (demoTimer) { clearInterval(demoTimer); demoTimer = null; }
    document.getElementById('demo-chip').hidden = true;
}

function conta() { return '****' + String(Math.floor(Math.random() * 40)).padStart(4, '0'); }

function sortearValor() {
    const s = Math.random();
    if (s < 0.90) return +(10 + Math.random() * 1990).toFixed(2);
    if (s < 0.98) return +(2000 + Math.random() * 13000).toFixed(2);
    return +(15000 + Math.random() * 65000).toFixed(2);
}

function classificar(conta, valor, momento) {
    const janela = janelaPorConta[conta] || (janelaPorConta[conta] = []);
    janela.push(momento);
    while (janela.length && janela[0] < momento - 10000) janela.shift();
    const velocidade = janela.length > 5;

    if (valor >= 50000) return { status: 'NEGADA', motivo: 'Valor acima do limite permitido' };
    if (valor >= 10000) return { status: 'SUSPEITA', motivo: 'Valor elevado para revisão manual' };
    if (velocidade) return { status: 'SUSPEITA', motivo: 'Múltiplas transações em curto intervalo' };
    return { status: 'APROVADA', motivo: null };
}

function gerarTransacao() {
    const origem = conta();
    const valor = sortearValor();
    const momento = Date.now();
    const r = classificar(origem, valor, momento);
    return {
        id: Math.random().toString(16).slice(2, 10),
        contaOrigem: origem, contaDestino: conta(),
        valor, metodo: METODOS[Math.floor(Math.random() * METODOS.length)],
        status: r.status, motivo: r.motivo,
        uf: UFS[Math.floor(Math.random() * UFS.length)], momentoEpochMs: momento
    };
}

// ---------------------------------------------------------------- Tema

function initTema() {
    const salvo = localStorage.getItem('tema');
    if (salvo) document.documentElement.setAttribute('data-theme', salvo);
    document.getElementById('theme-btn').addEventListener('click', () => {
        const novo = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', novo);
        localStorage.setItem('tema', novo);
    });
}

// ---------------------------------------------------------------- Init

initTema();
montarStatus();
montarBarras();
renderKpis();
bootstrap().then(conectarStream);
setTimeout(() => { if (!liveRecebido) iniciarDemo(); }, 2500);
