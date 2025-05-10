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

  document.getElementById('codePasteArea').addEventListener('input', () => {
    const fileInput = document.getElementById('fileInput');
    fileInput.value = '';
  });

  document.getElementById('fileInput').addEventListener('change', function () {
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
  });