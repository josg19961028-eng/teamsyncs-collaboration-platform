/* =========================================================================
 * 팀 설정 페이지 스크립트
 *  - 팀 기본 정보 수정 (TM-002)
 *  - 팀 초대: 초대코드(TM-004) / 이메일 초대(TM-005)
 *  - 위험 구역: 위임(TM-011) / 탈퇴(TM-010) / 삭제(TM-003)
 *  ※ jQuery 필요 (layout_main 공통 로드)
 * ========================================================================= */

//토스트
function tsToast(msg) {
    var $t = $('#_toast');
    if ($t.length === 0) {
        $t = $('<div id="_toast"></div>').css({
            position:'fixed', bottom:'28px', right:'28px', background:'#171a2b', color:'#fff',
            borderRadius:'18px', padding:'14px 18px', fontSize:'13px', fontWeight:900,
            boxShadow:'0 16px 44px rgba(73,47,133,.12)', zIndex:9999, display:'none'
        }).appendTo('body');
    }
    $t.text(msg).stop(true,true).show();
    clearTimeout($t.data('t'));
    $t.data('t', setTimeout(function(){ $t.hide(); }, 2600));
}

/* ===================== 1. 팀 기본 정보 (TM-002) ===================== */
(function () {
    // 팀장이 아니면 수정 스크립트 자체가 필요 없음
    var saveArea = document.getElementById('tName');
    if (!saveArea) return;

    var csrfHeaderMeta = document.querySelector('meta[name="csrf-header"]');
    var csrfTokenMeta  = document.querySelector('meta[name="csrf-token"]');
    var csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.content : null;
    var csrfToken  = csrfTokenMeta  ? csrfTokenMeta.content  : null;

    var removePhotoFlag = false;
    var selectedColor = (document.getElementById('initialColor') || {}).value || '#6c5ce7';

    var fileInput = document.getElementById('teamImgInput');
    if (fileInput) {
        fileInput.addEventListener('change', function () {
            var f = this.files[0];
            if (!f) return;
            if (f.size > 2 * 1024 * 1024) {
                alert('이미지는 최대 2MB까지 업로드할 수 있습니다.');
                this.value = '';
                return;
            }
            removePhotoFlag = false;
            var reader = new FileReader();
            reader.onload = function (e) {
                document.getElementById('teamImgPrev').innerHTML =
                    '<img src="' + e.target.result + '" alt="팀 이미지">';
            };
            reader.readAsDataURL(f);
        });
    }

    window.tsTriggerUpload = function () {
        document.getElementById('teamImgInput').click();
    };

    window.tsRemoveImg = function () {
        removePhotoFlag = true;
        if (fileInput) fileInput.value = '';
        document.getElementById('teamImgPrev').innerHTML = '<span>💻</span>';
    };

    window.tsSelColor = function (el) {
        var swatches = document.querySelectorAll('.ts-settings .colorSwatch');
        swatches.forEach(function (s) { s.classList.remove('active'); });
        el.classList.add('active');
        selectedColor = el.getAttribute('data-color');
        var custom = document.getElementById('customColor');
        if (custom) custom.value = selectedColor;
    };

    window.tsCustomColor = function (el) {
        selectedColor = el.value;
        document.querySelectorAll('.ts-settings .colorSwatch').forEach(function (s) {
            s.classList.remove('active');
        });
    };

    window.tsLoadTeamInfo = function () {
        // 저장된 상태로 되돌리기
        location.reload();
    };
	

	

    window.tsSaveTeamInfo = function () {
        var name = document.getElementById('tName').value.trim();
        if (name.length < 2 || name.length > 30) {
            alert('팀 이름은 2~30자로 입력하세요.');
            return;
        }
        var desc = document.getElementById('tDesc').value.trim();
        if (desc.length > 200) {
            alert('팀 설명은 200자 이하로 입력하세요.');
            return;
        }

        var fd = new FormData();
        fd.append('teamNum', document.getElementById('teamNum').value);
        fd.append('teamName', name);
        fd.append('description', desc);
        fd.append('color', selectedColor);
        fd.append('removePhoto', removePhotoFlag);
        if (fileInput && fileInput.files[0]) {
            fd.append('upload', fileInput.files[0]);
        }

        $.ajax({
            url: '/team/update',
            type: 'POST',
            data: fd,
            processData: false,
            contentType: false,
            beforeSend: function (xhr) {
                if (csrfHeader && csrfToken) xhr.setRequestHeader(csrfHeader, csrfToken);
            },
            success: function (res) {
                if (res === 'OK') {
                    alert('팀 정보가 저장되었습니다.');
                    location.reload();
                    return;
                }
                alert(tsMsgOf(res));
            },
            error: function () {
                alert('저장 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
            }
        });
    };

    function tsMsgOf(code) {
        switch (code) {
            case 'NO_AUTH':                    return '팀장만 수정할 수 있습니다.';
            case 'TEAM_NOT_FOUND':             return '팀을 찾을 수 없습니다.';
            case 'TEAM_NOT_ACTIVE':            return '비활성 또는 삭제된 팀은 수정할 수 없습니다.';
            case 'INVALID_NAME':               return '팀 이름을 입력하세요.';
            case 'INVALID_NAME_LENGTH':        return '팀 이름은 2~30자로 입력하세요.';
            case 'INVALID_COLOR':              return '올바른 색상 값이 아닙니다.';
            case 'INVALID_DESCRIPTION_LENGTH': return '팀 설명은 200자 이하로 입력하세요.';
            case 'DUPLICATE_NAME':             return '이미 사용 중인 팀 이름입니다.';
            case 'UPLOAD_FAIL':                return '이미지 업로드에 실패했습니다.';
            default:                           return '알 수 없는 오류가 발생했습니다. (' + code + ')';
        }
    }
})();


/* ===================== 2. 팀 초대 (TM-004 / TM-005) =====================
 * API 계약 (실제 TeamInviteController 기준)
 *  1) 현재 코드 조회 : GET  /team/invite/code            -> TeamInviteCodeVO(JSON) | null
 *                       (필드: code, status[1:유효 2:만료 3:비활성], expired_at, ...)
 *  2) 발급/재발급    : POST /team/invite/code/issue       -> 'OK:<code>' | 'NO_PERMISSION'
 *  3) 비활성화       : POST /team/invite/code/disable      -> 'OK' | 'NO_PERMISSION' | 'NO_CODE'
 *  4) 이메일 발송    : POST /team/invite/email/send  body: email
 *                       -> 'OK' | NO_PERMISSION | INVALID_EMAIL | NO_SUCH_USER
 *                          | ALREADY_MEMBER | ALREADY_PENDING | TEAM_NOT_FOUND | SEND_FAIL
 *  5) 이메일 자동완성: GET  /team/invite/email/search?keyword=..  -> ["email1", ...] (문자열 배열)
 *  ※ teamNum 은 전부 세션 기준(getTeamNum)이라 프론트에서 안 넘겨도 됨.
 * ===================================================================== */
(function () {
    var codeArea = document.getElementById('codeArea');
    if (!codeArea) return; // 팀 초대 섹션 자체가 없음 (팀 미소속 등)

    var emailInput = document.getElementById('inviteEmail'); // 매니저 이상만 존재
    var canManage = codeArea.getAttribute('data-can-manage') === 'true';

    var INVITE_API = {
        current: '/team/invite/code',
        issue:   '/team/invite/code/issue',
        disable: '/team/invite/code/disable',
        send:    '/team/invite/email/send',
        search:  '/team/invite/email/search'
    };

    var csrfHeaderMeta = document.querySelector('meta[name="csrf-header"]');
    var csrfTokenMeta  = document.querySelector('meta[name="csrf-token"]');
    var csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.content : null;
    var csrfToken  = csrfTokenMeta  ? csrfTokenMeta.content  : null;

    function post(url, data) {
        return $.ajax({
            url: url, type: 'POST', data: data,
            beforeSend: function (xhr) {
                if (csrfHeader && csrfToken) xhr.setRequestHeader(csrfHeader, csrfToken);
            }
        });
    }

    /* ---------- 초대코드 ---------- */
    var currentCode = null;       // 현재 표시 중인 코드 문자열

    function fmtDate(v) {
        if (v == null) return '';
        var d = new Date(typeof v === 'number' ? v : v);
        if (isNaN(d.getTime())) return '';
        var m = ('0' + (d.getMonth() + 1)).slice(-2);
        var day = ('0' + d.getDate()).slice(-2);
        return d.getFullYear() + '-' + m + '-' + day;
    }
    function esc(s) {
        return String(s == null ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function renderCodeArea(vo) {
        // vo 없음 -> 발급 필요 (발급 권한 없으면 안내만)
        if (!vo || !vo.code) {
            currentCode = null;
            codeArea.innerHTML = canManage
                ? '<div class="codeEmpty">'
                +   '<span>아직 발급된 초대코드가 없습니다.</span>'
                +   '<button type="button" class="btn primary" onclick="tsIssueCode()">초대코드 발급</button>'
                + '</div>'
                : '<div class="codeEmpty"><span>아직 발급된 초대코드가 없습니다.</span></div>';
            return;
        }
        currentCode = vo.code;
        var status = vo.status;                 // 1:유효 2:만료 3:비활성
        var valid = (status === 1);
        var badge = valid
            ? '<span class="codeStatus ok">유효</span>'
            : '<span class="codeStatus off">' + (status === 2 ? '만료됨' : '비활성') + '</span>';
        var expText = fmtDate(vo.expired_at);

        var buttons;
        if (canManage) {
            buttons = valid
                ? '<button type="button" class="btn sm" onclick="tsCopyCode()">📋 코드</button>'
                + '<button type="button" class="btn sm" onclick="tsCopyLink()">🔗 링크</button>'
                + '<button type="button" class="btn sm danger" onclick="tsDisableCode()">비활성화</button>'
                + '<button type="button" class="btn sm primary" onclick="tsIssueCode()">새 코드 발급</button>'
                : '<button type="button" class="btn sm primary" onclick="tsIssueCode()">새 코드 발급</button>';
        } else {
            // 일반 팀원: 조회 + 복사만 가능 (발급/비활성화 불가)
            buttons = valid
                ? '<button type="button" class="btn sm" onclick="tsCopyCode()">📋 코드</button>'
                + '<button type="button" class="btn sm" onclick="tsCopyLink()">🔗 링크</button>'
                : '';
        }

        codeArea.innerHTML =
            '<div class="codeBox">'
          +   '<div>'
          +     '<div class="codeVal" id="codeVal">' + esc(vo.code) + badge + '</div>'
          +     '<div class="codeMeta">' + (expText ? '만료: ' + expText : '') + '</div>'
          +   '</div>'
          +   '<div style="display:flex;gap:8px;flex-wrap:wrap">' + buttons + '</div>'
          + '</div>';
    }

    function loadCode() {
        $.ajax({ url: INVITE_API.current, type: 'GET' })
            .done(function (vo) { renderCodeArea(vo); })
            .fail(function () {
                codeArea.innerHTML = '<div class="codeEmpty">초대코드를 불러오지 못했습니다.</div>';
            });
    }

    window.tsCopyCode = function () {
        if (!currentCode) return;
        if (navigator.clipboard) {
            navigator.clipboard.writeText(currentCode)
                .then(function () { alert('초대코드가 복사되었습니다: ' + currentCode); })
                .catch(function () { alert('복사할 코드: ' + currentCode); });
        } else {
            alert('복사할 코드: ' + currentCode);
        }
    };

    window.tsCopyLink = function () {
        if (!currentCode) return;
        var link = location.origin + '/team/invite/link/' + currentCode;
        if (navigator.clipboard) {
            navigator.clipboard.writeText(link)
                .then(function () { alert('초대 링크가 복사되었습니다:\n' + link); })
                .catch(function () { window.prompt('초대 링크', link); });
        } else {
            window.prompt('초대 링크', link);
        }
    };

    window.tsIssueCode = function () {
        if (currentCode && !confirm('새 코드를 발급하면 기존 코드는 더 이상 사용할 수 없습니다. 계속할까요?')) return;
        post(INVITE_API.issue, {})
            .done(function (res) {
                if (typeof res === 'string' && res.indexOf('OK') === 0) {
                    alert('새 초대코드가 발급되었습니다.');
                    loadCode();
                } else if (res === 'NO_PERMISSION') {
                    alert('팀장 또는 매니저만 발급할 수 있습니다.');
                } else {
                    alert('발급 실패: ' + res);
                }
            })
            .fail(function () { alert('초대코드 발급 중 오류가 발생했습니다.'); });
    };

    window.tsDisableCode = function () {
        if (!confirm('초대코드를 비활성화하시겠습니까?')) return;
        post(INVITE_API.disable, {})
            .done(function (res) {
                if (res === 'OK') { alert('초대코드가 비활성화되었습니다.'); loadCode(); }
                else if (res === 'NO_PERMISSION') alert('팀장 또는 매니저만 비활성화할 수 있습니다.');
                else if (res === 'NO_CODE') alert('비활성화할 코드가 없습니다.');
                else alert('비활성화 실패: ' + res);
            })
            .fail(function () { alert('초대코드 비활성화 중 오류가 발생했습니다.'); });
    };

    /* ---------- 이메일 초대 발송 + 자동완성 (매니저 이상만 UI가 존재) ---------- */
    if (emailInput) {
    var EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

	window.tsSendInvite = function () {
	    var email = emailInput.value.trim();
	    if (!email) { alert('이메일을 입력하세요.'); return; }
	    if (!EMAIL_RE.test(email)) { alert('올바른 이메일 형식이 아닙니다.'); return; }

	    tsToast(email + ' 주소로 초대 메일을 발송했습니다.');
	    emailInput.value = '';
	    closeDrop();

	    post(INVITE_API.send, { email: email })
	        .done(function (res) {
	            if (res !== 'OK') {
	                tsToast(inviteMsgOf(res));
	            }
	        })
	        .fail(function () { tsToast('초대 메일 발송 중 오류가 발생했습니다.'); });
	};

    function inviteMsgOf(code) {
        switch (code) {
            case 'NO_PERMISSION':  return '팀장 또는 매니저만 초대할 수 있습니다.';
            case 'INVALID_EMAIL':  return '올바른 이메일이 아닙니다.';
            case 'NO_SUCH_USER':   return '가입된 회원 중 해당 이메일을 찾을 수 없습니다.';
            case 'ALREADY_MEMBER': return '이미 팀에 소속된 회원입니다.';
            case 'ALREADY_PENDING':return '이미 초대장이 발송되어 대기 중인 회원입니다.';
            case 'TEAM_NOT_FOUND': return '팀 정보를 찾을 수 없습니다.';
            case 'SEND_FAIL':      return '메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.';
            default:               return '초대 발송에 실패했습니다. (' + code + ')';
        }
    }

    /* ---------- 이메일 자동완성 (GET /team/invite/email/search) ----------
     * 드롭다운이 .panel(overflow:hidden) 안에서 잘리는 문제 해결:
     * body로 옮기고 position:fixed + getBoundingClientRect() 로 입력창 아래에 배치
     */
    var drop = document.getElementById('emailDrop');
    var timer = null;
    if (drop && drop.parentNode !== document.body) {
        document.body.appendChild(drop);   // overflow:hidden 컨텍스트 밖으로 탈출
    }
    drop.style.position = 'fixed';

    function positionDrop() {
        var r = emailInput.getBoundingClientRect();
        drop.style.left  = r.left + 'px';
        drop.style.top   = (r.bottom + 4) + 'px';
        drop.style.width = r.width + 'px';
        drop.style.right = 'auto';
    }

    function closeDrop() { drop.classList.remove('open'); drop.innerHTML = ''; }

    function renderDrop(list) {
        if (!list || !list.length) { closeDrop(); return; }
        var html = '';
        list.forEach(function (em) {
            em = String(em || '');
            var initial = em ? em.charAt(0).toUpperCase() : '?';
            html += '<div class="ts-emailItem" data-email="' + esc(em) + '">'
                  +   '<div class="ts-emailAvatar">' + initial + '</div>'
                  +   '<div class="ts-emailText"><div class="em">' + esc(em) + '</div></div>'
                  + '</div>';
        });
        drop.innerHTML = html;
        positionDrop();
        drop.classList.add('open');
        drop.querySelectorAll('.ts-emailItem').forEach(function (item) {
            item.addEventListener('click', function () {
                emailInput.value = item.getAttribute('data-email');
                closeDrop();
                emailInput.focus();
            });
        });
    }

    emailInput.addEventListener('input', function () {
        var kw = emailInput.value.trim();
        if (timer) clearTimeout(timer);
        if (kw.length < 2) { closeDrop(); return; }
        timer = setTimeout(function () {
            $.ajax({ url: INVITE_API.search, type: 'GET', data: { keyword: kw } })
                .done(function (res) { renderDrop(res); })
                .fail(function () { closeDrop(); });
        }, 300);
    });

    // 드롭다운이 열려있는 동안 스크롤/리사이즈 시 위치 갱신
    window.addEventListener('scroll', function () {
        if (drop.classList.contains('open')) positionDrop();
    }, true);
    window.addEventListener('resize', function () {
        if (drop.classList.contains('open')) positionDrop();
    });

    // 바깥 클릭 시 드롭다운 닫기 (입력창/드롭다운 내부 클릭은 제외)
    document.addEventListener('click', function (e) {
        if (!e.target.closest('.inviteEmailWrap') && !e.target.closest('.ts-emailDrop')) closeDrop();
    });
    emailInput.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') closeDrop();
        if (e.key === 'Enter') { e.preventDefault(); closeDrop(); window.tsSendInvite(); }
    });
    } // if (emailInput)

    // 페이지 로드 시 현재 초대코드 조회 후 렌더 (전체 팀원 공통)
    loadCode();
})();


/* ===================== 3. 위험 구역 (TM-011 / TM-010 / TM-003) =====================
 * API 계약 (실제 컨트롤러 기준)
 *  위임 : POST /team/members/delegate  body: targetUserNum
 *         -> 'OK' | CANNOT_DELEGATE_SELF | NO_PERMISSION | NOT_FOUND
 *  탈퇴 : POST /team/members/exit       (파라미터 없음, 세션 teamNum)
 *         -> 'OK' | NOT_FOUND | LEADER_CANNOT_EXIT
 *  삭제 : POST /team/delete             (파라미터 없음, 세션 teamNum)
 *         -> 'OK' | NO_PERMISSION
 *  성공(OK) 시: 위임->새로고침 / 탈퇴·삭제-> /main/home 이동
 * ============================================================================== */
(function () {
    // 위험 구역: 탈퇴 버튼은 전원에게 존재. 위임/삭제/모달은 팀장만.
    var csrfHeaderMeta = document.querySelector('meta[name="csrf-header"]');
    var csrfTokenMeta  = document.querySelector('meta[name="csrf-token"]');
    var csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.content : null;
    var csrfToken  = csrfTokenMeta  ? csrfTokenMeta.content  : null;

    function post(url, data) {
        return $.ajax({
            url: url, type: 'POST', data: data,
            beforeSend: function (xhr) {
                if (csrfHeader && csrfToken) xhr.setRequestHeader(csrfHeader, csrfToken);
            }
        });
    }

    var modal = document.getElementById('delegateModal');

    /* ---------- 팀장 위임 ---------- */
    window.tsOpenDelegate = function () {
        if (!modal) return;
        var sel = document.getElementById('delegateTarget');
        // 선택 가능한 팀원(자기 자신 제외 option)이 없으면 안내
        var real = sel ? sel.querySelectorAll('option:not([disabled])') : [];
        if (!real || real.length === 0) {
            alert('위임할 팀원이 없습니다. 먼저 팀원을 초대하세요.');
            return;
        }
        modal.classList.add('open');
    };
    window.tsCloseDelegate = function () {
        if (modal) modal.classList.remove('open');
    };
    window.tsDoDelegate = function () {
        var sel = document.getElementById('delegateTarget');
        var target = sel ? sel.value : '';
        if (!target) { alert('위임할 팀원을 선택하세요.'); return; }
        if (!confirm('선택한 팀원에게 팀장을 위임하시겠습니까? 되돌릴 수 없습니다.')) return;

        post('/team/members/delegate', { targetUserNum: target })
            .done(function (res) {
                if (res === 'OK') { alert('팀장 권한을 위임했습니다.'); location.reload(); }
                else if (res === 'CANNOT_DELEGATE_SELF') alert('자기 자신에게는 위임할 수 없습니다.');
                else if (res === 'NO_PERMISSION') alert('팀장만 위임할 수 있습니다.');
                else if (res === 'NOT_FOUND') alert('대상 팀원을 찾을 수 없습니다.');
                else alert('위임 실패: ' + res);
            })
            .fail(function () { alert('위임 중 오류가 발생했습니다.'); });
    };

    /* ---------- 팀 탈퇴 ---------- */
    window.tsLeaveTeam = function () {
        if (!confirm('이 팀에서 탈퇴하시겠습니까?')) return;
        post('/team/members/exit', {})
            .done(function (res) {
                if (res === 'OK') { alert('팀에서 탈퇴했습니다.'); location.href = '/main/home'; }
                else if (res === 'LEADER_CANNOT_EXIT') alert('팀장은 다른 팀원에게 위임한 뒤에 탈퇴할 수 있습니다.');
                else if (res === 'NOT_FOUND') alert('팀 소속 정보를 찾을 수 없습니다.');
                else alert('탈퇴 실패: ' + res);
            })
            .fail(function () { alert('탈퇴 중 오류가 발생했습니다.'); });
    };

    /* ---------- 팀 삭제 (강제 해산) ---------- */
    window.tsDeleteTeam = function () {
        var typed = prompt('팀과 모든 데이터가 영구 삭제됩니다. 계속하려면 "삭제" 를 입력하세요.');
        if (typed === null) return;
        if (typed.trim() !== '삭제') { alert('입력이 일치하지 않아 취소되었습니다.'); return; }

        post('/team/delete', {})
            .done(function (res) {
                if (res === 'OK') { alert('팀이 삭제되었습니다.'); location.href = '/main/home'; }
                else if (res === 'NO_PERMISSION') alert('팀장만 삭제할 수 있습니다.');
                else alert('삭제 실패: ' + res);
            })
            .fail(function () { alert('삭제 중 오류가 발생했습니다.'); });
    };

    // 오버레이 바깥 클릭 / ESC 로 모달 닫기
    if (modal) {
        modal.addEventListener('click', function (e) {
            if (e.target === modal) window.tsCloseDelegate();
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') window.tsCloseDelegate();
        });
    }
})();