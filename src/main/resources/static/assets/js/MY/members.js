/* CSRF 토큰 (jQuery ajax 공통 헤더) */
var csrfToken  = $('meta[name="csrf-token"]').attr('content');
var csrfHeader = $('meta[name="csrf-header"]').attr('content');
$(document).ajaxSend(function(e, xhr) {
    if (csrfHeader) xhr.setRequestHeader(csrfHeader, csrfToken);
});

/* ====================================================
   토스트
==================================================== */
function toast(msg){
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

function errorMessage(code){
    var map = {
        'NO_PERMISSION': '권한이 없습니다.',
        'CANNOT_CHANGE_SELF': '본인의 역할은 변경할 수 없습니다.',
        'CANNOT_CHANGE_LEADER': '팀장의 역할은 변경할 수 없습니다.',
        'CANNOT_KICK_SELF': '본인을 강퇴할 수 없습니다.',
        'CANNOT_DELEGATE_SELF': '본인에게는 위임할 수 없습니다.',
        'LEADER_CANNOT_EXIT': '팀원이 남아있어 직접 탈퇴할 수 없습니다. 먼저 팀장 위임을 진행해주세요.',
        'NOT_FOUND': '대상을 찾을 수 없습니다.',
        'INVALID_ROLE': '유효하지 않은 역할입니다.'
    };
    return map[code] || ('처리 중 오류가 발생했습니다. (' + code + ')');
}

/* ====================================================
   필터 (소속중만 조회하므로 단순 표시 토글)
==================================================== */
function setFilter(f){
    $('#f_all, #f_active').removeClass('active');
    $('#f_' + f).addClass('active');
    if (f === 'all') {
        $('.memberCard').show();
    } else {
        $('.memberCard').each(function(){
            $(this).toggle($(this).data('join') === 1);
        });
    }
}

/* ====================================================
   모달 공통
==================================================== */
function openModal(title, html){
    $('#modalTitle').text(title);
    $('#modalBody').html(html);
    $('#modalLayer').addClass('active');
}
function closeModal(){ $('#modalLayer').removeClass('active'); }

/* ====================================================
   역할 변경
==================================================== */
function openRoleModal(targetUserNum){
    var $card = $('.memberCard[data-usernum="' + targetUserNum + '"]');
    var name = $card.data('name');
    var role = $card.data('role');

    openModal('역할 변경',
        '<div class="field"><label>' + name + '님의 변경할 역할</label>' +
        '<select id="newRole">' +
        '<option value="1"' + (role === 1 ? ' selected' : '') + '>팀원 (MEMBER)</option>' +
        '<option value="2"' + (role === 2 ? ' selected' : '') + '>매니저 (MANAGER)</option>' +
        '</select></div>' +
        '<p style="font-size:12px;color:var(--muted);background:#f0eeff;border-radius:12px;padding:10px 12px;line-height:1.6">' +
        '※ 팀장 역할은 <b>팀장 위임</b> 기능을 통해서만 변경할 수 있습니다.</p>' +
        '<div class="actions">' +
        '<button class="btn" onclick="closeModal()">취소</button>' +
        '<button class="btn primary" onclick="changeRole(' + targetUserNum + ')">변경</button>' +
        '</div>'
    );
}
function changeRole(targetUserNum){
    var newRole = Number($('#newRole').val());
    $.post('/team/members/role', { targetUserNum: targetUserNum, newRole: newRole })
        .done(function(result){
            if (result === 'OK') {
                toast('역할이 변경되었습니다.');
                closeModal();
                location.reload();
            } else {
                toast(errorMessage(result));
            }
        })
        .fail(function(){ toast('요청 처리 중 오류가 발생했습니다.'); });
}

/* ====================================================
   팀장 위임
==================================================== */
function openDelegateModal(targetUserNum){
    var $card = $('.memberCard[data-usernum="' + targetUserNum + '"]');
    var name = $card.data('name');

    openModal('팀장 위임',
        '<p style="font-size:14px;line-height:1.7;margin:0 0 14px">' +
        '<b>' + name + '</b>님에게 팀장 권한을 위임합니다.<br>위임 후 나는 <b>팀원</b>으로 역할이 변경됩니다.</p>' +
        '<div style="background:var(--orangeS);border-radius:13px;padding:12px 14px;font-size:13px;color:#c46121;margin-bottom:14px;line-height:1.6">' +
        '⚠️ 팀장 위임은 <b>되돌릴 수 없습니다.</b> 신중히 결정해주세요.</div>' +
        '<div class="actions">' +
        '<button class="btn" onclick="closeModal()">취소</button>' +
        '<button class="btn orange" onclick="delegateLeader(' + targetUserNum + ')">위임 확정</button>' +
        '</div>'
    );
}
function delegateLeader(targetUserNum){
    $.post('/team/members/delegate', { targetUserNum: targetUserNum })
        .done(function(result){
            if (result === 'OK') {
                toast('팀장 위임이 완료되었습니다.');
                closeModal();
                location.reload();
            } else {
                toast(errorMessage(result));
            }
        })
        .fail(function(){ toast('요청 처리 중 오류가 발생했습니다.'); });
}

/* ====================================================
   강퇴
==================================================== */
function kickMember(targetUserNum, name){
    if (!confirm('"' + name + '"님을 강퇴하시겠습니까?\n강퇴된 팀원은 팀에 재참여할 수 없습니다(재초대 시 재가입 가능).')) return;
    $.post('/team/members/kick', { targetUserNum: targetUserNum })
        .done(function(result){
            if (result === 'OK') {
                toast(name + '님이 강퇴되었습니다.');
                location.reload();
            } else {
                toast(errorMessage(result));
            }
        })
        .fail(function(){ toast('요청 처리 중 오류가 발생했습니다.'); });
}

/* ====================================================
   팀 탈퇴
==================================================== */
function leaveTeam(){
    if (!confirm('팀을 탈퇴하시겠습니까?\n팀장은 팀원이 남아있으면 먼저 팀장 위임을 진행해야 합니다.')) return;
    $.post('/team/members/exit')
        .done(function(result){
            if (result === 'OK') {
                toast('팀에서 탈퇴했습니다.');
                location.href = '/main/home';
            } else {
                toast(errorMessage(result));
            }
        })
        .fail(function(){ toast('요청 처리 중 오류가 발생했습니다.'); });
}

/* ====================================================
   초대 모달
==================================================== */
function openInviteModal(){
    openModal('팀원 초대',
        '<div style="display:flex;gap:8px;margin-bottom:16px">' +
        '<button id="invTabE" style="flex:1;border-radius:14px;padding:10px;font-size:13px;font-weight:950;cursor:pointer" ' +
        'onclick="switchInviteTab(\'email\')">이메일 초대</button>' +
        '<button id="invTabC" style="flex:1;border-radius:14px;padding:10px;font-size:13px;font-weight:950;cursor:pointer" ' +
        'onclick="switchInviteTab(\'code\')">초대코드</button>' +
        '</div>' +
		'<div id="invEmail">' +
		            '<div class="field"><label>초대할 이메일</label>' +
		            '<input type="email" id="invEmailInput" placeholder="member@example.com" autocomplete="off" ' +
		            'style="border:1px solid var(--line);border-radius:14px;padding:11px 12px;font-size:14px;width:100%;outline:none">' +
		            '</div>' +
            '<div class="actions"><button class="btn" onclick="closeModal()">취소</button>' +
            '<button onclick="sendInvite()" style="border:0;border-radius:14px;padding:10px 14px;font-size:13px;' +
            'font-weight:950;background:linear-gradient(135deg,#6c5ce7,#7476ff);color:#fff;cursor:pointer">초대 발송</button></div>' +
        '</div>' +
        '<div id="invCode" style="display:none">' +
            '<div id="invCodeDisplay" style="font-size:13px;color:var(--muted);margin-bottom:14px">불러오는 중...</div>' +
            '<div class="actions">' +
            '<button class="btn" onclick="closeModal()">닫기</button>' +
            '<button class="btn" onclick="copyCode()">복사하기</button>' +
            '<button onclick="issueCode()" style="border:0;border-radius:14px;padding:10px 14px;font-size:13px;' +
            'font-weight:950;background:linear-gradient(135deg,#6c5ce7,#7476ff);color:#fff;cursor:pointer">새로 발급</button>' +
            '</div>' +
        '</div>'
    );
    switchInviteTab('email');
}

function switchInviteTab(tab){
    $('#invEmail').css('display', tab === 'email' ? 'block' : 'none');
    $('#invCode').css('display', tab === 'code' ? 'block' : 'none');

    var activeStyle = 'flex:1;border-radius:14px;padding:10px;font-size:13px;font-weight:950;cursor:pointer;' +
        'border:0;background:linear-gradient(135deg,#2b2346,#46367e);color:#fff';
    var inactiveStyle = 'flex:1;border-radius:14px;padding:10px;font-size:13px;font-weight:950;cursor:pointer;' +
        'border:1px solid var(--line);background:#fff;color:#5e6578';

    $('#invTabE').attr('style', tab === 'email' ? activeStyle : inactiveStyle);
    $('#invTabC').attr('style', tab === 'code' ? activeStyle : inactiveStyle);
    if (tab === 'code') loadInviteCode();
}

function loadInviteCode(){
    $('#invCodeDisplay').text('불러오는 중...');
    $.get('/team/invite/code')
        .done(function(data){
            if (!data || !data.code){
                $('#invCodeDisplay').html('아직 발급된 초대코드가 없습니다. 아래 "새로 발급"을 눌러주세요.');
                return;
            }
            var statusMap = {1:'✅ 사용 가능', 2:'⏰ 만료됨', 3:'🚫 비활성화됨'};
            $('#invCodeDisplay').html(
                '<div style="font-family:monospace;font-size:15px;font-weight:900;background:#f8f6ff;' +
                'padding:12px;border-radius:12px;margin-bottom:8px;word-break:break-all">' + data.code + '</div>' +
                '<div style="font-size:12px;color:var(--muted)">상태: ' + (statusMap[data.status] || '-') + '</div>'
            );
        })
        .fail(function(){ $('#invCodeDisplay').text('초대코드 조회에 실패했습니다.'); });
}

/* ====================================================
   이메일 초대 발송
==================================================== */
function sendInvite(){
  const email = document.getElementById('invEmailInput')?.value.trim();
  if (!email){ alert('이메일을 입력하세요'); return; }

  // 즉시 toast + 모달 닫기
  toast(email + '으로 초대 메일을 발송했습니다!');
  closeModal();

  $.post('/team/invite/email/send', { email: email })
    .done(function(result){
      if (result !== 'OK') {
        const msgMap = {
          'NO_PERMISSION': '권한이 없습니다.',
          'INVALID_EMAIL': '이메일을 확인해주세요.',
          'NO_SUCH_USER': '없는 회원입니다.',
          'ALREADY_MEMBER': '이미 팀에 소속된 회원입니다.',
          'ALREADY_PENDING': '이미 초대가 발송된 사용자입니다.',
          'TEAM_NOT_FOUND': '팀 정보를 찾을 수 없습니다.',
          'SEND_FAIL': '메일 발송에 실패했습니다.'
        };
        toast(msgMap[result] || ('처리 실패 (' + result + ')'));
      }
    })
    .fail(function(){ toast('요청 처리 중 오류가 발생했습니다.'); });
}

/* ====================================================
   이메일 초대 자동완성
==================================================== */
var inviteSearchTimer = null;

/* 드롭다운을 body에 fixed로 붙여서 모달 overflow에 잘리지 않게 함 */
function getSuggestBox(){
    var $box = $('#invEmailSuggest');
    if ($box.length === 0){
        $box = $('<div id="invEmailSuggest"></div>').css({
            position:'fixed', display:'none', zIndex:99999,
            background:'#fff', border:'1px solid #ece8ff', borderRadius:'16px',
            padding:'6px', maxHeight:'240px', overflowY:'auto',
            boxShadow:'0 18px 44px rgba(73,47,133,.18)'
        }).appendTo('body');
    }
    return $box;
}

/* 입력창 위치 기준으로 드롭다운 좌표 계산 */
function positionSuggestBox(){
    var el = document.getElementById('invEmailInput');
    if (!el) return;
    var r = el.getBoundingClientRect();
    getSuggestBox().css({
        left:  r.left + 'px',
        top:   (r.bottom + 6) + 'px',
        width: r.width + 'px'
    });
}

$(document).on('input', '#invEmailInput', function(){
    var kw = $(this).val().trim();
    clearTimeout(inviteSearchTimer);

    if (kw.length < 2){
        getSuggestBox().hide().empty();
        return;
    }

    inviteSearchTimer = setTimeout(function(){
        $.get('/team/invite/email/search', { keyword: kw })
            .done(function(emails){
                var $box = getSuggestBox();
                if (!emails || emails.length === 0){
                    $box.hide().empty();
                    return;
                }
                $box.empty();
                $.each(emails, function(i, email){
                    var letter = (email.charAt(0) || '@').toUpperCase();
                    var $item = $('<div></div>').css({
                        display:'flex', alignItems:'center', gap:'10px',
                        padding:'9px 10px', borderRadius:'11px', cursor:'pointer',
                        transition:'background .12s'
                    });
                    $('<div></div>').text(letter).css({
                        width:'30px', height:'30px', flex:'0 0 30px', borderRadius:'9px',
                        background:'#efeaff', color:'#6c5ce7', fontWeight:900, fontSize:'13px',
                        display:'flex', alignItems:'center', justifyContent:'center'
                    }).appendTo($item);
                    $('<span></span>').text(email).css({
                        fontSize:'13.5px', color:'#3a3f52', fontWeight:700,
                        whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis'
                    }).appendTo($item);

                    $item.on('mouseenter', function(){ $(this).css('background', '#f4f1ff'); })
                         .on('mouseleave', function(){ $(this).css('background', 'transparent'); })
                         .on('click', function(){
                             $('#invEmailInput').val(email);
                             $box.hide().empty();
                         })
                         .appendTo($box);
                });
                positionSuggestBox();
                $box.show();
            });
    }, 300);
});

/* 스크롤·리사이즈 시 위치 재계산 */
$(window).on('scroll resize', function(){
    if ($('#invEmailSuggest').is(':visible')) positionSuggestBox();
});

/* 바깥 클릭 시 닫기 (취소·닫기 버튼 클릭도 여기서 정리됨) */
$(document).on('click', function(e){
    if ($(e.target).closest('#invEmailInput, #invEmailSuggest').length === 0){
        $('#invEmailSuggest').hide();
    }
});

/* 드롭다운 항목 선택 후 발송하면 자동으로 닫힘 (sendInvite에서 closeModal 호출) */
/* 입력창/드롭다운 바깥 클릭 시 닫기 */
$(document).on('click', function(e){
    if ($(e.target).closest('#invEmailInput, #invEmailSuggest').length === 0){
        $('#invEmailSuggest').hide();
    }
});


/* ====================================================
   초대코드 복사 / 발급
==================================================== */
function copyCode(){
  $.get('/team/invite/code')
    .done(function(data){
      if (!data || !data.code || data.status !== 1){
        toast('유효한 초대코드가 없습니다. 먼저 발급해주세요.');
        return;
      }
      navigator.clipboard?.writeText(data.code)
        .then(() => toast('초대코드가 복사되었습니다: ' + data.code))
        .catch(() => toast('복사: ' + data.code));
    })
    .fail(function(){ toast('초대코드 조회 실패'); });
}

function issueCode(){
  if (!confirm('새 초대코드를 발급하면 기존 코드는 즉시 무효화됩니다. 계속할까요?')) return;

  $.post('/team/invite/code/issue')
    .done(function(result){
      if (result && result.indexOf('OK:') === 0){
        toast('새 초대코드가 발급되었습니다!');
        loadInviteCode();
      } else if (result === 'NO_PERMISSION'){
        toast('팀장만 초대코드를 발급할 수 있습니다.');
      } else {
        toast('발급 실패 (' + result + ')');
      }
    })
    .fail(function(){ toast('요청 처리 중 오류가 발생했습니다.'); });
}