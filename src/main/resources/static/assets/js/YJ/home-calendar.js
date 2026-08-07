/* assets/js/YJ/home-calendar.js */

let homeCalendar;
let selectedHomeTeamNum = 'ALL';
let selectedHomeScheduleEvent = null;

let todos = [
    {id: 1, txt: '(예시) 칸반/캘린더 모듈 연동 대기', done: false, pri: 2},
];

const HOME_SWATCHES = ['#f1edff', '#eef9f4', '#fff5ea', '#f5f0ff', '#fff1f7', '#e8f4ff'];
let selColor = HOME_SWATCHES[0];

document.addEventListener('DOMContentLoaded', function () {
    initCsrf();
    initLogout();
    initTodayLabel();
    initHomeCalendar();
    initHomeTeamFilter();
	initHomeScheduleModal();

    renderTodos();
    renderHomeSidePlaceholders();
    showInviteResultToast();
});

/* =========================
   CSRF / 로그아웃
========================= */
function initCsrf() {
    if (typeof $ === 'undefined') return;

    const csrfToken = $('meta[name="csrf-token"]').attr('content');
    const csrfHeader = $('meta[name="csrf-header"]').attr('content');

    $(document).ajaxSend(function (e, xhr) {
        if (csrfHeader) {
            xhr.setRequestHeader(csrfHeader, csrfToken);
        }
    });
}

function initLogout() {
    const logoutBtn = document.getElementById('btn-logout');
    const logoutForm = document.getElementById('frm_logout');

    if (logoutBtn && logoutForm) {
        logoutBtn.addEventListener('click', function () {
            logoutForm.submit();
        });
    }
}

/* =========================
   오늘 날짜
========================= */
function initTodayLabel() {
    const label = document.getElementById('todayLabel');
    if (!label) return;

    const d = new Date();
    const days = ['일', '월', '화', '수', '목', '금', '토'];

    label.textContent =
        d.getFullYear() + '년 ' +
        (d.getMonth() + 1) + '월 ' +
        d.getDate() + '일 ' +
        days[d.getDay()] + '요일';
}

/* =========================
   홈 FullCalendar
========================= */
function initHomeCalendar() {
    const calendarEl = document.getElementById('homeCalendar');

    if (!calendarEl || typeof FullCalendar === 'undefined') {
        console.log('homeCalendar 또는 FullCalendar를 찾을 수 없습니다.');
        return;
    }

    homeCalendar = new FullCalendar.Calendar(calendarEl, {
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
		
		// 날짜 칸 클릭
		dateClick: function (info) {
		    openHomeScheduleModal(info.dateStr);
		},

        events: function (fetchInfo, successCallback, failureCallback) {
            // 나중에 /calendar/my-events API 만들면 이 주소 사용
            // 지금 API 없으면 임시로 /calendar/events?team_num=2 써도 됨
            const url = calendarEl.dataset.eventsUrl || '/calendar/my-events';

            fetch(url)
                .then(response => response.json())
                .then(data => {
                    const filtered = data.filter(event => {
                        if (selectedHomeTeamNum === 'ALL') return true;
                        return String(event.team_num) === String(selectedHomeTeamNum);
                    });

                    successCallback(filtered);
                    renderTodayEvents(filtered);
                    renderHomeStats(filtered);
                })
                .catch(error => {
                    console.error(error);
                    failureCallback(error);
                });
        },

		eventClick: function (info) {
		    openHomeScheduleDetailModal(info.event);
		}
    });

    homeCalendar.render();
}

function initHomeTeamFilter() {
    document.querySelectorAll('.home-team-filter .chip').forEach(function (btn) {
        btn.addEventListener('click', function () {
            document.querySelectorAll('.home-team-filter .chip')
                .forEach(c => c.classList.remove('active'));

            this.classList.add('active');
            selectedHomeTeamNum = this.dataset.teamnum;

            if (homeCalendar) {
                homeCalendar.refetchEvents();
            }
        });
    });
}

/* =========================
   홈 일정 등록 모달
========================= */
function initHomeScheduleModal() {
    const closeBtn =
        document.getElementById('btnCloseHomeScheduleModal');

    const cancelBtn =
        document.getElementById('btnCancelHomeSchedule');

    const form =
        document.getElementById('homeScheduleForm');

    const allDayCheck =
        document.getElementById('homeScheduleAllDayCheck');

    if (closeBtn) {
        closeBtn.addEventListener(
            'click',
            closeHomeScheduleModal
        );
    }

    if (cancelBtn) {
        cancelBtn.addEventListener(
            'click',
            closeHomeScheduleModal
        );
    }

    if (form) {
        form.addEventListener(
            'submit',
            submitHomeSchedule
        );
    }

    if (allDayCheck) {
        allDayCheck.addEventListener(
            'change',
            toggleHomeScheduleTimeFields
        );
    }

    toggleHomeScheduleTimeFields();
}


function openHomeScheduleModal(selectedDate) {
    const layer =
        document.getElementById('homeScheduleModalLayer');

    const form =
        document.getElementById('homeScheduleForm');

    const teamSelect =
        document.getElementById('homeScheduleTeam');

    const startDate =
        document.getElementById('homeScheduleStartDate');

    const endDate =
        document.getElementById('homeScheduleEndDate');

    // 서버에 전달할 값
    // 1 = 종일, 2 = 시간 지정
    const allDay =
        document.getElementById('homeScheduleAllDay');

    // 사용자가 조작하는 체크박스
    const allDayCheck =
        document.getElementById('homeScheduleAllDayCheck');

    const startTime =
        document.getElementById('homeScheduleStartTime');

    const endTime =
        document.getElementById('homeScheduleEndTime');

    const title =
        document.getElementById('homeScheduleTitle');

    const color =
        document.getElementById('homeScheduleColor');

    if (!layer ||
        !form ||
        !teamSelect ||
        !startDate ||
        !endDate ||
        !allDay ||
        !allDayCheck) {

        console.error(
            '홈 일정 등록 모달 요소를 찾을 수 없습니다.'
        );
        return;
    }

    /*
     * 첫 번째 옵션은 "팀을 선택하세요"이므로
     * 옵션이 하나뿐이면 등록 권한이 있는 팀이 없음
     */
    if (teamSelect.options.length <= 1) {
        alert(
            '일정을 등록할 수 있는 팀이 없습니다.\n' +
            '팀장 또는 매니저만 일정을 등록할 수 있습니다.'
        );
        return;
    }

    form.reset();

    /*
     * 날짜 칸이 아닌 별도 버튼으로 열었을 경우
     * 오늘 날짜를 기본값으로 사용
     */
    const targetDate =
        selectedDate || formatLocalDate(new Date());

    startDate.value = targetDate;
    endDate.value = targetDate;

    /*
     * 등록 모달 기본 상태는 종일 일정
     */
    allDayCheck.checked = true;
    allDay.value = '1';

    if (startTime) {
        startTime.value = '09:00';
    }

    if (endTime) {
        endTime.value = '10:00';
    }

    if (color) {
        color.value = '#6C5CE7';
    }

    /*
     * 체크박스 상태에 맞춰
     * 시간 입력칸 표시 및 비활성화 처리
     */
    toggleHomeScheduleTimeFields();

    /*
     * 특정 팀 필터가 선택되어 있고
     * 해당 팀에 등록 권한이 있으면 자동 선택
     */
    if (selectedHomeTeamNum !== 'ALL') {
        const option =
            Array.from(teamSelect.options).find(
                function (item) {
                    return String(item.value) ===
                           String(selectedHomeTeamNum);
                }
            );

        if (option) {
            teamSelect.value = selectedHomeTeamNum;
        }
    }

    layer.classList.add('open');

    if (title) {
        title.focus();
    }
}


function closeHomeScheduleModal() {
    const layer =
        document.getElementById('homeScheduleModalLayer');

    if (layer) {
        layer.classList.remove('open');
    }
}

function submitHomeSchedule(event) {
    event.preventDefault();

    const form = event.currentTarget;
    const submitBtn = form.querySelector('button[type="submit"]');

    const teamNum = document.getElementById('homeScheduleTeam').value;
    const title = document.getElementById('homeScheduleTitle').value.trim();
    const category = document.getElementById('homeScheduleCategory').value;
	const color = document.getElementById('homeScheduleColor')?.value || '#6C5CE7';
    const startDate = document.getElementById('homeScheduleStartDate').value;
    const endDate = document.getElementById('homeScheduleEndDate').value;
    const content = document.getElementById('homeScheduleContent').value.trim();
	const startTime =
	    document.getElementById('homeScheduleStartTime')?.value;

	const endTime =
	    document.getElementById('homeScheduleEndTime')?.value;
    const allDay = Number(
        document.getElementById('homeScheduleAllDay').value
		
		
    );

    if (!teamNum) {
        alert('일정을 등록할 팀을 선택하세요.');
        return;
    }

    if (!title) {
        alert('일정 제목을 입력하세요.');
        return;
    }

    if (!startDate || !endDate) {
        alert('시작일과 종료일을 입력하세요.');
        return;
    }

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
	    team_num: Number(teamNum),
	    title: title,
	    category: category,
	    content: content,
	    color: color,
	    start_date: startValue,
	    end_date: endValue,
	    all_day: allDay
	};

    if (typeof $ === 'undefined') {
        alert('jQuery를 찾을 수 없습니다.');
        return;
    }

    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = '저장 중...';
    }

    $.ajax({
        url: '/calendar/write',
        type: 'POST',
        contentType: 'application/json; charset=UTF-8',
        dataType: 'json',
        data: JSON.stringify(scheduleData)
    })
    .done(function (response) {
        if (response.result === 'success') {
            closeHomeScheduleModal();
            showToast('일정이 등록되었습니다.');

            if (homeCalendar) {
                homeCalendar.refetchEvents();
            }

            return;
        }

        if (response.result === 'forbidden') {
            alert('팀장 또는 매니저만 일정을 등록할 수 있습니다.');
            return;
        }

        if (response.result === 'noTeam') {
            alert('등록할 팀을 확인할 수 없습니다.');
            return;
        }

        alert(response.message || '일정 등록에 실패했습니다.');
    })
    .fail(function (xhr) {
        console.error('일정 등록 실패', xhr);

        if (xhr.status === 403) {
            alert('일정 등록 권한이 없습니다.');
        } else if (xhr.status === 404) {
            alert('일정 등록 API를 찾을 수 없습니다.');
        } else {
            alert('일정 등록 중 오류가 발생했습니다.');
        }
    })
    .always(function () {
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = '저장';
        }
    });
}

/* =========================
   홈 일정 상세 모달
========================= */
function openHomeScheduleDetailModal(calendarEvent) {
	selectedHomeScheduleEvent = calendarEvent;
    const layer = document.getElementById('mLayer');
    const modalTitle = document.getElementById('mTitle2');
    const body = document.getElementById('mBody');

    if (!layer || !modalTitle || !body || !calendarEvent) {
        console.error('일정 상세 모달 요소를 찾을 수 없습니다.');
        return;
    }

    const props = calendarEvent.extendedProps || {};

    const title = calendarEvent.title || '제목 없음';
    const category = props.category || '일반 일정';
    const content = props.content || '등록된 내용이 없습니다.';
    const teamText = props.team_name
        ? props.team_name
        : props.team_num
            ? '팀 번호 ' + props.team_num
            : '-';

    const startText = formatCalendarDate(
        calendarEvent.start,
        calendarEvent.allDay
    );

    const endText = formatCalendarEndDate(
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

    modalTitle.textContent = '일정 상세';

    body.innerHTML = `
        <div class="schedule-detail">

            <div class="schedule-detail-title">
                <span class="schedule-detail-color"
                      style="background:${color}"></span>

                <div>
                    <h3>${escapeHtml(title)}</h3>
                    <span class="schedule-detail-category">
                        ${escapeHtml(category)}
                    </span>
                </div>
            </div>

            <div class="schedule-detail-grid">
                <div class="schedule-detail-item">
                    <span class="schedule-detail-label">팀</span>
                    <strong>${escapeHtml(teamText)}</strong>
                </div>

                <div class="schedule-detail-item">
                    <span class="schedule-detail-label">일정 유형</span>
                    <strong>
                        ${calendarEvent.allDay ? '종일 일정' : '시간 지정 일정'}
                    </strong>
                </div>

                <div class="schedule-detail-item">
                    <span class="schedule-detail-label">시작</span>
                    <strong>${escapeHtml(startText)}</strong>
                </div>

                <div class="schedule-detail-item">
                    <span class="schedule-detail-label">종료</span>
                    <strong>${escapeHtml(endText)}</strong>
                </div>

                <div class="schedule-detail-item full">
                    <span class="schedule-detail-label">내용</span>
                    <div class="schedule-detail-content">
                        ${escapeHtml(content).replace(/\n/g, '<br>')}
                    </div>
                </div>
            </div>

			<div class="acts">
			    <button type="button"
			            class="btn danger"
			            onclick="deleteHomeSchedule()">
			        삭제
			    </button>

			    <button type="button"
			            class="btn"
			            onclick="openHomeScheduleEditModal()">
			        수정
			    </button>

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

function openHomeScheduleEditModal() {
    const calendarEvent = selectedHomeScheduleEvent;

    const layer = document.getElementById('mLayer');
    const modalTitle = document.getElementById('mTitle2');
    const body = document.getElementById('mBody');

    if (!calendarEvent || !layer || !modalTitle || !body) {
        alert('수정할 일정 정보를 확인할 수 없습니다.');
        return;
    }

    const props = calendarEvent.extendedProps || {};

    const allDay = calendarEvent.allDay;
    const startDate = calendarEvent.startStr.slice(0, 10);

    let endDate = startDate;
    let startTime = '09:00';
    let endTime = '10:00';

    if (allDay) {
        if (calendarEvent.end) {
            const displayEnd = new Date(calendarEvent.end);
            displayEnd.setDate(displayEnd.getDate() - 1);
            endDate = formatLocalDate(displayEnd);
        }
    } else {
        startTime =
            calendarEvent.startStr.length >= 16
                ? calendarEvent.startStr.slice(11, 16)
                : '09:00';

        if (calendarEvent.endStr) {
            endDate = calendarEvent.endStr.slice(0, 10);
            endTime =
                calendarEvent.endStr.length >= 16
                    ? calendarEvent.endStr.slice(11, 16)
                    : '10:00';
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

    modalTitle.textContent = '일정 수정';

    body.innerHTML = `
        <div class="fG">

            <div class="f full">
                <label for="editScheduleTitle">제목 *</label>
                <input type="text"
                       id="editScheduleTitle"
                       maxlength="200"
                       value="${escapeAttribute(calendarEvent.title || '')}">
            </div>

            <div class="f">
                <label for="editScheduleCategory">카테고리</label>

                <select id="editScheduleCategory">
                    <option value="일반 일정"
                        ${props.category === '일반 일정' ? 'selected' : ''}>
                        일반 일정
                    </option>

                    <option value="회의"
                        ${props.category === '회의' ? 'selected' : ''}>
                        회의
                    </option>

                    <option value="개발"
                        ${props.category === '개발' ? 'selected' : ''}>
                        개발
                    </option>

                    <option value="마감"
                        ${props.category === '마감' ? 'selected' : ''}>
                        마감
                    </option>
                </select>
            </div>

            <div class="f">
                <label for="editScheduleColor">색상</label>
                <input type="color"
                       id="editScheduleColor"
                       value="${escapeAttribute(color)}">
            </div>

            <div class="f">
                <label for="editScheduleStartDate">시작일 *</label>
                <input type="date"
                       id="editScheduleStartDate"
                       value="${startDate}">
            </div>

            <div class="f">
                <label for="editScheduleEndDate">종료일 *</label>
                <input type="date"
                       id="editScheduleEndDate"
                       value="${endDate}">
            </div>

			<!-- 종일 일정 스위치 -->
			<div class="f full home-all-day-wrap">
			    <label class="home-switch-label"
			           for="editScheduleAllDay">

			        <span>종일 일정</span>

			        <span class="home-switch">
			            <input type="checkbox"
			                   id="editScheduleAllDay"
			                   ${allDay ? 'checked' : ''}
			                   onchange="toggleHomeEditTimeFields()">

			            <span class="home-switch-slider"></span>
			        </span>
			    </label>
			</div>

			<!-- 종일 일정 해제 시에만 표시 -->
			<div class="f home-edit-time-field"
			     ${allDay ? 'hidden' : ''}>

			    <label for="editScheduleStartTime">
			        시작 시간 *
			    </label>

			    <input type="time"
			           id="editScheduleStartTime"
			           value="${startTime}"
			           ${allDay ? 'disabled' : ''}>
			</div>

			<div class="f home-edit-time-field"
			     ${allDay ? 'hidden' : ''}>

			    <label for="editScheduleEndTime">
			        종료 시간 *
			    </label>

			    <input type="time"
			           id="editScheduleEndTime"
			           value="${endTime}"
			           ${allDay ? 'disabled' : ''}>
			</div>

			<div class="f full">
			    <label for="editScheduleContent">내용</label>

			    <textarea id="editScheduleContent"
			              maxlength="2000">${escapeHtml(props.content || '')}</textarea>
			</div>

        </div>

		        <div class="acts">
		            <button type="button"
		                    class="btn"
		                    onclick="openHomeScheduleDetailModal(selectedHomeScheduleEvent)">
		                취소
		            </button>

		            <button type="button"
		                    class="btn primary"
		                    id="btnUpdateHomeSchedule"
		                    onclick="submitHomeScheduleUpdate()">
		                수정 완료
		            </button>
		        </div>
		    `;

		    toggleHomeEditTimeFields();
		    layer.classList.add('open');
		}


function formatCalendarDate(date, allDay) {
    if (!date) return '-';

    const dateText = formatLocalDate(date);

    if (allDay) {
        return dateText;
    }

    return dateText + ' ' + formatLocalTime(date);
}


function formatCalendarEndDate(endDate, startDate, allDay) {
    if (!endDate) {
        return formatCalendarDate(startDate, allDay);
    }

    const displayEnd = new Date(endDate);

    /*
     * FullCalendar 종일 일정의 종료일은 exclusive end라서
     * 화면 표시 시 하루를 빼야 실제 마지막 날짜가 됨
     */
    if (allDay) {
        displayEnd.setDate(displayEnd.getDate() - 1);
        return formatLocalDate(displayEnd);
    }

    return formatLocalDate(displayEnd)
        + ' '
        + formatLocalTime(displayEnd);
}


function formatLocalDate(date) {
    const d = new Date(date);

    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
}


function formatLocalTime(date) {
    const d = new Date(date);

    const hour = String(d.getHours()).padStart(2, '0');
    const minute = String(d.getMinutes()).padStart(2, '0');

    return `${hour}:${minute}`;
}

function renderTodayEvents(events) {
    const todayList = document.getElementById('todayList');
    if (!todayList) return;

    const today = new Date().toISOString().slice(0, 10);

    const todayEvents = events
        .filter(e => getEventDate(e) === today)
        .slice(0, 4);

    if (todayEvents.length === 0) {
        todayList.innerHTML =
            '<div style="padding:12px;text-align:center;color:var(--muted);font-size:13px">오늘 일정이 없습니다.</div>';
        return;
    }

    todayList.innerHTML = todayEvents.map(e => `
        <div class="agItem">
            <div class="agTime">${getEventTime(e)}</div>
            <div>
                <div class="agTitle">${e.title}</div>
                <div class="agSub">${e.category || '일정'}</div>
            </div>
        </div>
    `).join('');
}

function renderHomeStats(events) {
    const stSch = document.getElementById('stSch');
    const stToday = document.getElementById('stToday');
    const stMin = document.getElementById('stMin');

    const today = new Date().toISOString().slice(0, 10);
    const todayCount = events.filter(e => getEventDate(e) === today).length;

    if (stSch) stSch.textContent = events.length;
    if (stToday) stToday.textContent = todayCount;
    if (stMin) stMin.textContent = '-';
}

function renderHomeSidePlaceholders() {
    const minutesList = document.getElementById('minutesList');

    if (minutesList) {
        minutesList.innerHTML =
            '<div style="padding:12px;text-align:center;color:var(--muted);font-size:13px">회의록 모듈 연동 대기 중</div>';
    }
}

/* =========================
   To-Do
========================= */
function renderTodos() {
    const todoList = document.getElementById('todoList');
    if (!todoList) return;

    const priMap = {3: '🔴', 2: '🟡', 1: '🟢'};

    todoList.innerHTML = todos.map(t =>
        '<div class="todoItem ' + (t.done ? 'done' : '') + '" onclick="togTodo(' + t.id + ')">' +
        '<span style="font-size:18px">' + (t.done ? '✅' : '⬜') + '</span>' +
        '<span style="flex:1;font-size:13px;font-weight:900">' + t.txt + '</span>' +
        '<span>' + (priMap[t.pri] || '') + '</span></div>'
    ).join('');
}

function togTodo(id) {
    const t = todos.find(x => x.id === id);
    if (t) t.done = !t.done;
    renderTodos();
}

/* =========================
   모달
========================= */
function openM(type) {
    const layer = document.getElementById('mLayer');
    const body = document.getElementById('mBody');
    const title = document.getElementById('mTitle2');

    if (!layer || !body || !title) return;

    layer.classList.add('open');

    if (type === 'createTeam') {
        title.textContent = '팀 만들기';
        body.innerHTML =
            '<div class="fG">' +
            '<div class="f full"><label>팀 이름 *</label><input id="tN" placeholder="예: FE 개발팀"></div>' +
            '<div class="f full"><label>팀 설명</label><input id="tD" placeholder="팀에 대한 간단한 설명"></div>' +
            '<div class="f full"><label>팀 색상 *</label><div class="swatches">' +
            HOME_SWATCHES.map(c =>
                '<div class="sw' + (c === selColor ? ' sel' : '') + '" style="background:' + c + '" onclick="selSw(\'' + c + '\',this)"></div>'
            ).join('') +
            '</div></div></div>' +
            '<div class="acts"><button class="btn" onclick="closeM()">취소</button>' +
            '<button class="btn primary" onclick="createTeam()">팀 생성</button></div>';
    } else if (type === 'joinTeam') {
        title.textContent = '초대코드로 팀 참여';
        body.innerHTML =
            '<p style="font-size:14px;color:var(--muted);margin:0 0 14px">팀장에게 받은 초대코드를 입력하세요.</p>' +
            '<input class="bigInput" id="jCode" placeholder="예: f39084b6-0b65-4188-857e-54c16789937a" style="font-size:14px;letter-spacing:.02em">' +
            '<div class="acts"><button class="btn" onclick="closeM()">취소</button>' +
            '<button class="btn primary" onclick="joinTeam()">팀 참여</button></div>';
    } else if (type === 'addTodo') {
        title.textContent = '개인 To-Do 추가';
        body.innerHTML =
            '<div class="fG">' +
            '<div class="f full"><label>할 일 제목 *</label><input id="tdT" placeholder="할 일 입력"></div>' +
            '<div class="f"><label>우선순위</label><select id="tdP"><option value="1">🟢 낮음</option><option value="2" selected>🟡 보통</option><option value="3">🔴 높음</option></select></div>' +
            '</div>' +
            '<p style="font-size:12px;color:var(--muted)">※ To-Do 영구 저장은 마이페이지 모듈 완성 후 연동됩니다.</p>' +
            '<div class="acts"><button class="btn" onclick="closeM()">취소</button>' +
            '<button class="btn primary" onclick="saveTodo()">저장</button></div>';
    }
}

function closeM() {
    const layer = document.getElementById('mLayer');
    if (layer) layer.classList.remove('open');
}

function selSw(c, el) {
    selColor = c;
    document.querySelectorAll('.sw').forEach(s => s.classList.remove('sel'));
    el.classList.add('sel');
}

function createTeam() {
    const name = document.getElementById('tN')?.value.trim();
    const desc = document.getElementById('tD')?.value.trim() || '';

    if (!name) {
        alert('팀 이름을 입력하세요');
        return;
    }

    if (typeof $ === 'undefined') {
        alert('jQuery가 필요합니다.');
        return;
    }

    $.post('/team/create', {
        teamName: name,
        description: desc,
        color: selColor
    })
    .done(function (result) {
        if (result && result.indexOf('OK:') === 0) {
            const teamNum = result.split(':')[1];
            closeM();
            showToast('팀이 생성되었습니다! 🎉');
            location.href = '/team/enter/' + teamNum;
        } else {
            showToast('팀 생성에 실패했습니다. (' + result + ')');
        }
    })
    .fail(function () {
        showToast('요청 처리 중 오류가 발생했습니다.');
    });
}

function joinTeam() {
    const c = document.getElementById('jCode')?.value.trim();

    if (!c) {
        alert('초대코드를 입력하세요');
        return;
    }

    if (typeof $ === 'undefined') {
        alert('jQuery가 필요합니다.');
        return;
    }

    $.post('/team/join/code', { code: c })
        .done(function (result) {
            if (result && result.indexOf('OK:') === 0) {
                const teamNum = result.split(':')[1];
                closeM();
                showToast('팀에 참여했습니다! 🎉');
                location.href = '/team/enter/' + teamNum;
            } else {
                const msgMap = {
                    'INVALID_CODE': '유효하지 않은 초대코드입니다.',
                    'EXPIRED_CODE': '만료된 초대코드입니다.',
                    'DISABLED_CODE': '비활성화된 초대코드입니다.',
                    'TEAM_NOT_FOUND': '팀을 찾을 수 없습니다.',
                    'ALREADY_JOINED': '이미 소속된 팀입니다.'
                };
                showToast(msgMap[result] || ('참여 실패 (' + result + ')'));
            }
        })
        .fail(function () {
            showToast('요청 처리 중 오류가 발생했습니다.');
        });
}

function saveTodo() {
    const t = document.getElementById('tdT')?.value.trim();

    if (!t) {
        alert('제목을 입력하세요');
        return;
    }

    todos.unshift({
        id: Date.now(),
        txt: t,
        done: false,
        pri: Number(document.getElementById('tdP')?.value) || 2
    });

    closeM();
    renderTodos();
    showToast('To-Do 추가 완료. (임시 저장, 새로고침 시 사라짐)');
}

function showToast(msg) {
    const t = document.getElementById('toast');
    if (!t) return;

    t.textContent = msg;
    t.classList.add('show');

    setTimeout(() => t.classList.remove('show'), 2800);
}

/* =========================
   이메일 초대 수락/거절 링크 복귀 토스트
========================= */
function showInviteResultToast() {
    const params = new URLSearchParams(location.search);
    const invite = params.get('invite');
    if (!invite) return;

    const msgMap = {
        'accepted': '팀 초대를 수락했습니다! 🎉',
        'rejected': '팀 초대를 거절했습니다.',
        'expired': '만료된 초대입니다.',
        'notPending': '이미 처리된 초대입니다.',
        'wrongUser': '본인 이메일로 받은 초대만 처리할 수 있습니다.',
        'alreadyJoined': '이미 소속된 팀입니다.',
        'notfound': '초대 정보를 찾을 수 없습니다.'
    };
    showToast(msgMap[invite] || '초대 처리 결과를 확인해주세요.');
    history.replaceState(null, '', location.pathname);
}

/* =========================
   공통 유틸
========================= */
function getEventDate(e) {
    return e.start ? e.start.slice(0, 10) : '';
}

function getEventTime(e) {
    if (!e.start || e.start.length < 16) return '종일';
    return e.start.slice(11, 16);
}

function escapeHtml(value) {
    const element = document.createElement('div');

    element.textContent =
        value === null || value === undefined
            ? ''
            : String(value);

    return element.innerHTML;
}

function getScheduleNum(calendarEvent) {
    if (!calendarEvent) {
        return null;
    }

    const props = calendarEvent.extendedProps || {};

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

function toggleHomeEditTimeFields() {
    const allDay =
        document.getElementById('editScheduleAllDay');

    const startTime =
        document.getElementById('editScheduleStartTime');

    const endTime =
        document.getElementById('editScheduleEndTime');

    const timeFields =
        document.querySelectorAll(
            '.home-edit-time-field'
        );

    if (!allDay || !startTime || !endTime) {
        return;
    }

    const isAllDay = allDay.checked;

    /*
     * 종일 일정 ON
     * → 시간 입력칸 숨김
     *
     * 종일 일정 OFF
     * → 시간 입력칸 표시
     */
    timeFields.forEach(function (field) {
        field.hidden = isAllDay;
    });

    startTime.disabled = isAllDay;
    endTime.disabled = isAllDay;
}

function submitHomeScheduleUpdate() {
    const scheduleNum =
        getScheduleNum(selectedHomeScheduleEvent);

    const title =
        document.getElementById('editScheduleTitle')
            ?.value.trim();

    const category =
        document.getElementById('editScheduleCategory')
            ?.value;

    const color =
        document.getElementById('editScheduleColor')
            ?.value || '#6C5CE7';

    const startDate =
        document.getElementById('editScheduleStartDate')
            ?.value;

    const endDate =
        document.getElementById('editScheduleEndDate')
            ?.value;

    const startTime =
        document.getElementById('editScheduleStartTime')
            ?.value;

    const endTime =
        document.getElementById('editScheduleEndTime')
            ?.value;

    const content =
        document.getElementById('editScheduleContent')
            ?.value.trim() || '';

    const isAllDay =
        document.getElementById('editScheduleAllDay')
            ?.checked;

    const updateButton =
        document.getElementById('btnUpdateHomeSchedule');

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
            alert('시작 시간과 종료 시간을 입력하세요.');
            return;
        }

        startValue = `${startDate}T${startTime}`;
        endValue = `${endDate}T${endTime}`;

        if (endValue <= startValue) {
            alert('종료 일시는 시작 일시보다 늦어야 합니다.');
            return;
        }

    } else if (endDate < startDate) {
        alert('종료일은 시작일보다 빠를 수 없습니다.');
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

    $.ajax({
        url: '/calendar/update',
        type: 'POST',
        contentType: 'application/json; charset=UTF-8',
        dataType: 'json',
        data: JSON.stringify(scheduleData)
    })
    .done(function (response) {
        if (response.result === 'success') {
            closeM();
            selectedHomeScheduleEvent = null;
            showToast('일정이 수정되었습니다.');

            if (homeCalendar) {
                homeCalendar.refetchEvents();
            }

            return;
        }

        alert(
            response.message ||
            '일정 수정에 실패했습니다.'
        );
    })
    .fail(function (xhr) {
        console.error('일정 수정 실패', xhr);
        alert('일정 수정 중 오류가 발생했습니다.');
    })
    .always(function () {
        if (updateButton) {
            updateButton.disabled = false;
            updateButton.textContent = '수정 완료';
        }
    });
}

function deleteHomeSchedule() {
    const scheduleNum =
        getScheduleNum(selectedHomeScheduleEvent);

    if (!scheduleNum) {
        alert('일정 번호를 확인할 수 없습니다.');
        return;
    }

    if (!confirm('이 일정을 삭제하시겠습니까?')) {
        return;
    }

    $.ajax({
        url: '/calendar/delete',
        type: 'POST',
        contentType: 'application/json; charset=UTF-8',
        dataType: 'json',
        data: JSON.stringify({
            schedule_num: scheduleNum
        })
    })
    .done(function (response) {
        if (response.result === 'success') {
            closeM();
            selectedHomeScheduleEvent = null;
            showToast('일정이 삭제되었습니다.');

            if (homeCalendar) {
                homeCalendar.refetchEvents();
            }

            return;
        }

        alert(
            response.message ||
            '일정 삭제에 실패했습니다.'
        );
    })
    .fail(function (xhr) {
        console.error('일정 삭제 실패', xhr);
        alert('일정 삭제 중 오류가 발생했습니다.');
    });
}

function escapeAttribute(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
}
function toggleHomeScheduleTimeFields() {
    const allDayCheck =
        document.getElementById('homeScheduleAllDayCheck');

    const allDayInput =
        document.getElementById('homeScheduleAllDay');

    const startTime =
        document.getElementById('homeScheduleStartTime');

    const endTime =
        document.getElementById('homeScheduleEndTime');

    const timeFields =
        document.querySelectorAll('.home-schedule-time-field');

    if (!allDayCheck || !allDayInput) {
        return;
    }

    const isAllDay = allDayCheck.checked;

    allDayInput.value = isAllDay ? '1' : '2';

    timeFields.forEach(function (field) {
        field.classList.toggle('show', !isAllDay);
    });

    if (startTime) {
        startTime.disabled = isAllDay;
    }

    if (endTime) {
        endTime.disabled = isAllDay;
    }
}

/* HTML onclick 대응 */
window.openM = openM;
window.closeM = closeM;
window.selSw = selSw;
window.createTeam = createTeam;
window.joinTeam = joinTeam;
window.saveTodo = saveTodo;
window.togTodo = togTodo;
window.closeHomeScheduleModal = closeHomeScheduleModal;

window.openHomeScheduleDetailModal = openHomeScheduleDetailModal;

window.openHomeScheduleEditModal = openHomeScheduleEditModal;

window.submitHomeScheduleUpdate =  submitHomeScheduleUpdate;

window.deleteHomeSchedule = deleteHomeSchedule;

window.toggleHomeEditTimeFields = toggleHomeEditTimeFields;