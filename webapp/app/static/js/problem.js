const resultMapping = {
  "-5": "Security Check Failed",
  "-4": "Compile Error",
  "-3": "Wrong Answer",
  "2": "Real Time Limit Exceeded",
  "3": "Memory Limit Exceeded",
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
  "Memory Limit Exceeded": "result-fail",
  "Runtime Error": "result-fail",
  "System Error": "result-fail",
  "Unknown Status": "result-default"
};

const MAX_TOASTS = 8;

function showToast(message, type = 'info') {
  console.log(`[Toast] type=${type}, message=${message}`);
  const $container = $('.toast-container');
  const $toasts = $container.find('.toast');
  if ($toasts.length >= MAX_TOASTS) {
    console.log(`[Toast] Max toasts reached, removing oldest`);
    $toasts.first().css('animation', 'slideOut 0.4s forwards');
    setTimeout(() => {
      $toasts.first().remove();
    }, 400);
  }
  const $toast = $(`<div class="toast toast-${type}"><div class="toast-body">${message}</div></div>`);
  $container.append($toast);
  setTimeout(() => {
    $toast.css('animation', 'slideOut 0.4s forwards');
    setTimeout(() => $toast.remove(), 400);
  }, 5000); 
}

function loadRequiredLibs() {
  console.log('[Libs] Checking required libraries...');
  return new Promise(resolve => {
    if (typeof marked !== 'undefined' && typeof renderMathInElement !== 'undefined') {
      console.log('[Libs] marked & KaTeX already loaded');
      resolve();
      return;
    }
    let loaded = 0;
    const total = 2;
    const check = () => {
      loaded++;
      console.log(`[LoadLibs] loaded ${loaded}/${total}`);
      if (loaded === total) resolve();
    };
    if (typeof marked === 'undefined') {
      console.log('[Libs] Loading marked.js');
      let s = document.createElement('script');
      s.src = 'https://cdn.jsdelivr.net/npm/marked/marked.min.js';
      s.onload = check;
      document.head.appendChild(s);
    } else check();
    if (typeof renderMathInElement === 'undefined') {
      console.log('[Libs] Loading KaTeX auto-render');
      let s = document.createElement('script');
      s.src = 'https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/contrib/auto-render.min.js';
      s.onload = check;
      document.head.appendChild(s);
    } else check();
  });
}


function renderContent() {
  console.log('[Render] Rendering content...');
  const $desc = $('.description div');
  if ($desc.length) { 
    const orig = $desc.html();
    try {
      $desc.html(marked.parse($desc.text()));
      console.log('[Render] Markdown parsed');

      if (typeof renderMathInElement !== 'undefined') {
        renderMathInElement($desc[0], {
          delimiters: [
            { left: "$$", right: "$$", display: true },
            { left: "$", right: "$", display: false },
            { left: "\\(", right: "\\)", display: false },
            { left: "\\[", right: "\\]", display: true }
          ],
          throwOnError: false
        });
        console.log('[Render] KaTeX render complete');
      }
    } catch (e) {
      $desc.html(orig); 
      console.error('[Render] Rendering error:', e);
    }
  } else {
    console.log('[Render] No description div found');
  }
}

function setupDragAndDrop() {
  console.log('[DragDrop] Setting up drag & drop');
  $(document).on('dragover', function (e) {
    e.preventDefault();
  });

  $(document).on('drop', function (e) {
    e.preventDefault(); 
    console.log('[DragDrop] Files dropped:', e.originalEvent.dataTransfer.files);
    const $fileInput = $('#fileInput');
    const files = e.originalEvent.dataTransfer.files;
    if (files.length > 0) {

      const reader = new FileReader();
      reader.onload = function (readerEvent) {
        console.log('[DragDrop] File content loaded');
        $('#codePasteArea').val(readerEvent.target.result);
      };
      reader.readAsText(files[0]);
    }
  });
}

function setupFormHandlers() {
  console.log('[Form] Setting up form handlers');

  $('#codePasteArea').on('input', function () {
    $('#fileInput').val('');
    console.log('[Form] Code pasted, cleared file input');
  });


  $('#fileInput').on('change', function () {
    const file = this.files[0]; 
    console.log('[Form] File input changed:', file?.name);
    if (file) {
      const reader = new FileReader();
      reader.onload = function (readerEvent) {
        console.log('[Form] File content loaded into textarea');
        $('#codePasteArea').val(readerEvent.target.result);
      };
      reader.readAsText(file);
    }
  });

  $('#submitForm').on('submit', async function (e) {
    e.preventDefault(); 
    console.log('[Submit] Form submitted');

    const codeText = $('#codePasteArea').val().trim();
    const $fileInput = $('#fileInput');
    const $resultArea = $('#resultArea');
    const $resultsDiv = $('#results');

    $resultsDiv.html('<p class="loading">Waiting...</p>');
    $resultArea.show();
    $('html, body').animate({ scrollTop: $(document).height() }, 'smooth');

    const formData = new FormData();
    formData.append('enableO2', $('#enableO2').is(':checked'));
    if (codeText) {
      formData.append('code', new Blob([codeText], { type: 'text/x-c++src' }), 'pasted_code.cpp');
      console.log('[Submit] Code appended from textarea');
    } else if ($fileInput[0].files.length > 0) {
      formData.append('code', $fileInput[0].files[0]);
      console.log('[Submit] Code appended from file input');
    } else {
      const msg = "请上传文件或粘贴代码后再提交。";
      showToast(msg, 'error');
      $resultsDiv.html(`<p class="result-fail">${msg}</p>`);
      console.log('[Submit] No code provided, aborting');
      return;
    }

    try {
      console.log('[Submit] Sending fetch request...');
      const response = await fetch(window.location.pathname.replace('/problem/', '/submit/'), {
        method: 'POST',
        body: formData
      });

      if (response.status === 401) {
        const msg = '未登录，请先登录';
        showToast(msg, 'error');
        $resultsDiv.html(`<p class="result-fail">${msg}</p>`);
        console.log('[Submit] Unauthorized, redirecting to login');
        window.location.href = '/login?next=' + encodeURIComponent(window.location.href);
        return;
      }

      if (response.ok) {
        console.log('[Submit] Response OK');
        const data = await response.json();
        console.log('[Submit] Response JSON:', data);
        $resultsDiv.empty();

        if (data.status === 'ok') {
          console.log('[Submit] Submission results:', data.results);
          data.results.forEach(res => {
            const resultText = resultMapping[res.result] || resultMapping["default"];
            const resultClass = resultColorMapping[resultText] || resultColorMapping["Unknown Status"];
            const $p = $(`<p>测试点 ${res.checkpoint}: <span class="${resultClass}">${resultText}</span> (Time used ${res.time} ms, Mem used ${res.mem || 0} KB)</p>`);
            $resultsDiv.append($p);
            console.log(`[Submit] Checkpoint ${res.checkpoint}: ${resultText}, ${res.time}ms`);
            showToast(`测试点 ${res.checkpoint}: ${resultText} (Time used ${res.time} ms, Mem used ${res.mem || 0} KB)`, resultText === 'Accepted' ? 'success' : 'error');
          });
        } else {
          const msg = '评测失败: ' + (data.error || '未知错误');
          showToast(msg, 'error');
          $resultsDiv.html(`<p class="result-fail">${msg}</p>`);
          console.log('[Submit] Evaluation failed:', data.error);
        }
      } else { 
        const msg = '提交失败，请稍后再试。';
        showToast(msg, 'error');
        $resultsDiv.html(`<p class="result-fail">${msg}</p>`);
        console.log('[Submit] Fetch response not OK, status=', response.status);
      }
    } catch (err) {
      const msg = '网络错误，请检查连接后重试。';
      $resultsDiv.html(`<p class="result-fail">${msg}</p>`);
      showToast(msg, 'error');
      console.error('[Submit] Network error:', err);
    }
  });
}

$(document).ready(function () {
  console.log('[DOM] DOM Loaded');
  loadRequiredLibs().then(renderContent);
  setupDragAndDrop();
  setupFormHandlers();
});