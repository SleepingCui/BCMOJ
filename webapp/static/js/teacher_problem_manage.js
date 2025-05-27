async function fetchTeacherData() {
    const res = await axios.get('/teacher/teacher_api')
    const data = res.data
    renderProblems(data.problems)
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
        <input value="${p.title}" placeholder="标题" readonly style="width: 100%;"><br><br>
        <label>题目描述：</label><br>
        <textarea readonly style="width: 100%; height: 150px;">${p.description}</textarea><br><br>
        时间限制(ms): <input type="number" value="${p.time_limit}" readonly><br><br>
        <div class="examples">
            <label>样例：</label>
            ${p.examples.map(e => `
                <div style="margin-bottom: 10px;">
                    输入:<br>
                    <textarea readonly style="width: 100%; height: 80px;">${e.input}</textarea><br>
                    输出:<br>
                    <textarea readonly style="width: 100%; height: 80px;">${e.output}</textarea>
                </div>`).join('')}
        </div>
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
        <textarea placeholder="描述" style="width: 100%; height: 150px;"></textarea><br><br>
        时间限制(ms): <input type="number" value="1000"><br><br>
        <div class="examples"></div>
        <button onclick="addExample(this)">添加样例</button>
        <button onclick="saveProblem(this)">保存</button>
    `
    container.appendChild(div)
}

function addExample(btn) {
    const div = btn.parentElement.querySelector('.examples')
    const ex = document.createElement('div')
    ex.style.marginBottom = '10px'
    ex.innerHTML = `
        输入:<br>
        <textarea style="width: 100%; height: 80px;"></textarea><br>
        输出:<br>
        <textarea style="width: 100%; height: 80px;"></textarea>
    `
    div.appendChild(ex)
}

function saveProblem(btn, problem_id = null) {
    const parent = btn.parentElement
    const title = parent.querySelector('input').value
    const description = parent.querySelector('textarea').value
    const time_limit = parent.querySelector('input[type=number]').value
    const exampleDivs = Array.from(parent.querySelectorAll('.examples > div'))

    const examples = exampleDivs.map(e => {
        const inputs = e.querySelectorAll('textarea')
        return {
            input: inputs[0].value,
            output: inputs[1].value
        }
    })

    const data = { title, description, time_limit, examples }
    if (problem_id) {
        data.problem_id = problem_id
        axios.post('/teacher/api/teacher_update_problem', data).then(fetchTeacherData)
    } else {
        axios.post('/teacher/api/teacher_create_problem', data).then(fetchTeacherData)
    }
}

fetchTeacherData()
