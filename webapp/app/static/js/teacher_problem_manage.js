async function fetchTeacherData() {
    try {
        const res = await axios.get('/teacher/api')
        const data = res.data
        console.log('[Data] Fetched teacher data:', data)
        
        if (!data) {
            alert('获取数据失败：服务器返回空数据')
            return
        }
        
        if (!data.problems) {
            alert('获取题目数据失败：数据格式错误')
            return
        }
        
        renderProblems(data.problems)
    } catch (error) {
        if (error.response) {
            const status = error.response.status
            const message = error.response.data?.message || '未知错误'
            
            if (status === 403) {
                alert('权限不足：admin用户可能没有访问teacher API的权限')
            } else if (status === 401) {
                alert('未授权访问，请重新登录')
            } else {
                alert(`服务器错误 (${status}): ${message}`)
            }
        } else if (error.request) {
            alert('网络错误：无法连接到服务器')
        } else {
            alert('请求配置错误：' + error.message)
        }
    }
}

function renderProblems(problems) {
    const select = document.getElementById('problemSelect')
    select.innerHTML = '<option disabled selected>请选择题目</option>'
    
    if (!Array.isArray(problems)) {
        alert('题目数据格式错误')
        return
    }
    
    if (problems.length === 0) {
        const option = document.createElement('option')
        option.disabled = true
        option.textContent = '暂无题目'
        select.appendChild(option)
        return
    }
    
    problems.forEach(p => {
    if (!p.problem_id) return
    const opt = document.createElement('option')
    opt.value = p.problem_id
    opt.textContent = `${p.problem_id} - ${p.title || '(未命名题目)'}`
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

    const title = p.title || ''
    const description = p.description || ''
    const time_limit = Number(p.time_limit) || 1000
    const example_visible_count = Number(p.example_visible_count) || 2
    const compare_mode = p.compare_mode || 1
    const examples = Array.isArray(p.examples) ? p.examples : []

    div.innerHTML = `
        <hr>
        <input value="${title}" placeholder="标题" style="width: 100%;"><br><br>
        <label>题目描述：</label><br>
        <textarea style="width: 100%; height: 150px;">${description}</textarea><br><br>
        时间限制(ms): <input type="number" value="${time_limit}" min="1"><br><br>
        判题模式:
        <select id="compareModeSelect">
            <option value="1" ${compare_mode == 1 ? 'selected' : ''}>STRICT (严格模式)</option>
            <option value="2" ${compare_mode == 2 ? 'selected' : ''}>IGNORE_SPACES (忽略空格)</option>
            <option value="3" ${compare_mode == 3 ? 'selected' : ''}>CASE_INSENSITIVE (忽略大小写)</option>
            <option value="4" ${compare_mode == 4 ? 'selected' : ''}>FLOAT_TOLERANT (浮点数容差)</option>
        </select>
        <br><br>
        显示示例数量: <input type="number" value="${example_visible_count}" min="0" max="${examples.length}"><br><br>
        <div class="examples">
            <label>样例：</label>
            ${examples.map(e => `
                <div style="margin-bottom: 10px; border: 1px solid #ddd; padding: 10px; position: relative;">
                    <button onclick="removeExample(this)" style="position: absolute; top: 5px; right: 5px; background-color: #dc3545; color: white; border: none; border-radius: 3px; padding: 2px 6px; cursor: pointer; font-size: 12px;">删除</button>
                    输入:<br>
                    <textarea style="width: 100%; height: 80px;">${e.input || ''}</textarea><br>
                    输出:<br>
                    <textarea style="width: 100%; height: 80px;">${e.output || ''}</textarea>
                </div>`).join('')}
        </div>
        <button onclick="addExample(this)">添加样例</button>
        <button onclick="saveProblem(this, ${p.problem_id})">保存修改</button>
        <button onclick="deleteProblem(${p.problem_id})" style="background-color: #dc3545;">删除题目</button>
    `
    container.appendChild(div)
}


function addProblemForm() {
    const container = document.getElementById('problemEditor')
    container.innerHTML = ''
    const div = document.createElement('div')
    div.innerHTML = `
        <hr>
        <input placeholder="标题" style="width: 100%;"><br><br>
        <label>题目描述：</label><br>
        <textarea placeholder="支持Markdown和LaTeX 公式示例: $ x ={-b \\pm \\sqrt{b^2-4ac}\\over 2a} $" style="width: 100%; height: 150px;"></textarea><br><br>
        时间限制(ms): <input type="number" value="1000"><br><br>
        判题模式:
        <select id="compareModeSelect">
            <option value="1" selected>STRICT (严格模式)</option>
            <option value="2">IGNORE_SPACES (忽略空格)</option>
            <option value="3">CASE_INSENSITIVE (忽略大小写)</option>
            <option value="4">FLOAT_TOLERANT (浮点数容差)</option>
        </select>
        <br><br>
        显示示例数量: <input type="number" value="2" min="0"><br><br>
        <div class="examples">
            <label>样例：</label>
        </div>
        <button onclick="addExample(this)">添加样例</button>
        <button onclick="saveProblem(this)">保存</button>
    `
    container.appendChild(div)
}


function addExample(btn) {
    const div = btn.parentElement.querySelector('.examples')
    const ex = document.createElement('div')
    ex.style.marginBottom = '10px'
    ex.style.border = '1px solid #ddd'
    ex.style.padding = '10px'
    ex.style.position = 'relative'
    ex.innerHTML = `
        <button onclick="removeExample(this)" style="position: absolute; top: 5px; right: 5px; background-color: #dc3545; color: white; border: none; border-radius: 3px; padding: 2px 6px; cursor: pointer; font-size: 12px;">删除</button>
        输入:<br>
        <textarea style="width: 100%; height: 80px;"></textarea><br>
        输出:<br>
        <textarea style="width: 100%; height: 80px;"></textarea>
    `
    div.appendChild(ex)
}

function removeExample(btn) {
    if (confirm('确定要删除这个样例吗？')) {
        const exampleDiv = btn.parentElement
        exampleDiv.remove()
    }
}

function saveProblem(btn, problem_id = null) {
    const parent = btn.parentElement
    const inputs = parent.querySelectorAll('input')
    const textareas = parent.querySelectorAll('textarea')
    const compareModeSelect = parent.querySelector('#compareModeSelect')

    const title = inputs[0].value
    const description = textareas[0].value
    const time_limit = inputs[1].value
    const compare_mode = compareModeSelect ? parseInt(compareModeSelect.value) : 1
    const example_visible_count = inputs[2] ? parseInt(inputs[2].value) || 2 : 2
    const exampleDivs = Array.from(parent.querySelectorAll('.examples > div'))
    const examples = exampleDivs.map(e => {
        const inputs = e.querySelectorAll('textarea')
        return {
            input: inputs[0].value.trim(),
            output: inputs[1].value.trim()
        }
    }).filter(example => example.input !== '' || example.output !== '')

    const data = { title, description, time_limit, example_visible_count, examples, compare_mode }

    if (problem_id) {
        data.problem_id = problem_id
        axios.post('/teacher/api/update_problem', data)
            .then(() => {
                alert('题目更新成功！')
                fetchTeacherData()
                document.getElementById('problemEditor').innerHTML = ''
            })
            .catch(error => {
                alert('更新题目失败: ' + (error.response?.data?.message || error.message))
            })
    } else {
        axios.post('/teacher/api/create_problem', data)
            .then(() => {
                alert('题目创建成功！')
                fetchTeacherData()
                document.getElementById('problemEditor').innerHTML = ''
            })
            .catch(error => {
                alert('创建题目失败: ' + (error.response?.data?.message || error.message))
            })
    }
}



function deleteProblem(problem_id) {
    if (confirm('确定要删除这个题目吗？')) {
        axios.post('/teacher/api/delete_problem', { problem_id })
            .then(() => {
                alert('题目删除成功！')
                fetchTeacherData()
                document.getElementById('problemEditor').innerHTML = ''
            })
            .catch(error => {
                alert('删除题目失败: ' + (error.response?.data?.message || error.message))
            })
    }
}

fetchTeacherData()