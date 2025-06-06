function searchResults() {
            const searchInput = document.getElementById('searchInput').value.toLowerCase();
            const rows = document.getElementById('resultTable').getElementsByTagName('tr');

            for (let i = 1; i < rows.length; i++) {
                let cells = rows[i].getElementsByTagName('td');
                let match = false;

                for (let j = 0; j < cells.length; j++) {
                    let cellText = cells[j].textContent || cells[j].innerText;
                    if (cellText.toLowerCase().includes(searchInput)) {
                        match = true;
                    }
                }

                if (match) {
                    rows[i].style.display = '';
                } else {
                    rows[i].style.display = 'none';
                }
            }
        }