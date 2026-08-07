///* CSRF 공통 헤더 */
//var csrfToken  = $('meta[name="csrf-token"]').attr('content');
//var csrfHeader = $('meta[name="csrf-header"]').attr('content');
//$(document).ajaxSend(function(e, xhr) {
//    if (csrfHeader) xhr.setRequestHeader(csrfHeader, csrfToken);
//});
//
///* 로그아웃 버튼은 header.html 프래그먼트 자체 스크립트가 처리함 */
//
///* 오늘 날짜 라벨 */
//(function(){
//    var d = new Date();
//    var days = ['일','월','화','수','목','금','토'];
//    document.getElementById('todayLabel').textContent =
//        d.getFullYear()+'년 '+(d.getMonth()+1)+'월 '+d.getDate()+'일 '+days[d.getDay()]+'요일';
//})();
//
///* ====================================================
//   캘린더/투두/회의록 (목업 mock 데이터 - 캘린더/회의록/투두 모듈 완성 전까지 임시)
//==================================================== */
//let curY = new Date().getFullYear(), curM = new Date().getMonth()+1;
//let todos = [
//  {id:1, txt:'(예시) 칸반/캘린더 모듈 연동 대기', done:false, pri:2},
//];
//
//function mv(d){curM+=d;if(curM>12){curM=1;curY++}if(curM<1){curM=12;curY--}renderCal();}
//function renderCal(){
//  document.getElementById('mTitle').textContent = curY+'년 '+curM+'월';
//  const fd=new Date(curY,curM-1,1).getDay(), dim=new Date(curY,curM,0).getDate(), pd=new Date(curY,curM-1,0).getDate();
//  const today=new Date(); const isT=(y,m,d)=>y===today.getFullYear()&&m===(today.getMonth()+1)&&d===today.getDate();
//  let h='';
//  for(let i=0;i<fd;i++) h+='<div class="day dim"><div class="dn">'+(pd-fd+1+i)+'</div></div>';
//  for(let d=1;d<=dim;d++){
//    const tc = isT(curY,curM,d) ? 'today' : '';
//    h += '<div class="day '+tc+'"><div class="dn">'+d+'</div></div>';
//  }
//  const tot=fd+dim, rem = tot%7 ? 7-tot%7 : 0;
//  for(let i=1;i<=rem;i++) h += '<div class="day dim"><div class="dn">'+i+'</div></div>';
//  document.getElementById('days').innerHTML = h;
//}
//function renderTodos(){
//  const priMap = {3:'🔴',2:'🟡',1:'🟢'};
//  document.getElementById('todoList').innerHTML = todos.map(t =>
//    '<div class="todoItem '+(t.done?'done':'')+'" onclick="togTodo('+t.id+')">'+
//    '<span style="font-size:18px">'+(t.done?'✅':'⬜')+'</span>'+
//    '<span style="flex:1;font-size:13px;font-weight:900">'+t.txt+'</span>'+
//    '<span>'+(priMap[t.pri]||'')+'</span></div>'
//  ).join('');
//}
//function togTodo(id){ const t = todos.find(x=>x.id===id); if(t) t.done=!t.done; renderTodos(); }
//document.getElementById('todayList').innerHTML = '<div style="padding:12px;text-align:center;color:var(--muted);font-size:13px">캘린더 모듈 연동 대기 중</div>';
//document.getElementById('minutesList').innerHTML = '<div style="padding:12px;text-align:center;color:var(--muted);font-size:13px">회의록 모듈 연동 대기 중</div>';
//renderCal();
//renderTodos();
//
///* ====================================================
//   모달
//==================================================== */
//const SWATCHES = ['#f1edff','#eef9f4','#fff5ea','#f5f0ff','#fff1f7','#e8f4ff'];
//let selColor = SWATCHES[0];
//
//function openM(type){
//  document.getElementById('mLayer').classList.add('open');
//  const body = document.getElementById('mBody'), title = document.getElementById('mTitle2');
//
//  if (type === 'createTeam'){
//    title.textContent = '팀 만들기';
//    body.innerHTML =
//      '<div class="fG">'+
//      '<div class="f full"><label>팀 이름 *</label><input id="tN" placeholder="예: FE 개발팀"></div>'+
//      '<div class="f full"><label>팀 설명</label><input id="tD" placeholder="팀에 대한 간단한 설명"></div>'+
//      '<div class="f full"><label>팀 색상 *</label><div class="swatches">'+
//      SWATCHES.map(c => '<div class="sw'+(c===selColor?' sel':'')+'" style="background:'+c+'" onclick="selSw(\''+c+'\',this)"></div>').join('')+
//      '</div></div></div>'+
//      '<div class="acts"><button class="btn" onclick="closeM()">취소</button>'+
//      '<button class="btn primary" onclick="createTeam()">팀 생성</button></div>';
//  } else if (type === 'joinTeam'){
//    title.textContent = '초대코드로 팀 참여';
//    body.innerHTML =
//      '<p style="font-size:14px;color:var(--muted);margin:0 0 14px">팀장에게 받은 초대코드를 입력하세요.</p>'+
//      '<input class="bigInput" id="jCode" placeholder="예: f39084b6-0b65-4188-857e-54c16789937a" style="font-size:14px;letter-spacing:.02em">'+
//      '<div class="acts"><button class="btn" onclick="closeM()">취소</button>'+
//      '<button class="btn primary" onclick="joinTeam()">팀 참여</button></div>';
//  } else if (type === 'addTodo'){
//    title.textContent = '개인 To-Do 추가';
//    body.innerHTML =
//      '<div class="fG">'+
//      '<div class="f full"><label>할 일 제목 *</label><input id="tdT" placeholder="할 일 입력"></div>'+
//      '<div class="f"><label>우선순위</label><select id="tdP"><option value="1">🟢 낮음</option><option value="2" selected>🟡 보통</option><option value="3">🔴 높음</option></select></div>'+
//      '</div>'+
//      '<p style="font-size:12px;color:var(--muted)">※ To-Do 영구 저장은 마이페이지 모듈 완성 후 연동됩니다.</p>'+
//      '<div class="acts"><button class="btn" onclick="closeM()">취소</button>'+
//      '<button class="btn primary" onclick="saveTodo()">저장</button></div>';
//  }
//}
//function closeM(){ document.getElementById('mLayer').classList.remove('open'); }
//function selSw(c, el){
//  selColor = c;
//  document.querySelectorAll('.sw').forEach(s => s.classList.remove('sel'));
//  el.classList.add('sel');
//}
//
//function createTeam(){
//  const name = document.getElementById('tN')?.value.trim();
//  if (!name){ alert('팀 이름을 입력하세요'); return; }
//  const desc = document.getElementById('tD')?.value.trim() || '';
//
//  $.post('/team/create', { teamName: name, description: desc, color: selColor })
//    .done(function(result){
//      if (result && result.indexOf('OK:') === 0){
//        const teamNum = result.split(':')[1];
//        closeM();
//        showToast('팀이 생성되었습니다! 🎉');
//        location.href = '/team/enter/' + teamNum;
//      } else {
//        showToast('팀 생성에 실패했습니다. (' + result + ')');
//      }
//    })
//    .fail(function(){ showToast('요청 처리 중 오류가 발생했습니다.'); });
//}
//
//function joinTeam(){
//  const c = document.getElementById('jCode')?.value.trim();
//  if (!c){ alert('초대코드를 입력하세요'); return; }
//
//  $.post('/team/join/code', { code: c })
//    .done(function(result){
//      if (result && result.indexOf('OK:') === 0){
//        const teamNum = result.split(':')[1];
//        closeM();
//        showToast('팀에 참여했습니다! 🎉');
//        location.href = '/team/enter/' + teamNum;
//      } else {
//        const msgMap = {
//          'INVALID_CODE': '유효하지 않은 초대코드입니다.',
//          'EXPIRED_CODE': '만료된 초대코드입니다.',
//          'DISABLED_CODE': '비활성화된 초대코드입니다.',
//          'TEAM_NOT_FOUND': '팀을 찾을 수 없습니다.',
//          'ALREADY_JOINED': '이미 소속된 팀입니다.'
//        };
//        showToast(msgMap[result] || ('참여 실패 (' + result + ')'));
//      }
//    })
//    .fail(function(){ showToast('요청 처리 중 오류가 발생했습니다.'); });
//}
//
//function saveTodo(){
//  const t = document.getElementById('tdT')?.value.trim();
//  if (!t){ alert('제목을 입력하세요'); return; }
//  todos.unshift({ id: Date.now(), txt: t, done: false, pri: Number(document.getElementById('tdP')?.value) || 2 });
//  closeM();
//  renderTodos();
//  showToast('To-Do 추가 완료. (임시 저장, 새로고침 시 사라짐)');
//}
//function showToast(msg){
//  const t = document.getElementById('toast');
//  t.textContent = msg;
//  t.classList.add('show');
//  setTimeout(() => t.classList.remove('show'), 2800);
//}
//
///* 이메일 초대 수락/거절 링크로 리다이렉트된 경우 토스트 표시 */
//(function(){
//  const params = new URLSearchParams(location.search);
//  const invite = params.get('invite');
//  if (!invite) return;
//  const msgMap = {
//    'accepted': '팀 초대를 수락했습니다! 🎉',
//    'rejected': '팀 초대를 거절했습니다.',
//    'expired': '만료된 초대입니다.',
//    'notPending': '이미 처리된 초대입니다.',
//    'wrongUser': '본인 이메일로 받은 초대만 처리할 수 있습니다.',
//    'alreadyJoined': '이미 소속된 팀입니다.',
//    'notfound': '초대 정보를 찾을 수 없습니다.'
//  };
//  showToast(msgMap[invite] || '초대 처리 결과를 확인해주세요.');
//  history.replaceState(null, '', location.pathname);
//})();