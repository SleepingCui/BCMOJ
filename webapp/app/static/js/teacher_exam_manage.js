async function fetchTeacherExams() {
    try {
        const res = await axios.get('/exam/teacher/api');
        const data = res.data;
        if (!data || !data.exams) {
            alert('获取试卷数据失败');
            return;
        }
        renderExams(data.exams);
    } catch (error) {
        alert('获取试卷数据失败：' + (error.message || '未知错误'));
    }
}

function renderExams(exams) {
    const select = document.getElementById('examSelect');
    select.innerHTML = '<option disabled selected>请选择试卷</option>';

    if (!Array.isArray(exams) || exams.length === 0) {
        const option = document.createElement('option');
        option.disabled = true;
        option.textContent = '暂无试卷';
        select.appendChild(option);
        return;
    }

    window._examData = exams;

    exams.forEach(e => {
        if (!e.exam_id || !e.title) return;
        const opt = document.createElement('option');
        opt.value = e.exam_id;
        opt.textContent = `${e.exam_id} - ${e.title}`;
        select.appendChild(opt);
    });
}

function selectExam(select) {
    const exam_id = parseInt(select.value);
    const exam = window._examData.find(e => e.exam_id === exam_id);
    if (exam) renderExamEditor(exam);
}

function renderExamEditor(exam) {
    const container = document.getElementById('examEditor');
    container.innerHTML = '';

    const div = document.createElement('div');
    div.innerHTML = `
        <hr>
        <input value="${exam.title || ''}" placeholder="试卷标题" style="width: 100%; font-size: 18px; font-weight: bold;"><br><br>
        <label>考试限时（分钟，0表示无时限）:</label>
        <input type="number" min="0" value="${exam.time_limit || 0}"><br><br>
        <div class="questions-container">
            <h3>题目列表</h3>
            ${exam.questions.map((q, idx) => renderQuestionHTML(q, idx)).join('')}
        </div>
        <button onclick="addQuestion(this)">添加题目</button>
        <br><br>
        <button onclick="saveExam(this, ${exam.exam_id})">保存修改</button>
        <button onclick="deleteExam(${exam.exam_id})" style="background-color: #dc3545; color:white;">删除试卷</button>
    `;
    container.appendChild(div);
}

function renderQuestionHTML(q, idx) {
    const options = q.options || {};
    const optsHtml = Object.entries(options).map(([key, val]) =>
        `<div><label><input type="${q.is_multiple ? 'checkbox' : 'radio'}" disabled> ${key}: <input type="text" value="${val}" style="width: 80%;"></label></div>`
    ).join('');

    const correctAns = Array.isArray(q.correct_answer) ? q.correct_answer.join(',') : '';

    return `
        <fieldset style="border:1px solid #ddd; padding:10px; margin-bottom:10px; position:relative;">
            <legend>题目 ${idx + 1}</legend>
            <button onclick="removeQuestion(this)" style="position:absolute; right:10px; top:10px; background:#dc3545; color:#fff; border:none; cursor:pointer;">删除</button>
            <label>题干（Markdown支持）:<br>
                <textarea style="width:100%; height:80px;">${q.question_text || ''}</textarea>
            </label><br>
            <label>是否多选: <input type="checkbox" ${q.is_multiple ? 'checked' : ''}></label><br>
            <div>选项（键值对）:<br>
                <div class="options-container">
                    ${optsHtml}
                </div>
                <button onclick="addOption(this)">添加选项</button>
            </div><br>
            <label>正确答案 (用逗号分隔，如 A,B): <input type="text" value="${correctAns}" style="width: 100%;"></label>
        </fieldset>
    `;
}

function addExamForm() {
    const container = document.getElementById('examEditor');
    container.innerHTML = '';

    const div = document.createElement('div');
    div.innerHTML = `
        <hr>
        <input placeholder="试卷标题" style="width: 100%; font-size: 18px; font-weight: bold;"><br><br>
        <label>考试限时（分钟，0表示无时限）:</label>
        <input type="number" min="0" value="0"><br><br>
        <div class="questions-container">
            <h3>题目列表</h3>
        </div>
        <button onclick="addQuestion(this)">添加题目</button>
        <br><br>
        <button onclick="saveExam(this)">保存试卷</button>
    `;
    container.appendChild(div);
}

function addQuestion(btn) {
    const container = btn.parentElement.querySelector('.questions-container');
    const qHTML = `
        <fieldset style="border:1px solid #ddd; padding:10px; margin-bottom:10px; position:relative;">
            <legend>新题目</legend>
            <button onclick="removeQuestion(this)" style="position:absolute; right:10px; top:10px; background:#dc3545; color:#fff; border:none; cursor:pointer;">删除</button>
            <label>题干（Markdown支持）:<br>
                <textarea style="width:100%; height:80px;"></textarea>
            </label><br>
            <label>是否多选: <input type="checkbox"></label><br>
            <div>选项（键值对）:<br>
                <div class="options-container"></div>
                <button onclick="addOption(this)">添加选项</button>
            </div><br>
            <label>正确答案 (用逗号分隔，如 A,B): <input type="text" style="width: 100%;"></label>
        </fieldset>
    `;
    const div = document.createElement('div');
    div.innerHTML = qHTML;
    container.appendChild(div);
}

function removeQuestion(btn) {
    if(confirm('确定删除该题目吗？')){
        btn.parentElement.remove();
    }
}

function addOption(btn) {
    const container = btn.parentElement.querySelector('.options-container');
    const optionHTML = `
        <div style="margin-bottom:5px;">
            选项键（例如 A）: <input type="text" style="width: 40px;"> 
            内容: <input type="text" style="width: 70%;">
            <button onclick="removeOption(this)" style="background:#dc3545; color:#fff; border:none; cursor:pointer;">删除</button>
        </div>
    `;
    const div = document.createElement('div');
    div.innerHTML = optionHTML;
    container.appendChild(div);
}

function removeOption(btn) {
    btn.parentElement.remove();
}

function saveExam(btn, exam_id = null) {
    const parent = btn.parentElement;
    const title = parent.querySelector('input[type=text]').value.trim();
    const time_limit = parseInt(parent.querySelector('input[type=number]').value) || 0;
    const questionFields = parent.querySelectorAll('fieldset');
    if (!title) {
        alert('试卷标题不能为空');
        return;
    }
    const questions = [];

    for (const qf of questionFields) {
        const question_text = qf.querySelector('textarea').value.trim();
        const is_multiple = qf.querySelector('input[type=checkbox]').checked;
        const optionDivs = qf.querySelectorAll('.options-container > div');
        const options = {};
        for (const optDiv of optionDivs) {
            const key = optDiv.querySelector('input[type=text]:nth-child(1)').value.trim();
            const val = optDiv.querySelector('input[type=text]:nth-child(2)').value.trim();
            if (key) {
                options[key] = val;
            }
        }
        const correct_answer = qf.querySelector('input[type=text]:last-child').value.trim().split(',').map(s => s.trim()).filter(s => s);
        if (!question_text) {
            alert('题干不能为空');
            return;
        }
        if (Object.keys(options).length === 0) {
            alert('选项不能为空');
            return;
        }
        if (correct_answer.length === 0) {
            alert('正确答案不能为空');
            return;
        }
        questions.push({
            question_text,
            is_multiple,
            options,
            correct_answer
        });
    }

    const data = {
        title,
        time_limit,
        questions
    };

    if (exam_id) {
        data.exam_id = exam_id;
        axios.post('/exam/teacher/api/update_exam', data)
            .then(() => {
                alert('试卷更新成功！');
                fetchTeacherExams();
                document.getElementById('examEditor').innerHTML = '';
            })
            .catch(error => {
                alert('更新试卷失败: ' + (error.response?.data?.message || error.message));
            });
    } else {
        axios.post('/exam/teacher/api/create_exam', data)
            .then(() => {
                alert('试卷创建成功！');
                fetchTeacherExams();
                document.getElementById('examEditor').innerHTML = '';
            })
            .catch(error => {
                alert('创建试卷失败: ' + (error.response?.data?.message || error.message));
            });
    }
}

function deleteExam(exam_id) {
    if(confirm('确定删除该试卷吗？')) {
        axios.post('/exam/teacher/api/delete_exam', {exam_id})
            .then(() => {
                alert('试卷删除成功！');
                fetchTeacherExams();
                document.getElementById('examEditor').innerHTML = '';
            })
            .catch(error => {
                alert('删除试卷失败: ' + (error.response?.data?.message || error.message));
            }); 
    }
}

fetchTeacherExams();
