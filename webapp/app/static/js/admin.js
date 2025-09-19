async function fetchAdminData() {
    console.log('[Init] Fetching admin data from /admin/api...');
    try {
        const res = await axios.get('/admin/api');
        const data = res.data;
        console.log('[Init] Admin data fetched:', data);

        document.getElementById('configYmlContent').value = data.config_yml;
        console.log('[Render] Rendering user table...');
        renderUsers(data.users);
    } catch (err) {
        console.error('[Error] Failed to fetch admin data:', err);
        alert('获取管理员数据失败: ' + err.message);
    }
}

function saveYml() {
    console.log('[Action] Saving config.yml...');
    axios.post('/admin/api/save_config_yml', {
        content: document.getElementById('configYmlContent').value
    }).then(response => {
        if (response.data === 'OK') {
            console.log('[Action] Config saved successfully');
            alert("操作成功!");
        } else {
            console.error('[Error] Config save failed:', response.data.reason);
            alert("操作失败: " + response.data.reason);
        }
    }).catch(err => {
        console.error('[Error] Config save exception:', err);
        alert('操作失败: ' + err.message);
    });
}

function renderUsers(users) {
    console.log('[Render] Users:', users.map(u => u.usergroup));
    const tbody = document.querySelector('#userTable tbody');
    tbody.innerHTML = '';

    users.forEach(user => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td><input value="${user.username}" onchange="userChanged(this, ${user.userid}, 'username')"></td>
            <td><input value="${user.passwd}" onchange="userChanged(this, ${user.userid}, 'passwd')"></td>
            <td><input value="${user.email}" onchange="userChanged(this, ${user.userid}, 'email')"></td>
            <td>
                <select onchange="userChanged(this, ${user.userid}, 'usergroup')">
                    <option value="user">user</option>
                    <option value="teacher">teacher</option>
                    <option value="admin">admin</option>
                </select>
            </td>
            <td><button onclick="deleteUser(${user.userid})">删除</button></td>
        `;
        const select = row.querySelector('select');
        select.value = ['user','teacher','admin'].includes(user.usergroup) ? user.usergroup : 'user';

        tbody.appendChild(row);
    });
    console.log('[Render] User table rendered successfully');
}

function userChanged(input, id) {
    const row = input.parentElement.parentElement;
    const data = {
        userid: id,
        username: row.children[0].children[0].value,
        passwd: row.children[1].children[0].value,
        email: row.children[2].children[0].value,
        usergroup: row.children[3].children[0].value
    };
    console.log(`[Action] Updating user ID ${id}`, data);

    axios.post('/admin/api/update_user', data)
        .then(response => {
            if (response.data === 'OK') {
                console.log(`[Action] User ID ${id} updated successfully`);
                alert("操作成功!");
            } else {
                console.error(`[Error] Failed to update user ID ${id}:`, response.data.reason);
                alert("操作失败: " + response.data.reason);
            }
        }).catch(err => {
            console.error(`[Error] Update user ID ${id} exception:`, err);
            alert("操作失败: " + err.message);
        });
}

function deleteUser(id) {
    console.log(`[Action] Deleting user ID ${id}...`);
    axios.post('/admin/api/delete_user', { userid: id })
        .then(response => {
            if (response.data === 'OK') {
                console.log(`[Action] User ID ${id} deleted successfully`);
                alert("操作成功!");
            } else {
                console.error(`[Error] Failed to delete user ID ${id}:`, response.data.reason);
                alert("操作失败: " + response.data.reason);
            }
            fetchAdminData();
        }).catch(err => {
            console.error(`[Error] Delete user ID ${id} exception:`, err);
            alert("操作失败: " + err.message);
        });
}

document.addEventListener('DOMContentLoaded', () => {
    console.log('[Init] DOM loaded. Fetching admin data...');
    fetchAdminData();
});
