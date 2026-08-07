const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
const teamNum = parseInt(document.getElementById('teamNum').value);

let currentFolderNum = null;  // null = 전체보기
let currentFileType = 'ALL';  // ALL, IMAGE, FILE
let allFiles = [];            // 현재 로드된 파일 전체

// ── 초기 로드 ──
document.addEventListener('DOMContentLoaded', () => {
    loadAllFiles(document.querySelector('.folder-item.active'));

    // 파일 업로드
    document.getElementById('uploadFileInput').addEventListener('change', function(e) {
        const files = Array.from(e.target.files);
        if (files.length === 0) return;
        uploadFiles(files);
        e.target.value = '';
    });
});

// ── 전체 파일 로드 ──
function loadAllFiles(clickedEl) {
    currentFolderNum = null;
    setActiveFolder(clickedEl);
    document.getElementById('currentFolderName').textContent = '전체 파일';

    fetch(`/storage/files/all`)
        .then(res => res.json())
        .then(files => {
            allFiles = files;
            renderFiles(files);
        });
}

// ── 폴더 클릭 시 파일 로드 ──
function loadFolderFiles(folderNum, clickedEl) {
    currentFolderNum = folderNum;
    setActiveFolder(clickedEl);

    const folderName = clickedEl.querySelector('span').textContent;
    document.getElementById('currentFolderName').textContent = folderName;

    fetch(`/storage/files/${folderNum}`)
        .then(res => res.json())
        .then(files => {
            allFiles = files;
            renderFiles(files);
        });
}

// ── 파일 렌더링 ──
function renderFiles(files) {
    const container = document.getElementById('fileListContainer');

    // 탭 필터 적용
    const filtered = currentFileType === 'ALL'
        ? files
        : files.filter(f => f.file_type === currentFileType);

    document.getElementById('fileCount').textContent = filtered.length + '개';

    if (filtered.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <i class="fa-regular fa-folder-open"></i>
                <p>파일이 없습니다.</p>
            </div>
        `;
        return;
    }

    const grid = document.createElement('div');
    grid.className = 'file-grid';

    filtered.forEach(file => {
        const isImage = file.file_type === 'IMAGE';
        const fileSize = file.file_size >= 1024 * 1024
            ? (file.file_size / (1024 * 1024)).toFixed(1) + 'MB'
            : (file.file_size / 1024).toFixed(1) + 'KB';

        const uploadDate = file.upload_date
            ? new Date(file.upload_date).toLocaleDateString('ko-KR')
            : '';

        grid.innerHTML += `
            <div class="file-card">
                <div class="file-card-icon ${isImage ? 'image' : ''}">
                    <i class="fa-solid ${isImage ? 'fa-image' : 'fa-file'}"></i>
                </div>
                <div class="file-card-name" title="${file.origin_name}">
                    ${file.origin_name}
                </div>
                <div class="file-card-meta">
                    <span><i class="fa-solid fa-user" style="font-size:10px;"></i> ${file.uploaderName || '알 수 없음'}</span>
                    <span><i class="fa-solid fa-calendar" style="font-size:10px;"></i> ${uploadDate}</span>
                    <span><i class="fa-solid fa-weight-hanging" style="font-size:10px;"></i> ${fileSize}</span>
                </div>
                <div class="file-card-actions">
                    <button class="btn-file-action" onclick="downloadFile(${file.file_num})" title="다운로드">
                        <i class="fa-solid fa-download"></i>
                    </button>
                    <button class="btn-file-action delete" onclick="deleteFile(${file.file_num}, event)" title="삭제">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </div>
            </div>
        `;
    });

    container.innerHTML = '';
    container.appendChild(grid);
}

// ── 탭 필터 ──
function filterByType(type, clickedEl) {
    currentFileType = type;

    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    clickedEl.classList.add('active');

    renderFiles(allFiles);
}

// ── 폴더 활성화 ──
function setActiveFolder(el) {
    document.querySelectorAll('.folder-item').forEach(item => item.classList.remove('active'));
    if (el) el.classList.add('active');
}

// ── 파일 다운로드 ──
function downloadFile(fileNum) {
    window.location.href = `/chat/download/${fileNum}`;
}

// ── 파일 삭제 ──
function deleteFile(fileNum, event) {
    event.stopPropagation();
    if (!confirm('파일을 삭제하시겠습니까?')) return;

    fetch(`/storage/file/${fileNum}`, {
        method: 'DELETE',
        headers: { [csrfHeader]: csrfToken }
    })
    .then(res => {
        if (!res.ok) throw new Error('삭제 실패');
        // 현재 폴더 새로고침
        if (currentFolderNum) {
            loadFolderFiles(currentFolderNum, document.querySelector('.folder-item.active'));
        } else {
            loadAllFiles(document.querySelector('.folder-item.active'));
        }
    })
    .catch(err => alert(err.message));
}

// ── 폴더 생성 모달 ──
function openCreateFolderModal(parentFolderNum) {
    document.getElementById('parentFolderNum').value = parentFolderNum || '';
    document.getElementById('newFolderName').value = '';
    document.getElementById('createFolderModal').classList.add('active');
    document.getElementById('newFolderName').focus();
}

function closeCreateFolderModal() {
    document.getElementById('createFolderModal').classList.remove('active');
}

// ── 폴더 생성 ──
function createFolder() {
    const folderName = document.getElementById('newFolderName').value.trim();
    const parentFolderNum = document.getElementById('parentFolderNum').value || null;

    if (!folderName) {
        alert('폴더 이름을 입력해주세요.');
        return;
    }

    const formData = new FormData();
    formData.append('folder_name', folderName);
    formData.append('team_num', teamNum);
    if (parentFolderNum) formData.append('parent_folder_num', parentFolderNum);

    fetch('/storage/folder', {
        method: 'POST',
        headers: { [csrfHeader]: csrfToken },
        body: formData
    })
    .then(res => {
        if (!res.ok) throw new Error('폴더 생성 실패');
        return res.json();
    })
    .then(() => {
        closeCreateFolderModal();
        location.reload(); // 폴더 트리 새로고침
    })
    .catch(err => alert(err.message));
}

// ── 폴더 삭제 ──
function deleteFolder(folderNum) {
    if (!confirm('폴더를 삭제하시겠습니까?\n폴더 안의 파일도 모두 삭제됩니다.')) return;

    fetch(`/storage/folder/${folderNum}`, {
        method: 'DELETE',
        headers: { [csrfHeader]: csrfToken }
    })
    .then(res => {
        if (!res.ok) return res.text().then(msg => { throw new Error(msg); });
        location.reload();
    })
    .catch(err => alert(err.message));
}

// ── 파일 업로드 ──
function uploadFiles(files) {
    if (!currentFolderNum) {
        alert('파일을 올릴 폴더를 먼저 선택해주세요.');
        return;
    }

    const formData = new FormData();
    files.forEach(file => formData.append('files', file));
    formData.append('folder_num', currentFolderNum);
    formData.append('team_num', teamNum);

    fetch('/storage/upload', {
        method: 'POST',
        headers: { [csrfHeader]: csrfToken },
        body: formData
    })
    .then(res => {
        if (!res.ok) throw new Error('업로드 실패');
        loadFolderFiles(currentFolderNum, document.querySelector('.folder-item.active'));
    })
    .catch(err => alert(err.message));
}