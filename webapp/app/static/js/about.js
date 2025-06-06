axios.get('/api/contributors')
            .then(response => {
                const container = document.getElementById('contributors');
                container.innerHTML = '';
                response.data.forEach(user => {
                    const div = document.createElement('div');
                    div.className = 'contributor';
                    div.innerHTML = `
    <a href="${user.html_url}" target="_blank">
        <img src="${user.avatar_url}" alt="${user.login}">
    </a>
    <p>${user.login}</p>
`;

                    container.appendChild(div);
                });
            })
            .catch(error => {
                document.getElementById('contributors').innerHTML = '<p>加载失败</p>';
            });