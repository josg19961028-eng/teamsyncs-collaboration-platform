// ── NAVBAR SCROLL ACTION ──
window.addEventListener('scroll', () => {
  const navbar = document.getElementById('navbar');
  if (window.scrollY > 50) {
    navbar.classList.add('scrolled');
  } else {
    navbar.classList.remove('scrolled');
  }
});

// ── SCREEN SHOWCASE TAB SWITCHER ──
function switchTab(tabId, btn) {
  // 모든 탭 컨텐츠 숨기기
  document.querySelectorAll('.tabContent').forEach(content => {
    content.classList.remove('active');
  });
  // 모든 탭 버튼 비활성화
  document.querySelectorAll('.tabBtn').forEach(button => {
    button.classList.remove('active');
  });

  // 해당 탭 및 버튼 활성화
  document.getElementById(tabId).classList.add('active');
  btn.classList.add('active');
}

// ── DATABASE MODAL LOGIC ──
function openModal() {
  document.getElementById('dbModal').classList.add('active');
  document.body.style.overflow = 'hidden';
}

function closeModal() {
  document.getElementById('dbModal').classList.remove('active');
  document.body.style.overflow = 'auto';
}

// ESC 키 입력 시 모달 닫기
window.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    closeModal();
  }
});

// ── TEAM MEMBERS DATA DYNAMIC RENDERING ──
const teamMembers = [
  { name: '이민호', role: '팀장 / 풀스택', intro: '아키텍처 설계, Spring Security 구축, 실시간 채팅방 Websocket 및 메인 대시보드 API 설계', color: '#6c5ce7', rb: 'roleLeader', techs: ['Spring Boot', 'WebSocket', 'Oracle'] },
  { name: '김민준', role: '백엔드 개발', intro: 'REST API 개발, MyBatis 연동, 캘린더 API 구현', color: '#34b77b', rb: 'roleMember', techs: ['Spring Boot', 'MyBatis', 'API 설계'] },
  { name: '이수진', role: '매니저 / 프론트', intro: '캘린더 UI, FullCalendar 연동, 화면 레이아웃 구현', color: '#f0a04b', rb: 'roleManager', techs: ['JSP', 'jQuery', 'FullCalendar'] },
  { name: '박지훈', role: 'DB 설계', intro: 'ERD 설계, 테이블 정의서 작성, 회의록/보관함 기능', color: '#8f65f6', rb: 'roleMember', techs: ['Oracle', 'ERD', 'MyBatis'] },
  { name: '최유진', role: '프론트 / QA', intro: '칸반 보드 UI, 드래그앤드롭 구현, QA 테스트 케이스', color: '#e46aa1', rb: 'roleMember', techs: ['JavaScript', 'CSS', '테스트'] },
  { name: '정서연', role: '프론트 / 문서', intro: '팀원 관리 UI, 발표 자료 작성, 기능 테스트', color: '#37c4da', rb: 'roleMember', techs: ['JSP', 'jQuery', '문서화'] },
];

const roleBadgeMap = {
  roleLeader: 'background:var(--roseS);color:var(--rose)',
  roleManager: 'background:var(--orangeS);color:#c46121',
  roleMember: 'background:var(--blueS);color:var(--primary)',
};

document.addEventListener('DOMContentLoaded', () => {
  const teamGrid = document.getElementById('teamGrid');
  if (teamGrid) {
    teamMembers.forEach(m => {
      const card = document.createElement('div');
      card.className = 'teamCard';
      
      const badgeStyle = roleBadgeMap[m.rb] || '';
      const initial = m.name.charAt(0);
      
      const techSpans = m.techs.map(t => `<span class="teamTech">${t}</span>`).join('');
      
      card.innerHTML = `
        <div class="teamHead">
          <div class="teamInitial" style="background:${m.color}">${initial}</div>
          <div class="teamName">${m.name}</div>
          <span class="teamBadge" style="${badgeStyle}">${m.role}</span>
        </div>
        <div class="teamIntro">"${m.intro}"</div>
        <div class="teamDivider"></div>
        <div class="teamTechs">${techSpans}</div>
      `;
      teamGrid.appendChild(card);
    });
  }
});