/*==========================================
/전역변수/
/==========================================*/
let currentTag = null;
let myOnly = false;
/*==========================================
 * 요청 버튼 잠금
 *==========================================*/
function lockRequestButton(button, loadingText) {
    const $button = $(button);

    // 이미 요청 중이면 중복 실행 방지
    if ($button.data('requesting')) {
        return false;
    }

    // 원래 버튼 문구 보관
    $button.data('original-text', $button.text());
    $button.data('requesting', true);

    $button
        .prop('disabled', true)
        .text(loadingText || '처리 중...');

    return true;
}

/*==========================================
 * 요청 버튼 잠금 해제
 *==========================================*/
function unlockRequestButton(button) {
    const $button = $(button);

    const originalText =
        $button.data('original-text') || '확인';

    $button
        .prop('disabled', false)
        .text(originalText)
        .removeData('requesting')
        .removeData('original-text');
}
/*==========================================
/Http 상태 문구 메서드
/==========================================*/
function handleAjaxError(xhr, defaultMessage) {
	if (xhr.responseJSON) {
		if (xhr.responseJSON.errors) {
			handleValidationErrors(xhr.responseJSON.errors);
			return;
		}
		if (xhr.responseJSON.message) {
			showToast(xhr.responseJSON.message, 'error');
			return;
		}
	}
	if (xhr.status === 400) {
		showToast('잘못된 요청입니다.', 'error');
	} else if (xhr.status === 401) {
		showToast('로그인이 필요합니다.', 'error');
	} else if (xhr.status === 403) {
		showToast('접근 권한이 없습니다.', 'error');
	} else if (xhr.status === 404) {
		showToast('요청한 데이터를 찾을 수 없습니다.', 'error');
	} else {
		showToast(defaultMessage || '요청 처리 중 오류가 발생했습니다.', 'error');
	}
}
/* 토스트 입력문구 */
function handleValidationErrors(errors) {
	$('.input-error').removeClass('input-error');
	let msg = "⚠ 필수 입력 항목을 확인해주세요.<br>";
	if (errors.title) {
		$("#title").addClass("input-error");
		msg += "• " + errors.title + "<br>";
	}
	if (errors.deadline) {
		$("#deadline").addClass("input-error");
		msg += "• " + errors.deadline;
	}
	showToast(msg, "error");
}
/*==========================================
/ 칸반 보드 조회
/==========================================*/
function loadKanbanBoard() {
	const teamNum = $('#team_num').val();
	const keyword = $('#kSearch').val();

	if (!teamNum) return;

	$.ajax({
		url: `/kanban/board/${teamNum}/cards`,
		type: 'GET',
		dataType: 'json',
		data: {
			tag: currentTag,
			keyword: keyword,
			myOnly: myOnly
		},
		success: function(cardList) {
			render(cardList);
		},
		error: function(xhr) {
			console.log("ajax 실패");
			console.log(xhr);
			handleAjaxError(xhr, '칸반 보드 데이터 조회 실패');
		}
	});
}
/*==========================================
/ 칸반 보드 렌더링
/==========================================*/
function render(cardList) {
	const board = $('#board');
	board.empty(); 
	const lanes = [
		{ id: 1, title: '📋 할 일', class: 'todo' },
		{ id: 2, title: '⚡ 진행 중', class: 'inprogress' },
		{ id: 3, title: '🔍 검토', class: 'feedback' },
		{ id: 4, title: '✅ 완료', class: 'done' }
	];
	// 4개의 레인 프레임을 먼저 생성
	lanes.forEach(function(lane) {
	     board.append(createLaneHtml(lane));
	});
	let cardCounts = { 1: 0, 2: 0, 3: 0, 4: 0 };
	let completedCount = 0;

	// 전달받은 카드 리스트를 순회하며 적절한 레인 박스에 분환 배치합니다.
	if (cardList && cardList.length > 0) {
		cardList.forEach(card => {
			const title = card.title || "";
			const tag = card.tag || "기타";
			const content = card.content || "";
			const tagColor = tag === '기타'
			    ? '#8b95a5'
			    : (card.tag_color || '#6c5ce7');
			
			// 카드가 속할 상태값 추출 (기본값 0)
			const status = card.kanban_status;
			cardCounts[status]++;
			if (status === 4) completedCount++; // 완료된 카드 카운트

			// 날짜 포맷
			const deadlineStr = card.deadline
				? new Date(card.deadline).toISOString().split('T')[0]
				: '없음';

			// 마감 임박/초과 아이콘
			let urgentHtml = '';
			if (card.deadline) {
				const today = new Date();
				today.setHours(0, 0, 0, 0);
				const deadlineDate = new Date(card.deadline);
				deadlineDate.setHours(0, 0, 0, 0);
				const diffDays = Math.ceil(
					(deadlineDate - today) / (1000 * 60 * 60 * 24)
				);
				if (status !== 4) {
					if (diffDays < 0) {
						urgentHtml = `<span class="overdueIcon" title="마감일 초과">❗</span>`;
					} else if (diffDays <= 3) {
						urgentHtml = `<span class="urgentIcon" title="마감 임박">⚠</span>`;
					}
				}
			}

			// 담당자 아이콘
			const assignList = card.assignList || [];
			const visibleAssigns = assignList.slice(0, 4);
			const hiddenCount = assignList.length - visibleAssigns.length;
			const assignHtml = `
			    ${visibleAssigns.map(assign => `
			        <div class="cardAssignAv" title="${assign.user_name}">
			            ${assign.user_name ? assign.user_name.charAt(0) : '?'}
			        </div>
			    `).join('')}
			    ${hiddenCount > 0 ? `
			        <div class="cardAssignMore" title="담당자 ${hiddenCount}명 더 있음">
			            +${hiddenCount}
			        </div>
			    ` : ''}
			`;
			
			//카드에 체크리스트 표시를 위한 html생성
			let checklistHtml = '';
			if(card.checklist_total_count > 0){
			    checklistHtml = `
			        <div class="cardProg">
			            <div class="cProgBar">
			                <div class="cProgFill" style="width:${card.checklist_progress}%"></div>
			            </div>
			            <div class="cProgLbl">
			                ${card.checklist_done_count}/${card.checklist_total_count} 완료
			            </div>
			        </div>
			    `;
			}
			// 동적 카드 태그 컴포넌트 생성
			const cardHtml = `
			    <div class="kanbanCard" 
			        draggable="true"
			        data-card-num="${card.card_num}"
			        data-status="${status}">

			        <div class="cardTag rowBox">
			            <span class="chip"
			                  style="background:${tagColor}22; color:${tagColor};">
			                ${tag}
			            </span>
			        </div>

			        <h4 class="cardTitle">${title}</h4>

			        <div class="cardFooter">
			            <div class="cardDeadlineBox">
			                ${urgentHtml}
			                <span class="deadline">
			                    ${urgentHtml ? '' : '📅'} ${deadlineStr}
			                </span>
			            </div>

			            <div class="cardAssignList">
			                ${assignHtml}
			            </div>
			        </div>

			        ${checklistHtml}
			    </div>
			`;
			// 해당 상태의 레인 박스 컨테이너 내부로 쏙 꽂아넣기
			$(`#lane-box-${status}`).append(cardHtml);
		});
	}

	// 각 레인별 카드 개수 뱃지 갱신 및 전체 진행률 바 계산 바인딩
	lanes.forEach(lane => {
		$(`#count-${lane.id}`).text(cardCounts[lane.id]);
	});

	// 전체 진행률 계산 및 상단 UI 반영
	const totalCards = cardList ? cardList.length : 0;
	const progressPercent = totalCards > 0 ? Math.round((completedCount / totalCards) * 100) : 0;
	$('#progText').text(`${progressPercent}% (${completedCount}/${totalCards})`);
	$('#progFill').css('width', `${progressPercent}%`);

	// 카드 클릭 시 상세 모달 열기
	bindKanbanCardClickEvent();
	bindKanbanDragEvent();
}
/*==========================================
/ 레인 생성 함수
/==========================================*/
function createLaneHtml(lane) {
    return `
        <div class="kanbanLane ${lane.className}"
             data-status="${lane.id}">

            <div class="laneHeader">
                <h3>${lane.title}</h3>
                <span class="countBadge"
                      id="count-${lane.id}">
                    0
                </span>
            </div>
            <div class="cardContainer"
                 id="lane-box-${lane.id}">
            </div>
        </div>
    `;
}

/*==========================================
/기타 메서드
/==========================================*/
// 페이지가 로드되면 자동으로 DB에서 카드를 긁어와 화면을 그리도록 시작점 설정
$(document).ready(function() {
	if (typeof loadKanbanBoard === 'function') {
		loadKanbanBoard();
	}
});
// 모달 닫기
function closeModal() {
	$('#mLayer').css('display', 'none');
	$('#mBody').html('');
	$('#modal').removeClass('writeModal detailModal');
}
// 태그 셋팅
function setTag(tag, btn) {
	currentTag = tag;
	$('.filterBar .chip').removeClass('on');
	$(btn).addClass('on');
	loadKanbanBoard();
}
function toggleMyOnly() {
	myOnly = !myOnly;
	$('#myOnlyBtn').toggleClass('on', myOnly);
	loadKanbanBoard();
}

$(document).on('input change', '#title, #deadline', function() {
	$(this).removeClass('input-error');
});

function showToast(message, type = "info") {
	const toast = $("#toast");
	toast.removeClass("success error info on");
	toast
		.addClass(type)
		.html(message)
		.addClass("on");
	clearTimeout(window.toastTimer);
	window.toastTimer = setTimeout(function() {
		toast.removeClass("on");
	}, 2500);
}

function showConfirm(message, okCallback) {
	$('#confirmMsg').text(message);
	$('#confirmLayer').css('display', 'flex');

	$('#confirmOk').off('click').on('click', function() {
		$('#confirmLayer').hide();
		okCallback();
	});

	$('#confirmCancel').off('click').on('click', function() {
		$('#confirmLayer').hide();
	});
}

/*==========================================
/칸반카드 작성폼 열기
/==========================================*/
function openWriteModal() {
	console.log("카드추가 클릭");
	const teamNum = $('#team_num').val();
	$.ajax({
		url: `/kanban/board/${teamNum}/write-form`,
		type: 'GET',
		success: function(htmlData) {
			$('#modal')
				.removeClass('detailModal')
				.addClass('writeModal');
			console.log("ajax 성공");
			console.log(htmlData);
			$('#mTitle').text('새로운 칸반 카드 추가');
			$('#mBody').html(htmlData);
			console.log("mBody 길이 :", $("#mBody").length);
			console.log("mBody 내용 :", $("#mBody").html());
			$('#mLayer').css('display', 'flex');
			console.log("모달 열기 완료");
			setDeadlineMinDate();
			initFormEvent(teamNum);
		},
		error: function(xhr) {
			console.log("ajax 실패");
			console.log(xhr);
			handleAjaxError(xhr, '카드 작성폼을 불러오지 못했습니다.');
		}
	});
}

/*==========================================
/칸반카드 등록 실행
/==========================================*/
function initFormEvent(teamNum) {
	$('#btn_submit_card').off('click').on('click', function() {
		const button = this;

		if (!lockRequestButton(button, '등록 중...')) {
			return;
		}

		const form = $('#kanban_card_form')[0];
		const formData = new FormData(form);

		const csrfHeader = $('meta[name="csrf-header"]').attr('content');
		const csrfToken = $('meta[name="csrf-token"]').attr('content');

		$.ajax({
			url: `/kanban/board/${teamNum}/cards`,
			type: 'POST',
			data: formData,
			processData: false,
			contentType: false,
			dataType: 'json',
			beforeSend: function(xhr) {
				xhr.setRequestHeader(csrfHeader, csrfToken);
			},
			success: function(param) {
				$('.input-error').removeClass('input-error');

				if (param.result === "success") {
					showToast("✅ 카드가 등록되었습니다.", "success");
					closeModal();
					loadKanbanBoard();
					return;
				}
				if (param.errors) {
					handleValidationErrors(param.errors);
				}
			},
			error: function(xhr) {
				console.log(xhr);
				handleAjaxError(xhr, '카드 등록 요청 실패');
			},
			complete: function() {
				unlockRequestButton(button);
			}
		});
	});
}


/*==========================================
/ 태그명 및 색상 처리
/==========================================*/

//태그 입력 감지
$(document).on('input', 'input[name="tag"]', function(){
    console.log('태그 입력 감지:', $(this).val());
    checkExistingTagColor(this);
});
//기존 태그 색상 확인
function checkExistingTagColor(input){
    const tag = $(input).val().trim();
    console.log('입력 태그:', tag);
    console.log('기존 태그 목록:', existingTagList);
    const matched = existingTagList.find(function(t){
        return t.tag === tag;
    });
    console.log('찾은 태그:', matched);
    if(matched){
        $('input[name="tag_color"]')
            .val(matched.tag_color)
            .data('locked', true)
            .data('locked-color', matched.tag_color)
            .addClass('lockedColor');
        showToast(
            '기존 태그 색상이 자동 적용됩니다.',
            'info'
        );
    }else{
        $('input[name="tag_color"]')
            .data('locked', false)
            .removeData('locked-color')
            .removeClass('lockedColor');
    }
}

//기존 태그 색상 변경 방지
$(document).on('input', 'input[name="tag_color"]', function(){
    if($(this).data('locked')){
        const lockedColor = $(this).data('locked-color');
        $(this).val(lockedColor);
        showToast('기존 태그는 색상을 변경할 수 없습니다.', 'error');
    }
});

/*==========================================
/ 마감일 처리
/==========================================*/
function setDeadlineMinDate(){
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    const todayStr = `${year}-${month}-${day}`;
    $('#deadline').attr('min', todayStr);
}
/*==========================================
/칸반카드 추가 시 파일 선택하면 파일명 표시
/==========================================*/
$(document).on('change', '.fileInput', function() {
    const file = this.files[0];
    const fileName = file
        ? file.name
        : '파일을 선택하세요';

    $(this)
        .siblings('.fileUploadLabel')
        .find('.fileText')
        .text(fileName);
});
/*==========================================
/입력감지 콘솔
/==========================================*/

$(document).on('input', '#mBody input', function(){
    console.log('모달 input 입력 감지');
    console.log('id:', $(this).attr('id'));
    console.log('name:', $(this).attr('name'));
    console.log('value:', $(this).val());
});
