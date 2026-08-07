/* CSRF 공통 헤더 (백엔드 연동 시 바로 쓸 수 있게 미리 세팅) */
var csrfToken  = $('meta[name="csrf-token"]').attr('content');
var csrfHeader = $('meta[name="csrf-header"]').attr('content');
$(document).ajaxSend(function(e, xhr) {
    if (csrfHeader) xhr.setRequestHeader(csrfHeader, csrfToken);
});

/* ====================================================
   탭 전환
==================================================== */
const TAB_MAP = {
    profile: { nav: 'navProfile', panel: 'tabProfile' },
    pw:      { nav: 'navPw',      panel: 'tabPw' },
    todo:    { nav: 'navTodo',    panel: 'tabTodo' },
    social:  { nav: 'navSocial',  panel: 'tabSocial' },
    alarm:   { nav: 'navAlarm',   panel: 'tabAlarm' },
    danger:  { nav: 'navDanger',  panel: 'tabDanger' }
};

function showTab(key){
    Object.keys(TAB_MAP).forEach(function(k){
        document.getElementById(TAB_MAP[k].nav)?.classList.toggle('active', k === key);
        document.getElementById(TAB_MAP[k].panel)?.classList.toggle('active', k === key);
    });
    if (key === 'todo') renderTodos();
}

/* ====================================================
   개인 To-Do (※ 임시 저장 - 새로고침 시 초기화, 백엔드 연동 대기 중)
==================================================== */
let todos = [
    { id: 1, txt: '(예시) 마이페이지 To-Do 백엔드 연동 대기', done: false, pri: 2 }
];

function renderTodos(){
    const list = document.getElementById('todoListMy');
    if (!list) return;
    const priMap = { 3: '🔴', 2: '🟡', 1: '🟢' };
    list.innerHTML = todos.map(t =>
        '<div class="todoItem ' + (t.done ? 'done' : '') + '" onclick="togTodo(' + t.id + ')">' +
        '<span style="font-size:18px">' + (t.done ? '✅' : '⬜') + '</span>' +
        '<span style="flex:1;font-size:13px;font-weight:900">' + t.txt + '</span>' +
        '<span>' + (priMap[t.pri] || '') + '</span></div>'
    ).join('');
}
function togTodo(id){
    const t = todos.find(x => x.id === id);
    if (t) t.done = !t.done;
    renderTodos();
}
function openAddTodo(){
    openModal('To-Do 추가',
        '<div class="field"><label>할 일 제목 *</label><input id="tdTitle" placeholder="할 일을 입력하세요"></div>' +
        '<div class="field"><label>우선순위</label><select id="tdPri">' +
        '<option value="1">🟢 낮음</option><option value="2" selected>🟡 보통</option><option value="3">🔴 높음</option>' +
        '</select></div>' +
        '<div class="actions"><button class="btn" onclick="closeModal()">취소</button>' +
        '<button class="btn primary" onclick="saveTodo()">추가</button></div>'
    );
}
function saveTodo(){
    const title = document.getElementById('tdTitle')?.value.trim();
    if (!title){ alert('제목을 입력하세요'); return; }
    todos.unshift({
        id: Date.now(),
        txt: title,
        done: false,
        pri: Number(document.getElementById('tdPri')?.value) || 2
    });
    closeModal();
    renderTodos();
    toast('To-Do가 추가되었습니다! (임시 저장)');
}

/* ====================================================
   프로필 / 비밀번호 / 소셜 / 알림 / 계정 관리
   ※ 아래는 전부 CM 모듈(회원 정보 수정/비밀번호 변경/소셜 연동/탈퇴) 백엔드
     연동 전까지는 UI만 제공합니다. 실제 저장 API가 준비되면 $.post(...)로 교체하면 됩니다.
==================================================== */
function saveProfile(){
    toast('프로필 저장 기능은 준비 중입니다.');
}
function changePassword(){
    const cur = document.getElementById('pwCur')?.value;
    const n1 = document.getElementById('pwNew')?.value;
    const n2 = document.getElementById('pwNew2')?.value;
    if (!cur || !n1 || !n2){ alert('모든 항목을 입력하세요'); return; }
    if (n1 !== n2){ alert('새 비밀번호가 일치하지 않습니다'); return; }
    toast('비밀번호 변경 기능은 준비 중입니다.');
}
function unlinkGoogle(){
    if (!confirm('Google 연동을 해제하시겠습니까?\n해제 후 Google로 로그인할 수 없습니다.')) return;
    toast('소셜 연동 해제 기능은 준비 중입니다.');
}
function linkGoogle(){
    toast('소셜 연동 기능은 준비 중입니다.');
}
function saveAlarm(){
    toast('알림 설정 저장 기능은 준비 중입니다.');
}
function exportData(){
    toast('데이터 내보내기 기능은 준비 중입니다.');
}
function withdraw(){
    const txt = prompt('회원 탈퇴를 확인하려면 "탈퇴"를 입력하세요:', '');
    if (txt === null) return;
    if (txt !== '탈퇴'){ alert('"탈퇴"를 정확히 입력해야 합니다.'); return; }
    toast('회원 탈퇴 기능은 준비 중입니다.');
}
function changePhoto(){
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.onchange = function(e){
        const f = e.target.files[0];
        if (!f) return;
        const r = new FileReader();
        r.onload = function(ev){
            const av = document.querySelector('.profileAvatar');
            if (av) av.innerHTML =
                '<img src="' + ev.target.result + '" style="width:100%;height:100%;object-fit:cover;border-radius:28px">' +
                '<div class="profileAvatarEdit">✏️</div>';
        };
        r.readAsDataURL(f);
    };
    input.click();
    toast('사진 업로드 저장 기능은 준비 중입니다. (미리보기만 가능)');
}

/* ====================================================
   모달 / 토스트
==================================================== */
function openModal(title, html){
    document.getElementById('modalTitle').textContent = title;
    document.getElementById('modalBody').innerHTML = html;
    document.getElementById('modalLayer').classList.add('active');
}
function closeModal(){
    document.getElementById('modalLayer').classList.remove('active');
}
function toast(msg){
    const t = document.getElementById('toast');
    if (!t) return;
    t.textContent = msg;
    t.classList.add('show');
    clearTimeout(t._t);
    t._t = setTimeout(() => t.classList.remove('show'), 2600);
}

/* ====================================================
   초기화
==================================================== */
renderTodos();
