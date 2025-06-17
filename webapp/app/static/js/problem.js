const resultMapping = {
  "-5": "Security Check Failed",
  "-4": "Compile Error",
  "-3": "Wrong Answer",
  "2": "Real Time Limit Exceeded",
  "4": "Runtime Error",
  "5": "System Error",
  "1": "Accepted",
  "default": "Unknown Status"
};

const resultColorMapping = {
  "Accepted": "result-pass",
  "Security Check Failed": "result-fail",
  "Compile Error": "result-fail",
  "Wrong Answer": "result-fail",
  "Real Time Limit Exceeded": "result-fail",
  "Runtime Error": "result-fail",
  "System Error": "result-fail",
  "Unknown Status": "result-default"
};


function loadRequiredLibs() {
  return new Promise((resolve) => {
    if (typeof marked !== 'undefined' && typeof MathJax !== 'undefined') {
      resolve();
      return;
    }

    let loadedCount = 0;
    const totalToLoad = 2;

    function checkAllLoaded() {
      loadedCount++;
      if (loadedCount === totalToLoad) {
        resolve();
      }
    }

    if (typeof marked === 'undefined') {
      const markedScript = document.createElement('script');
      markedScript.src = 'https://cdn.jsdelivr.net/npm/marked/marked.min.js';
      markedScript.onload = checkAllLoaded;
      document.head.appendChild(markedScript);
    } else {
      checkAllLoaded();
    }

    if (typeof MathJax === 'undefined') {
      const mathjaxScript = document.createElement('script');
      mathjaxScript.src = 'https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js';
      mathjaxScript.onload = checkAllLoaded;
      document.head.appendChild(mathjaxScript);
    } else {
      checkAllLoaded();
    }
  });
}

function renderContent() {
  const descriptionDiv = document.querySelector('.description div');
  if (descriptionDiv) {
    const originalContent = descriptionDiv.innerHTML;
    
    try {
      const markdownContent = descriptionDiv.textContent;
      descriptionDiv.innerHTML = marked.parse(markdownContent);
      if (typeof MathJax !== 'undefined') {
        MathJax.typesetPromise().catch((err) => {
          console.error('MathJax渲染错误:', err);
        });
      }
    } catch (e) {
      console.error('内容解析失败:', e);
      descriptionDiv.innerHTML = originalContent;
    }
  }
}

function setupDragAndDrop() {
  document.addEventListener('dragover', e => e.preventDefault());
  document.addEventListener('drop', e => {
    e.preventDefault();
    const fileInput = document.getElementById('fileInput');
    if (e.dataTransfer.files.length > 0) {
      fileInput.files = e.dataTransfer.files;
      const reader = new FileReader();
      reader.onload = function(e) {
        document.getElementById('codePasteArea').value = e.target.result;
      };
      reader.readAsText(e.dataTransfer.files[0]);
    }
  });
}

function setupFormHandlers() {
  document.getElementById('codePasteArea').addEventListener('input', () => {
    const fileInput = document.getElementById('fileInput');
    fileInput.value = '';
  });

  document.getElementById('fileInput').addEventListener('change', function() {
    const file = this.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = function(e) {
        document.getElementById('codePasteArea').value = e.target.result;
      };
      reader.readAsText(file);
    }
  });

  document.getElementById('submitForm').addEventListener('submit', async function(event) {
    event.preventDefault();
    const fileInput = document.getElementById('fileInput');
    const codeText = document.getElementById('codePasteArea').value.trim();
    const resultArea = document.getElementById('resultArea');
    const resultsDiv = document.getElementById('results');

    resultsDiv.innerHTML = '<p class="loading">Waiting...</p>';
    resultArea.style.display = 'block';
    window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });

    const formData = new FormData();

    if (codeText) {
      const blob = new Blob([codeText], { type: 'text/x-c++src' });
      formData.append('code', blob, 'pasted_code.cpp');
    } else if (fileInput.files.length > 0) {
      formData.append('code', fileInput.files[0]);
    } else {
      alert("请上传文件或粘贴代码后再提交。");
      return;
    }

    try {
      const response = await fetch(window.location.pathname.replace('/problem/', '/submit/'), {
        method: 'POST',
        body: formData
      });

      if (response.status === 401) {
        alert('未登录，请先登录');
        window.location.href = '/login?next=' + encodeURIComponent(window.location.href);
        return;
      }

      if (response.ok) {
        const data = await response.json();
        resultsDiv.innerHTML = '';
        if (data.status === 'ok') {
          data.results.forEach(res => {
            const p = document.createElement('p');
            const resultText = resultMapping[res.result] || resultMapping["default"];
            const resultClass = resultColorMapping[resultText] || resultColorMapping["Unknown Status"];
            p.innerHTML = `测试点 ${res.checkpoint}: <span class="${resultClass}">${resultText}</span> (用时 ${res.time} ms)`;
            resultsDiv.appendChild(p);
          });
        } else {
          resultsDiv.innerHTML = '<p class="result-fail">评测失败: ' + (data.error || '未知错误') + '</p>';
        }
      } else {
        resultsDiv.innerHTML = '<p class="result-fail">提交失败，请稍后再试。</p>';
      }
    } catch (error) {
      resultsDiv.innerHTML = '<p class="result-fail">网络错误，请检查连接后重试。</p>';
      console.error('提交出错:', error);
    }
  });
}


document.addEventListener('DOMContentLoaded', function() {
  loadRequiredLibs().then(() => {
    renderContent();
  });
  setupDragAndDrop();
  setupFormHandlers();
});