'use strict';

const METODOS = ['PIX', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'TED', 'BOLETO'];
const METODO_LABEL = {
    PIX: 'Pix',
    CARTAO_CREDITO: 'Credito',
    CARTAO_DEBITO: 'Debito',
    TED: 'TED',
    BOLETO: 'Boleto'
};
const STATUS_LABEL = { APROVADA: 'Aprovada', SUSPEITA: 'Suspeita', NEGADA: 'Negada' };
const STATUS_CLASS = { APROVADA: 'aprovada', SUSPEITA: 'suspeita', NEGADA: 'negada' };

const MAX_LINHAS = 12;

const brlCompacto = new Intl.NumberFormat('pt-BR', {
    style: 'currency', currency: 'BRL', maximumFractionDigits: 0
});
const brlExato = new Intl.NumberFormat('pt-BR', {
    style: 'currency', currency: 'BRL', minimumFractionDigits: 2
});
const inteiro = new Intl.NumberFormat('pt-BR');

const estado = {
    total: 0,
    volumeTotal: 0,
    aprovadas: 0,
    suspeitas: 0,
    negadas: 0,
    volumePorMetodo: Object.fromEntries(METODOS.map(m => [m, 0]))
};

const barras = {};
const valores = {};

// --- Renderizacao ---------------------------------------------------------

function fmtHora(epochMs) {
    return new Date(epochMs).toLocaleTimeString('pt-BR', { hour12: false });
}

function renderStats() {
    document.getElementById('stat-volume').textContent = brlCompacto.format(estado.volumeTotal);
    document.getElementById('stat-total').textContent = inteiro.format(estado.total);
    const taxa = estado.total === 0 ? 0 : (estado.aprovadas / estado.total) * 100;
    document.getElementById('stat-taxa').textContent = taxa.toFixed(1) + '%';
    document.getElementById('stat-bloqueadas').textContent = inteiro.format(estado.suspeitas + estado.negadas);
}

function montarBarras() {
    const viz = document.getElementById('viz');
    viz.innerHTML = '';
    METODOS.forEach(metodo => {
        const coluna = document.createElement('div');
        coluna.className = 'metodo';

        const track = document.createElement('div');
        track.className = 'metodo-bar-track';
        const barra = document.createElement('div');
        barra.className = 'metodo-bar';
        track.appendChild(barra);

        const valor = document.createElement('span');
        valor.className = 'metodo-valor';
        valor.textContent = brlCompacto.format(0);

        const label = document.createElement('span');
        label.className = 'metodo-label';
        label.textContent = METODO_LABEL[metodo];

        coluna.append(track, valor, label);
        viz.appendChild(coluna);

        barras[metodo] = barra;
        valores[metodo] = valor;
    });
}

function atualizarBarras() {
    const maximo = Math.max(1, ...METODOS.map(m => estado.volumePorMetodo[m]));
    METODOS.forEach(metodo => {
        const volume = estado.volumePorMetodo[metodo];
        barras[metodo].style.height = (volume / maximo) * 100 + '%';
        valores[metodo].textContent = brlCompacto.format(volume);
    });
}

function criarLinha(v) {
    const tr = document.createElement('tr');
    tr.innerHTML = `
        <td class="mono">${fmtHora(v.momentoEpochMs)}</td>
        <td class="mono col-id">${v.id}</td>
        <td>${METODO_LABEL[v.metodo] || v.metodo}</td>
        <td class="col-uf">${v.uf || ''}</td>
        <td class="num">${brlExato.format(v.valor)}</td>
        <td><span class="badge ${STATUS_CLASS[v.status]}" title="${v.motivo || ''}">${STATUS_LABEL[v.status]}</span></td>
    `;
    return tr;
}

function adicionarAoVivo(v) {
    const feed = document.getElementById('feed');
    const vazio = feed.querySelector('.empty');
    if (vazio) {
        feed.innerHTML = '';
    }
    const linha = criarLinha(v);
    linha.classList.add('nova');
    feed.prepend(linha);
    while (feed.children.length > MAX_LINHAS) {
        feed.removeChild(feed.lastChild);
    }
}

// --- Eventos --------------------------------------------------------------

function contabilizar(v) {
    estado.total++;
    estado.volumeTotal += v.valor;
    if (v.status === 'APROVADA') estado.aprovadas++;
    else if (v.status === 'SUSPEITA') estado.suspeitas++;
    else estado.negadas++;
    if (estado.volumePorMetodo[v.metodo] !== undefined) {
        estado.volumePorMetodo[v.metodo] += v.valor;
    }
}

function onTransacao(v) {
    contabilizar(v);
    renderStats();
    atualizarBarras();
    adicionarAoVivo(v);
}

// --- Carga inicial e stream ----------------------------------------------

async function bootstrap() {
    try {
        const est = await fetch('/api/estatisticas').then(r => r.json());
        estado.total = est.total;
        estado.volumeTotal = est.volumeTotal;
        estado.aprovadas = est.aprovadas;
        estado.suspeitas = est.suspeitas;
        estado.negadas = est.negadas;
        METODOS.forEach(m => {
            estado.volumePorMetodo[m] = (est.volumePorMetodo && est.volumePorMetodo[m]) || 0;
        });
        renderStats();
        atualizarBarras();
    } catch (e) {
        console.warn('Falha ao carregar estatisticas iniciais', e);
    }

    try {
        const recentes = await fetch('/api/transacoes/recentes').then(r => r.json());
        const feed = document.getElementById('feed');
        if (recentes.length > 0) {
            feed.innerHTML = '';
            recentes.forEach(v => feed.appendChild(criarLinha(v)));
        }
    } catch (e) {
        console.warn('Falha ao carregar transacoes recentes', e);
    }
}

function conectarStream() {
    const fonte = new EventSource('/api/stream');
    fonte.addEventListener('transacao', ev => {
        onTransacao(JSON.parse(ev.data));
    });
    fonte.onerror = () => {
        // O EventSource reconecta sozinho; aqui apenas registramos.
        console.debug('Stream reconectando...');
    };
}

// --- Tema -----------------------------------------------------------------

function initTema() {
    const salvo = localStorage.getItem('tema');
    if (salvo) {
        document.documentElement.setAttribute('data-theme', salvo);
    }
    document.getElementById('theme-btn').addEventListener('click', () => {
        const atual = document.documentElement.getAttribute('data-theme');
        const novo = atual === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', novo);
        localStorage.setItem('tema', novo);
    });
}

// --- Init -----------------------------------------------------------------

initTema();
montarBarras();
bootstrap().then(conectarStream);
