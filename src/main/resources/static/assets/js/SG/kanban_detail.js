/*==========================================
 * 요청 버튼 비활성화
 *==========================================*/
function lockRequestButton(button, loadingText) {
	const btn = $(button);

	// 이미 요청 중이면 중복 요청 방지
	if (btn.prop('disabled')) {
		return false;
	}

	// 기존 버튼 문구 저장
	btn.data('original-text', btn.text());

	// 버튼 비활성화
	btn.prop('disabled', true);
	btn.text(loadingText);

	return true;
}

/*==========================================
 * 요청 버튼 활성화
 *==========================================*/
function unlockRequestButton(button) {
	const btn = $(button);

	// 기존 버튼 문구 가져오기
	const originalText = btn.data('original-text');

	// 버튼 활성화 및 기존 문구 복구
	btn.prop('disabled', false);
	btn.text(originalText);
}
/*==========================================
/기타메서드
/==========================================*/
function bindKanbanCardClickEvent() {
    $('.kanbanCard').off('click').on('click', function() {
        const card_num = $(this).data('card-num');
        openDetailModal(card_num);
    });
}

/*==========================================
/칸반카드 상세보기
/==========================================*/
function openDetailModal(card_num) {
    const team_num = $('#team_num').val();

    $.ajax({
        url: `/kanban/board/${team_num}/card/${card_num}`,
        type: 'GET',
        success: function(htmlData) {
            $('#modal').removeClass('writeModal').addClass('detailModal');
            $('#mTitle').text('카드 상세');
            $('#mBody').html(htmlData);
            $('#mLayer').css('display', 'flex');
        },
        error: function(xhr) {
            console.log(xhr);
  			handleAjaxError(xhr, '카드 상세 정보를 불러오지 못했습니다.');
        }
    });
}

/*==========================================
/칸반카드 상태변경
/==========================================*/
function changeKanbanStatus(selectEl){
	const team_num = $('#team_num').val();
	const card_num = $(selectEl).data('card-num');
	const kanban_status = $(selectEl).val();
	const prev_status = $(selectEl).data('prev-status');
	
	const csrfHeader = $('meta[name="csrf-header"]').attr('content');
	const csrfToken = $('meta[name="csrf-token"]').attr('content');
	
	$.ajax({
		url : `/kanban/board/${team_num}/card/${card_num}/status`,
		type : 'POST',
		dataType : 'json',
		data : {
			kanban_status : kanban_status
		},
		beforeSend: function(xhr){
			xhr.setRequestHeader(csrfHeader,csrfToken);
		},
		success: function(param) {
			showToast('✅ 상태가 변경되었습니다.', 'success');
			$(selectEl)
				.removeClass('s1 s2 s3 s4')
				.addClass('s' + kanban_status)
				.data('prev-status', kanban_status);
			loadKanbanBoard();
		},
		error: function(xhr) {
			// 상태 변경 실패 시 이전 상태로 복구
			$(selectEl)
				.val(prev_status)
				.removeClass('s1 s2 s3 s4')
				.addClass('s' + prev_status);
			console.log(xhr);
			handleAjaxError(xhr, '상태 변경 요청 실패');
		}
	});
}

/*==========================================
/카드수정 모달 폼 열기
/==========================================*/
function openUpdateModal(btn){
	const team_num = $('#team_num').val();
	const card_num = $(btn).data('card-num');
	
	$.ajax({
		url : `/kanban/board/${team_num}/card/${card_num}/update-form`,
		type : 'GET',
		success : function(htmlData){
			$('#modal').removeClass('detailModal').addClass('writeModal');
			
			$('#mTitle').text('카드 수정');
			$('#mBody').html(htmlData);
			$('#mLayer').css('display','flex');
			
			setDeadlineMinDate();
			
			initUpdateFormEvent(team_num,card_num);	
		},
		error : function(xhr){
		    handleAjaxError(xhr, '수정 폼을 불러오지 못했습니다.');
		}
	});
}

/*==========================================
/카드수정 저장
/==========================================*/
function initUpdateFormEvent(team_num,card_num){
	$('#btn_update_card').off('click').on('click',function(){
		const form = $('#kanban_update_form')[0];
		const formData = new FormData(form);
		
		const csrfHeader = $('meta[name="csrf-header"]').attr('content');
		const csrfToken = $('meta[name="csrf-token"]').attr('content');
		
		$.ajax({
			url : `/kanban/board/${team_num}/card/${card_num}/update`,
			type : 'POST',
			data : formData,
			processData : false,
			contentType : false,
			dataType : 'json',
			beforeSend : function(xhr) {
				xhr.setRequestHeader(csrfHeader,csrfToken);
			},
			success: function(param) {
				$('.input-error').removeClass('input-error');

				showToast('✅ 카드가 수정되었습니다.', 'success');

				closeModal();
				loadKanbanBoard();
			},
			error: function(xhr) {
				handleAjaxError(xhr, '카드 수정 요청 실패');
			}
		});
	});
}

/*==========================================
/카드 삭제
/==========================================*/
function deleteKanbanCard(btn){
	const team_num = $('#team_num').val();
	const card_num = $(btn).data('card-num');
	
	showConfirm('카드를 삭제하시겠습니까?', function(){
	const csrfHeader = $('meta[name="csrf-header"]').attr('content');
	const csrfToken = $('meta[name="csrf-token"]').attr('content');
	
	$.ajax({
		url : `/kanban/board/${team_num}/card/${card_num}/delete`,
		type:'POST',
		dataType:'json',
		beforeSend : function(xhr){
			xhr.setRequestHeader(csrfHeader,csrfToken);
		},
		success: function(param) {
			showToast('✅ 카드가 삭제되었습니다.', 'success');
			closeModal();
			loadKanbanBoard();
		},
		error: function(xhr) {
			handleAjaxError(xhr, '카드 삭제 요청 실패');
			}
		});
	});
}
/*==========================================
/체크리스트 추가
/==========================================*/
function addKanbanChecklist(card_num,button){
	const team_num = $('#team_num').val();
	const content = $('#check_content_' + card_num).val();
	
	// 버튼으로 실행한 경우 중복 클릭 방지
		if (button && !lockRequestButton(button, '추가 중...')) {
			return;
		}

		// 입력값 검사
		if (content == null || content.trim() === '') {
			showToast('체크리스트 내용을 입력하세요.', 'error');

			if (button) {
				unlockRequestButton(button);
			}

			return;
		}
	const csrfHeader = $('meta[name="csrf-header"]').attr('content');
	const csrfToken = $('meta[name="csrf-token"]').attr('content');
	
	$.ajax({
		url : `/kanban/board/${team_num}/card/${card_num}/checklist`,
		type : 'POST',
		dataType : 'json',
		data:{
			content : content
		},
		beforeSend : function(xhr){
			xhr.setRequestHeader(csrfHeader,csrfToken);
		},
		success:function(param){
		    showToast('✅ 체크리스트가 추가되었습니다.', 'success');
			loadKanbanBoard();
			openDetailModal(card_num);
		},
		error:function(xhr){
		    handleAjaxError(xhr, '체크리스트 추가 요청 실패');
		},
		complete: function() {
			if (button) {
				unlockRequestButton(button);
			}
		}
	});
}

/*==========================================
/체크리스트 완료/미완료 변경
/==========================================*/
function changeChecklistChecked(chk){
	const team_num = $('#team_num').val();
	const card_num = $(chk).data('card-num');
	const checklist_num = $(chk).data('checklist-num');
	const checked = $(chk).is(':checked')?2:1;

	const csrfHeader = $('meta[name="csrf-header"]').attr('content');
	const csrfToken = $('meta[name="csrf-token"]').attr('content');
	
	$.ajax({
		url : `/kanban/board/${team_num}/card/${card_num}/checklist/${checklist_num}/checked`,
		type : 'POST',
		dataType : 'json',
		data : {
			checked : checked
		},
		beforeSend : function(xhr){
			xhr.setRequestHeader(csrfHeader,csrfToken);
		},
		success: function(param) {
			showToast('✅ 체크리스트 상태가 변경되었습니다.', 'success');
			loadKanbanBoard();
			openDetailModal(card_num);
		},
		error: function(xhr) {
			// 실패하면 체크박스 상태 원상복구
			$(chk).prop('checked', !$(chk).is(':checked'));
			handleAjaxError(xhr, '체크리스트 상태 변경 요청 실패');
		}
	});
}

/*==========================================
/체크리스트 삭제
/==========================================*/
function deleteKanbanChecklist(btn){
	const team_num = $('#team_num').val();
	const card_num = $(btn).data('card-num');
	const checklist_num = $(btn).data('checklist-num');
	
	showConfirm('체크리스트를 삭제하시겠습니까?', function(){

	const csrfHeader = $('meta[name="csrf-header"]').attr('content');
	const csrfToken = $('meta[name="csrf-token"]').attr('content');
	
	$.ajax({
		url : `/kanban/board/${team_num}/card/${card_num}/checklist/${checklist_num}/delete`,
		type : 'POST',
		dataType : 'json',
		beforeSend : function(xhr){
			xhr.setRequestHeader(csrfHeader,csrfToken);
		},
		success : function(param){
		    showToast('✅ 체크리스트가 삭제되었습니다.', 'success');
			loadKanbanBoard();
		    openDetailModal(card_num);
		},
		error: function(xhr){
		    handleAjaxError(xhr, '체크리스트 삭제 요청 실패');
			}
		});
	});
}

/*==========================================
/댓글 추가
/==========================================*/
function addKanbanComment(card_num,button){
	const team_num = $('#team_num').val();
	const content = $('#comment_content_' + card_num).val();

	// 버튼 연속 클릭 방지
	if (button && !lockRequestButton(button, '전송 중...')) {
		return;
	}

	// 입력값 검사
	if (content == null || content.trim() === '') {
		showToast('댓글 내용을 입력하세요.', 'error');

		if (button) {
			unlockRequestButton(button);
		}

		return;
	}

	
	const csrfHeader = $('meta[name="csrf-header"]').attr('content');
	const csrfToken = $('meta[name="csrf-token"]').attr('content');
	
	$.ajax({
		url : `/kanban/board/${team_num}/card/${card_num}/comments`,
		type : 'POST',
		dataType : 'json',
		data : {
			content : content
		},
		beforeSend : function(xhr){
			xhr.setRequestHeader(csrfHeader, csrfToken);
		},
		success : function(param){
		    showToast('✅ 댓글이 등록되었습니다.', 'success');
			loadKanbanBoard();
		    openDetailModal(card_num);
		},
		error : function(xhr){
		    handleAjaxError(xhr, '댓글 등록 요청 실패');
		},
		complete: function() {
			if (button) {
				unlockRequestButton(button);
			}
		}
	});
}
/*==========================================
/댓글 수정 폼 열기
/==========================================*/
function openUpdateCommentForm(btn){
	const card_num = $(btn).data('card-num');
	const comment_num = $(btn).data('comment-num');

	const cmtBox = $(btn).closest('.cmtB');
	const textDiv = cmtBox.find('.cmtText');
	const cmtActs = cmtBox.find('.cmtActs');
	const oldContent = textDiv.text();

	// 이미 수정폼이 열려있으면 중복 생성 방지
	if(cmtBox.find('.commentUpdateForm').length > 0){
		return;
	}

	// 기존 댓글 내용 숨기기
	textDiv.hide();

	// 수정/삭제 버튼 숨기기
	cmtActs.hide();

	const formHtml = `
		<div class="commentUpdateForm">
			<input type="text"
				   class="commentUpdateContent"
				   value="${oldContent}"
				   placeholder="댓글 수정 후 Enter"
				   onkeydown="if(event.key==='Enter') updateKanbanComment(this, ${card_num}, ${comment_num})">

			<div class="commentUpdateBtns">
				<button type="button"
						class="btn sm primary"
						onclick="updateKanbanComment(this, ${card_num}, ${comment_num})">
					저장
				</button>

				<button type="button"
						class="btn sm"
						onclick="cancelUpdateComment(this)">
					취소
				</button>
			</div>
		</div>
	`;

	textDiv.after(formHtml);
}
/*==========================================
/댓글 수정 취소
/==========================================*/
function cancelUpdateComment(btn){
	const cmtBox = $(btn).closest('.cmtB');

	// 수정폼 삭제
	cmtBox.find('.commentUpdateForm').remove();

	// 기존 댓글 내용 표시
	cmtBox.find('.cmtText').show();

	// 수정/삭제 버튼 표시
	cmtBox.find('.cmtActs').show();
}
/*==========================================
/댓글 수정 저장
/==========================================*/
function updateKanbanComment(btn, card_num, comment_num){
	const team_num = $('#team_num').val();
	const content = $(btn)
		.closest('.commentUpdateForm')
		.find('.commentUpdateContent')
		.val();

	if(content == null || content.trim() === ''){
		showToast('댓글 내용을 입력하세요.', 'error');
		return;
	}

	const csrfHeader = $('meta[name="csrf-header"]').attr('content');
	const csrfToken = $('meta[name="csrf-token"]').attr('content');

	$.ajax({
		url : `/kanban/board/${team_num}/card/${card_num}/comments/${comment_num}/update`,
		type : 'POST',
		dataType : 'json',
		data : {
			content : content
		},
		beforeSend : function(xhr){
			xhr.setRequestHeader(csrfHeader, csrfToken);
		},
		success : function(param){
			showToast('✅ 댓글이 수정되었습니다.', 'success');
			openDetailModal(card_num);
		},
		error : function(xhr){
			handleAjaxError(xhr, '댓글 수정 요청 실패');
		}
	});
}

/*==========================================
/댓글 삭제
/==========================================*/
function deleteKanbanComment(btn){
	const team_num = $('#team_num').val();
	const card_num = $(btn).data('card-num');
	const comment_num = $(btn).data('comment-num');

	showConfirm('댓글을 삭제하시겠습니까?', function(){

		const csrfHeader = $('meta[name="csrf-header"]').attr('content');
		const csrfToken = $('meta[name="csrf-token"]').attr('content');

		$.ajax({
			url : `/kanban/board/${team_num}/card/${card_num}/comments/${comment_num}/delete`,
			type : 'POST',
			dataType : 'json',
			beforeSend : function(xhr){
				xhr.setRequestHeader(csrfHeader, csrfToken);
			},
			success : function(param){
				showToast('✅ 댓글이 삭제되었습니다.', 'success');
				openDetailModal(card_num);
			},
			error : function(xhr){
				handleAjaxError(xhr, '댓글 삭제 요청 실패');
			}
		});
	});
}
/*==========================================
/담당자 추가
/==========================================*/
function addKanbanAssign(sel){
	const team_num = $('#team_num').val();
	const card_num = $(sel).data('card-num');
	const user_num = $(sel).val();

	if(user_num === ''){
		return;
	}

	const csrfHeader = $('meta[name="csrf-header"]').attr('content');
	const csrfToken = $('meta[name="csrf-token"]').attr('content');

	$.ajax({
		url : `/kanban/board/${team_num}/card/${card_num}/assign`,
		type : 'POST',
		dataType : 'json',
		data : {
			user_num : user_num
		},
		beforeSend : function(xhr){
			xhr.setRequestHeader(csrfHeader, csrfToken);
		},
		success : function(param){
			showToast('✅ 담당자가 추가되었습니다.', 'success');
			loadKanbanBoard();
			openDetailModal(card_num);
		},
		error : function(xhr){
			$(sel).val('');
			handleAjaxError(xhr, '담당자 추가 요청 실패');
		}
	});
}

/*==========================================
/담당자 삭제
/==========================================*/
function deleteKanbanAssign(btn){
	const team_num = $('#team_num').val();
	const card_num = $(btn).data('card-num');
	const user_num = $(btn).data('user-num');

	showConfirm('담당자를 삭제하시겠습니까?', function(){

		const csrfHeader = $('meta[name="csrf-header"]').attr('content');
		const csrfToken = $('meta[name="csrf-token"]').attr('content');

		$.ajax({
			url : `/kanban/board/${team_num}/card/${card_num}/assign/delete`,
			type : 'POST',
			dataType : 'json',
			data : {
				user_num : user_num
			},
			beforeSend : function(xhr){
				xhr.setRequestHeader(csrfHeader, csrfToken);
			},
			success : function(param){
				showToast('✅ 담당자가 삭제되었습니다.', 'success');
				loadKanbanBoard();
				openDetailModal(card_num);
			},
			error : function(xhr){
				handleAjaxError(xhr, '담당자 삭제 요청 실패');
			}
		});
	});
}











