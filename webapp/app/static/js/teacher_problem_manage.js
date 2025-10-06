function showToast(message, type='info') {
    const toastContainer = document.querySelector('.toast-container');
    const toastId = 'toast-' + Date.now();
    const toastEl = document.createElement('div');
    toastEl.className = `toast align-items-center text-white bg-${type} border-0`;
    toastEl.id = toastId;
    toastEl.setAttribute('role', 'alert');
    toastEl.setAttribute('aria-live', 'assertive');
    toastEl.setAttribute('aria-atomic', 'true');
    toastEl.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">${message}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
    `;
    toastContainer.appendChild(toastEl);
    const toast = new bootstrap.Toast(toastEl, { delay: 3000 });
    toast.show();
    toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
}

function showConfirm(message) {
    return new Promise(resolve => {
        const overlay = document.createElement('div');
        overlay.className = 'custom-confirm';
        overlay.innerHTML = `
            <div class="custom-confirm-box">
                <p>${message}</p>
                <button class="btn btn-primary">确定</button>
                <button class="btn btn-secondary">取消</button>
            </div>
        `;
        document.body.appendChild(overlay);
        overlay.querySelector('.btn-primary').onclick = () => { resolve(true); overlay.remove(); }
        overlay.querySelector('.btn-secondary').onclick = () => { resolve(false); overlay.remove(); }
    });
}

async function fetchTeacherData() {
    try {
        const res = await axios.get('/teacher/api');
        const data = res.data;
        if (!data) { showToast('获取数据失败：服务器返回空数据','warning'); return; }
        if (!data.problems) { showToast('获取题目数据失败：数据格式错误','warning'); return; }
        renderProblems(data.problems);
    } catch (error) {
        if (error.response) {
            const status = error.response.status;
            const message = error.response.data?.message || '未知错误';
            if (status===403) showToast('权限不足：admin用户可能没有访问teacher API的权限','warning');
            else if (status===401) showToast('未授权访问，请重新登录','warning');
            else showToast(`服务器错误 (${status}): ${message}`,'danger');
        } else if (error.request) showToast('网络错误：无法连接到服务器','danger');
        else showToast('请求配置错误：' + error.message,'danger');
    }
}

function renderProblems(problems) {
    const select = document.getElementById('problemSelect');
    select.innerHTML = '<option disabled selected>请选择题目</option>';
    if (!Array.isArray(problems)) { showToast('题目数据格式错误','warning'); return; }
    if (problems.length === 0) {
        const option = document.createElement('option');
        option.disabled = true;
        option.textContent = '暂无题目';
        select.appendChild(option);
        return;
    }
    problems.forEach(p => {
        if (!p.problem_id) return;
        const opt = document.createElement('option');
        opt.value = p.problem_id;
        opt.textContent = `${p.problem_id} - ${p.title || '(未命名题目)'}`;
        select.appendChild(opt);
    });
    window._problemData = problems;
}

function selectProblem(select) {
    const problem_id = parseInt(select.value);
    const problem = window._problemData.find(p => p.problem_id === problem_id);
    if (problem) renderProblemEditor(problem);
}

function renderProblemEditor(p) {
    const container = document.getElementById('problemEditor');
    container.innerHTML = '';
    const div = document.createElement('div');
    const title = p.title || '';
    const description = p.description || '';
    const time_limit = Number(p.time_limit) || 1000;
    const example_visible_count = Number(p.example_visible_count) || 2;
    const compare_mode = p.compare_mode || 1;
    const examples = Array.isArray(p.examples) ? p.examples : [];

    div.innerHTML = `
        <hr>
        <div style="margin-bottom:10px;">
            <input value="${title}" placeholder="标题" style="width:100%; padding:6px 10px;">
        </div>
        <div style="margin-bottom:10px;">
            <label>题目描述：</label>
            <textarea style="width:100%; height:320px; padding:6px 10px;">${description}</textarea>
        </div>
        <div style="display:flex; gap:10px; margin-bottom:10px; flex-wrap:wrap;">
            <div>时间限制(ms): <input type="number" value="${time_limit}" min="1" style="width:100px; padding:4px;"></div>
            <div>判题模式:
                <select id="compareModeSelect" style="padding:4px;">
                    <option value="1" ${compare_mode==1?'selected':''}>严格匹配</option>
                    <option value="2" ${compare_mode==2?'selected':''}>忽略空格</option>
                    <option value="3" ${compare_mode==3?'selected':''}>忽略大小写</option>
                    <option value="4" ${compare_mode==4?'selected':''}>浮点容差</option>
                </select>
            </div>
            <div>显示示例数量: <input type="number" value="${example_visible_count}" min="0" max="${examples.length}" style="width:60px; padding:4px;"></div>
        </div>
        <div class="example-section" style="margin-bottom:10px;">
            <div class="examples" style="margin-bottom:10px;">
                <label>样例：</label>
                ${examples.map(e => `
                    <div style="margin-bottom:6px; border:1px solid #ddd; padding:6px; border-radius:6px; background:#f8f9fc; position:relative;">
                        <button onclick="removeExample(this)" style="position:absolute; top:4px; right:4px;">删除</button>
                        输入:<textarea style="width:100%; height:60px; margin-top:4px;">${e.input||''}</textarea>
                        输出:<textarea style="width:100%; height:60px; margin-top:4px;">${e.output||''}</textarea>
                    </div>`).join('')}
            </div>
            <div style="display:flex; gap:10px; flex-wrap:wrap;">
                <button onclick="addExample(this)">添加样例</button>
                <button onclick="saveProblem(this, ${p.problem_id})">保存修改</button>
                <button onclick="deleteProblem(${p.problem_id})" style="background-color:#dc3545;">删除题目</button>
            </div>
        </div>
    `;
    container.appendChild(div);
}

function addProblemForm() {
    const container = document.getElementById('problemEditor');
    container.innerHTML = '';
    const div = document.createElement('div');

    div.innerHTML = `
        <hr>
        <div style="margin-bottom:10px;">
            <input placeholder="标题" style="width:100%; padding:6px 10px;">
        </div>
        <div style="margin-bottom:10px;">
            <label>题目描述：</label>
            <textarea placeholder="支持Markdown和LaTeX公式" style="width:100%; height:120px; padding:6px 10px;"></textarea>
        </div>
        <div style="display:flex; gap:10px; margin-bottom:10px; flex-wrap:wrap;">
            <div>时间限制(ms): <input type="number" value="1000" style="width:100px; padding:4px;"></div>
            <div>判题模式:
                <select id="compareModeSelect" style="padding:4px;">
                    <option value="1" selected>STRICT</option>
                    <option value="2">IGNORE_SPACES</option>
                    <option value="3">CASE_INSENSITIVE</option>
                    <option value="4">FLOAT_TOLERANT</option>
                </select>
            </div>
            <div>显示示例数量: <input type="number" value="2" min="0" style="width:60px; padding:4px;"></div>
        </div>
        <div class="example-section" style="margin-bottom:10px;">
            <div class="examples" style="margin-bottom:10px;"><label>样例：</label></div>
            <div style="display:flex; gap:10px; flex-wrap:wrap;">
                <button onclick="addExample(this)">添加样例</button>
                <button onclick="saveProblem(this)">保存</button>
            </div>
        </div>
    `;
    container.appendChild(div);
}
function addExample(btn){
    const section = btn.closest('.example-section');
    const div = section.querySelector('.examples');
    if(!div){ showToast('无法找到样例容器','warning'); return; }

    const ex = document.createElement('div');
    ex.style.marginBottom = '6px';
    ex.style.border = '1px solid #ddd';
    ex.style.padding = '6px';
    ex.style.borderRadius = '6px';
    ex.style.background = '#f8f9fc';
    ex.style.position = 'relative';
    ex.innerHTML = `
        <button onclick="removeExample(this)" style="position:absolute; top:4px; right:4px;">删除</button>
        输入:<textarea style="width:100%; height:60px; margin-top:4px;"></textarea>
        输出:<textarea style="width:100%; height:60px; margin-top:4px;"></textarea>
    `;
    div.appendChild(ex);
}


async function removeExample(btn) {
    if(await showConfirm('确定要删除这个样例吗？')) btn.parentElement.remove();
}

function saveProblem(btn, problem_id=null){
    const container = btn.closest('#problemEditor > div');
    if(!container){ showToast('无法找到题目容器','warning'); return; }

    const titleInput = container.querySelector('input[placeholder="标题"]');
    const descTextarea = container.querySelector('textarea[placeholder], textarea:not(.example-text)');
    const timeLimitInput = container.querySelector('div > input[type="number"]');
    const compareModeSelect = container.querySelector('#compareModeSelect');
    const exampleCountInput = Array.from(container.querySelectorAll('input[type="number"]')).find(i => i!==timeLimitInput);

    if(!titleInput || !descTextarea || !timeLimitInput){
        showToast('无法读取题目信息','warning');
        return;
    }

    const title = titleInput.value.trim();
    const description = descTextarea.value.trim();
    const time_limit = Number(timeLimitInput.value) || 1000;
    const compare_mode = compareModeSelect ? parseInt(compareModeSelect.value) : 1;
    const example_visible_count = exampleCountInput ? parseInt(exampleCountInput.value) || 2 : 2;
    const exampleDivs = Array.from(container.querySelectorAll('.examples > div'));
    const examples = exampleDivs.map(e => {
        const areas = e.querySelectorAll('textarea');
        return { input: areas[0]?.value.trim()||'', output: areas[1]?.value.trim()||'' };
    }).filter(e => e.input || e.output);

    const data = { title, description, time_limit, compare_mode, example_visible_count, examples };
    if(problem_id) data.problem_id = problem_id;

    const url = problem_id ? '/teacher/api/update_problem' : '/teacher/api/create_problem';
    axios.post(url, data)
        .then(()=>{
            showToast(problem_id?'题目更新成功！':'题目创建成功！','success');
            fetchTeacherData();
            container.innerHTML='';
        })
        .catch(e=>{
            showToast('保存失败: '+(e.response?.data?.message||e.message),'danger');
        });
}

async function deleteProblem(problem_id) {
    if(await showConfirm('确定要删除这个题目吗？')){
        axios.post('/teacher/api/delete_problem',{ problem_id })
            .then(()=>{ showToast('题目删除成功！','success'); fetchTeacherData(); document.getElementById('problemEditor').innerHTML=''; })
            .catch(e=>showToast('删除题目失败: '+(e.response?.data?.message||e.message),'danger'));
    }
}
fetchTeacherData();