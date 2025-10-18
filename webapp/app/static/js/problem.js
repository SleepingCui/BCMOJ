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

function showToast(message, type='info'){
  console.log(`[Toast] type=${type}, message=${message}`);
  const container = document.querySelector('.toast-container');
  const toasts = container.querySelectorAll('.toast');
  if(toasts.length >= MAX_TOASTS){
    console.log(`[Toast] Max toasts reached, removing oldest`);
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
  console.log('[Libs] Checking required libraries...');
  return new Promise(resolve=>{
    if(typeof marked!=='undefined' && typeof renderMathInElement!=='undefined'){ 
      console.log('[Libs] marked & KaTeX already loaded');
      resolve(); 
      return; 
    }
    let loaded=0; const total=2;
    const check=()=>{loaded++; console.log(`[LoadLibs] loaded ${loaded}/${total}`); if(loaded===total) resolve();}
    if(typeof marked==='undefined'){ 
      console.log('[Libs] Loading marked.js'); 
      let s=document.createElement('script'); 
      s.src='https://cdn.jsdelivr.net/npm/marked/marked.min.js'; 
      s.onload=check; 
      document.head.appendChild(s); 
    } else check();
    if(typeof renderMathInElement==='undefined'){ 
      console.log('[Libs] Loading KaTeX'); 
      let s=document.createElement('script'); 
      s.src='https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/contrib/auto-render.min.js'; 
      s.onload=check; 
      document.head.appendChild(s); 
    } else check();
  });
}

function renderContent() {
  console.log('[Render] Rendering content...');
  const desc = document.querySelector('.description div');
  if (desc) {
      const orig = desc.innerHTML;
      try {
          desc.innerHTML = marked.parse(desc.textContent);
          console.log('[Render] Markdown parsed');

          if (typeof renderMathInElement !== 'undefined') {
              renderMathInElement(desc, {
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
          desc.innerHTML = orig;
          console.error('[Render] Rendering error:', e);
      }
  } else {
      console.log('[Render] No description div found');
  }
}

function setupDragAndDrop(){
  console.log('[DragDrop] Setting up drag & drop');
  document.addEventListener('dragover', e=>{ e.preventDefault(); });
  document.addEventListener('drop', e=>{
    e.preventDefault();
    console.log('[DragDrop] Files dropped:', e.dataTransfer.files);
    const fileInput=document.getElementById('fileInput');
    if(e.dataTransfer.files.length>0){
      fileInput.files=e.dataTransfer.files;
      const reader=new FileReader();
      reader.onload=e=>{
        console.log('[DragDrop] File content loaded');
        document.getElementById('codePasteArea').value=e.target.result;
      };
      reader.readAsText(e.dataTransfer.files[0]);
    }
  });
}

function setupFormHandlers(){
  console.log('[Form] Setting up form handlers');
  document.getElementById('codePasteArea').addEventListener('input', ()=>{ 
    document.getElementById('fileInput').value=''; 
    console.log('[Form] Code pasted, cleared file input');
  });

  document.getElementById('fileInput').addEventListener('change', function(){
    const file=this.files[0];
    console.log('[Form] File input changed:', file?.name);
    if(file){ 
      const reader=new FileReader(); 
      reader.onload=e=>{
        console.log('[Form] File content loaded into textarea');
        document.getElementById('codePasteArea').value=e.target.result; 
      };
      reader.readAsText(file);
    }
  });

  document.getElementById('submitForm').addEventListener('submit', async function(e){
    e.preventDefault();
    console.log('[Submit] Form submitted');
    const fileInput=document.getElementById('fileInput');
    const codeText=document.getElementById('codePasteArea').value.trim();
    const resultArea=document.getElementById('resultArea');
    const resultsDiv=document.getElementById('results');
    resultsDiv.innerHTML='<p class="loading">Waiting...</p>';
    resultArea.style.display='block';
    window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    console.log('[Submit] Preparing FormData');

    const formData=new FormData();
    formData.append('enableO2', document.getElementById('enableO2')?.checked || false);

    if(codeText) {
      formData.append('code', new Blob([codeText],{type:'text/x-c++src'}),'pasted_code.cpp');
      console.log('[Submit] Code appended from textarea');
    } else if(fileInput.files.length>0) {
      formData.append('code', fileInput.files[0]);
      console.log('[Submit] Code appended from file input');
    } else { 
      const msg = "请上传文件或粘贴代码后再提交。";
      showToast(msg,'error'); 
      resultsDiv.innerHTML=`<p class="result-fail">${msg}</p>`;
      console.log('[Submit] No code provided, aborting');
      return; 
    }

    try{
      console.log('[Submit] Sending fetch request...');
      const response=await fetch(window.location.pathname.replace('/problem/','/submit/'), {method:'POST',body:formData});
      
      if(response.status===401){ 
        const msg = '未登录，请先登录';
        showToast(msg,'error'); 
        resultsDiv.innerHTML=`<p class="result-fail">${msg}</p>`;
        console.log('[Submit] Unauthorized, redirecting to login');
        window.location.href='/login?next='+encodeURIComponent(window.location.href); 
        return; 
      }

      if(response.ok){
        console.log('[Submit] Response OK');
        const data=await response.json();
        console.log('[Submit] Response JSON:', data);
        resultsDiv.innerHTML='';

        if(data.status==='ok'){
          console.log('[Submit] Submission results:', data.results);
          data.results.forEach(res=>{
            const resultText=resultMapping[res.result]||resultMapping["default"];
            const resultClass=resultColorMapping[resultText]||resultColorMapping["Unknown Status"];
            const p=document.createElement('p');
            p.innerHTML=`测试点 ${res.checkpoint}: <span class="${resultClass}">${resultText}</span> (Time used ${res.time} ms, Mem used ${res.mem || 0} KB)`;

            resultsDiv.appendChild(p);
            console.log(`[Submit] Checkpoint ${res.checkpoint}: ${resultText}, ${res.time}ms`);
            showToast(`测试点 ${res.checkpoint}: ${resultText} (Time used ${res.time} ms, Mem used ${res.mem || 0} KB)`, resultText==='Accepted'?'success':'error');

          });
        } else {
          const msg = '评测失败: '+(data.error||'未知错误');
          showToast(msg,'error');
          resultsDiv.innerHTML=`<p class="result-fail">${msg}</p>`;
          console.log('[Submit] Evaluation failed:', data.error);
        }
      } else {
        const msg = '提交失败，请稍后再试。';
        showToast(msg,'error');
        resultsDiv.innerHTML=`<p class="result-fail">${msg}</p>`;
        console.log('[Submit] Fetch response not OK, status=', response.status);
      }
    } catch(err){ 
      const msg = '网络错误，请检查连接后重试。';
      resultsDiv.innerHTML=`<p class="result-fail">${msg}</p>`; 
      showToast(msg,'error');
      console.error('[Submit] Network error:', err);
    }
  });
}

document.addEventListener('DOMContentLoaded', function(){ 
  console.log('[DOM] DOM Loaded');
  loadRequiredLibs().then(renderContent); 
  setupDragAndDrop(); 
  setupFormHandlers(); 
});