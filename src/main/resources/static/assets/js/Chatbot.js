/* ================================================
   TeamSync Chatbot Widget
   ================================================ */
(function () {
    "use strict";

    var bot    = document.getElementById('tsBot');
    var toast  = document.getElementById('tsToast');
    var msgs   = document.getElementById('tsMsgs');
    var typing = document.getElementById('tsTyping');
    var input  = document.getElementById('tsInput');
    var badge  = document.getElementById('tsBadge');

    if (!bot) return; // 위젯이 없는 페이지면 종료

    // 로그인 여부에 따라 호출할 엔드포인트/기능을 분기
    var isGuest = bot.dataset.guest === 'true';
    var historyLoaded = false;

    window.toggleBot = function () {
        bot.classList.contains('open') ? closeBot() : openBot();
    };

    window.openBot = function (e) {
        if (e) e.stopPropagation();
        bot.classList.add('open');
        toast.classList.add('hide');
        badge.style.display = 'none';
        if (!isGuest && !historyLoaded) loadHistory();
        setTimeout(function () { input.focus(); }, 300);
    };

    window.closeBot = function () {
        bot.classList.remove('open');
    };

    window.dismissToast = function (e) {
        e.stopPropagation();
        toast.classList.add('hide');
    };

    function escapeHtml(str) {
        var div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    function formatText(str) {
        return escapeHtml(str).replace(/\n/g, '<br>');
    }

    function addMsg(rawText, who) {
        var d = document.createElement('div');
        d.className = 'ts-m ' + who;
        d.innerHTML = formatText(rawText);
        msgs.appendChild(d);
        msgs.scrollTop = msgs.scrollHeight;
        return d;
    }

    // 팀 데이터 질문이라 로그인이 필요할 때, 답변 말풍선 아래에 로그인 링크 버튼을 붙임
    function addLoginPrompt() {
        var d = document.createElement('div');
        d.className = 'ts-m bot';
        var a = document.createElement('a');
        a.href = '/member/login';
        a.className = 'ts-chip';
        a.style.display = 'inline-block';
        a.style.marginTop = '2px';
        a.textContent = '🔐 로그인하러 가기';
        d.appendChild(a);
        msgs.appendChild(d);
        msgs.scrollTop = msgs.scrollHeight;
    }

    // 공통 헤더 fragment(header.html)와 동일한 CSRF 헤더 구성 패턴
    // 페이지마다 메타 태그 이름이 다를 수 있어(csrf-header/csrf-token 또는 _csrf/_csrf_header) 둘 다 확인
    function csrfFetchOpts(baseOpts) {
        var headerMeta = document.querySelector('meta[name="csrf-header"]') || document.querySelector('meta[name="_csrf_header"]');
        var tokenMeta  = document.querySelector('meta[name="csrf-token"]') || document.querySelector('meta[name="_csrf"]');
        var opts = baseOpts || {};
        if (headerMeta && tokenMeta) {
            opts.headers = opts.headers || {};
            opts.headers[headerMeta.content] = tokenMeta.content;
        }
        return opts;
    }

    // 패널을 처음 열 때 이전 대화 이력 복원 (로그인 사용자만)
    function loadHistory() {
        historyLoaded = true;
        fetch('/bot/history')
            .then(function (res) { return res.json(); })
            .then(function (list) {
                if (!list || !list.length) return;
                list.forEach(function (log) {
                    addMsg(log.message, log.sender === 1 ? 'me' : 'bot');
                });
            })
            .catch(function (err) {
                console.error('챗봇 이력 로드 실패', err);
            });
    }

    window.ask = function (q) {
        input.value = q;
        send();
    };

    window.send = function () {
        var t = input.value.trim();
        if (!t) return;
        addMsg(t, 'me');
        input.value = '';
        typing.classList.add('show');
        msgs.scrollTop = msgs.scrollHeight;

        var opts = csrfFetchOpts({
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: t })
        });

        var endpoint = isGuest ? '/bot/guest-ask' : '/bot/ask';

        fetch(endpoint, opts)
            .then(function (res) { return res.json(); })
            .then(function (data) {
                typing.classList.remove('show');
                addMsg(data.answer, 'bot');
                if (data.loginRequired) addLoginPrompt();
            })
            .catch(function (err) {
                console.error('챗봇 응답 실패', err);
                typing.classList.remove('show');
                addMsg('잠시 후 다시 시도해 주세요. 답변을 가져오지 못했어요.', 'bot');
            });
    };
})();