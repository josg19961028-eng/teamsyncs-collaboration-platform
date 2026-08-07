let drag_card_num = null;
let drag_old_status = null;

function bindKanbanDragEvent() {
    $('.kanbanCard').off('dragstart').on('dragstart', function(e) {
        drag_card_num = $(this).data('card-num');
        drag_old_status = $(this).data('status');
        $(this).addClass('drag');
        e.originalEvent.dataTransfer.effectAllowed = 'move';
    });
    $('.kanbanCard').off('dragend').on('dragend', function() {
        $(this).removeClass('drag');
        $('.kanbanLane').removeClass('hov');
    });
    $('.kanbanLane').off('dragover').on('dragover', function(e) {
        e.preventDefault();
        $(this).addClass('hov');
    });
    $('.kanbanLane').off('dragleave').on('dragleave', function() {
        $(this).removeClass('hov');
    });
    $('.kanbanLane').off('drop').on('drop', function(e) {
        e.preventDefault();
        const new_status = $(this).data('status');
        $('.kanbanLane').removeClass('hov');
        if (!drag_card_num) {
            return;
        }
        if (Number(drag_old_status) === Number(new_status)) {
            return;
        }
        updateKanbanStatusByDrag(drag_card_num, new_status);
    });
}

function updateKanbanStatusByDrag(card_num, kanban_status){
    const team_num = $('#team_num').val();

    const csrfHeader = $('meta[name="csrf-header"]').attr('content');
    const csrfToken = $('meta[name="csrf-token"]').attr('content');

    $.ajax({
        url : `/kanban/board/${team_num}/card/${card_num}/status`,
        type : 'POST',
        dataType : 'json',
        data : {
            kanban_status : kanban_status
        },
        beforeSend : function(xhr){
            xhr.setRequestHeader(csrfHeader, csrfToken);
        },
        success : function(param){
            showToast('✅ 카드 상태가 변경되었습니다.', 'success');
            loadKanbanBoard();
        },
        error : function(xhr){
            handleAjaxError(xhr, '상태 변경 요청 실패');
            loadKanbanBoard();
        }
    });
}

























