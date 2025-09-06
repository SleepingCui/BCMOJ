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

const MAX_TOASTS = 8;

function showToast(message, type='info'){
  console.log(`[Toast] ${message} (${type})`);
  const container = document.querySelector('.toast-container');
  const toasts = container.querySelectorAll('.toast');
  if(toasts.length >= MAX_TOASTS){
    toasts[0].style.animation = 'slideOut 0.4s forwards';
    setTimeout(()=>{ toasts[0].remove(); },400);
  }
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `<div class="toast-body">${message}</div>`;
  container.appendChild(toast);
  setTimeout(()=>{
    toast.style.animation = 'slideOut 0.4s forwards';
    setTimeout(()=>toast.remove(),400);
  },5000);
}

function loadRequiredLibs(){
  console.log('[Init] Loading required libraries...');
  return new Promise(resolve=>{
    if(typeof marked!=='undefined' && typeof MathJax!=='undefined'){ 
      console.log('[Init] Libraries already loaded.');
      resolve(); 
      return; 
    }
    let loaded=0; 
    const total=2; 
    const check=()=>{loaded++; if(loaded===total) { console.log('[Init] All libraries loaded.'); resolve(); }};
    if(typeof marked==='undefined'){ 
      let s=document.createElement('script'); 
      s.src='https://cdn.jsdelivr.net/npm/marked/marked.min.js'; 
      s.onload=check; 
      document.head.appendChild(s); 
      console.log('[Init] Loading marked.js...');
    } else check();
    if(typeof MathJax==='undefined'){ 
      let s=document.createElement('script'); 
      s.src='https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js'; 
      s.onload=check; 
      document.head.appendChild(s); 
      console.log('[Init] Loading MathJax...');
    } else check();
  });
}

function renderContent() {
    console.log('[Render] Rendering problem description...');
    const desc = document.querySelector('.description div');
    if (desc) {
        const orig = desc.innerHTML;
        try {
            desc.innerHTML = marked.parse(desc.textContent);
            if (typeof MathJax !== 'undefined') {
                MathJax.typesetPromise([desc])
                    .then(() => console.log('[Render] MathJax finished.'))
                    .catch(e => console.error('[Render] MathJax error:', e));
            }

            console.log('[Render] Rendering finished.');
        } catch (e) {
            desc.innerHTML = orig;
            console.error('[Render] Rendering error:', e);
        }
    }
}

function setupDragAndDrop(){
  console.log('[Setup] Setting up drag-and-drop...');
  document.addEventListener('dragover', e=>e.preventDefault());
  document.addEventListener('drop', e=>{
    e.preventDefault();
    console.log('[DragDrop] File dropped.');
    const fileInput=document.getElementById('fileInput');
    if(e.dataTransfer.files.length>0){
      fileInput.files=e.dataTransfer.files;
      const reader=new FileReader();
      reader.onload=e=>document.getElementById('codePasteArea').value=e.target.result;
      reader.readAsText(e.dataTransfer.files[0]);
    }
  });
}

function setupFormHandlers(){
  console.log('[Setup] Setting up form handlers...');
  document.getElementById('codePasteArea').addEventListener('input', ()=>{ 
    document.getElementById('fileInput').value=''; 
  });
  document.getElementById('fileInput').addEventListener('change', function(){
    const file=this.files[0];
    if(file){ 
      console.log(`[FileInput] File selected: ${file.name}`);
      const reader=new FileReader(); 
      reader.onload=e=>document.getElementById('codePasteArea').value=e.target.result; 
      reader.readAsText(file);
    }
  });
  document.getElementById('submitForm').addEventListener('submit', async function(e){
    e.preventDefault();
    console.log('[Submit] Submission started...');
    const fileInput=document.getElementById('fileInput');
    const codeText=document.getElementById('codePasteArea').value.trim();
    const resultArea=document.getElementById('resultArea');
    const resultsDiv=document.getElementById('results');
    resultsDiv.innerHTML='<p class="loading">Waiting...</p>';
    resultArea.style.display='block';
    window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    const formData=new FormData();
    formData.append('enableO2', document.getElementById('enableO2')?.checked || false);
    if(codeText) {
      formData.append('code', new Blob([codeText],{type:'text/x-c++src'}),'pasted_code.cpp');
      console.log('[Submit] Code from paste area added.');
    } else if(fileInput.files.length>0) {
      formData.append('code', fileInput.files[0]);
      console.log('[Submit] Code from file added.');
    } else { 
      showToast("请上传文件或粘贴代码后再提交。",'error'); 
      console.log('[Submit] No code provided.'); 
      return; 
    }

    try{
      const response=await fetch(window.location.pathname.replace('/problem/','/submit/'), {method:'POST',body:formData});
      if(response.status===401){ 
        showToast('未登录，请先登录','error'); 
        console.log('[Submit] Unauthorized, redirecting to login.');
        window.location.href='/login?next='+encodeURIComponent(window.location.href); 
        return; 
      }
      if(response.ok){
        const data=await response.json();
        console.log('[Submit] Response received:', data);
        resultsDiv.innerHTML='';
        if(data.status==='ok'){
          data.results.forEach(res=>{
            const resultText=resultMapping[res.result]||resultMapping["default"];
            const resultClass=resultColorMapping[resultText]||resultColorMapping["Unknown Status"];
            const p=document.createElement('p');
            p.innerHTML=`测试点 ${res.checkpoint}: <span class="${resultClass}">${resultText}</span> (用时 ${res.time} ms)`;
            resultsDiv.appendChild(p);
            console.log(`[Submit] Checkpoint ${res.checkpoint}: ${resultText} (${res.time}ms)`);
            showToast(`测试点 ${res.checkpoint}: ${resultText} (用时 ${res.time} ms)`, resultText==='Accepted'?'success':'error');
          });
        } else {
          showToast('评测失败: '+(data.error||'未知错误'),'error');
          console.log('[Submit] Judging failed:', data.error);
        }
      } else {
        showToast('提交失败，请稍后再试。','error');
        console.log('[Submit] Submission failed with status:', response.status);
      }
    } catch(err){ 
      resultsDiv.innerHTML='<p class="result-fail">网络错误，请检查连接后重试。</p>'; 
      console.error('[Submit] Network error:', err);
    }
  });
}

document.addEventListener('DOMContentLoaded', function(){ 
  console.log('[Init] DOM loaded.');
  loadRequiredLibs().then(renderContent); 
  setupDragAndDrop(); 
  setupFormHandlers(); 
});
