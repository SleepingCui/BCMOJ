function showToast(message, type = 'info', title = '系统通知') {
    const toastContainer = document.querySelector('.toast-container');
    const toastId = 'toast-' + Date.now();
    let icon = 'bi-info-circle';
    if (type === 'success') icon = 'bi-check-circle';
    else if (type === 'error') icon = 'bi-x-circle';
    else if (type === 'warning') icon = 'bi-exclamation-circle';
    const toastEl = document.createElement('div');
    toastEl.className = `toast toast-${type}`;
    toastEl.id = toastId;
    toastEl.setAttribute('role', 'alert');
    toastEl.setAttribute('aria-live', 'assertive');
    toastEl.setAttribute('aria-atomic', 'true');
    toastEl.innerHTML = `
        <div class="toast-header">
            <i class="bi ${icon} me-2"></i>
            <strong class="me-auto">${title}</strong>
            <small>刚刚</small>
            <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
        <div class="toast-body">
            ${message}
        </div>
    `;
    toastContainer.appendChild(toastEl);
    const toast = new bootstrap.Toast(toastEl, { delay: 5000 });
    toast.show();
    toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
}
function loadContributors() {
    const container = document.getElementById('contributors');
    container.innerHTML = '<p>Loading...</p>';
    axios.get('/api/contributors')
        .then(response => {
            const contributors = response.data;
            if (!contributors || contributors.length === 0) {
                container.innerHTML = '<p>暂无贡献者</p>';
                return;
            }
            const ul = document.createElement('div');
            ul.className = 'd-flex flex-wrap justify-content-center gap-3';
            contributors.forEach(user => {
                const a = document.createElement('a');
                a.href = user.html_url;
                a.target = '_blank';
                a.className = 'd-flex flex-column align-items-center text-decoration-none text-dark';
                a.style.width = '100px';
                const img = document.createElement('img');
                img.src = user.avatar_url;
                img.alt = user.login;
                img.style.width = '80px';
                img.style.height = '80px';
                img.style.borderRadius = '50%';
                img.style.objectFit = 'cover';
                img.style.boxShadow = '0 4px 10px rgba(0,0,0,0.15)';
                img.className = 'mb-2';
                const name = document.createElement('span');
                name.textContent = user.login;
                name.style.fontSize = '0.9rem';
                name.style.textAlign = 'center';
                a.appendChild(img);
                a.appendChild(name);
                ul.appendChild(a);
            });
            container.innerHTML = '';
            container.appendChild(ul);
        })
        .catch(err => {
            container.innerHTML = '<p>获取贡献者失败</p>';
            console.error(err);
            showToast('获取贡献者信息失败，请稍后重试', 'error', '加载失败');
        });
}
document.addEventListener('DOMContentLoaded', () => {
    console.log("loading...")
    loadContributors();
});
