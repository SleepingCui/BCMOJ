function initUserInfo(username, userId, usergroup) {
    console.log(usergroup);
    const userInfoDiv = document.getElementById('userInfo');

    if (username === "" || userId === "" || username === "None" || userId === "None") {
        userInfoDiv.textContent = '未登录';
        userInfoDiv.onclick = function() {
            window.location.href = '/login?next=' + encodeURIComponent(window.location.href);
        };
    } else {
        userInfoDiv.textContent = `${username} [${userId}]`;
        userInfoDiv.onclick = function() {
            const confirmLogout = confirm('确定要登出吗？');
            if (confirmLogout) {
                window.location.href = '/logout';
            }
        };
    }
}

function initSearchInput() {
    document.getElementById('searchInput').addEventListener('input', function() {
        const query = this.value;
        const url = new URL(window.location.href);
        url.searchParams.set('q', query);
        window.location.href = url.toString();
    });
}

function jumpToPage() {
    const input = document.getElementById('jumpPage');
    const page = parseInt(input.value);
    const maxPage = parseInt(totalPages);

    if (!page || page < 1 || page > maxPage) {
        alert(`请输入有效页码（1 - ${maxPage}）`);
        return;
    }

    const url = new URL(window.location.href);
    url.searchParams.set('page', page);
    if (query) url.searchParams.set('q', query);
    window.location.href = url.toString();
}

function initCheckUpdate() {
    document.getElementById("checkUpdateBtn").onclick = function () {
        fetch("/check_update")
            .then(response => response.json())
            .then(data => {
                console.info(data)
                if (data.error) {
                    alert("错误：" + data.error);
                } else if (data.latest) {
                    alert("发现新版本：" + data.latest);
                    window.open("https://github.com/SleepingCui/BCMOJ/releases/latest", "_blank");
                } else {
                    alert("当前是最新版！");
                }
            })
            .catch(err => {
                alert("请求失败：" + err);
            });
    };
}