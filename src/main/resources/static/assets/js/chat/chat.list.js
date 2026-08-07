/**
 * TeamSync - 실시간 웹소켓(STOMP) 채팅 및 채널 연동 스크립트
 */

// ── 🌟 전역 변수: 웹소켓과 구독 상태를 관리합니다 ──
let stompClient = null;          // STOMP 클라이언트 객체
let currentChannelId = null;     // 현재 접속 중인 방 번호
let currentSubscription = null;  // 현재 방의 구독(수신) 상태
let myUserId = -1;               // 내 회원 번호
let myUserName = '알 수 없음';     // 🌟 내 이름 (새로 추가됨)
let replyTargetMessageNum = null;
let replyTargetContent = null;
let replyTargetUserName = null;

// ── 1. 웹소켓 최초 연결 함수 ──
function connectWebSocket() {
    const loginUserEl = document.getElementById('loginUserId');
    myUserId = loginUserEl ? parseInt(loginUserEl.value) : -1;

    const loginNameEl = document.getElementById('loginUserName');
    if (loginNameEl && loginNameEl.value) {
        myUserName = loginNameEl.value;
    }

    const socket = new SockJS('/ws-chat');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function (frame) {
        console.log('✅ WebSocket 실시간 연결 성공! (접속자: ' + myUserName + ')');
        // 연결 성공 후 모든 채널 구독
        subscribeAllChannels();
    }, function (error) {
        console.error('❌ WebSocket 연결 에러: ', error);
    });
}

// ── 전체 채널 구독 ──
function subscribeAllChannels() {
    const channelItems = document.querySelectorAll('.channel-item[data-channel-num]');
    channelItems.forEach(item => {
        const channelNum = parseInt(item.getAttribute('data-channel-num'));
        if (channelNum) {
            subscribeToChannelBackground(channelNum);
        }
    });
}

// ── 2. 방 클릭 시: 과거 메시지 로드 및 실시간 방 구독 ──
function switchChannel(channelNum, channelName, clickedElement) {
    currentChannelId = channelNum;

    const allItems = document.querySelectorAll('.channel-item');
    allItems.forEach(item => item.classList.remove('active'));
    if(clickedElement) clickedElement.classList.add('active');

    document.getElementById('chatRoomName').textContent = channelName;

    const msgContainer = document.getElementById('chatMessageContainer');
    msgContainer.innerHTML = `<div style="text-align:center; padding:48px; color:#7b7394; font-size:13px; font-weight:700;">[ ${channelName} ] 방의 대화 내역을 불러오는 중...</div>`;

    fetch(`/chat/messages/${channelNum}`)
        .then(response => {
            if (!response.ok) throw new Error("API 네트워크 에러");
            return response.json();
        })
        .then(messageList => {
            renderMessages(messageList, msgContainer);
            subscribeToChannel(channelNum);
            markAsRead(channelNum, clickedElement);
        })
        .catch(error => {
            console.error("메시지 로드 에러:", error);
        });
}

function markAsRead(channelNum, channelItem) {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    fetch(`/chat/read/${channelNum}`, {
        method: 'POST',
        headers: { [csrfHeader]: csrfToken }
    })
    .then(res => {
        if (!res.ok) return;
        // 뱃지 숨김
        if (channelItem) {
            const badge = channelItem.querySelector('.unread-badge');
            if (badge) {
                badge.textContent = '0';
                badge.style.display = 'none';
            }
        }
    });
}
// ── 3. 특정 방 구독 함수 (실시간 메시지 수신 대기) ──
function subscribeToChannel(channelId) {
	// 이미 백그라운드로 구독 중이면 별도 구독 불필요
	// currentChannelId만 업데이트하면 백그라운드 구독에서 분기 처리됨
	if (!backgroundSubscriptions[channelId]) {
		subscribeToChannelBackground(channelId);
	}
	console.log(`📡 [${channelId}]번 방 활성화`);
}


// ── 파일 선택 시 미리보기 ──
document.getElementById('fileInput').addEventListener('change', function(e) {
    const file = e.target.files[0];
    if (!file) return;

    if (file.size > 10 * 1024 * 1024) {
        alert('파일 크기는 10MB를 초과할 수 없습니다.');
        clearFile();
        return;
    }

    // 파일명 미리보기 표시
    document.getElementById('filePreviewName').textContent = file.name;
    document.getElementById('filePreview').style.display = 'flex';
});

// ── 파일 선택 취소 ──
function clearFile() {
    document.getElementById('fileInput').value = '';
    document.getElementById('filePreview').style.display = 'none';
    document.getElementById('filePreviewName').textContent = '';
}



// ── 6. 과거 메시지 리스트 그리기 함수 (기존 코드 유지) ──
function renderMessages(messageList, container) {
    container.innerHTML = ''; 

    if (messageList.length === 0) {
        container.innerHTML = `<div style="text-align:center; padding: 48px; color: #7b7394; font-size:13px; font-weight:700;">아직 대화 내역이 없습니다. 첫 메시지를 보내보세요!</div>`;
        return;
    }

    messageList.forEach(msg => {
        appendSingleMessage(msg); // 단일 말풍선 그리기 함수 재활용!
    });
}

// ── 7. 모달창 관리 및 초기화 이벤트 ──
function openCreateChannelModal() {
    const modal = document.getElementById('createChannelModal');
    if (modal) {
        modal.classList.add('active');
        document.getElementById('newChannelName').focus();
    }
}

function closeCreateChannelModal() {
    const modal = document.getElementById('createChannelModal');
    if (modal) {
        modal.classList.remove('active');
        document.getElementById('newChannelName').value = '';
        document.getElementById('newChannelDesc').value = '';
    }
}

document.addEventListener("DOMContentLoaded", () => {
    connectWebSocket();

    const teamNumEl = document.getElementById('teamNum');
    if (teamNumEl) {
        loadTeamMembers(parseInt(teamNumEl.value));
    }

    const chatInput = document.getElementById('chatInput');
    if (chatInput) {
        // keyup - 멘션 팝업 처리
        chatInput.addEventListener('keyup', function(e) {
            const value = this.value;
            const cursorPos = this.selectionStart;
            const textBeforeCursor = value.substring(0, cursorPos);
            const atIndex = textBeforeCursor.lastIndexOf('@');

            if (atIndex !== -1) {
                const keyword = textBeforeCursor.substring(atIndex + 1);
                if (!keyword.includes(' ')) {
                    isMentioning = true;
                    showMentionPopup(keyword);
                    return;
                }
            }
            isMentioning = false;
            hideMentionPopup();
        });

        // keydown - 엔터 전송 + 멘션 팝업 중 ESC 닫기
        chatInput.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                hideMentionPopup();
                isMentioning = false;
                return;
            }
            if (e.key === 'Enter' && !e.shiftKey) {
                if (isMentioning) {
                    // 팝업 열려있으면 엔터로 전송 막기
                    e.preventDefault();
                    return;
                }
                e.preventDefault();
                sendMessage();
            }
        });
    }

    // 외부 클릭 시 멘션 팝업 닫기
    document.addEventListener('click', function(e) {
        if (!e.target.closest('#mentionPopup') && !e.target.closest('#chatInput')) {
            hideMentionPopup();
            isMentioning = false;
        }
    });

    const modalOverlay = document.getElementById('createChannelModal');
    if (modalOverlay) {
        modalOverlay.addEventListener('click', function(e) {
            if (e.target === modalOverlay) {
                closeCreateChannelModal();
            }
        });
    }

    const createForm = document.getElementById('createChannelForm');
    if (createForm) {
        createForm.addEventListener('submit', function(e) {
            e.preventDefault();
            const formData = new FormData(this);

            fetch(this.action, {
                method: 'POST',
                body: formData
            })
            .then(response => {
                if (!response.ok) throw new Error("방 생성 네트워크 에러");
                return response.json();
            })
            .then(newChannel => {
                closeCreateChannelModal();

                const listScroll = document.querySelector('.channel-list-scroll');
                const emptyMsg = listScroll.querySelector('div[style*="padding: 20px"]');
                if(emptyMsg) emptyMsg.remove();

                const newItem = document.createElement('div');
                newItem.className = 'channel-item';
                newItem.setAttribute('onclick', `switchChannel(${newChannel.channel_num}, '${newChannel.channel_name}', this)`);
                newItem.innerHTML = `
                  <div class="channel-info">
                    <span class="channel-icon">#</span>
                    <span class="channel-name">${newChannel.channel_name}</span>
                  </div>
                `;

                listScroll.appendChild(newItem);
                newItem.click();
            })
            .catch(error => {
                console.error("방 생성 에러:", error);
                alert("채팅방 생성에 실패했습니다.");
            });
        });
    }
});

function deleteChannel(channelNum, event) {
    event.stopPropagation(); // 채널 클릭 이벤트 방지

    if (!confirm('채널을 삭제하시겠습니까? 채널 삭제 시 보관함도 같이 삭제 됩니다. 삭제된 채널은 복구할 수 없습니다.')) return;

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    fetch(`/chat/channel/${channelNum}`, {
        method: 'DELETE',
        headers: {
            [csrfHeader]: csrfToken
        }
    })
    .then(res => {
        if (!res.ok) return res.text().then(msg => { throw new Error(msg); });
        
        // 사이드바에서 채널 제거
        const channelItem = document.querySelector(`.channel-item[data-channel-num="${channelNum}"]`);
        if (channelItem) channelItem.remove();

        // 삭제된 채널이 현재 보고 있던 채널이면 초기화
        if (currentChannelId === channelNum) {
            currentChannelId = null;
            document.getElementById('chatRoomName').textContent = '채팅방을 선택해주세요';
            document.getElementById('chatMessageContainer').innerHTML = 
                `<div style="text-align:center; padding:48px; color:#7b7394; font-size:13px;">
                    왼쪽에서 채팅방을 선택하면 대화 내역이 표시됩니다.
                </div>`;
        }
    })
    .catch(err => alert(err.message));
}

// 답글 버튼 클릭
function setReply(messageNum, content, userName) {
    replyTargetMessageNum = messageNum;
    replyTargetContent = content;
    replyTargetUserName = userName;

    // 답글 미리보기 표시
    document.getElementById('replyPreview').style.display = 'flex';
    document.getElementById('replyUserName').textContent = userName + '에게 답글';
    document.getElementById('replyContent').textContent = content;
    document.getElementById('chatInput').focus();
}

// 답글 취소
function clearReply() {
    replyTargetMessageNum = null;
    replyTargetContent = null;
    replyTargetUserName = null;
    document.getElementById('replyPreview').style.display = 'none';
}

// sendMessage() 수정 - formData에 parent_message 추가
function sendMessage() {
    if (!currentChannelId) {
        alert("왼쪽에서 채팅방을 먼저 선택해주세요.");
        return;
    }

    const content = document.getElementById('chatInput').value.trim();
    const file = document.getElementById('fileInput').files[0];

    if (!content && !file) return;

    if (file && file.size > 10 * 1024 * 1024) {
        alert('파일 크기는 10MB를 초과할 수 없습니다.');
        return;
    }

    const formData = new FormData();
    formData.append('channel_num', currentChannelId);
    formData.append('content', content);
    if (file) formData.append('file', file);

    // 답글이면 parent_message 추가
    if (replyTargetMessageNum) {
        formData.append('parent_message', replyTargetMessageNum);
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    fetch('/chat/send', {
        method: 'POST',
        headers: { [csrfHeader]: csrfToken },
        body: formData
    })
    .then(res => {
        if (!res.ok) throw new Error('전송 실패');
        document.getElementById('chatInput').value = '';
        clearFile();
        clearReply(); // 답글 초기화
    })
    .catch(err => alert(err.message));
}

// appendSingleMessage() 수정 - 원본 메시지 인용 표시 추가
function appendSingleMessage(msg) {
    const container = document.getElementById('chatMessageContainer');

    const emptyMsg = container.querySelector('div[style*="padding: 48px"]');
    if (emptyMsg && emptyMsg.textContent.includes('대화 내역이 없습니다')) {
        emptyMsg.remove();
    }

    const senderId = parseInt(msg.userId || msg.user_num);
    const isMine = (senderId === myUserId);
    const mineClass = isMine ? 'mine' : '';

    const senderName = msg.userName || '알 수 없음';
    const initial = senderName.charAt(0);

    let timeStr = '';
    const rawDate = msg.sendDate || msg.send_date || new Date();
    const d = new Date(rawDate);
    if (!isNaN(d.getTime())) {
        timeStr = d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
    }

    // 답글 인용 HTML
	let replyHtml = '';
	if (msg.parentContent || msg.parent_message > 0) {
	    replyHtml = `
	        <div class="reply-quote" onclick="scrollToMessage(${msg.parent_message})" 
	             style="cursor:pointer;">
	            <span class="reply-quote-user">${msg.parentUserName || '알 수 없음'}</span>
	            <span class="reply-quote-content">${msg.parentContent || '(삭제된 메시지)'}</span>
	        </div>
	    `;
	}

    // 파일 HTML (기존 코드 유지)
    let fileHtml = '';
    if (msg.origin_name) {
        const fileSize = msg.file_size > 0
            ? (msg.file_size >= 1024 * 1024
                ? (msg.file_size / (1024 * 1024)).toFixed(1) + 'MB'
                : (msg.file_size / 1024).toFixed(1) + 'KB')
            : '';
        const isImage = msg.file_type === 'IMAGE';
        const icon = isImage ? 'fa-image' : 'fa-file';
        const downloadUrl = msg.file_num ? `/chat/download/${msg.file_num}` : '#';

        fileHtml = `
            <a href="${downloadUrl}" class="chat-file-attachment"
               style="margin-top:8px; text-decoration:none; display:flex;
                      align-items:center; gap:10px; cursor:pointer;">
                <span class="file-attach-icon" style="font-size:20px;">
                    <i class="fa-solid ${icon}"></i>
                </span>
                <div class="file-attach-info">
                    <span class="file-attach-name" style="font-weight:bold; font-size:13px;">
                        ${msg.origin_name}
                    </span>
                    <span class="file-attach-size" style="font-size:11px;">${fileSize}</span>
                </div>
                <span style="font-size:11px; margin-left:auto;">
                    <i class="fa-solid fa-download"></i>
                </span>
            </a>
        `;
    }

	const contentHtml = msg.content ? highlightMentions(msg.content) : '';

    // 답글 버튼
    const replyBtn = `
        <button class="btn-reply" 
                onclick="setReply(${msg.message_num}, '${(msg.content || '').replace(/'/g, "\\'")}', '${senderName}')">
            <i class="fa-solid fa-reply"></i>
        </button>
    `;

	const html = `
	    <div class="msg-group ${mineClass}" data-message-num="${msg.message_num}">
	        <div class="user-avatar">${initial}</div>
	        <div class="msg-content-wrapper">
	            <div class="msg-sender-info">
	                ${senderName} <span class="msg-time">${timeStr}</span>
	            </div>
	            <div class="msg-bubble">
	                ${replyHtml}   <!-- msg-bubble 안으로 이동 -->
	                ${contentHtml}
	                ${fileHtml}
	            </div>
	            <div class="msg-actions">
	                ${replyBtn}
	            </div>
	        </div>
	    </div>
	`;

    container.insertAdjacentHTML('beforeend', html);
    container.scrollTop = container.scrollHeight;
}

function highlightMentions(content) {
    if (!content) return '';
    return content.replace(/@(\S+)/g, function(match, name) {
        // mentionMembers 목록에 실제로 있는 이름인지 확인
        const isRealMember = mentionMembers.some(m => 
            m.user_name === name
        );
        if (isRealMember) {
            // 실제 팀원이면 강조
            return `<span class="mention-tag">@${name}</span>`;
        }
        // 아니면 그냥 원래 텍스트
        return match;
    });
}

function scrollToMessage(messageNum) {
    const target = document.querySelector(`.msg-group[data-message-num="${messageNum}"]`);
    
    if (!target) return;

    // 스크롤 이동
    target.scrollIntoView({ behavior: 'smooth', block: 'center' });

    // 하이라이트 효과
    target.classList.add('msg-highlight');
    setTimeout(() => {
        target.classList.remove('msg-highlight');
    }, 1500);
}


let mentionMembers = [];      // 팀원 목록 캐시
let isMentioning = false;     // @ 입력 중 여부
let mentionKeyword = '';      // @ 이후 입력된 키워드

// 페이지 로드 시 팀원 목록 미리 조회
function loadTeamMembers(teamNum) {
    fetch(`/chat/members/${teamNum}`)
        .then(res => res.json())
        .then(members => {
            mentionMembers = members;
        });
}

// 멘션 팝업 표시
function showMentionPopup(keyword) {
    const filtered = mentionMembers.filter(m =>
        m.user_name.toLowerCase().includes(keyword.toLowerCase())
    );

    if (filtered.length === 0) {
        hideMentionPopup();
        return;
    }

    const list = document.getElementById('mentionList');
    list.innerHTML = filtered.map(m => `
        <li onclick="selectMention('${m.user_name}')" 
            class="mention-item">
            <span class="mention-avatar">${m.user_name.charAt(0)}</span>
            <span class="mention-name">${m.user_name}</span>
        </li>
    `).join('');

    document.getElementById('mentionPopup').style.display = 'block';
}

// 멘션 팝업 숨기기
function hideMentionPopup() {
    document.getElementById('mentionPopup').style.display = 'none';
}

// 멘션 선택
function selectMention(userName) {
    const input = document.getElementById('chatInput');
    const value = input.value;
    const cursorPos = input.selectionStart;
    const textBeforeCursor = value.substring(0, cursorPos);
    const atIndex = textBeforeCursor.lastIndexOf('@');

    // @keyword 부분을 @이름 으로 교체
    const newValue = value.substring(0, atIndex) + '@' + userName + ' ' + value.substring(cursorPos);
    input.value = newValue;

    // 커서 위치 이동
    const newCursorPos = atIndex + userName.length + 2;
    input.setSelectionRange(newCursorPos, newCursorPos);
    input.focus();

    hideMentionPopup();
    isMentioning = false;
}

const backgroundSubscriptions = {}; // 백그라운드 구독 보관소

function subscribeToChannelBackground(channelId) {
    if (backgroundSubscriptions[channelId]) return; // 중복 구독 방지

    if (stompClient && stompClient.connected) {
        const sub = stompClient.subscribe('/sub/chat/room/' + channelId, function(response) {
            const messageObj = JSON.parse(response.body);

            if (channelId === currentChannelId) {
                // 현재 보고 있는 방 → 말풍선 추가 + 읽음 처리
                appendSingleMessage(messageObj);
                markAsRead(channelId, document.querySelector(`.channel-item[data-channel-num="${channelId}"]`));
            } else {
                // 다른 방 → 뱃지 증가 + 사이드바 갱신
                updateSidebarBadge(channelId, messageObj);
            }

            // 사이드바 마지막 메시지/시간 갱신
            updateSidebarLastMessage(channelId, messageObj);
        });

        backgroundSubscriptions[channelId] = sub;
        console.log(`📡 [${channelId}]번 방 백그라운드 구독 완료`);
    }
}

function updateSidebarBadge(channelId, msg) {
    const channelItem = document.querySelector(`.channel-item[data-channel-num="${channelId}"]`);
    if (!channelItem) return;

    let badge = channelItem.querySelector('.unread-badge');
    if (!badge) {
        // 뱃지 없으면 새로 생성
        badge = document.createElement('span');
        badge.className = 'unread-badge';
        const bottomRow = channelItem.querySelector('.channel-row-bottom');
        if (bottomRow) bottomRow.appendChild(badge);
    }

    const current = parseInt(badge.textContent) || 0;
    badge.textContent = current + 1;
    badge.style.display = 'flex';
}

// ── 사이드바 마지막 메시지/시간 갱신 ──
function updateSidebarLastMessage(channelId, msg) {
    const channelItem = document.querySelector(`.channel-item[data-channel-num="${channelId}"]`);
    if (!channelItem) return;

    const lastMsgEl = channelItem.querySelector('.last-message');
    const timeEl = channelItem.querySelector('.channel-time');

    let content = msg.content ? msg.content : '';
    if (msg.origin_name) content = '📁 ' + msg.origin_name;

    if (lastMsgEl) lastMsgEl.textContent = content;
    if (timeEl) {
        const d = new Date(msg.send_date || new Date());
        if (!isNaN(d.getTime())) {
            timeEl.textContent = d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
        }
    }
}