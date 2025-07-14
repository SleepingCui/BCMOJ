function renderAllMarkdown() {
  document.querySelectorAll('.markdown-content').forEach(el => {
    try {
      const md = el.textContent;
      el.innerHTML = marked.parse(md);
    } catch (err) {
      console.error('Markdown 渲染失败:', err);
    }
  });

  if (typeof MathJax !== 'undefined') {
    MathJax.typesetPromise().catch(err => console.error("MathJax 渲染失败:", err));
  }
}

document.addEventListener('DOMContentLoaded', function () {
  renderAllMarkdown();
});
