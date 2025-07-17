async function fetchAdminData() {
    const res = await axios.get('/admin/api')
    const data = res.data
    document.getElementById('configYmlContent').value = data.config_yml
    renderUsers(data.users)
}

function saveYml() {
    axios.post('/admin/api/save_config_yml', {
        content: document.getElementById('configYmlContent').value
    }).then(response => alert(response.data === 'OK' ? "操作成功!" : "操作失败: " + response.data.reason))
}

function renderUsers(users) {
    console.log(users.map(u => u.usergroup))
    const tbody = document.querySelector('#userTable tbody')
    tbody.innerHTML = ''
    users.forEach(user => {
        const row = document.createElement('tr')
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
        `
        const select = row.querySelector('select')
        if ([...select.options].some(opt => opt.value === user.usergroup)) {
            select.value = user.usergroup
        } else {
            select.value = 'user'
        }
        tbody.appendChild(row)
    })
}


function userChanged(input, id) {
    const row = input.parentElement.parentElement
    const data = {
        userid: id,
        username: row.children[0].children[0].value,
        passwd: row.children[1].children[0].value,
        email: row.children[2].children[0].value,
        usergroup: row.children[3].children[0].value
    }
    axios.post('/admin/api/update_user', data)
        .then(response => alert(response.data === 'OK' ? "操作成功!" : "操作失败: " + response.data.reason))
}

function deleteUser(id) {
    axios.post('/admin/api/delete_user', { userid: id })
        .then(response => {
            alert(response.data === 'OK' ? "操作成功!" : "操作失败: " + response.data.reason)
            fetchAdminData()
        })
}

fetchAdminData()