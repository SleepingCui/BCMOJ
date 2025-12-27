function flattenConfig(obj, prefix = '', result = {}) {
    for (let key in obj) {
        if (obj.hasOwnProperty(key)) {
            const value = obj[key];
            const newKey = prefix ? `${prefix}.${key}` : key;

            if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
                flattenConfig(value, newKey, result);
            } else {
                result[newKey] = value;
            }
        }
    }
    return result;
}

function unflattenConfig(flatObj) {
    const result = {};
    for (let key in flatObj) {
        if (flatObj.hasOwnProperty(key)) {
            let parts = key.split('.');
            let current = result;
            for (let i = 0; i < parts.length - 1; i++) {
                if (!current[parts[i]]) {
                    current[parts[i]] = {};
                }
                current = current[parts[i]];
            }
            current[parts[parts.length - 1]] = flatObj[key];
        }
    }
    return result;
}

async function fetchAdminData() {
    console.log('[Init] Fetching admin data from /admin/api...');
    try {
        const res = await axios.get('/admin/api');
        const data = res.data;
        console.log('[Init] Admin data fetched:', data);

        const flattenedConfig = flattenConfig(data.general_config);
        console.log('[Init] Flattened config for form:', flattenedConfig);
        Object.keys(flattenedConfig).forEach(key => {
            const element = document.querySelector(`[name="${key}"]`);
            if (element) {
                const value = flattenedConfig[key];
                if (element.type === 'checkbox') {
                    element.checked = !!value;
                } else {
                    element.value = value;
                }
            }
        });

        console.log('[Render] Rendering user table...');
        renderUsers(data.users);
    } catch (err) {
        console.error('[Error] Failed to fetch admin data:', err);
        alert('获取管理员数据失败: ' + err.message);
    }
}

function saveGeneralConfig(event) {
    event.preventDefault();
    console.log('[Action] Saving general config from form...');
    const form = document.getElementById('configForm');
    const formData = new FormData(form);
    const formObject = {};

    for (const [key, value] of formData.entries()) {
        const element = document.querySelector(`[name="${key}"]`);
        if (element && element.type === 'checkbox') {
            formObject[key] = element.checked;
        } else {
            if(element && element.type === 'number' && !isNaN(Number(value))) {
                 formObject[key] = Number(value);
            } else {
                 formObject[key] = value;
            }
        }
    }

    console.log('[Action] Form data collected (before unflattening):', formObject);
    const nestedConfig = unflattenConfig(formObject);
    console.log('[Action] Nested config to save:', nestedConfig);
    axios.post('/admin/api/save_general_config', {
        general_config: nestedConfig
    }).then(response => {
        if (response.data === 'OK') {
            console.log('[Action] General config saved successfully');
            alert("通用配置保存成功! 请重启应用使更改生效。");
        } else {
            console.error('[Error] Config save failed:', response.data);
            alert("通用配置保存失败: " + (response.data.reason || "未知错误"));
        }
    }).catch(err => {
        console.error('[Error] Config save exception:', err);
        alert('通用配置保存失败: ' + err.message);
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

async function testJudgeConnection() {
    const hostInput = document.getElementById('judgeHost');
    const portInput = document.getElementById('judgePort');
    const statusElement = document.getElementById('connectionStatus');

    const host = hostInput.value.trim();
    const port = portInput.value.trim();

    if (!host) {
        statusElement.textContent = '请先填写判题服务器地址';
        statusElement.style.color = 'red';
        return;
    }
    if (!port) {
        statusElement.textContent = '请先填写判题服务器端口';
        statusElement.style.color = 'red';
        return;
    }

    statusElement.textContent = '测试中...';
    statusElement.style.color = 'orange';

    try {
        const response = await axios.post('/admin/api/test_connection', {
            host: host,
            port: parseInt(port, 10)
        });

        if (response.data && response.data.success) {
            statusElement.textContent = `连接成功 - 延迟: ${response.data.ping_time}ms`;
            statusElement.style.color = 'green';
        } else {
            statusElement.textContent = `连接失败: ${response.data.message || '未知错误'}`;
            statusElement.style.color = 'red';
        }
    } catch (error) {
        console.error('[Test Connection Error]', error);
        statusElement.textContent = `测试失败: ${error.message}`;
        statusElement.style.color = 'red';
    }
}


document.addEventListener('DOMContentLoaded', () => {
    console.log('[Init] DOM loaded. Fetching admin data...');
    fetchAdminData();
    const configForm = document.getElementById('configForm');
    if (configForm) {
        configForm.addEventListener('submit', saveGeneralConfig);
    }
});
