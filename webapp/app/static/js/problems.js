const { username, userId, usergroup, totalPages, query, version } = window.appData;

function showToast(message, type = 'info') {
    console.log(`[Toast][${type.toUpperCase()}] ${message}`);
    const $toastContainer = $('.toast-container');
    const toastId = 'toast-' + Date.now();
    const $toastEl = $(`
        <div id="${toastId}" class="toast align-items-center text-white bg-${type} border-0" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">${message}</div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
    `);
    $toastContainer.append($toastEl);
    const toast = new bootstrap.Toast($toastEl[0], { delay: 3000 });
    toast.show();
    $toastEl.on('hidden.bs.toast', function() {
        $toastEl.remove();
    });
}

function initUserInfo(username, userId, usergroup) {
    console.log('[Init] Initializing user info');
    const $userInfo = $('#userInfo');
    const $loginBtn = $('#loginBtn');
    const $logoutBtn = $('#logoutBtn');
    const $editBtn = $('#editAccountBtn');

    if (username && userId && userId !== 'None') {
        let badgeColor = 'secondary';
        if (usergroup === 'admin') badgeColor = 'danger';
        else if (usergroup === 'teacher') badgeColor = 'info';
        else if (usergroup === 'student') badgeColor = 'success';

        $userInfo.html(`
            <i class="bi bi-person-circle"></i>
            <span>${username} [${userId}]</span>
            <span class="badge bg-${badgeColor} ms-1">${usergroup}</span>
        `);
        $loginBtn.addClass('d-none');
        $logoutBtn.removeClass('d-none');
        $editBtn.removeClass('d-none');
        console.log(`[Init] User logged in: ${username} [${userId}], group: ${usergroup}`);
    } else {
        $userInfo.html(`<i class="bi bi-person-circle"></i><span>未登录</span>`);
        $loginBtn.removeClass('d-none');
        $logoutBtn.addClass('d-none');
        $editBtn.addClass('d-none');
        console.log('[Init] User not logged in');
    }
}

function initSearchInput() {
    console.log('[Init] Initializing search input focus effect');
    const $searchInput = $('#searchInput');
    if ($searchInput.length) {
        $searchInput.on('focus', function() {
            $(this).parent().addClass('shadow');
        });
        $searchInput.on('blur', function() {
            $(this).parent().removeClass('shadow');
        });
    }
}

function jumpToPage() {
    const $pageInput = $('#jumpPage');
    const pageNum = parseInt($pageInput.val());

    if (isNaN(pageNum) || pageNum < 1 || pageNum > parseInt(totalPages)) {
        showToast('请输入有效的页码', 'danger');
        return;
    }
    window.location.href = `{{ url_for('problems') }}?q=${encodeURIComponent(query)}&page=${pageNum}`;
}

$(document).ready(function() {
    console.log('[Init] DOM loaded. Initializing UI...');
    initUserInfo(username, userId, usergroup);
    initSearchInput();

    $('#loginBtn').on('click', function() {
        console.log('[Action] Login button clicked');
        showToast('正在跳转...', 'info');
        window.location.href = '/login';
    });

    $('#logoutBtn').on('click', function() {
        console.log('[Action] Logout button clicked');
        showToast('您已成功登出', 'success');
        window.location.href = '/logout';
    });

    $('#editAccountBtn').on('click', function() {
        console.log('[Action] Edit account button clicked');
        window.location.href = '/edit_account';
    });

    if (usergroup === 'admin') {
        $('#checkUpdateBtn').on('click', function() {
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
                    } else {
                        showToast(data.message, 'success');
                        if (data.latest && data.latest !== version) {
                            window.open('https://github.com/SleepingCui/BCMOJ/releases/latest', '_blank');
                        }
                    }
                })
                .catch(err => {
                    showToast('检查更新失败: ' + err.message, 'danger');
                });
        });
    }
});