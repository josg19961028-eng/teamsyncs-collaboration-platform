/* =========================================================
   데이터 (외부 파일 의존 없이 자체 포함)
   실제 구현 시 API 응답으로 교체
========================================================= */
const ME = { user_num:1, user_name:'홍영준' };
const TODAY = '2026-07-10';

const TEAM_MEMBERS = [
  { user_num:1, user_name:'홍영준', role:3 },
  { user_num:2, user_name:'김민준', role:1 },
  { user_num:3, user_name:'이수진', role:2 },
  { user_num:4, user_name:'박지훈', role:1 },
  { user_num:5, user_name:'최유진', role:1 },
  { user_num:6, user_name:'정서연', role:1 },
];

let CARDS = [
  { id:1, title:'캘린더 필터 UI 구현', tag:'UI',  status:1, deadline:'2026-07-12', content:'전체/일정/칸반 필터 UI 구현. 칩 형태로 토글 방식.', writer_num:1, reg_date:'2026-07-01' },
  { id:2, title:'FullCalendar 모달 연동', tag:'개발', status:2, deadline:'2026-07-14', content:'eventClick 핸들러에서 일정 상세 모달 연결.', writer_num:2, reg_date:'2026-07-02' },
  { id:3, title:'회의록 PDF 내보내기', tag:'기능', status:3, deadline:'2026-07-18', content:'회의록 상세에서 인쇄/PDF 다운로드 흐름 연결.', writer_num:1, reg_date:'2026-07-03' },
  { id:4, title:'공통 사이드바 스타일', tag:'UI',  status:4, deadline:'2026-07-08', content:'팀 내부 공통 사이드바 스타일 정리 완료.', writer_num:2, reg_date:'2026-07-01' },
  { id:5, title:'로그인 이메일 인증',  tag:'인증', status:1, deadline:'2026-07-20', content:'6자리 코드 인증 UI 연결. EMAIL_VERIFICATION 테이블 연동.', writer_num:3, reg_date:'2026-07-05' },
  { id:6, title:'WebSocket STOMP 연결', tag:'개발', status:2, deadline:'2026-07-22', content:'채팅 실시간 연결. CHAT_CHANNEL / CHAT_MESSAGE 연동.', writer_num:1, reg_date:'2026-07-05' },
];

let ASSIGNS = [
  { id:1, card_id:1, user_num:3 },
  { id:2, card_id:2, user_num:5 }, { id:3, card_id:2, user_num:1 },
  { id:4, card_id:3, user_num:4 },
  { id:5, card_id:4, user_num:2 },
  { id:6, card_id:5, user_num:1 },
  { id:7, card_id:6, user_num:1 }, { id:8, card_id:6, user_num:2 },
];

let CHECKLISTS = [
  { id:1, card_id:2, content:'FullCalendar v6 설치 확인', done:true },
  { id:2, card_id:2, content:'eventClick 핸들러 구현',   done:true },
  { id:3, card_id:2, content:'모달 UI 연결',              done:false },
  { id:4, card_id:3, content:'PDF 라이브러리 선택',        done:true },
  { id:5, card_id:3, content:'회의록 상세 페이지 연결',    done:false },
];

let COMMENTS = [
  { id:1, card_id:2, user_num:2, text:'eventClick 핸들러 붙였어요, 확인해주세요!', date:'2026-07-05', del:false },
  { id:2, card_id:2, user_num:1, text:'확인했어요! 모달 연결만 남았네요.', date:'2026-07-05', del:false },
  { id:3, card_id:3, user_num:4, text:'jsPDF 쓰는 게 제일 편할 것 같아요.', date:'2026-07-06', del:false },
];

/* =========================================================
   레인 정의
========================================================= */
const LANES = [
  { status:1, label:'할 일 목록', dot:'#a098c8', bg:'#f8f6ff', border:'rgba(160,152,200,.2)' },
  { status:2, label:'진행 중',    dot:'#f0a04b', bg:'#fffbf5', border:'rgba(240,160,75,.2)'  },
  { status:3, label:'검토',       dot:'#8f65f6', bg:'#fdf9ff', border:'rgba(143,101,246,.2)' },
  { status:4, label:'완료',       dot:'#34b77b', bg:'#f4fcf7', border:'rgba(52,183,123,.2)'  },
];
const STATUS_NAMES = { 1:'할 일 목록', 2:'진행 중', 3:'검토', 4:'완료' };
const TAG_STYLE = {
  'UI'  :['#f1edff','#6c5ce7'], '개발':['#eef9f4','#34b77b'],
  '기능':['#f5f0ff','#8f65f6'], '인증':['#fff5ea','#c46121'],
  'API' :['#fff1f7','#e46aa1'], '기타':['#f4f6fb','#555'],
};

/* =========================================================
   상태
========================================================= */
let tagF   = null;
let myOnly = false;
let dragId = null;
let nextId = 100;

/* =========================================================
   유틸
========================================================= */
function getUser(uid){ return TEAM_MEMBERS.find(m=>m.user_num===uid)||{user_num:uid,user_name:'?'}; }
function getAssigns(cid){ return ASSIGNS.filter(a=>a.card_id===cid).map(a=>getUser(a.user_num)); }
function getChks(cid)   { return CHECKLISTS.filter(c=>c.card_id===cid); }
function getCmts(cid)   { return COMMENTS.filter(c=>c.card_id===cid&&!c.del); }
function isNear(d){ if(!d) return false; const diff=(new Date(d)-new Date(TODAY))/86400000; return diff>=0&&diff<=3; }
function isOver(d){ if(!d) return false; return new Date(d)<new Date(TODAY); }
function tagHtml(tag){
  const [bg,tc]=TAG_STYLE[tag]||TAG_STYLE['기타'];
  return `<span class="cardTag" style="background:${bg};color:${tc}">${tag}</span>`;
}
function toast(msg){
  const t=document.getElementById('toast');t.textContent=msg;t.classList.add('on');
  setTimeout(()=>t.classList.remove('on'),2600);
}
function filtered(status){
  let c=CARDS.filter(k=>k.status===status);
  if(tagF) c=c.filter(k=>k.tag===tagF);
  if(myOnly) c=c.filter(k=>ASSIGNS.some(a=>a.card_id===k.id&&a.user_num===ME.user_num));
  const kw=(document.getElementById('kSearch')?.value||'').trim().toLowerCase();
  if(kw) c=c.filter(k=>k.title.toLowerCase().includes(kw)||(k.content||'').toLowerCase().includes(kw));
  return c;
}

/* =========================================================
   렌더
========================================================= */
function render(){
  const all=CARDS;
  const done=all.filter(k=>k.status===4).length;
  const pct=all.length?Math.round(done/all.length*100):0;
  document.getElementById('progFill').style.width=pct+'%';
  document.getElementById('progText').textContent=pct+'% ('+done+'/'+all.length+')';

  document.getElementById('board').innerHTML=LANES.map(lane=>{
    const cards=filtered(lane.status);
    const body=cards.length
      ? cards.map(cardHtml).join('')
      : `<div class="emptyLane" onclick="openForm(0,${lane.status})">
           <div style="font-size:28px;margin-bottom:6px">+</div>
           카드를 여기에 드래그하거나<br>클릭해서 추가하세요
         </div>`;
    return `<div class="lane" id="L${lane.status}"
        style="background:${lane.bg};border-color:${lane.border}"
        ondragover="dOver(event)"
        ondragleave="dLeave(event)"
        ondrop="dDrop(event,${lane.status})">
      <div class="laneHead">
        <div class="laneTitleWrap">
          <div class="laneDot" style="background:${lane.dot}"></div>
          <div class="laneTitle">${lane.label}</div>
        </div>
        <div class="laneRight">
          <span class="laneCnt">${cards.length}</span>
          <button class="laneAdd" onclick="openForm(0,${lane.status})" title="카드 추가">+</button>
        </div>
      </div>
      <div class="laneBody">${body}</div>
    </div>`;
  }).join('');

  renderSide();
}

function cardHtml(k){
  const assigns=getAssigns(k.id);
  const chks=getChks(k.id);
  const doneCnt=chks.filter(c=>c.done).length;
  const near=isNear(k.deadline), over=isOver(k.deadline);
  const dlCls=over?'over':near?'near':'';
  const dlIcon=over?'⚠️':near?'⚠️':'📅';
  const avHtml=assigns.slice(0,4).map(u=>`<div class="cardAV" title="${u.user_name}">${u.user_name[0]}</div>`).join('')
    +(assigns.length>4?`<div class="cardAV" style="background:var(--muted)">+${assigns.length-4}</div>`:'');
  return `<div class="kCard" draggable="true" data-id="${k.id}"
    ondragstart="dStart(event,${k.id})" ondragend="dEnd(event)"
    onclick="openDetail(${k.id})">
    <div class="cardTags">${tagHtml(k.tag||'기타')}${k.status===4?'<span class="cardTag" style="background:var(--greenS);color:var(--green)">✓ 완료</span>':''}</div>
    <div class="cardTitle">${k.title}</div>
    <div class="cardBottom">
      <div class="cardDL ${dlCls}">${k.deadline?dlIcon+' '+k.deadline:'<span style="color:var(--muted)">마감 없음</span>'}</div>
      <div class="cardAVs">${avHtml}</div>
    </div>
    ${chks.length?`<div class="cardProg"><div class="cProgBar"><div class="cProgFill" style="width:${Math.round(doneCnt/chks.length*100)}%"></div></div><div class="cProgLbl">${doneCnt}/${chks.length} 완료</div></div>`:''}
  </div>`;
}

function renderSide(){
  document.getElementById('sideStats').innerHTML=LANES.map(l=>{
    const cnt=CARDS.filter(k=>k.status===l.status).length;
    const pct=CARDS.length?Math.round(cnt/CARDS.length*100):0;
    return `<div class="sideStatRow">
      <div class="sideStatTop"><span style="color:${l.dot}">${l.label}</span><span>${cnt}개</span></div>
      <div class="sideStatBar"><div class="sideStatFill" style="width:${pct}%;background:${l.dot}"></div></div>
    </div>`;
  }).join('');
}

/* =========================================================
   필터
========================================================= */
function setTag(tag,el){
  tagF=tag;
  document.querySelectorAll('.filterBar .chip').forEach(c=>c.classList.remove('on'));
  el.classList.add('on');
  render();
}
function toggleMyOnly(){
  myOnly=!myOnly;
  const b=document.getElementById('myOnlyBtn');
  b.classList.toggle('on',myOnly);
  b.textContent=myOnly?'✓ 내 카드만':'내 카드만';
  render();
}

/* =========================================================
   드래그앤드롭
========================================================= */
function dStart(ev,id){
  dragId=id;
  ev.dataTransfer.effectAllowed='move';
  setTimeout(()=>ev.currentTarget.classList.add('drag'),0);
}
function dEnd(ev){
  ev.currentTarget.classList.remove('drag');
  document.querySelectorAll('.lane').forEach(l=>l.classList.remove('hov'));
  dragId=null;
}
function dOver(ev){
  ev.preventDefault();
  ev.currentTarget.classList.add('hov');
}
function dLeave(ev){
  if(!ev.currentTarget.contains(ev.relatedTarget)) ev.currentTarget.classList.remove('hov');
}
function dDrop(ev,status){
  ev.preventDefault();
  document.querySelectorAll('.lane').forEach(l=>l.classList.remove('hov'));
  if(!dragId) return;
  const card=CARDS.find(k=>k.id===dragId);
  if(card&&card.status!==status){
    card.status=status;
    toast('"'+card.title+'" → '+STATUS_NAMES[status]);
  }
  render();
}

/* =========================================================
   카드 상세 모달
========================================================= */
function openDetail(id){
  const k=CARDS.find(x=>x.id===id);
  if(!k) return;
  const assigns=getAssigns(id);
  const chks=getChks(id);
  const cmts=getCmts(id);
  const doneCnt=chks.filter(c=>c.done).length;
  const writer=getUser(k.writer_num);
  const near=isNear(k.deadline), over=isOver(k.deadline);
  const dlStyle=over||near?'color:var(--rose);font-weight:950':'';
  const dlWarn=over?' ⚠️ 마감 초과':near?' ⚠️ 마감 임박':'';

  const assignHtml=assigns.map(u=>`<div class="aChip">
    <div style="width:20px;height:20px;border-radius:7px;background:var(--primary);color:#fff;display:grid;place-items:center;font-size:10px;font-weight:950">${u.user_name[0]}</div>
    ${u.user_name}
    ${u.user_num!==ME.user_num?`<button class="aChipRm" onclick="rmAssign(${id},${u.user_num})">×</button>`:''}
  </div>`).join('')||'<span style="font-size:12px;color:var(--muted)">없음</span>';

  const chkHtml=chks.map(c=>`<div class="chkItem" id="ci_${c.id}">
    <input type="checkbox" ${c.done?'checked':''} onchange="togChk(${c.id},${id},this.checked)">
    <span class="chkLbl${c.done?' done':''}">${c.content}</span>
    <button class="chkDel" onclick="delChk(${c.id},${id})">✕</button>
  </div>`).join('')||'<div style="color:var(--muted);font-size:13px;padding:8px 0">체크 항목 없음</div>';

  const cmtHtml=cmts.map(c=>{const u=getUser(c.user_num); return `<div class="cmt">
    <div class="cmtAv">${u.user_name[0]}</div>
    <div class="cmtB">
      <div class="cmtMeta"><span class="cmtAuthor">${u.user_name}</span><span class="cmtDate">${c.date}</span></div>
      <div class="cmtText">${c.text}</div>
      <div class="cmtActs">
        ${c.user_num===ME.user_num?`<button onclick="editCmt(${c.id},${id})">수정</button><button class="cdel" onclick="delCmt(${c.id},${id})">삭제</button>`:'<button>답글</button>'}
      </div>
    </div>
  </div>`;}).join('')||'<div style="color:var(--muted);font-size:13px;padding:8px 0">댓글 없음</div>';

  document.getElementById('modal').classList.remove('sm');
  document.getElementById('mTitle').textContent='카드 상세';
  document.getElementById('mBody').innerHTML=`
  <div class="detailGrid">
    <div class="detailL">
      <!-- 제목 + 상태 -->
      <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap">
        <h2 style="margin:0;font-size:20px;font-weight:950;letter-spacing:-.04em">${k.title}</h2>
        <select class="statusSel s${k.status}" id="stSel_${id}" onchange="changeStatus(${id},this.value,this)">
          <option value="1" ${k.status===1?'selected':''}>할 일 목록</option>
          <option value="2" ${k.status===2?'selected':''}>진행 중</option>
          <option value="3" ${k.status===3?'selected':''}>검토</option>
          <option value="4" ${k.status===4?'selected':''}>완료</option>
        </select>
      </div>
      <!-- 메타 -->
      <div style="display:flex;gap:10px;flex-wrap:wrap;align-items:center">
        ${tagHtml(k.tag||'기타')}
        ${k.deadline?`<span style="font-size:13px;font-weight:900;${dlStyle}">📅 ${k.deadline}${dlWarn}</span>`:'<span style="font-size:13px;color:var(--muted)">마감일 없음</span>'}
        <span style="font-size:12px;color:var(--muted)">작성자: ${writer.user_name} · ${k.reg_date}</span>
      </div>
      <!-- 캘린더 연동 안내 -->
      <div class="calLink">📅 마감일이 설정되면 팀 캘린더에 자동으로 연동됩니다 (KB-011 칸반↔캘린더 연동)</div>
      <!-- 내용 -->
      <div class="dsec"><h4>내용</h4><div class="dContent">${k.content||'내용 없음'}</div></div>
      <!-- 체크리스트 -->
      <div class="dsec">
        <h4>체크리스트 (${doneCnt}/${chks.length})</h4>
        ${chks.length?`<div style="margin-bottom:8px"><div class="cProgBar"><div class="cProgFill" style="width:${chks.length?Math.round(doneCnt/chks.length*100):0}%"></div></div></div>`:''}
        <div class="chkList">${chkHtml}</div>
        <div class="addRow">
          <input id="nChk_${id}" placeholder="체크 항목 추가 후 Enter" onkeydown="if(event.key==='Enter')addChk(${id})">
          <button class="btn sm" onclick="addChk(${id})">추가</button>
        </div>
      </div>
      <!-- 댓글 -->
      <div class="dsec">
        <h4>댓글 (${cmts.length})</h4>
        <div class="cmtList">${cmtHtml}</div>
        <div class="addRow" style="margin-top:12px">
          <input id="nCmt_${id}" placeholder="댓글 입력 후 Enter" onkeydown="if(event.key==='Enter')addCmt(${id})">
          <button class="btn sm primary" onclick="addCmt(${id})">전송</button>
        </div>
      </div>
    </div>
    <div class="detailR">
      <!-- 담당자 -->
      <div class="rBox">
        <h5>담당자</h5>
        <div class="assignChips" id="ach_${id}">${assignHtml}</div>
        <select class="assignAddSel" id="asel_${id}" onchange="addAssign(${id},this.value)">
          <option value="">+ 담당자 추가</option>
          ${TEAM_MEMBERS.map(m=>`<option value="${m.user_num}">${m.user_name}</option>`).join('')}
        </select>
      </div>
      <!-- 이력 -->
      <div class="rBox">
        <h5>작업 이력</h5>
        <div class="histItem">📝 ${k.reg_date} — 카드 생성</div>
        ${k.status===4?'<div class="histItem">✅ 완료 처리됨</div>':''}
        <div class="histItem">👤 담당자 ${assigns.length}명</div>
      </div>
      <!-- 액션 -->
      <div style="display:flex;flex-direction:column;gap:8px">
        <button class="btn" onclick="openForm(${id},${k.status})">✏️ 카드 수정</button>
        <button class="btn danger" onclick="deleteCard(${id})">🗑️ 카드 삭제</button>
        <button class="btn" onclick="closeModal();location.href='04_calendar.html'">📅 캘린더에서 보기</button>
      </div>
    </div>
  </div>`;
  openModal();
}

/* =========================================================
   상태 변경
========================================================= */
function changeStatus(id,status,sel){
  const k=CARDS.find(x=>x.id===id);
  if(k){ k.status=Number(status); toast('"'+k.title+'" → '+STATUS_NAMES[status]); }
  sel.className='statusSel s'+status;
  render();
}

/* =========================================================
   체크리스트
========================================================= */
function togChk(cid,cardId,val){
  const c=CHECKLISTS.find(x=>x.id===cid); if(c) c.done=val;
  openDetail(cardId);
}
function delChk(cid,cardId){
  if(!confirm('삭제할까요?')) return;
  const i=CHECKLISTS.findIndex(x=>x.id===cid); if(i>=0) CHECKLISTS.splice(i,1);
  openDetail(cardId);
}
function addChk(cardId){
  const inp=document.getElementById('nChk_'+cardId);
  if(!inp||!inp.value.trim()) return;
  CHECKLISTS.push({id:++nextId,card_id:cardId,content:inp.value.trim(),done:false});
  openDetail(cardId);
}

/* =========================================================
   댓글
========================================================= */
function addCmt(cardId){
  const inp=document.getElementById('nCmt_'+cardId);
  if(!inp||!inp.value.trim()){alert('댓글을 입력하세요');return;}
  COMMENTS.push({id:++nextId,card_id:cardId,user_num:ME.user_num,text:inp.value.trim(),date:TODAY,del:false});
  openDetail(cardId);
}
function editCmt(cid,cardId){
  const c=COMMENTS.find(x=>x.id===cid);
  const txt=prompt('댓글 수정',c?.text||''); if(txt===null) return;
  if(c) c.text=txt; openDetail(cardId);
}
function delCmt(cid,cardId){
  if(!confirm('삭제할까요?')) return;
  const c=COMMENTS.find(x=>x.id===cid); if(c) c.del=true; openDetail(cardId);
}

/* =========================================================
   담당자
========================================================= */
function addAssign(cardId,userNum){
  if(!userNum) return;
  if(ASSIGNS.some(a=>a.card_id===cardId&&a.user_num==userNum)){alert('이미 담당자입니다.');return;}
  ASSIGNS.push({id:++nextId,card_id:cardId,user_num:Number(userNum)});
  openDetail(cardId); render();
}
function rmAssign(cardId,userNum){
  const i=ASSIGNS.findIndex(a=>a.card_id===cardId&&a.user_num===userNum);
  if(i>=0) ASSIGNS.splice(i,1);
  openDetail(cardId); render();
}

/* =========================================================
   카드 추가 / 수정 폼
========================================================= */
function openForm(editId, defaultStatus){
  const k=editId?CARDS.find(x=>x.id===editId):null;
  const curAssigns=editId?ASSIGNS.filter(a=>a.card_id===editId).map(a=>a.user_num):[ME.user_num];

  document.getElementById('modal').classList.add('sm');
  document.getElementById('mTitle').textContent=k?'카드 수정':'카드 추가';
  document.getElementById('mBody').innerHTML=`
  <div class="fGrid">
    <div class="f full">
      <label>카드 제목 <span style="color:var(--rose)">*</span></label>
      <input id="fTitle" value="${k?k.title:''}" placeholder="카드 제목을 입력하세요">
    </div>
    <div class="f">
      <label>태그</label>
      <select id="fTag">
        ${['UI','개발','기능','인증','API','기타'].map(t=>`<option value="${t}" ${k&&k.tag===t?'selected':''}>${t}</option>`).join('')}
      </select>
    </div>
    <div class="f">
      <label>상태</label>
      <select id="fStatus">
        ${[1,2,3,4].map(s=>`<option value="${s}" ${(k?k.status:defaultStatus||1)===s?'selected':''}>${STATUS_NAMES[s]}</option>`).join('')}
      </select>
    </div>
    <div class="f full">
      <label>마감일</label>
      <input id="fDeadline" type="date" value="${k&&k.deadline?k.deadline:''}">
    </div>
    <div class="f full">
      <label>내용</label>
      <textarea id="fContent" placeholder="카드에 대한 상세 내용">${k?k.content||'':''}</textarea>
    </div>
    <div class="f full">
      <label>담당자</label>
      <div class="memberCheckWrap">
        ${TEAM_MEMBERS.map(m=>`
          <label class="memberCheckItem">
            <input type="checkbox" id="fAss_${m.user_num}" ${curAssigns.includes(m.user_num)?'checked':''}
              style="accent-color:var(--primary)">
            ${m.user_name}
          </label>`).join('')}
      </div>
    </div>
  </div>
  <div class="acts">
    <button class="btn" onclick="closeModal()">취소</button>
    ${k?`<button class="btn danger" onclick="deleteCard(${k.id})">삭제</button>`:''}
    <button class="btn primary" onclick="saveCard(${k?k.id:0})">${k?'수정 저장':'카드 추가 +'}</button>
  </div>`;
  openModal();
  // 포커스
  setTimeout(()=>document.getElementById('fTitle')?.focus(),100);
}

function saveCard(editId){
  const title=(document.getElementById('fTitle')?.value||'').trim();
  if(!title){alert('카드 제목을 입력하세요');document.getElementById('fTitle')?.focus();return;}
  const tag   =document.getElementById('fTag')?.value||'기타';
  const status=Number(document.getElementById('fStatus')?.value||1);
  const dead  =document.getElementById('fDeadline')?.value||null;
  const cont  =document.getElementById('fContent')?.value||'';
  const selU  =TEAM_MEMBERS.filter(m=>document.getElementById('fAss_'+m.user_num)?.checked).map(m=>m.user_num);

  if(editId){
    const k=CARDS.find(x=>x.id===editId);
    if(k){Object.assign(k,{title,tag,status,deadline:dead,content:cont});}
    // 담당자 갱신
    const ex=ASSIGNS.filter(a=>a.card_id===editId).map(a=>a.user_num);
    selU.filter(u=>!ex.includes(u)).forEach(u=>ASSIGNS.push({id:++nextId,card_id:editId,user_num:u}));
    ex.filter(u=>!selU.includes(u)).forEach(u=>{const i=ASSIGNS.findIndex(a=>a.card_id===editId&&a.user_num===u);if(i>=0)ASSIGNS.splice(i,1);});
    toast('카드가 수정되었습니다.');
  } else {
    const newId=++nextId;
    CARDS.push({id:newId,title,tag,status,deadline:dead,content:cont,writer_num:ME.user_num,reg_date:TODAY});
    selU.forEach(u=>ASSIGNS.push({id:++nextId,card_id:newId,user_num:u}));
    toast('카드가 추가되었습니다! 🎉');
  }
  closeModal();
  render();
}

function deleteCard(id){
  if(!confirm('카드를 삭제하시겠습니까?')) return;
  const i=CARDS.findIndex(x=>x.id===id); if(i>=0) CARDS.splice(i,1);
  closeModal(); render(); toast('카드가 삭제되었습니다.');
}

/* =========================================================
   모달 열기/닫기
========================================================= */
function openModal(){ document.getElementById('mLayer').classList.add('open'); }
function closeModal(){
  document.getElementById('mLayer').classList.remove('open');
  document.getElementById('modal').classList.remove('sm');
}

/* =========================================================
   초기 렌더
========================================================= */
render();
