/**
 * notice.js - 공지사항 페이지 JS
 *
 * 의존 변수 (list.html의 th:inline="javascript"로 주입):
 *   NOTICES    - List<NoticeVO> (content 제외, reg_date_str 포함)
 *   myRole     - 현재 사용자 역할 (1:팀원, 2:매니저, 3:팀장)
 *   myUserNum  - 현재 사용자 user_num
 */

/* =====================================================
   공지 목록 렌더 (고정/일반 분리)
===================================================== */
function renderNotices() {
    const kw = (document.getElementById('searchInput')?.value || '').toLowerCase();

    const filtered = NOTICES.filter(function(n) {
        if (!kw) return true;
        return (n.title && n.title.toLowerCase().includes(kw)) ||
               (n.writer_name && n.writer_name.toLowerCase().includes(kw));
    });

    const pinned = filtered.filter(function(n) { return n.is_fixed === 'Y'; });
    const normal = filtered.filter(function(n) { return n.is_fixed !== 'Y'; });
    // 서버에서 이미 날짜 내림차순 정렬되어 옴, 클라이언트에서 유지
    normal.sort(function(a, b) {
        return (b.reg_date_str || '').localeCompare(a.reg_date_str || '');
    });

    /* 고정 공지 */
    var pinnedEl = document.getElementById('pinnedSection');
    if (pinned.length) {
        pinnedEl.innerHTML = '<div class="pinnedLabel">📌 고정 공지</div>' +
            pinned.map(function(n) {
                return '<div class="pinnedCard" onclick="openDetail(' + n.notice_num + ')">' +
                    '<span style="font-size:20px">📢</span>' +
                    '<div style="flex:1;min-width:0">' +
                    '<div class="pt">' + esc(n.title) + '</div>' +
                    '<div class="pm">' + esc(n.writer_name) + ' · ' + (n.reg_date_str || '') + ' · 조회 ' + n.view_count + '</div>' +
                    '</div>' +
                    '<span style="font-size:12px;color:var(--muted)">›</span>' +
                    '</div>';
            }).join('');
    } else {
        pinnedEl.innerHTML = '';
    }

    /* 일반 공지 */
    var listEl = document.getElementById('noticeList');
    if (!normal.length) {
        listEl.innerHTML = '<div style="padding:40px;text-align:center;color:var(--muted);font-size:13px">공지사항이 없습니다.</div>';
        return;
    }

    var threeDaysAgo = new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);

    listEl.innerHTML = normal.map(function(n, i) {
        var isNew = n.reg_date_str && n.reg_date_str >= threeDaysAgo;
        return '<div class="noticeRow' + (isNew ? ' unread' : '') + '" onclick="openDetail(' + n.notice_num + ')">' +
            '<div class="noticeNum">' + (normal.length - i) + '</div>' +
            '<div class="noticeBody">' +
            '<div class="noticeTitle">' +
            (isNew ? '<span class="newBadge">NEW</span>' : '') +
            esc(n.title) +
            '</div>' +
            '<div class="noticeMeta">' + esc(n.writer_name) + ' · ' + (n.reg_date_str || '') + '</div>' +
            '</div>' +
            '<div class="noticeViews">👁 ' + n.view_count + '</div>' +
            '</div>';
    }).join('');
}

/* =====================================================
   공지 상세 (AJAX + 모달)
===================================================== */
function openDetail(noticeNum) {
    $.getJSON('/notice/detail', { noticeNum: noticeNum }, function(data) {
        if (!data || data.error) {
            alert('공지를 불러올 수 없습니다.');
            return;
        }

        // 로컬 조회수 갱신 → 목록 재렌더
        var local = NOTICES.find(function(n) { return n.notice_num == noticeNum; });
        if (local) {
            local.view_count = data.view_count;
            renderNotices();
        }

        var canManage = myRole >= 2 || data.user_num == myUserNum;
        var canPin    = myRole >= 2;
        var contentHtml = esc(data.content || '').replace(/\n/g, '<br>');

        var editBtns = '';
        if (canManage) {
            editBtns += '<button class="btn" onclick="closeModal();openWriteModal(' + data.notice_num + ')">✏️ 수정</button>';
        }
        if (canPin) {
            editBtns += '<button class="btn" onclick="doPinToggle(' + data.notice_num + ')">' +
                (data.is_fixed === 'Y' ? '📌 고정 해제' : '📌 고정') + '</button>';
        }
        if (canManage) {
            editBtns += '<button class="btn danger" onclick="doDelete(' + data.notice_num + ')">삭제</button>';
        }

        openModal('공지사항',
            '<div style="margin-bottom:14px">' +
            (data.is_fixed === 'Y'
                ? '<span style="background:var(--blueS);color:var(--primary);border-radius:999px;padding:3px 10px;font-size:11px;font-weight:950;margin-right:6px">📌 고정 공지</span>'
                : '') +
            '<h2 style="font-size:20px;font-weight:950;letter-spacing:-.04em;margin:10px 0 6px;line-height:1.3">' + esc(data.title) + '</h2>' +
            '<div style="font-size:13px;color:var(--muted);display:flex;gap:12px;flex-wrap:wrap">' +
            '<span>✏️ ' + esc(data.writer_name) + '</span>' +
            '<span>📅 ' + (data.reg_date_str || '') + '</span>' +
            '<span>👁 ' + data.view_count + '</span>' +
            '</div>' +
            '</div>' +
            '<div style="font-size:14px;line-height:1.85;background:#f9f7ff;border-radius:14px;padding:18px;min-height:80px;color:var(--ink);word-break:break-word">' +
            contentHtml +
            '</div>' +
            '<div class="actions">' +
            '<button class="btn" onclick="closeModal()">닫기</button>' +
            editBtns +
            '</div>'
        );
    }).fail(function() {
        alert('공지를 불러올 수 없습니다.');
    });
}

/* =====================================================
   공지 작성 / 수정 모달
   editNoticeNum: 0이면 작성 모드, >0이면 수정 모드
===================================================== */
function openWriteModal(editNoticeNum) {
    if (!editNoticeNum) {
        // ── 작성 모드 ──
        openModal('공지 작성', buildWriteForm(0));
        // value는 비어있으므로 별도 세팅 불필요
    } else {
        // ── 수정 모드: 기존 내용 AJAX 로드 ──
        $.getJSON('/notice/detail', { noticeNum: editNoticeNum }, function(data) {
            if (!data || data.error) { alert('공지를 불러올 수 없습니다.'); return; }
            openModal('공지 수정', buildWriteForm(data.notice_num));
            // innerHTML 이후 DOM에 안전하게 값 세팅
            document.getElementById('nTitle').value   = data.title   || '';
            document.getElementById('nContent').value = data.content || '';
            document.getElementById('nFixed').checked = data.is_fixed === 'Y';
        }).fail(function() { alert('공지를 불러올 수 없습니다.'); });
    }
}

/** 작성/수정 폼 HTML 생성 (값은 별도 세팅) */
function buildWriteForm(noticeNum) {
    return '<div class="field"><label>제목 *</label>' +
        '<input id="nTitle" placeholder="공지 제목을 입력하세요" maxlength="60"></div>' +
        '<div class="field"><label>내용 *</label>' +
        '<textarea id="nContent" style="min-height:180px" placeholder="공지 내용을 입력하세요"></textarea></div>' +
        '<div style="display:flex;align-items:center;gap:8px;margin-bottom:14px;cursor:pointer">' +
        '<input type="checkbox" id="nFixed" style="width:16px;height:16px;accent-color:var(--primary)">' +
        '<label for="nFixed" style="font-size:13px;font-weight:950;cursor:pointer">📌 고정 공지로 등록</label>' +
        '</div>' +
        '<div class="actions">' +
        '<button class="btn" onclick="closeModal()">취소</button>' +
        '<button class="btn primary" onclick="doSave(' + noticeNum + ')">' + (noticeNum ? '수정' : '등록') + '</button>' +
        '</div>';
}

/* =====================================================
   공지 저장 (작성 / 수정)
===================================================== */
function doSave(editNoticeNum) {
    var title   = (document.getElementById('nTitle')?.value   || '').trim();
    var content = (document.getElementById('nContent')?.value || '').trim();
    if (!title)   { alert('제목을 입력하세요.'); return; }
    if (!content) { alert('내용을 입력하세요.'); return; }
    if (title.length > 60) {
        alert('제목은 60자 이내로 입력해 주세요.');
        return;
    }
    var isFixed = document.getElementById('nFixed')?.checked ? 'Y' : 'N';

    var url  = editNoticeNum ? '/notice/update' : '/notice/write';
    var data = { title: title, content: content, isFixed: isFixed };
    if (editNoticeNum) data.noticeNum = editNoticeNum;

    $.post(url, data, function(result) {
        if (result === 'OK') {
            closeModal();
            location.reload();
        } else {
            alert('처리 중 오류가 발생했습니다: ' + result);
        }
    });
}

/* =====================================================
   공지 삭제
===================================================== */
function doDelete(noticeNum) {
    if (!confirm('공지사항을 삭제하시겠습니까?')) return;
    $.post('/notice/delete', { noticeNum: noticeNum }, function(result) {
        if (result === 'OK') {
            closeModal();
            location.reload();
        } else {
            alert('삭제 실패: ' + result);
        }
    });
}

/* =====================================================
   고정 토글
===================================================== */
function doPinToggle(noticeNum) {
    $.post('/notice/pin', { noticeNum: noticeNum }, function(result) {
        if (result === 'OK') {
            closeModal();
            location.reload();
        } else {
            alert('처리 실패: ' + result);
        }
    });
}

/* =====================================================
   모달 유틸
===================================================== */
function openModal(title, html) {
    document.getElementById('modalTitle').textContent = title;
    document.getElementById('modalBody').innerHTML    = html;
    document.getElementById('modalLayer').classList.add('active');
}

function closeModal() {
    document.getElementById('modalLayer').classList.remove('active');
}

/* =====================================================
   XSS 방지 이스케이프
===================================================== */
function esc(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

/* =====================================================
   초기화
===================================================== */
$(function() {
    // CSRF 토큰을 모든 AJAX 요청 헤더에 자동 포함
    $.ajaxSetup({
        beforeSend: function(xhr) {
            xhr.setRequestHeader(CSRF_HEADER, CSRF_TOKEN);
        }
    });

    renderNotices();
    $('#searchInput').on('input', renderNotices);

    // 모달 바깥 클릭 닫기
    document.getElementById('modalLayer').addEventListener('click', function(e) {
        if (e.target === this) closeModal();
    });
});