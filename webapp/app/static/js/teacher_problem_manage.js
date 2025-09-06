async function fetchTeacherData() {
    console.log("[Init] Fetching teacher data from /teacher/api...")
    try {
        const res = await axios.get('/teacher/api')
        const data = res.data
        
        if (!data) {
            console.error("[Error] Server returned empty data")
            alert('获取数据失败：服务器返回空数据')
            return
        }
        
        if (!data.problems) {
            console.error("[Error] Invalid response format: problems not found")
            alert('获取题目数据失败：数据格式错误')
            return
        }
        
        console.log(`[Init] Successfully fetched ${data.problems.length} problems`)
        renderProblems(data.problems)
    } catch (error) {
        console.error("[Error] Failed to fetch teacher data", error)
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
    console.log("[Render] Rendering problem list...")
    const select = document.getElementById('problemSelect')
    select.innerHTML = '<option disabled selected>请选择题目</option>'
    
    if (!Array.isArray(problems)) {
        console.error("[Error] Problems data is not an array")
        alert('题目数据格式错误')
        return
    }
    
    if (problems.length === 0) {
        console.warn("[Render] No problems available")
        const option = document.createElement('option')
        option.disabled = true
        option.textContent = '暂无题目'
        select.appendChild(option)
        return
    }
    
    problems.forEach(p => {
        if (!p.problem_id || !p.title) {
            console.warn("[Render] Skipped invalid problem entry:", p)
            return
        }
        
        const opt = document.createElement('option')
        opt.value = p.problem_id
        opt.textContent = `${p.problem_id} - ${p.title}`
        select.appendChild(opt)
    })
    
    window._problemData = problems
    console.log("[Render] Problem list rendered successfully")
}

function selectProblem(select) {
    const problem_id = parseInt(select.value)
    console.log(`[Setup] Problem selected: ID ${problem_id}`)
    const problem = window._problemData.find(p => p.problem_id === problem_id)
    if (problem) {
        renderProblemEditor(problem)
    } else {
        console.error("[Error] Problem not found:", problem_id)
    }
}

function renderProblemEditor(p) {
    console.log(`[Render] Rendering problem editor for ID ${p.problem_id}`)
    const container = document.getElementById('problemEditor')
    container.innerHTML = ''
    const div = document.createElement('div')

    const title = p.title || ''
    const description = p.description || ''
    const time_limit = p.time_limit || 1000
    const example_visible_count = p.example_visible_count ?? 2
    const compare_mode = p.compare_mode || 1
    const examples = Array.isArray(p.examples) ? p.examples : []

    div.innerHTML = `
        <hr>
        <input value="${title}" placeholder="标题" style="width: 100%;"><br><br>
        <label>题目描述：</label><br>
        <textarea style="width: 100%; height: 150px;">${description}</textarea><br><br>
        时间限制(ms): <input type="number" value="${time_limit}"><br><br>
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
    console.log("[Render] Problem editor rendered")
}

document.addEventListener("DOMContentLoaded", () => {
    console.log("[Init] DOM loaded.")
    fetchTeacherData()
})
