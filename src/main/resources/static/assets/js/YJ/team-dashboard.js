/* assets/js/YJ/team-dashboard.js */

let teamCalendar;
let teamCalendarFilter = 'ALL';
let selectedTeamScheduleEvent = null;

/* TODO: 백엔드 연동 전까지는 빈 배열.
   실데이터 연결 시 컨트롤러에서 내려주는 값으로 교체 예정 */
const kanban = [];
const notices = [];
const members = [];

document.addEventListener('DOMContentLoaded', function () {
    initTeamCalendar();
    initTeamCalendarFilter();
    renderAll();
});

/* =========================
   팀 홈 FullCalendar
========================= */
function initTeamCalendar() {
    const calendarEl = document.getElementById('teamHomeCalendar');

    if (!calendarEl || typeof FullCalendar === 'undefined') {
        console.log('teamHomeCalendar 또는 FullCalendar를 찾을 수 없습니다.');
        return;
    }

    teamCalendar = new FullCalendar.Calendar(calendarEl, {
        initialView: 'dayGridMonth',
        locale: 'ko',
        height: 'auto',

        headerToolbar: {
            left: 'prev,next today',
            center: 'title',
            right: ''
        },

        buttonText: {
            today: '오늘'
        },

        dayMaxEventRows: 2,
        moreLinkText: '더보기',
		
		// 날짜 클릭 시 해당 날짜로 일정 등록 모달 열기
		dateClick: function (info) {
		    openM('addSch', info.dateStr);
        },


        events: function (fetchInfo, successCallback, failureCallback) {
            // 지금은 일정만 조회
            // 나중에 칸반까지 합치면 /calendar/team-events?team_num= 로 변경
            const url = calendarEl.dataset.eventsUrl || '/calendar/current-team-events';

            const startDate = fetchInfo.startStr.slice(0, 10);
            const endDate = fetchInfo.endStr.slice(0, 10);
            const separator = url.includes('?') ? '&' : '?';
            const rangedEventsUrl =
                `${url}${separator}` +
                `start=${encodeURIComponent(startDate)}` +
                `&end=${encodeURIComponent(endDate)}`;

            fetch(rangedEventsUrl)
                .then(response => response.json())
                .then(data => {
                    const filtered = data.filter(event => {
                        if (teamCalendarFilter === 'ALL') return true;
                        return event.type === teamCalendarFilter;
                    });

                    successCallback(filtered);
                    renderUpcomingFromEvents(filtered);
                })
                .catch(error => {
                    console.error(error);
                    failureCallback(error);
                });
        },

		eventClick: function (info) {
		    openTeamScheduleDetailModal(info.event);
        }
    });

    teamCalendar.render();
}

function initTeamCalendarFilter() {
    document.querySelectorAll('.team-calendar-head .chip').forEach(function (btn) {
        btn.addEventListener('click', function () {
            document.querySelectorAll('.team-calendar-head .chip')
                .forEach(c => c.classList.remove('active'));

            this.classList.add('active');
            teamCalendarFilter = this.dataset.filter;

            if (teamCalendar) {
                teamCalendar.refetchEvents();
            }
        });
    });
}

function renderUpcomingFromEvents(events) {
    const upcomingList = document.getElementById('upcomingList');
    if (!upcomingList) return;

    const today = getToday();

    const upcoming = events
        .filter(e => getEventDate(e) >= today)
        .sort((a, b) => getEventDate(a).localeCompare(getEventDate(b)))
        .slice(0, 4);

    if (upcoming.length === 0) {
        upcomingList.innerHTML = '<div class="emptyBox">예정된 일정이 없습니다</div>';
        return;
    }

    upcomingList.innerHTML = upcoming.map(e => {
        const date = getEventDate(e);
        const typeName = e.type === 'KANBAN' ? '칸반' : '일정';

        return `
            <div class="agendaItem">
                <div class="agTime">${date.slice(5)}</div>
                <div>
                    <div class="agTitle">${e.title}</div>
                    <div class="agSub">${typeName}</div>
                </div>
            </div>
        `;
    }).join('');
}

/* =========================
   공지 / 칸반 / 팀원 / 차트
========================= */
function roleBadge(r) {
    const m = {
        LEADER: ['roleLeader', '팀장'],
        MANAGER: ['roleManager', '매니저'],
        MEMBER: ['roleMember', '팀원']
    };

    const [c, t] = m[r] || m.MEMBER;
    return `<span class="roleBadge ${c}">${t}</span>`;
}

function renderNotices() {
    const noticeList = document.getElementById('noticeList');
    if (!noticeList) return;

    if (notices.length === 0) {
        noticeList.innerHTML = '<div class="emptyBox">등록된 공지가 없습니다</div>';
        return;
    }

    noticeList.innerHTML = notices.map(n => `
        <div class="noticeItem ${n.pinned ? 'pinned' : ''}">
            <div class="noticeTitle">
                ${n.pinned ? '<span class="pinnedBadge">📌 고정</span>' : ''}
                ${!n.read ? '<span class="newBadge">NEW</span>' : ''}
                ${n.title}
            </div>
            <div class="noticeMeta">${n.author} · ${n.date} · 조회 ${n.views}</div>
        </div>
    `).join('');
}

function renderMiniKanban() {
    const miniKanban = document.getElementById('miniKanban');
    if (!miniKanban) return;

    updateKanbanProgress();

    if (kanban.length === 0) {
        miniKanban.innerHTML = '<div class="emptyBox">등록된 칸반 카드가 없습니다</div>';
        return;
    }

    const lanes = [
        {k: 'TODO', label: '할 일'},
        {k: 'DOING', label: '진행 중'},
        {k: 'REVIEW', label: '검토 중'},
        {k: 'DONE', label: '완료'}
    ];

    const tagColors = {
        FE: ['#f1edff', '#6c5ce7'],
        BE: ['#eef9f4', '#34b77b'],
        UX: ['#fff5ea', '#c46121']
    };

    miniKanban.innerHTML = lanes.map(l => {
        const cards = kanban.filter(c => c.status === l.k);

        return `
            <div class="lane">
                <div class="laneHead">${l.label}<span class="cnt">${cards.length}</span></div>
                ${cards.map(c => {
                    const [bg, tc] = tagColors[c.tag] || ['#f4f6fb', '#555'];
                    const over = c.deadline < new Date().toISOString().slice(0, 10);

                    return `
                        <div class="kCard">
                            <span class="kTag" style="background:${bg};color:${tc}">${c.tag}</span>
                            <div>${c.title}</div>
                            <div class="deadline ${over ? 'over' : ''}">${over ? '⚠ ' : ''} ~${c.deadline}</div>
                        </div>
                    `;
                }).join('')}
            </div>
        `;
    }).join('');
}

function updateKanbanProgress() {
    const label = document.getElementById('kanbanProgressLabel');
    const bar = document.getElementById('kanbanProgressBar');
    if (!label || !bar) return;

    const total = kanban.length;
    const done = kanban.filter(c => c.status === 'DONE').length;
    const percent = total === 0 ? 0 : Math.round((done / total) * 100);

    label.textContent = `진행률 ${percent}%`;
    bar.style.width = `${percent}%`;
}

function renderMembers() {
    const memberList = document.getElementById('memberList');
    if (!memberList) return;

    if (members.length === 0) {
        memberList.innerHTML = '<div class="emptyBox">팀에 팀원이 없습니다</div>';
        return;
    }

    memberList.innerHTML = members.map(m => `
        <div class="memberRow">
            <div class="mAv">${m.name[0]}</div>
            <div style="flex:1">
                <div class="mName">${m.name} ${roleBadge(m.role)}</div>
                <div class="mRole">완료 ${m.done}개 · ${m.lastActive}</div>
            </div>
        </div>
    `).join('');
}

/* TODO: 백엔드 연동 전까지는 빈 배열. 주별 일정 집계 쿼리 연결 시 교체 예정 */
const weeklyScheduleCounts = [];

function renderCharts() {
    if (typeof Chart === 'undefined') return;

    const lc = document.getElementById('chartLine');
    const lcEmpty = document.getElementById('chartLineEmpty');
    const bc = document.getElementById('chartBar');
    const bcEmpty = document.getElementById('chartBarEmpty');

    if (lc && lcEmpty) {
        if (weeklyScheduleCounts.length === 0) {
            lc.style.display = 'none';
            lcEmpty.style.display = 'block';
        } else {
            lc.style.display = 'block';
            lcEmpty.style.display = 'none';

            if (lc._chart) lc._chart.destroy();

            lc._chart = new Chart(lc, {
                type: 'line',
                data: {
                    labels: weeklyScheduleCounts.map(w => w.label),
                    datasets: [{
                        label: '일정',
                        data: weeklyScheduleCounts.map(w => w.count),
                        borderColor: '#6c5ce7',
                        backgroundColor: 'rgba(108,92,231,.1)',
                        fill: true,
                        tension: .4,
                        pointBackgroundColor: '#6c5ce7'
                    }]
                },
                options: {
                    plugins: {legend: {display: false}},
                    scales: {y: {beginAtZero: true, ticks: {stepSize: 1}}}
                }
            });
        }
    }

    if (bc && bcEmpty) {
        const doneMembers = members.filter(m => m.done > 0);

        if (doneMembers.length === 0) {
            bc.style.display = 'none';
            bcEmpty.style.display = 'block';
        } else {
            bc.style.display = 'block';
            bcEmpty.style.display = 'none';

            if (bc._chart) bc._chart.destroy();

            bc._chart = new Chart(bc, {
                type: 'bar',
                data: {
                    labels: doneMembers.map(m => m.name),
                    datasets: [{
                        label: '완료',
                        data: doneMembers.map(m => m.done),
                        backgroundColor: 'rgba(108,92,231,.7)',
                        borderRadius: 8
                    }]
                },
                options: {
                    plugins: {legend: {display: false}},
                    scales: {y: {beginAtZero: true, ticks: {stepSize: 1}}}
                }
            });
        }
    }
}

function renderAll() {
    //renderNotices();
    renderMiniKanban();
    //renderMembers();
    renderCharts();
}

/* =========================
   일정 등록 모달
========================= */
function openM(type, data) {
    const layer =
        document.getElementById('mLayer');

    const body =
        document.getElementById('mBody');

    const title =
        document.getElementById('mTitle2');

    if (!layer || !body || !title) {
        return;
    }

    if (type === 'addSch') {
        title.textContent = '일정 등록';

        const selectedDate =
            typeof data === 'string'
                ? data
                : getToday();

        body.innerHTML = `
            <div class="fG">

                <div class="f full">
                    <label for="sT">제목 *</label>

                    <input type="text"
                           id="sT"
                           maxlength="200"
                           placeholder="일정 제목">
                </div>

                <div class="f">
                    <label for="sCa">카테고리</label>

                    <select id="sCa">
                        <option value="일반 일정">
                            일반 일정
                        </option>

                        <option value="회의">
                            회의
                        </option>

                        <option value="개발">
                            개발
                        </option>

                        <option value="마감">
                            마감
                        </option>
                    </select>
                </div>

                <div class="f">
                    <label for="sColor">색상</label>

                    <input type="color"
                           id="sColor"
                           value="#6C5CE7">
                </div>

                <div class="f">
                    <label for="sS">시작일 *</label>

                    <input type="date"
                           id="sS"
                           value="${selectedDate}">
                </div>

                <div class="f">
                    <label for="sE">종료일 *</label>

                    <input type="date"
                           id="sE"
                           value="${selectedDate}">
                </div>

                <!-- 종일 일정 스위치 -->
                <div class="f full schedule-all-day-row">
                    <label class="schedule-switch-label"
                           for="sAllDay">

                        <span>종일 일정</span>

                        <span class="schedule-switch">
                            <input type="checkbox"
                                   id="sAllDay"
                                   checked
                                   onchange="toggleScheduleTimeFields()">

                            <span class="schedule-switch-slider"></span>
                        </span>
                    </label>
                </div>

				<!-- 종일 일정 해제 시에만 표시 -->
				<div class="f schedule-time-field" hidden>
				    <label for="sST">시작 시간 *</label>

				    <input type="time"
				           id="sST"
				           value="09:00"
				           disabled>
				</div>

				<div class="f schedule-time-field" hidden>
				    <label for="sET">종료 시간 *</label>

				    <input type="time"
				           id="sET"
				           value="10:00"
				           disabled>
				</div>

                <div class="f full">
                    <label for="sCo">내용</label>

                    <textarea id="sCo"
                              maxlength="2000"
                              placeholder="일정 상세 내용"></textarea>
                </div>

            </div>

            <div class="acts">
                <button type="button"
                        class="btn"
                        onclick="closeM()">
                    취소
                </button>

                <button type="button"
                        class="btn primary"
                        id="btnSaveSch"
                        onclick="saveSch()">
                    저장
                </button>
            </div>
        `;

        toggleScheduleTimeFields();
    }

    layer.classList.add('open');

    const scheduleTitle =
        document.getElementById('sT');

    if (scheduleTitle) {
        scheduleTitle.focus();
    }
}


function closeM() {
    const layer =
        document.getElementById('mLayer');

    if (layer) {
        layer.classList.remove('open');
    }
}


function toggleScheduleTimeFields() {
    const allDay =
        document.getElementById('sAllDay');

    const startTime =
        document.getElementById('sST');

    const endTime =
        document.getElementById('sET');

    const timeFields =
        document.querySelectorAll(
            '.schedule-time-field'
        );

    if (!allDay || !startTime || !endTime) {
        return;
    }

    const isAllDay = allDay.checked;

    /*
     * 종일 일정 ON
     * → 시간 영역 숨김
     *
     * 종일 일정 OFF
     * → 시간 영역 표시
     */
    timeFields.forEach(function (field) {
        field.hidden = isAllDay;
    });

    startTime.disabled = isAllDay;
    endTime.disabled = isAllDay;
}


async function saveSch() {
    const title =
        document.getElementById('sT')?.value.trim();

    const category =
        document.getElementById('sCa')?.value;

    const color =
        document.getElementById('sColor')?.value;

    const startDate =
        document.getElementById('sS')?.value;

    const endDate =
        document.getElementById('sE')?.value;

    const startTime =
        document.getElementById('sST')?.value;

    const endTime =
        document.getElementById('sET')?.value;

    const content =
        document.getElementById('sCo')?.value.trim() || '';

    const allDayChecked =
        document.getElementById('sAllDay')?.checked;

    const saveButton =
        document.getElementById('btnSaveSch');

    if (!title) {
        alert('제목을 입력하세요.');
        return;
    }

    if (!startDate || !endDate) {
        alert('시작일과 종료일을 입력하세요.');
        return;
    }

    const allDay = allDayChecked ? 1 : 2;

    let startValue = startDate;
    let endValue = endDate;

    if (allDay === 2) {
        if (!startTime || !endTime) {
            alert('시작 시간과 종료 시간을 입력하세요.');
            return;
        }

        startValue = `${startDate}T${startTime}`;
        endValue = `${endDate}T${endTime}`;

        if (endValue <= startValue) {
            alert('종료 일시는 시작 일시보다 늦어야 합니다.');
            return;
        }
    } else {
        if (endDate < startDate) {
            alert('종료일은 시작일보다 빠를 수 없습니다.');
            return;
        }
    }

    const scheduleData = {
        title: title,
        category: category,
        color: color,
        content: content,
        start_date: startValue,
        end_date: endValue,
        all_day: allDay
    };

    const csrfToken =
        document.querySelector(
            'meta[name="csrf-token"]'
        )?.content;

    const csrfHeader =
        document.querySelector(
            'meta[name="csrf-header"]'
        )?.content;

    const headers = {
        'Content-Type': 'application/json; charset=UTF-8'
    };

    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    if (saveButton) {
        saveButton.disabled = true;
        saveButton.textContent = '저장 중...';
    }

    try {
        const response = await fetch('/calendar/write', {
            method: 'POST',
            headers: headers,
            credentials: 'same-origin',
            body: JSON.stringify(scheduleData)
        });

        if (!response.ok) {
            throw new Error(`HTTP 오류: ${response.status}`);
        }

        const result = await response.json();

        if (result.result === 'success') {
            closeM();
            showToast('일정이 등록되었습니다.');

            if (teamCalendar) {
                teamCalendar.refetchEvents();
            }

            return;
        }

        alert(
            result.message ||
            '일정 등록에 실패했습니다.'
        );

    } catch (error) {
        console.error('일정 등록 오류:', error);
        alert('일정 등록 중 오류가 발생했습니다.');

    } finally {
        if (saveButton) {
            saveButton.disabled = false;
            saveButton.textContent = '저장';
        }
    }
}


/* =========================
   공통 유틸
========================= */
function getEventDate(e) {
    return e.start
        ? e.start.slice(0, 10)
        : '';
}


function getToday() {
    const today = new Date();

    const year = today.getFullYear();
    const month =
        String(today.getMonth() + 1).padStart(2, '0');
    const day =
        String(today.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
}


function showToast(message) {
    const toast =
        document.getElementById('toast');

    if (!toast) return;

    toast.textContent = message;
    toast.classList.add('show');

    setTimeout(function () {
        toast.classList.remove('show');
    }, 2500);
}

function openTeamScheduleDetailModal(calendarEvent) {
    selectedTeamScheduleEvent = calendarEvent;

    const layer = document.getElementById('mLayer');
    const modalTitle = document.getElementById('mTitle2');
    const body = document.getElementById('mBody');

    if (!layer || !modalTitle || !body || !calendarEvent) {
        console.error('팀 일정 상세 모달 요소를 찾을 수 없습니다.');
        return;
    }

    const props = calendarEvent.extendedProps || {};

    const title = calendarEvent.title || '제목 없음';
    const category = props.category || '일반 일정';
    const content = props.content || '등록된 내용이 없습니다.';

    const typeText =
        props.type === 'KANBAN'
            ? '칸반'
            : '일정';

    const startText = formatTeamCalendarDate(
        calendarEvent.start,
        calendarEvent.allDay
    );

    const endText = formatTeamCalendarEndDate(
        calendarEvent.end,
        calendarEvent.start,
        calendarEvent.allDay
    );

    const rawColor =
        calendarEvent.backgroundColor ||
        props.color ||
        '#6C5CE7';

    const color =
        /^#[0-9a-fA-F]{6}$/.test(rawColor)
            ? rawColor
            : '#6C5CE7';

    /*
     * 나중에 칸반 일정이 합쳐질 경우
     * 칸반에는 일정 수정·삭제 버튼을 표시하지 않음
     */
    const manageButtons =
        props.type === 'KANBAN'
            ? ''
            : `
                <button type="button"
                        class="btn danger"
                        onclick="deleteTeamSchedule()">
                    삭제
                </button>

                <button type="button"
                        class="btn"
                        onclick="openTeamScheduleEditModal()">
                    수정
                </button>
            `;

    modalTitle.textContent = '일정 상세';

    body.innerHTML = `
        <div class="schedule-detail">

            <div class="schedule-detail-title">
                <span class="schedule-detail-color"
                      style="background:${color}">
                </span>

                <div>
                    <h3>${escapeTeamHtml(title)}</h3>

                    <span class="schedule-detail-category">
                        ${escapeTeamHtml(category)}
                    </span>
                </div>
            </div>

            <div class="schedule-detail-grid">

                <div class="schedule-detail-item">
                    <span class="schedule-detail-label">
                        구분
                    </span>

                    <strong>
                        ${escapeTeamHtml(typeText)}
                    </strong>
                </div>

                <div class="schedule-detail-item">
                    <span class="schedule-detail-label">
                        일정 유형
                    </span>

                    <strong>
                        ${
                            calendarEvent.allDay
                                ? '종일 일정'
                                : '시간 지정 일정'
                        }
                    </strong>
                </div>

                <div class="schedule-detail-item">
                    <span class="schedule-detail-label">
                        시작
                    </span>

                    <strong>
                        ${escapeTeamHtml(startText)}
                    </strong>
                </div>

                <div class="schedule-detail-item">
                    <span class="schedule-detail-label">
                        종료
                    </span>

                    <strong>
                        ${escapeTeamHtml(endText)}
                    </strong>
                </div>

                <div class="schedule-detail-item full">
                    <span class="schedule-detail-label">
                        내용
                    </span>

                    <div class="schedule-detail-content">
                        ${
                            escapeTeamHtml(content)
                                .replace(/\n/g, '<br>')
                        }
                    </div>
                </div>

            </div>

            <div class="acts">
                ${manageButtons}

                <button type="button"
                        class="btn primary"
                        onclick="closeM()">
                    확인
                </button>
            </div>

        </div>
    `;

    layer.classList.add('open');
}

function formatTeamCalendarDate(date, allDay) {
    if (!date) {
        return '-';
    }

    const dateText =
        formatTeamLocalDate(date);

    if (allDay) {
        return dateText;
    }

    return dateText + ' ' +
           formatTeamLocalTime(date);
}


function formatTeamCalendarEndDate(
    endDate,
    startDate,
    allDay
) {
    if (!endDate) {
        return formatTeamCalendarDate(
            startDate,
            allDay
        );
    }

    const displayEnd =
        new Date(endDate);

    /*
     * FullCalendar 종일 일정 종료일은
     * 실제 종료일 다음 날로 전달됨
     */
    if (allDay) {
        displayEnd.setDate(
            displayEnd.getDate() - 1
        );

        return formatTeamLocalDate(displayEnd);
    }

    return formatTeamLocalDate(displayEnd)
        + ' '
        + formatTeamLocalTime(displayEnd);
}


function formatTeamLocalDate(date) {
    const d =
        date instanceof Date
            ? date
            : new Date(date);

    const year =
        d.getFullYear();

    const month =
        String(d.getMonth() + 1)
            .padStart(2, '0');

    const day =
        String(d.getDate())
            .padStart(2, '0');

    return `${year}-${month}-${day}`;
}


function formatTeamLocalTime(date) {
    const d =
        date instanceof Date
            ? date
            : new Date(date);

    const hour =
        String(d.getHours())
            .padStart(2, '0');

    const minute =
        String(d.getMinutes())
            .padStart(2, '0');

    return `${hour}:${minute}`;
}

function escapeTeamHtml(value) {
    const element =
        document.createElement('div');

    element.textContent =
        value === null ||
        value === undefined
            ? ''
            : String(value);

    return element.innerHTML;
}

function openTeamScheduleEditModal() {
    const calendarEvent = selectedTeamScheduleEvent;

    const layer = document.getElementById('mLayer');
    const modalTitle = document.getElementById('mTitle2');
    const body = document.getElementById('mBody');

    if (!calendarEvent || !layer || !modalTitle || !body) {
        alert('수정할 일정 정보를 확인할 수 없습니다.');
        return;
    }

    const props = calendarEvent.extendedProps || {};
    const allDay = calendarEvent.allDay;

    const startDate =
        calendarEvent.startStr
            ? calendarEvent.startStr.slice(0, 10)
            : formatTeamLocalDate(calendarEvent.start);

    let endDate = startDate;
    let startTime = '09:00';
    let endTime = '10:00';

    if (allDay) {
        if (calendarEvent.end) {
            const displayEnd =
                new Date(calendarEvent.end);

            displayEnd.setDate(
                displayEnd.getDate() - 1
            );

            endDate =
                formatTeamLocalDate(displayEnd);
        }

    } else {
        if (calendarEvent.startStr?.length >= 16) {
            startTime =
                calendarEvent.startStr.slice(11, 16);
        }

        if (calendarEvent.endStr) {
            endDate =
                calendarEvent.endStr.slice(0, 10);

            if (calendarEvent.endStr.length >= 16) {
                endTime =
                    calendarEvent.endStr.slice(11, 16);
            }
        }
    }

    const rawColor =
        calendarEvent.backgroundColor ||
        props.color ||
        '#6C5CE7';

    const color =
        /^#[0-9a-fA-F]{6}$/.test(rawColor)
            ? rawColor
            : '#6C5CE7';

    const currentCategory =
        props.category || '일반 일정';

    const categories = [
        currentCategory,
        '일반 일정',
        '회의',
        '개발',
        '마감'
    ].filter(function (item, index, array) {
        return array.indexOf(item) === index;
    });

    const categoryOptions =
        categories.map(function (item) {
            return `
                <option value="${escapeTeamAttribute(item)}"
                        ${
                            item === currentCategory
                                ? 'selected'
                                : ''
                        }>
                    ${escapeTeamHtml(item)}
                </option>
            `;
        }).join('');

    modalTitle.textContent = '일정 수정';

    body.innerHTML = `
        <div class="fG">

            <div class="f full">
                <label for="teamEditTitle">
                    제목 *
                </label>

                <input type="text"
                       id="teamEditTitle"
                       maxlength="200"
                       value="${
                           escapeTeamAttribute(
                               calendarEvent.title || ''
                           )
                       }">
            </div>

            <div class="f">
                <label for="teamEditCategory">
                    카테고리
                </label>

                <select id="teamEditCategory">
                    ${categoryOptions}
                </select>
            </div>

            <div class="f">
                <label for="teamEditColor">
                    색상
                </label>

                <input type="color"
                       id="teamEditColor"
                       value="${
                           escapeTeamAttribute(color)
                       }">
            </div>

            <div class="f">
                <label for="teamEditStartDate">
                    시작일 *
                </label>

                <input type="date"
                       id="teamEditStartDate"
                       value="${startDate}">
            </div>

            <div class="f">
                <label for="teamEditEndDate">
                    종료일 *
                </label>

                <input type="date"
                       id="teamEditEndDate"
                       value="${endDate}">
            </div>

            <div class="f full schedule-all-day-row">
                <label class="schedule-switch-label"
                       for="teamEditAllDay">

                    <span>종일 일정</span>

                    <span class="schedule-switch">
                        <input type="checkbox"
                               id="teamEditAllDay"
                               ${allDay ? 'checked' : ''}
                               onchange="toggleTeamEditTimeFields()">

                        <span class="schedule-switch-slider">
                        </span>
                    </span>
                </label>
            </div>

            <div class="f team-edit-time-field"
                 ${allDay ? 'hidden' : ''}>

                <label for="teamEditStartTime">
                    시작 시간 *
                </label>

                <input type="time"
                       id="teamEditStartTime"
                       value="${startTime}"
                       ${allDay ? 'disabled' : ''}>
            </div>

            <div class="f team-edit-time-field"
                 ${allDay ? 'hidden' : ''}>

                <label for="teamEditEndTime">
                    종료 시간 *
                </label>

                <input type="time"
                       id="teamEditEndTime"
                       value="${endTime}"
                       ${allDay ? 'disabled' : ''}>
            </div>

            <div class="f full">
                <label for="teamEditContent">
                    내용
                </label>

                <textarea id="teamEditContent"
                          maxlength="2000">${
                              escapeTeamHtml(
                                  props.content || ''
                              )
                          }</textarea>
            </div>

        </div>

        <div class="acts">
            <button type="button"
                    class="btn"
                    onclick="reopenTeamScheduleDetail()">
                취소
            </button>

            <button type="button"
                    class="btn primary"
                    id="btnUpdateTeamSchedule"
                    onclick="updateTeamSchedule()">
                수정 완료
            </button>
        </div>
    `;

    toggleTeamEditTimeFields();
    layer.classList.add('open');
}

function toggleTeamEditTimeFields() {
    const allDay =
        document.getElementById('teamEditAllDay');

    const startTime =
        document.getElementById('teamEditStartTime');

    const endTime =
        document.getElementById('teamEditEndTime');

    const timeFields =
        document.querySelectorAll(
            '.team-edit-time-field'
        );

    if (!allDay || !startTime || !endTime) {
        return;
    }

    const isAllDay = allDay.checked;

    timeFields.forEach(function (field) {
        field.hidden = isAllDay;
    });

    startTime.disabled = isAllDay;
    endTime.disabled = isAllDay;
}

async function updateTeamSchedule() {
    const scheduleNum =
        getTeamScheduleNum(
            selectedTeamScheduleEvent
        );

    const title =
        document.getElementById('teamEditTitle')
            ?.value.trim();

    const category =
        document.getElementById('teamEditCategory')
            ?.value;

    const color =
        document.getElementById('teamEditColor')
            ?.value || '#6C5CE7';

    const startDate =
        document.getElementById('teamEditStartDate')
            ?.value;

    const endDate =
        document.getElementById('teamEditEndDate')
            ?.value;

    const startTime =
        document.getElementById('teamEditStartTime')
            ?.value;

    const endTime =
        document.getElementById('teamEditEndTime')
            ?.value;

    const content =
        document.getElementById('teamEditContent')
            ?.value.trim() || '';

    const isAllDay =
        document.getElementById('teamEditAllDay')
            ?.checked;

    const updateButton =
        document.getElementById(
            'btnUpdateTeamSchedule'
        );

    if (!scheduleNum) {
        alert('일정 번호를 확인할 수 없습니다.');
        return;
    }

    if (!title) {
        alert('제목을 입력하세요.');
        return;
    }

    if (!startDate || !endDate) {
        alert('시작일과 종료일을 입력하세요.');
        return;
    }

    const allDay = isAllDay ? 1 : 2;

    let startValue = startDate;
    let endValue = endDate;

    if (allDay === 2) {
        if (!startTime || !endTime) {
            alert(
                '시작 시간과 종료 시간을 입력하세요.'
            );
            return;
        }

        startValue =
            `${startDate}T${startTime}`;

        endValue =
            `${endDate}T${endTime}`;

        if (endValue <= startValue) {
            alert(
                '종료 일시는 시작 일시보다 늦어야 합니다.'
            );
            return;
        }

    } else if (endDate < startDate) {
        alert(
            '종료일은 시작일보다 빠를 수 없습니다.'
        );
        return;
    }

    const scheduleData = {
        schedule_num: scheduleNum,
        title: title,
        category: category,
        color: color,
        content: content,
        start_date: startValue,
        end_date: endValue,
        all_day: allDay
    };

    if (updateButton) {
        updateButton.disabled = true;
        updateButton.textContent = '수정 중...';
    }

    try {
        const response =
            await fetch('/calendar/update', {
                method: 'POST',
                headers: getTeamJsonHeaders(),
                credentials: 'same-origin',
                body: JSON.stringify(scheduleData)
            });

        if (!response.ok) {
            throw new Error(
                `HTTP 오류: ${response.status}`
            );
        }

        const result =
            await response.json();

        if (result.result === 'success') {
            closeM();

            selectedTeamScheduleEvent = null;

            showToast('일정이 수정되었습니다.');

            if (teamCalendar) {
                teamCalendar.refetchEvents();
            }

            return;
        }

        alert(
            result.message ||
            '일정 수정에 실패했습니다.'
        );

    } catch (error) {
        console.error('일정 수정 오류:', error);

        alert(
            '일정 수정 중 오류가 발생했습니다.'
        );

    } finally {
        if (updateButton) {
            updateButton.disabled = false;
            updateButton.textContent = '수정 완료';
        }
    }
}

async function deleteTeamSchedule() {
    const scheduleNum =
        getTeamScheduleNum(
            selectedTeamScheduleEvent
        );

    if (!scheduleNum) {
        alert('일정 번호를 확인할 수 없습니다.');
        return;
    }

    if (!confirm('이 일정을 삭제하시겠습니까?')) {
        return;
    }

    try {
        const response =
            await fetch('/calendar/delete', {
                method: 'POST',
                headers: getTeamJsonHeaders(),
                credentials: 'same-origin',
                body: JSON.stringify({
                    schedule_num: scheduleNum
                })
            });

        if (!response.ok) {
            throw new Error(
                `HTTP 오류: ${response.status}`
            );
        }

        const result =
            await response.json();

        if (result.result === 'success') {
            closeM();

            selectedTeamScheduleEvent = null;

            showToast('일정이 삭제되었습니다.');

            if (teamCalendar) {
                teamCalendar.refetchEvents();
            }

            return;
        }

        alert(
            result.message ||
            '일정 삭제에 실패했습니다.'
        );

    } catch (error) {
        console.error('일정 삭제 오류:', error);

        alert(
            '일정 삭제 중 오류가 발생했습니다.'
        );
    }
}

function getTeamScheduleNum(calendarEvent) {
    if (!calendarEvent) {
        return null;
    }

    const props =
        calendarEvent.extendedProps || {};

    const rawScheduleNum =
        props.schedule_num ??
        calendarEvent.id;

    if (rawScheduleNum === null ||
        rawScheduleNum === undefined) {
        return null;
    }

    const matched =
        String(rawScheduleNum).match(/(\d+)$/);

    return matched
        ? Number(matched[1])
        : null;
}


function getTeamJsonHeaders() {
    const headers = {
        'Content-Type':
            'application/json; charset=UTF-8'
    };

    const csrfToken =
        document.querySelector(
            'meta[name="csrf-token"]'
        )?.content;

    const csrfHeader =
        document.querySelector(
            'meta[name="csrf-header"]'
        )?.content;

    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    return headers;
}


function reopenTeamScheduleDetail() {
    if (selectedTeamScheduleEvent) {
        openTeamScheduleDetailModal(
            selectedTeamScheduleEvent
        );
    }
}



function escapeTeamAttribute(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
}

/* HTML onclick 대응 */
window.openM = openM;
window.closeM = closeM;
window.saveSch = saveSch;

window.toggleScheduleTimeFields =
    toggleScheduleTimeFields;

window.openTeamScheduleDetailModal =
    openTeamScheduleDetailModal;

window.openTeamScheduleEditModal =
    openTeamScheduleEditModal;

window.reopenTeamScheduleDetail =
    reopenTeamScheduleDetail;

window.toggleTeamEditTimeFields =
    toggleTeamEditTimeFields;

window.updateTeamSchedule =
    updateTeamSchedule;

window.deleteTeamSchedule =
    deleteTeamSchedule;
