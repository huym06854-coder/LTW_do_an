(() => {
    const sidebar = document.getElementById('adminSidebar');
    const toggle = document.getElementById('adminSidebarToggle');

    if (sidebar && toggle) {
        const closeSidebar = () => {
            sidebar.classList.remove('mobile-open');
            toggle.setAttribute('aria-expanded', 'false');
        };
        toggle.addEventListener('click', () => {
            const isOpen = sidebar.classList.toggle('mobile-open');
            toggle.setAttribute('aria-expanded', String(isOpen));
        });

        document.addEventListener('click', (event) => {
            if (window.innerWidth <= 650 && sidebar.classList.contains('mobile-open')
                && !sidebar.contains(event.target) && !toggle.contains(event.target)) {
                closeSidebar();
            }
        });
        document.addEventListener('keydown', event => {
            if (event.key === 'Escape' && sidebar.classList.contains('mobile-open')) {
                closeSidebar();
                toggle.focus();
            }
        });
        window.addEventListener('resize', () => {
            if (window.innerWidth > 650) closeSidebar();
        });
    }

    const today = document.getElementById('adminToday');
    if (today) {
        today.textContent = new Intl.DateTimeFormat('vi-VN', {
            weekday: 'short', day: '2-digit', month: '2-digit', year: 'numeric'
        }).format(new Date());
    }
})();
