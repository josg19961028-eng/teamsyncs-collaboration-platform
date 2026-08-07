/* assets/js/YJ/calendar-page.js */

let pageCalendar;
let pageTeamNum = null;
let selectedPageScheduleEvent = null;

document.addEventListener('DOMContentLoaded', function () {
    initCalendarPage();
});

/* =========================
   큰 캘린더 초기화
========================= */
function initCalendarPage() {
    const calendarEl = document.getElementById('calendar');

    if (!calendarEl || typeof FullCalendar === 'undefined') {
        console.log('calendar 또는 FullCalendar를 찾을 수 없습니다.');
        return;
    }

    const rawTeamNum =
        calendarEl.dataset.teamNum || getTeamNumFromUrl();

    pageTeamNum = Number(rawTeamNum);

    if (!Number.isFinite(pageTeamNum) || pageTeamNum <= 0) {
        alert('팀 번호가 없습니다. 팀 화면에서 캘린더로 다시 진입해주세요.');
        return;
    }

    const eventsUrl =
        calendarEl.dataset.eventsUrl ||
        `/calendar/events?team_num=${pageTeamNum}`;

    pageCalendar = new FullCalendar.Calendar(calendarEl, {
        initialView: 'dayGridMonth',
        locale: 'ko',
        height: 'auto',

        headerToolbar: {
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,timeGridWeek,listWeek'
        },

        buttonText: {
            today: '오늘',
            month: '월',
            week: '주',
            list: '목록'
        },

        dayMaxEventRows: 3,
        moreLinkText: '더보기',

        events: function (fetchInfo, successCallback, failureCallback) {
            const startDate = fetchInfo.startStr.slice(0, 10);
            const endDate = fetchInfo.endStr.slice(0, 10);
            const separator = eventsUrl.includes('?') ? '&' : '?';
            const rangedEventsUrl =
                `${eventsUrl}${separator}` +
                `start=${encodeURIComponent(startDate)}` +
                `&end=${encodeURIComponent(endDate)}`;

            fetch(rangedEventsUrl, {
                credentials: 'same-origin'
            })
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error(`HTTP 오류: ${response.status}`);
                    }

                    return response.json();
                })
                .then(function (data) {
                    successCallback(data);
                })
                .catch(function (error) {
                    console.error('캘린더 일정 조회 오류:', error);
                    failureCallback(error);
                });
        },

        dateClick: function (info) {
            openPageScheduleCreateModal(info.dateStr);
        },

        eventClick: function (info) {
            openPageScheduleDetailModal(info.event);
        }
    });

    pageCalendar.render();
}

/* =========================
   일정 등록 모달
========================= */
function openPageScheduleCreateModal(selectedDate) {
    const layer = document.getElementById('mLayer');
    const modalTitle = document.getElementById('mTitle2');
    const body = document.getElementById('mBody');

    if (!layer || !modalTitle || !body) {
        console.error('캘린더 모달 요소를 찾을 수 없습니다.');
        return;
    }

    const targetDate = selectedDate || getLocalToday();

    modalTitle.textContent = '일정 등록';

    body.innerHTML = `
        <div class="fG">
            <div class="f full">
                <label for="pageCreateTitle">제목 *</label>
                <input type="text"
                       id="pageCreateTitle"
                       maxlength="200"
                       placeholder="일정 제목">
            </div>

            <div class="f">
                <label for="pageCreateCategory">카테고리</label>
                <select id="pageCreateCategory">
                    <option value="일반 일정">일반 일정</option>
                    <option value="회의">회의</option>
                    <option value="개발">개발</option>
                    <option value="마감">마감</option>
                </select>
            </div>

            <div class="f">
                <label for="pageCreateColor">색상</label>
                <input type="color"
                       id="pageCreateColor"
                       value="#6C5CE7">
            </div>

            <div class="f">
                <label for="pageCreateStartDate">시작일 *</label>
                <input type="date"
                       id="pageCreateStartDate"
                       value="${targetDate}">
            </div>

            <div class="f">
                <label for="pageCreateEndDate">종료일 *</label>
                <input type="date"
                       id="pageCreateEndDate"
                       value="${targetDate}">
            </div>

            <div class="f full schedule-all-day-row">
                <label class="schedule-switch-label"
                       for="pageCreateAllDay">
                    <span>종일 일정</span>

                    <span class="schedule-switch">
                        <input type="checkbox"
                               id="pageCreateAllDay"
                               checked
                               onchange="togglePageCreateTimeFields()">
                        <span class="schedule-switch-slider"></span>
                    </span>
                </label>
            </div>

            <div class="f calendar-page-time-field"
                 data-time-group="create"
                 hidden>
                <label for="pageCreateStartTime">시작 시간 *</label>
                <input type="time"
                       id="pageCreateStartTime"
                       value="09:00"
                       disabled>
            </div>

            <div class="f calendar-page-time-field"
                 data-time-group="create"
                 hidden>
                <label for="pageCreateEndTime">종료 시간 *</label>
                <input type="time"
                       id="pageCreateEndTime"
                       value="10:00"
                       disabled>
            </div>

            <div class="f full">
                <label for="pageCreateContent">내용</label>
                <textarea id="pageCreateContent"
                          maxlength="2000"
                          placeholder="일정 상세 내용"></textarea>
            </div>
        </div>

        <div class="acts">
            <button type="button"
                    class="btn"
                    onclick="closeCalendarModal()">
                취소
            </button>

            <button type="button"
                    class="btn primary"
                    id="btnCreatePageSchedule"
                    onclick="createPageSchedule()">
                저장
            </button>
        </div>
    `;

    togglePageCreateTimeFields();
    layer.classList.add('open');

    document.getElementById('pageCreateTitle')?.focus();
}

function togglePageCreateTimeFields() {
    toggleCalendarTimeFields({
        allDayId: 'pageCreateAllDay',
        startTimeId: 'pageCreateStartTime',
        endTimeId: 'pageCreateEndTime',
        selector: '[data-time-group="create"]'
    });
}

async function createPageSchedule() {
    const title =
        document.getElementById('pageCreateTitle')?.value.trim();

    const category =
        document.getElementById('pageCreateCategory')?.value;

    const color =
        document.getElementById('pageCreateColor')?.value || '#6C5CE7';

    const startDate =
        document.getElementById('pageCreateStartDate')?.value;

    const endDate =
        document.getElementById('pageCreateEndDate')?.value;

    const startTime =
        document.getElementById('pageCreateStartTime')?.value;

    const endTime =
        document.getElementById('pageCreateEndTime')?.value;

    const content =
        document.getElementById('pageCreateContent')?.value.trim() || '';

    const isAllDay =
        document.getElementById('pageCreateAllDay')?.checked;

    const saveButton =
        document.getElementById('btnCreatePageSchedule');

    if (!title) {
        alert('제목을 입력하세요.');
        return;
    }

    if (!startDate || !endDate) {
        alert('시작일과 종료일을 입력하세요.');
        return;
    }

    const dateTimeValues = buildScheduleDateTimeValues({
        startDate: startDate,
        endDate: endDate,
        startTime: startTime,
        endTime: endTime,
        isAllDay: isAllDay
    });

    if (!dateTimeValues) {
        return;
    }

    const scheduleData = {
        team_num: pageTeamNum,
        title: title,
        category: category,
        color: color,
        content: content,
        start_date: dateTimeValues.startValue,
        end_date: dateTimeValues.endValue,
        all_day: dateTimeValues.allDay
    };

    setButtonLoading(saveButton, true, '저장 중...');

    try {
        const result = await requestCalendarApi(
            '/calendar/write',
            scheduleData
        );

        if (result.result === 'success') {
            closeCalendarModal();
            showCalendarToast('일정이 등록되었습니다.');
            pageCalendar?.refetchEvents();
            return;
        }

        alert(result.message || '일정 등록에 실패했습니다.');

    } catch (error) {
        console.error('일정 등록 오류:', error);
        alert('일정 등록 중 오류가 발생했습니다.');

    } finally {
        setButtonLoading(saveButton, false, '저장');
    }
}

/* =========================
   일정 상세 모달
========================= */
function openPageScheduleDetailModal(calendarEvent) {
    selectedPageScheduleEvent = calendarEvent;

    const layer = document.getElementById('mLayer');
    const modalTitle = document.getElementById('mTitle2');
    const body = document.getElementById('mBody');

    if (!layer || !modalTitle || !body || !calendarEvent) {
        console.error('일정 상세 모달 요소를 찾을 수 없습니다.');
        return;
    }

    const props = calendarEvent.extendedProps || {};
    const type = props.type || 'SCHEDULE';

    const title = calendarEvent.title || '제목 없음';
    const category = props.category || '일반 일정';
    const content = props.content || '등록된 내용이 없습니다.';
    const typeText = type === 'KANBAN' ? '칸반' : '일정';

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

    const color = isValidHexColor(rawColor)
        ? rawColor
        : '#6C5CE7';

    const manageButtons = type === 'KANBAN'
        ? ''
        : `
            <button type="button"
                    class="btn danger"
                    onclick="deletePageSchedule()">
                삭제
            </button>

            <button type="button"
                    class="btn"
                    onclick="openPageScheduleEditModal()">
                수정
            </button>
        `;

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
                    <span class="schedule-detail-label">구분</span>
                    <strong>${escapeHtml(typeText)}</strong>
                </div>

                <div class="schedule-detail-item">
                    <span class="schedule-detail-label">일정 유형</span>
                    <strong>
                        ${calendarEvent.allDay
                            ? '종일 일정'
                            : '시간 지정 일정'}
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
                ${manageButtons}

                <button type="button"
                        class="btn primary"
                        onclick="closeCalendarModal()">
                    확인
                </button>
            </div>
        </div>
    `;

    layer.classList.add('open');
}

/* =========================
   일정 수정 모달
========================= */
function openPageScheduleEditModal() {
    const calendarEvent = selectedPageScheduleEvent;

    const layer = document.getElementById('mLayer');
    const modalTitle = document.getElementById('mTitle2');
    const body = document.getElementById('mBody');

    if (!calendarEvent || !layer || !modalTitle || !body) {
        alert('수정할 일정 정보를 확인할 수 없습니다.');
        return;
    }

    const props = calendarEvent.extendedProps || {};

    if (props.type === 'KANBAN') {
        alert('칸반 일정은 칸반 페이지에서 수정해주세요.');
        return;
    }

    const allDay = calendarEvent.allDay;

    const startDate = calendarEvent.startStr
        ? calendarEvent.startStr.slice(0, 10)
        : formatLocalDate(calendarEvent.start);

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
        if (calendarEvent.startStr?.length >= 16) {
            startTime = calendarEvent.startStr.slice(11, 16);
        }

        if (calendarEvent.endStr) {
            endDate = calendarEvent.endStr.slice(0, 10);

            if (calendarEvent.endStr.length >= 16) {
                endTime = calendarEvent.endStr.slice(11, 16);
            }
        }
    }

    const rawColor =
        calendarEvent.backgroundColor ||
        props.color ||
        '#6C5CE7';

    const color = isValidHexColor(rawColor)
        ? rawColor
        : '#6C5CE7';

    const currentCategory = props.category || '일반 일정';

    const categories = [
        currentCategory,
        '일반 일정',
        '회의',
        '개발',
        '마감'
    ].filter(function (item, index, array) {
        return array.indexOf(item) === index;
    });

    const categoryOptions = categories.map(function (item) {
        return `
            <option value="${escapeAttribute(item)}"
                    ${item === currentCategory ? 'selected' : ''}>
                ${escapeHtml(item)}
            </option>
        `;
    }).join('');

    modalTitle.textContent = '일정 수정';

    body.innerHTML = `
        <div class="fG">
            <div class="f full">
                <label for="pageEditTitle">제목 *</label>
                <input type="text"
                       id="pageEditTitle"
                       maxlength="200"
                       value="${escapeAttribute(calendarEvent.title || '')}">
            </div>

            <div class="f">
                <label for="pageEditCategory">카테고리</label>
                <select id="pageEditCategory">
                    ${categoryOptions}
                </select>
            </div>

            <div class="f">
                <label for="pageEditColor">색상</label>
                <input type="color"
                       id="pageEditColor"
                       value="${escapeAttribute(color)}">
            </div>

            <div class="f">
                <label for="pageEditStartDate">시작일 *</label>
                <input type="date"
                       id="pageEditStartDate"
                       value="${startDate}">
            </div>

            <div class="f">
                <label for="pageEditEndDate">종료일 *</label>
                <input type="date"
                       id="pageEditEndDate"
                       value="${endDate}">
            </div>

            <div class="f full schedule-all-day-row">
                <label class="schedule-switch-label"
                       for="pageEditAllDay">
                    <span>종일 일정</span>

                    <span class="schedule-switch">
                        <input type="checkbox"
                               id="pageEditAllDay"
                               ${allDay ? 'checked' : ''}
                               onchange="togglePageEditTimeFields()">
                        <span class="schedule-switch-slider"></span>
                    </span>
                </label>
            </div>

            <div class="f calendar-page-time-field"
                 data-time-group="edit"
                 ${allDay ? 'hidden' : ''}>
                <label for="pageEditStartTime">시작 시간 *</label>
                <input type="time"
                       id="pageEditStartTime"
                       value="${startTime}"
                       ${allDay ? 'disabled' : ''}>
            </div>

            <div class="f calendar-page-time-field"
                 data-time-group="edit"
                 ${allDay ? 'hidden' : ''}>
                <label for="pageEditEndTime">종료 시간 *</label>
                <input type="time"
                       id="pageEditEndTime"
                       value="${endTime}"
                       ${allDay ? 'disabled' : ''}>
            </div>

            <div class="f full">
                <label for="pageEditContent">내용</label>
                <textarea id="pageEditContent"
                          maxlength="2000">${escapeHtml(props.content || '')}</textarea>
            </div>
        </div>

        <div class="acts">
            <button type="button"
                    class="btn"
                    onclick="reopenPageScheduleDetail()">
                취소
            </button>

            <button type="button"
                    class="btn primary"
                    id="btnUpdatePageSchedule"
                    onclick="updatePageSchedule()">
                수정 완료
            </button>
        </div>
    `;

    togglePageEditTimeFields();
    layer.classList.add('open');
}

function togglePageEditTimeFields() {
    toggleCalendarTimeFields({
        allDayId: 'pageEditAllDay',
        startTimeId: 'pageEditStartTime',
        endTimeId: 'pageEditEndTime',
        selector: '[data-time-group="edit"]'
    });
}

async function updatePageSchedule() {
    const scheduleNum = getScheduleNum(selectedPageScheduleEvent);

    const title =
        document.getElementById('pageEditTitle')?.value.trim();

    const category =
        document.getElementById('pageEditCategory')?.value;

    const color =
        document.getElementById('pageEditColor')?.value || '#6C5CE7';

    const startDate =
        document.getElementById('pageEditStartDate')?.value;

    const endDate =
        document.getElementById('pageEditEndDate')?.value;

    const startTime =
        document.getElementById('pageEditStartTime')?.value;

    const endTime =
        document.getElementById('pageEditEndTime')?.value;

    const content =
        document.getElementById('pageEditContent')?.value.trim() || '';

    const isAllDay =
        document.getElementById('pageEditAllDay')?.checked;

    const updateButton =
        document.getElementById('btnUpdatePageSchedule');

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

    const dateTimeValues = buildScheduleDateTimeValues({
        startDate: startDate,
        endDate: endDate,
        startTime: startTime,
        endTime: endTime,
        isAllDay: isAllDay
    });

    if (!dateTimeValues) {
        return;
    }

    const scheduleData = {
        schedule_num: scheduleNum,
        title: title,
        category: category,
        color: color,
        content: content,
        start_date: dateTimeValues.startValue,
        end_date: dateTimeValues.endValue,
        all_day: dateTimeValues.allDay
    };

    setButtonLoading(updateButton, true, '수정 중...');

    try {
        const result = await requestCalendarApi(
            '/calendar/update',
            scheduleData
        );

        if (result.result === 'success') {
            closeCalendarModal();
            selectedPageScheduleEvent = null;
            showCalendarToast('일정이 수정되었습니다.');
            pageCalendar?.refetchEvents();
            return;
        }

        alert(result.message || '일정 수정에 실패했습니다.');

    } catch (error) {
        console.error('일정 수정 오류:', error);
        alert('일정 수정 중 오류가 발생했습니다.');

    } finally {
        setButtonLoading(updateButton, false, '수정 완료');
    }
}

/* =========================
   일정 삭제
========================= */
async function deletePageSchedule() {
    const scheduleNum = getScheduleNum(selectedPageScheduleEvent);

    if (!scheduleNum) {
        alert('일정 번호를 확인할 수 없습니다.');
        return;
    }

    if (!confirm('이 일정을 삭제하시겠습니까?')) {
        return;
    }

    try {
        const result = await requestCalendarApi(
            '/calendar/delete',
            { schedule_num: scheduleNum }
        );

        if (result.result === 'success') {
            closeCalendarModal();
            selectedPageScheduleEvent = null;
            showCalendarToast('일정이 삭제되었습니다.');
            pageCalendar?.refetchEvents();
            return;
        }

        alert(result.message || '일정 삭제에 실패했습니다.');

    } catch (error) {
        console.error('일정 삭제 오류:', error);
        alert('일정 삭제 중 오류가 발생했습니다.');
    }
}

/* =========================
   공통 모달 / 요청
========================= */
function closeCalendarModal() {
    document.getElementById('mLayer')?.classList.remove('open');
}

function reopenPageScheduleDetail() {
    if (selectedPageScheduleEvent) {
        openPageScheduleDetailModal(selectedPageScheduleEvent);
    }
}

function toggleCalendarTimeFields(config) {
    const allDay = document.getElementById(config.allDayId);
    const startTime = document.getElementById(config.startTimeId);
    const endTime = document.getElementById(config.endTimeId);
    const timeFields = document.querySelectorAll(config.selector);

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

function buildScheduleDateTimeValues(config) {
    const allDay = config.isAllDay ? 1 : 2;

    let startValue = config.startDate;
    let endValue = config.endDate;

    if (allDay === 2) {
        if (!config.startTime || !config.endTime) {
            alert('시작 시간과 종료 시간을 입력하세요.');
            return null;
        }

        startValue = `${config.startDate}T${config.startTime}`;
        endValue = `${config.endDate}T${config.endTime}`;

        if (endValue <= startValue) {
            alert('종료 일시는 시작 일시보다 늦어야 합니다.');
            return null;
        }
    } else if (config.endDate < config.startDate) {
        alert('종료일은 시작일보다 빠를 수 없습니다.');
        return null;
    }

    return {
        startValue: startValue,
        endValue: endValue,
        allDay: allDay
    };
}

async function requestCalendarApi(url, data) {
    const response = await fetch(url, {
        method: 'POST',
        headers: getJsonHeaders(),
        credentials: 'same-origin',
        body: JSON.stringify(data)
    });

    if (!response.ok) {
        throw new Error(`HTTP 오류: ${response.status}`);
    }

    return response.json();
}

function getJsonHeaders() {
    const headers = {
        'Content-Type': 'application/json; charset=UTF-8'
    };

    const csrfToken =
        document.querySelector('meta[name="csrf-token"]')?.content;

    const csrfHeader =
        document.querySelector('meta[name="csrf-header"]')?.content;

    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    return headers;
}

function setButtonLoading(button, loading, text) {
    if (!button) {
        return;
    }

    button.disabled = loading;
    button.textContent = text;
}

/* =========================
   날짜 / 문자열 유틸
========================= */
function getTeamNumFromUrl() {
    const params = new URLSearchParams(location.search);
    return params.get('team_num');
}

function getLocalToday() {
    return formatLocalDate(new Date());
}

function getScheduleNum(calendarEvent) {
    if (!calendarEvent) {
        return null;
    }

    const props = calendarEvent.extendedProps || {};

    const rawScheduleNum =
        props.schedule_num ?? calendarEvent.id;

    if (rawScheduleNum === null || rawScheduleNum === undefined) {
        return null;
    }

    const matched = String(rawScheduleNum).match(/(\d+)$/);

    return matched ? Number(matched[1]) : null;
}

function formatCalendarDate(date, allDay) {
    if (!date) {
        return '-';
    }

    const dateText = formatLocalDate(date);

    if (allDay) {
        return dateText;
    }

    return `${dateText} ${formatLocalTime(date)}`;
}

function formatCalendarEndDate(endDate, startDate, allDay) {
    if (!endDate) {
        return formatCalendarDate(startDate, allDay);
    }

    const displayEnd = new Date(endDate);

    if (allDay) {
        displayEnd.setDate(displayEnd.getDate() - 1);
        return formatLocalDate(displayEnd);
    }

    return `${formatLocalDate(displayEnd)} ${formatLocalTime(displayEnd)}`;
}

function formatLocalDate(date) {
    const d = date instanceof Date ? date : new Date(date);

    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
}

function formatLocalTime(date) {
    const d = date instanceof Date ? date : new Date(date);

    const hour = String(d.getHours()).padStart(2, '0');
    const minute = String(d.getMinutes()).padStart(2, '0');

    return `${hour}:${minute}`;
}

function isValidHexColor(value) {
    return /^#[0-9a-fA-F]{6}$/.test(value || '');
}

function escapeHtml(value) {
    const element = document.createElement('div');

    element.textContent =
        value === null || value === undefined
            ? ''
            : String(value);

    return element.innerHTML;
}

function escapeAttribute(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
}

function showCalendarToast(message) {
    const toast = document.getElementById('toast');

    if (!toast) {
        return;
    }

    toast.textContent = message;
    toast.classList.add('show');

    setTimeout(function () {
        toast.classList.remove('show');
    }, 2500);
}

/* HTML onclick / onchange 대응 */
window.openPageScheduleCreateModal = openPageScheduleCreateModal;
window.togglePageCreateTimeFields = togglePageCreateTimeFields;
window.createPageSchedule = createPageSchedule;
window.openPageScheduleDetailModal = openPageScheduleDetailModal;
window.openPageScheduleEditModal = openPageScheduleEditModal;
window.togglePageEditTimeFields = togglePageEditTimeFields;
window.updatePageSchedule = updatePageSchedule;
window.deletePageSchedule = deletePageSchedule;
window.reopenPageScheduleDetail = reopenPageScheduleDetail;
window.closeCalendarModal = closeCalendarModal;
