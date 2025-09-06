// 从全局变量读取模板数据
const { username, userId, usergroup, totalPages, query, version } = window.appData;

// Toast 提示函数
function showToast(message, type = 'info') {
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

// 初始化用户信息显示
function initUserInfo(username, userId, usergroup) {
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
    } else {
        userInfo.innerHTML = `<i class="bi bi-person-circle"></i><span>未登录</span>`;
        loginBtn.classList.remove('d-none');
        logoutBtn.classList.add('d-none');
        editBtn.classList.add('d-none');
    }
}

// 搜索框聚焦效果
function initSearchInput() {
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('focus', function() {
            this.parentElement.classList.add('shadow');
        });
        searchInput.addEventListener('blur', function() {
            this.parentElement.classList.remove('shadow');
        });
    }
}

// 跳转页码
function jumpToPage() {
    const pageInput = document.getElementById('jumpPage');
    const pageNum = parseInt(pageInput.value);

    if (isNaN(pageNum) || pageNum < 1 || pageNum > parseInt(totalPages)) {
        showToast('请输入有效的页码', 'danger');
        return;
    }
    window.location.href = `{{ url_for('problems') }}?q=${encodeURIComponent(query)}&page=${pageNum}`;
}

// 页面初始化
document.addEventListener('DOMContentLoaded', () => {
    initUserInfo(username, userId, usergroup);
    initSearchInput();

    document.getElementById('loginBtn').addEventListener('click', () => {
        showToast('正在跳转...', 'info');
        window.location.href = '/login';
    });

    document.getElementById('logoutBtn').addEventListener('click', () => {
        showToast('您已成功登出', 'success');
        window.location.href = '/logout';
    });

    document.getElementById('editAccountBtn').addEventListener('click', () => {
        window.location.href = '/edit_account';
    });

    if (usergroup === 'admin') {
        const checkUpdateBtn = document.getElementById('checkUpdateBtn');
        if (checkUpdateBtn) {
            checkUpdateBtn.addEventListener('click', () => {
                showToast('正在检查更新...', 'info');
                fetch('/check_update')
                    .then(res => { if (!res.ok) throw new Error('网络响应不正常'); return res.json(); })
                    .then(data => {
                        if (data.error) showToast(data.error, 'danger');
                        else {
                            showToast(data.message, 'success');
                            if (data.latest && data.latest !== version) {
                                window.open('https://github.com/SleepingCui/BCMOJ/releases/latest', '_blank');
                            }
                        }
                    })
                    .catch(err => {
                        showToast('检查更新失败: ' + err.message, 'danger');
                        console.error('检查更新错误:', err);
                    });
            });
        }
    }
});
