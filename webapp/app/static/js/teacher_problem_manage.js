let currentTab = 'problems';

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

function switchTab(tabName) {
    if (tabName === currentTab) return;

    document.getElementById(`${currentTab}-tab`).style.display = 'none';
    document.querySelector(`.tab-button[data-tab="${currentTab}"]`)?.classList.remove('active');
    document.getElementById(`${tabName}-tab`).style.display = 'block';
    document.querySelector(`.tab-button[data-tab="${tabName}"]`)?.classList.add('active');
    currentTab = tabName;
    if (tabName === 'problems') {
        fetchTeacherData();
    } else if (tabName === 'groups') {
        fetchGroupData();
    }
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
    const problem = window._problemData ? window._problemData.find(p => p.problem_id === problem_id) : null;
    if (problem) renderProblemEditor(problem);
}

function renderProblemEditor(p) {
    const container = document.getElementById('problemEditor');
    container.innerHTML = '';
    const div = document.createElement('div');
    const title = p.title || '';
    const description = p.description || '';
    const time_limit = Number(p.time_limit) || 1000;
    const mem_limit = Number(p.mem_limit) || 1000;
    const example_visible_count = Number(p.example_visible_count) || 2;
    const compare_mode = p.compare_mode || 1;
    const examples = Array.isArray(p.examples) ? p.examples : [];
    const group_id = (p.group_id === null || p.group_id === undefined || p.group_id === 0) ? null : p.group_id;
    console.info(group_id);

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
            <div>内存限制(KB): <input type="number" value="${mem_limit}" min="1" style="width:100px; padding:4px;"></div>
            <div>判题模式:
                <select id="compareModeSelect" style="padding:4px;">
                    <option value="1" ${compare_mode==1?'selected':''}>严格匹配</option>
                    <option value="2" ${compare_mode==2?'selected':''}>忽略空格</option>
                    <option value="3" ${compare_mode==3?'selected':''}>忽略大小写</option>
                    <option value="4" ${compare_mode==4?'selected':''}>浮点容差</option>
                </select>
            </div>
            <div>显示示例数量: <input type="number" value="${example_visible_count}" min="0" max="${examples.length}" style="width:60px; padding:4px;"></div>
            <div>所属题组:
                <select id="problemGroupSelect" style="padding:4px;">
                    <option value="" ${!group_id?'selected':''}>无题组</option>
                    <!-- 选项将通过JavaScript动态加载 -->
                </select>
            </div>
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
    loadGroupOptionsIntoSelect('problemGroupSelect', group_id);
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
            <div>内存限制(KB): <input type="number" value="1000" style="width:100px; padding:4px;"></div>
            <div>判题模式:
                <select id="compareModeSelect" style="padding:4px;">
                    <option value="1" selected>STRICT</option>
                    <option value="2">IGNORE_SPACES</option>
                    <option value="3">CASE_INSENSITIVE</option>
                    <option value="4">FLOAT_TOLERANT</option>
                </select>
            </div>
            <div>显示示例数量: <input type="number" value="2" min="0" style="width:60px; padding:4px;"></div>
            <div>所属题组:
                <select id="problemGroupSelect" style="padding:4px;">
                    <option value="">无题组</option>
                    <!-- 选项将通过JavaScript动态加载 -->
                </select>
            </div>
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
    loadGroupOptionsIntoSelect('problemGroupSelect', null);
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
    const inputs = container.querySelectorAll('div > input[type="number"]');
    const timeLimitInput = inputs[0];      
    const memLimitInput = inputs[1];    
    const compareModeSelect = container.querySelector('#compareModeSelect');
    const exampleCountInput = Array.from(inputs).find(i => i!==timeLimitInput && i!==memLimitInput);
    const groupSelect = container.querySelector('#problemGroupSelect');

    if(!titleInput || !descTextarea || !timeLimitInput || !memLimitInput){
        showToast('无法读取题目信息','warning');
        return;
    }

    const title = titleInput.value.trim();
    const description = descTextarea.value.trim();
    const time_limit = Number(timeLimitInput.value) || 1000;
    const mem_limit = Number(memLimitInput.value) || 1000;  
    const compare_mode = compareModeSelect ? parseInt(compareModeSelect.value) : 1;
    const example_visible_count = exampleCountInput ? parseInt(exampleCountInput.value) || 2 : 2;
    const group_id = groupSelect ? groupSelect.value || null : null;

    const exampleDivs = Array.from(container.querySelectorAll('.examples > div'));
    const examples = exampleDivs.map(e => {
        const areas = e.querySelectorAll('textarea');
        return { input: areas[0]?.value.trim()||'', output: areas[1]?.value.trim()||'' };
    }).filter(e => e.input || e.output);

    const data = { title, description, time_limit, mem_limit, compare_mode, example_visible_count, examples, group_id };
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
            .then(()=>{ 
                showToast('题目删除成功！','success'); 
                fetchTeacherData(); 
                document.getElementById('problemEditor').innerHTML=''; 
            })
            .catch(e=>showToast('删除题目失败: '+(e.response?.data?.message||e.message),'danger'));
    }
}

// ----------------- pg ------------------
async function fetchGroupData() {
    try {
        const res = await axios.get('/teacher/api/groups');
        const data = res.data;
        if (!data) { showToast('获取题组数据失败：服务器返回空数据','warning'); return; }
        if (!data.groups) { showToast('获取题组数据失败：数据格式错误','warning'); return; }
        renderGroups(data.groups);
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

function renderGroups(groups) {
    const select = document.getElementById('groupSelect');
    select.innerHTML = '<option disabled selected>请选择题组</option>';
    if (!Array.isArray(groups)) { showToast('题组数据格式错误','warning'); return; }
    if (groups.length === 0) {
        const option = document.createElement('option');
        option.disabled = true;
        option.textContent = '暂无题组';
        select.appendChild(option);
        return;
    }
    groups.forEach(g => {
        if (!g.group_id) return;
        const opt = document.createElement('option');
        opt.value = g.group_id;
        opt.textContent = `${g.group_id} - ${g.group_name} (${g.problem_count}题)`;
        select.appendChild(opt);
    });
    window._groupData = groups;
}

function selectGroup(select) {
    const group_id = parseInt(select.value);
    const group = window._groupData ? window._groupData.find(g => g.group_id === group_id) : null;
    if (group) renderGroupEditor(group);
}

function renderGroupEditor(g) {
    const container = document.getElementById('groupEditor');
    container.innerHTML = '';
    const div = document.createElement('div');
    const group_name = g.group_name || '';
    const description = g.description || '';

    div.innerHTML = `
        <hr>
        <div style="margin-bottom:10px;">
            <input value="${group_name}" placeholder="题组名称" style="width:100%; padding:6px 10px;">
        </div>
        <div style="margin-bottom:10px;">
            <label>题组描述：</label>
            <textarea style="width:100%; height:120px; padding:6px 10px;">${description}</textarea>
        </div>
        <div class="group-problem-list" id="groupProblemList_${g.group_id}">
            <h5>题组内题目:</h5>
            <div id="problemListContent_${g.group_id}">加载中...</div>
        </div>
        <div class="assign-problem-section">
            <h5>为题组添加题目:</h5>
            <select id="unassignedProblemSelect_${g.group_id}" style="width:100%; margin-bottom:10px; padding:4px;">
                <option disabled selected>加载中...</option>
            </select>
            <button onclick="assignProblemToGroup(${g.group_id})">添加到题组</button>
        </div>
        <div style="display:flex; gap:10px; margin-top:20px;">
            <button onclick="saveGroup(this, ${g.group_id})">保存修改</button>
            <button onclick="deleteGroup(${g.group_id})" style="background-color:#dc3545;">删除题组</button>
        </div>
    `;
    container.appendChild(div);
    loadProblemsInGroup(g.group_id);
    loadUnassignedProblemsForAssignment(g.group_id);
}

function addGroupForm() {
    const container = document.getElementById('groupEditor');
    container.innerHTML = '';
    const div = document.createElement('div');

    div.innerHTML = `
        <hr>
        <div style="margin-bottom:10px;">
            <input placeholder="题组名称" style="width:100%; padding:6px 10px;">
        </div>
        <div style="margin-bottom:10px;">
            <label>题组描述：</label>
            <textarea placeholder="描述该题组的内容" style="width:100%; height:120px; padding:6px 10px;"></textarea>
        </div>
        <div style="display:flex; gap:10px; margin-top:20px;">
            <button onclick="saveGroup(this)">保存</button>
        </div>
    `;
    container.appendChild(div);
}

async function saveGroup(btn, group_id=null){
    const container = btn.closest('#groupEditor > div');
    if(!container){ showToast('无法找到题组容器','warning'); return; }

    const nameInput = container.querySelector('input[placeholder="题组名称"]');
    const descTextarea = container.querySelector('textarea[placeholder], textarea:not(#problemListContent_' + (group_id || '') + ')');

    if(!nameInput){
        showToast('无法读取题组名称','warning');
        return;
    }

    const group_name = nameInput.value.trim();
    const description = descTextarea ? descTextarea.value.trim() : '';

    const data = { group_name, description };
    if(group_id) data.group_id = group_id;

    const url = group_id ? '/teacher/api/update_group' : '/teacher/api/create_group';
    axios.post(url, data)
        .then(()=>{
            showToast(group_id?'题组更新成功！':'题组创建成功！','success');
            fetchGroupData();
            container.innerHTML='';
        })
        .catch(e=>{
            showToast('保存失败: '+(e.response?.data?.message||e.message),'danger');
        });
}

async function deleteGroup(group_id) {
    if(await showConfirm('确定要删除这个题组吗？')){
        axios.post('/teacher/api/delete_group',{ group_id })
            .then(()=>{ 
                showToast('题组删除成功！','success'); 
                fetchGroupData(); 
                document.getElementById('groupEditor').innerHTML=''; 
            })
            .catch(e=>showToast('删除题组失败: '+(e.response?.data?.message||e.message),'danger'));
    }
}

async function loadGroupOptionsIntoSelect(selectId, currentValue = null) {
    const selectElement = document.getElementById(selectId);
    if (!selectElement) return;
    while (selectElement.children.length > 1) {
        selectElement.removeChild(selectElement.lastChild);
    }

    try {
        const res = await axios.get('/teacher/api/groups');
        const groups = res.data?.groups || [];

        groups.forEach(g => {
            const option = document.createElement('option');
            option.value = g.group_id;
            option.textContent = `${g.group_name}`;
            if (currentValue != null && g.group_id == currentValue) {
                option.selected = true;
            }
            selectElement.appendChild(option);
        });
    } catch (e) {
        console.error('加载题组选项失败:', e);
        showToast('加载题组选项失败: ' + (e.response?.data?.message||e.message), 'danger');
    }
}

async function loadProblemsInGroup(group_id) {
    try {
        const res = await axios.get('/teacher/api');
        const allProblems = res.data?.problems || [];
        const problemsInGroup = allProblems.filter(p => p.group_id === group_id);

        const contentDiv = document.getElementById(`problemListContent_${group_id}`);
        if (!contentDiv) return;

        if (problemsInGroup.length === 0) {
            contentDiv.innerHTML = '<p class="text-muted">题组内暂无题目</p>';
            return;
        }

        contentDiv.innerHTML = problemsInGroup.map(p => `
            <div class="group-problem-item">
                <div class="group-problem-info">
                    <strong>${p.problem_id}</strong> - ${p.title || '(未命名题目)'}
                </div>
                <div class="group-problem-actions">
                    <button onclick="removeProblemFromGroup(${p.problem_id})">移出题组</button>
                </div>
            </div>
        `).join('');
    } catch (e) {
        console.error('加载题组内题目失败:', e);
        showToast('加载题组内题目失败: ' + (e.response?.data?.message||e.message), 'danger');
    }
}

async function loadUnassignedProblemsForAssignment(group_id) {
    try {
        const res = await axios.get('/teacher/api');
        const allProblems = res.data?.problems || [];
        const unassignedProblems = allProblems.filter(p => p.group_id == null || p.group_id == 0);

        const selectElement = document.getElementById(`unassignedProblemSelect_${group_id}`);
        if (!selectElement) return;

        while (selectElement.children.length > 1) {
            selectElement.removeChild(selectElement.lastChild);
        }
        if (unassignedProblems.length === 0) {
            const option = document.createElement('option');
            option.disabled = true;
            option.textContent = '暂无未分配题目';
            selectElement.appendChild(option);
            return;
        }
        unassignedProblems.forEach(p => {
            const option = document.createElement('option');
            option.value = p.problem_id;
            option.textContent = `${p.problem_id} - ${p.title || '(未命名题目)'}`;
            selectElement.appendChild(option);
        });
    } catch (e) {
        console.error('加载未分配题目列表失败:', e);
        showToast('加载未分配题目列表失败: ' + (e.response?.data?.message||e.message), 'danger');
    }
}

async function assignProblemToGroup(group_id) {
    const selectElement = document.getElementById(`unassignedProblemSelect_${group_id}`);
    if (!selectElement || !selectElement.value) {
        showToast('请选择要添加的题目', 'warning');
        return;
    }

    const problem_id = parseInt(selectElement.value);
    if (isNaN(problem_id)) {
        showToast('无效的题目ID', 'warning');
        return;
    }

    try {
        const allProblemsRes = await axios.get('/teacher/api');
        const allProblems = allProblemsRes.data?.problems || [];
        const problemToUpdate = allProblems.find(p => p.problem_id === problem_id);
        if (!problemToUpdate) {
            showToast('找不到指定的题目', 'danger');
            return;
        }

        const res = await axios.post('/teacher/api/update_problem', {
            problem_id: problem_id,
            title: problemToUpdate.title,
            description: problemToUpdate.description,
            time_limit: problemToUpdate.time_limit,
            mem_limit: problemToUpdate.mem_limit,
            compare_mode: problemToUpdate.compare_mode,
            example_visible_count: problemToUpdate.example_visible_count,
            examples: problemToUpdate.examples || [],
            group_id: group_id
        });

        showToast('添加成功！', 'success');
        const group = window._groupData.find(g => g.group_id === group_id);
        if (group) renderGroupEditor(group);

    } catch (e) {
        console.error('添加失败:', e);
        showToast('添加失败: ' + (e.response?.data?.message||e.message), 'danger');
    }
}

async function removeProblemFromGroup(problem_id) {
    if (!await showConfirm('确定要将此题目移出题组吗？')) return;

    try {
        const allProblemsRes = await axios.get('/teacher/api');
        const allProblems = allProblemsRes.data?.problems || [];
        const problemToUpdate = allProblems.find(p => p.problem_id === problem_id);
        if (!problemToUpdate) {
            showToast('找不到指定的题目', 'danger');
            return;
        }
        const res = await axios.post('/teacher/api/update_problem', {
            problem_id: problem_id,
            title: problemToUpdate.title,
            description: problemToUpdate.description,
            time_limit: problemToUpdate.time_limit,
            mem_limit: problemToUpdate.mem_limit,
            compare_mode: problemToUpdate.compare_mode,
            example_visible_count: problemToUpdate.example_visible_count,
            examples: problemToUpdate.examples || [],
            group_id: null
        });

        showToast('移出成功！', 'success');
        const currentGroupSelect = document.getElementById('groupSelect');
        if (currentGroupSelect && currentGroupSelect.value) {
            const currentGroupId = parseInt(currentGroupSelect.value);
            const group = window._groupData.find(g => g.group_id === currentGroupId);
            if (group) renderGroupEditor(group);
        }

    } catch (e) {
        console.error('移出失败:', e);
        showToast('移出失败: ' + (e.response?.data?.message||e.message), 'danger');
    }
}

document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.tab-button').forEach((btn, index) => {
        btn.setAttribute('data-tab', index === 0 ? 'problems' : 'groups');
    });

    fetchTeacherData();
});