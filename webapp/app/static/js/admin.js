async function fetchAdminData() {
    const res = await axios.get('/admin/api')
    const data = res.data
    document.getElementById('configYmlContent').value = data.config_yml
    renderUsers(data.users)
    renderProblems(data.problems)
}

function saveYml() {
    axios.post('/admin/api/save_config_yml', {
        content: document.getElementById('configYmlContent').value
    }).then(response => alert(response.data === 'OK' ? "操作成功!" : "操作失败: " + response.data.reason))
}


function renderUsers(users) {
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
        row.querySelector('select').value = user.usergroup || 'user'
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

function renderProblems(problems) {
    const select = document.getElementById('problemSelect')
    select.innerHTML = '<option disabled selected>请选择题目</option>'
    problems.forEach(p => {
        const opt = document.createElement('option')
        opt.value = p.problem_id
        opt.textContent = `${p.problem_id} - ${p.title}`
        select.appendChild(opt)
    })
    window._problemData = problems
}

function selectProblem(select) {
    const problem_id = parseInt(select.value)
    const problem = window._problemData.find(p => p.problem_id === problem_id)
    if (problem) renderProblemEditor(problem)
}

function renderProblemEditor(p) {
    const container = document.getElementById('problemEditor')
    container.innerHTML = ''
    const div = document.createElement('div')
    div.innerHTML = `
        <hr>
        <input value="${p.title}" placeholder="标题" style="width: 80%;"><br>
        <textarea placeholder="描述" rows="6" style="width: 80%;">${p.description}</textarea><br>
        时间限制(ms): <input type="number" value="${p.time_limit}"><br>
        <div class="examples">
            ${p.examples.map(e => `
                <div>
                    输入:<br><textarea rows="3" style="width: 80%;">${e.input}</textarea><br>
                    输出:<br><textarea rows="3" style="width: 80%;">${e.output}</textarea><br>
                </div>`).join('')}
        </div>
        <button onclick="addExample(this)">添加样例</button>
        <button onclick="saveProblem(this, ${p.problem_id})">保存</button>
        <button onclick="deleteProblem(${p.problem_id})">删除</button>
    `
    container.appendChild(div)
}

function addProblemForm() {
    const container = document.getElementById('problemEditor')
    container.innerHTML = ''
    const div = document.createElement('div')
    div.innerHTML = `
        <hr>
        <input placeholder="标题" style="width: 80%;"><br>
        <textarea placeholder="描述" rows="6" style="width: 80%;"></textarea><br>
        时间限制(ms): <input type="number" value="1000"><br>
        <div class="examples"></div>
        <button onclick="addExample(this)">添加样例</button>
        <button onclick="saveProblem(this)">保存</button>
    `
    container.appendChild(div)
}

function addExample(btn) {
    const div = btn.parentElement.querySelector('.examples')
    const ex = document.createElement('div')
    ex.innerHTML = `
        输入:<br><textarea rows="3" style="width: 80%;"></textarea><br>
        输出:<br><textarea rows="3" style="width: 80%;"></textarea><br>
    `
    div.appendChild(ex)
}

function saveProblem(btn, problem_id = null) {
    const parent = btn.parentElement
    const title = parent.querySelector('input').value
    const description = parent.querySelector('textarea').value
    const time_limit = parent.querySelector('input[type=number]').value
    const examples = Array.from(parent.querySelectorAll('.examples > div')).map(e => ({
        input: e.querySelectorAll('textarea')[0].value,
        output: e.querySelectorAll('textarea')[1].value
    }))
    const data = { title, description, time_limit, examples }

    const url = problem_id ? '/admin/api/update_problem' : '/admin/api/create_problem'
    if (problem_id) data.problem_id = problem_id

    axios.post(url, data).then(response => {
        alert(response.data === 'OK' ? "操作成功!" : "操作失败: " + response.data.reason)
        fetchAdminData()
    })
}

function deleteProblem(id) {
    axios.post('/admin/api/delete_problem', { problem_id: id }).then(response => {
        alert(response.data === 'OK' ? "操作成功!" : "操作失败: " + response.data.reason)
        fetchAdminData()
    })
}

fetchAdminData()