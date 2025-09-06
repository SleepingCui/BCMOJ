const { username, userId, usergroup, totalPages, query, version } = window.appData;

function showToast(message, type = 'info') {
    console.log(`[Toast][${type.toUpperCase()}] ${message}`);
    const toastContainer = document.querySelector('.toast-container');
    const toastId = 'toast-' + Date.now();
    const toastEl = document.createElement('div');
    toastEl.className = `toast align-items-center text-white bg-${type} border-0`;
    toastEl.id = toastId;
    toastEl.setAttribute('role', 'alert');
    toastEl.setAttribute('aria-live', 'assertive');
    toastEl.setAttribute('aria-atomic', 'true');
    toastEl.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">
                ${message}
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
    `;
    toastContainer.appendChild(toastEl);
    const toast = new bootstrap.Toast(toastEl, { delay: 3000 });
    toast.show();
    toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
}

function initUserInfo(username, userId, usergroup) {
    console.log('[Init] Initializing user info');
    const userInfo = document.getElementById('userInfo');
    const loginBtn = document.getElementById('loginBtn');
    const logoutBtn = document.getElementById('logoutBtn');
    const editBtn = document.getElementById('editAccountBtn');

    if (username && userId && userId !== 'None') {
        let badgeColor = 'secondary';
        if (usergroup === 'admin') badgeColor = 'danger';
        else if (usergroup === 'teacher') badgeColor = 'info';
        else if (usergroup === 'student') badgeColor = 'success';

        userInfo.innerHTML = `
            <i class="bi bi-person-circle"></i>
            <span>${username} [${userId}]</span>
            <span class="badge bg-${badgeColor} ms-1">${usergroup}</span>
        `;
        loginBtn.classList.add('d-none');
        logoutBtn.classList.remove('d-none');
        editBtn.classList.remove('d-none');
        console.log(`[Init] User logged in: ${username} [${userId}], group: ${usergroup}`);
    } else {
        userInfo.innerHTML = `<i class="bi bi-person-circle"></i><span>未登录</span>`;
        loginBtn.classList.remove('d-none');
        logoutBtn.classList.add('d-none');
        editBtn.classList.add('d-none');
        console.log('[Init] User not logged in');
    }
}

function initSearchInput() {
    console.log('[Init] Initializing search input focus effect');
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('focus', function() {
            this.parentElement.classList.add('shadow');
            console.log('[Action] Search input focused');
        });
        searchInput.addEventListener('blur', function() {
            this.parentElement.classList.remove('shadow');
            console.log('[Action] Search input blurred');
        });
    }
}

function jumpToPage() {
    const pageInput = document.getElementById('jumpPage');
    const pageNum = parseInt(pageInput.value);

    if (isNaN(pageNum) || pageNum < 1 || pageNum > parseInt(totalPages)) {
        showToast('请输入有效的页码', 'danger');
        console.error('[Error] Invalid page number:', pageInput.value);
        return;
    }
    console.log(`[Action] Jumping to page ${pageNum}`);
    window.location.href = `{{ url_for('problems') }}?q=${encodeURIComponent(query)}&page=${pageNum}`;
}


document.addEventListener('DOMContentLoaded', () => {
    console.log('[Init] DOM loaded. Initializing UI...');
    initUserInfo(username, userId, usergroup);
    initSearchInput();

    const loginBtn = document.getElementById('loginBtn');
    const logoutBtn = document.getElementById('logoutBtn');
    const editBtn = document.getElementById('editAccountBtn');

    if (loginBtn) loginBtn.addEventListener('click', () => {
        console.log('[Action] Login button clicked');
        showToast('正在跳转...', 'info');
        window.location.href = '/login';
    });

    if (logoutBtn) logoutBtn.addEventListener('click', () => {
        console.log('[Action] Logout button clicked');
        showToast('您已成功登出', 'success');
        window.location.href = '/logout';
    });

    if (editBtn) editBtn.addEventListener('click', () => {
        console.log('[Action] Edit account button clicked');
        window.location.href = '/edit_account';
    });

    if (usergroup === 'admin') {
        const checkUpdateBtn = document.getElementById('checkUpdateBtn');
        if (checkUpdateBtn) {
            checkUpdateBtn.addEventListener('click', () => {
                console.log('[Action] Check update button clicked');
                showToast('正在检查更新...', 'info');
                fetch('/check_update')
                    .then(res => { 
                        if (!res.ok) throw new Error('Network response was not ok'); 
                        return res.json(); 
                    })
                    .then(data => {
                        if (data.error) {
                            showToast(data.error, 'danger');
                            console.error('[Error] Update check failed:', data.error);
                        } else {
                            showToast(data.message, 'success');
                            console.log('[Action] Update check success:', data.message);
                            if (data.latest && data.latest !== version) {
                                console.log('[Action] New version available:', data.latest);
                                window.open('https://github.com/SleepingCui/BCMOJ/releases/latest', '_blank');
                            }
                        }
                    })
                    .catch(err => {
                        showToast('检查更新失败: ' + err.message, 'danger');
                        console.error('[Error] Update check exception:', err);
                    });
            });
        }
    }
});
