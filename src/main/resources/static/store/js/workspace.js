(() => {
    document.querySelectorAll('[data-confirm]').forEach(form => {
        form.addEventListener('submit', event => {
            if (!window.confirm(form.dataset.confirm)) event.preventDefault();
        });
    });
    document.querySelectorAll('[data-table-search]').forEach(input => {
        const target = document.getElementById(input.dataset.tableSearch);
        if (!target) return;
        const rows = [...target.querySelectorAll('[data-search-row]')];
        const empty = target.querySelector('[data-search-empty]');
        const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/gi, 'd').toLocaleLowerCase('vi');
        input.addEventListener('input', () => {
            const query = normalize(input.value.trim());
            let shown = 0;
            rows.forEach(row => {
                row.hidden = !normalize(row.textContent).includes(query);
                if (!row.hidden) shown++;
            });
            if (empty) empty.hidden = shown > 0 || !query;
        });
    });
})();
