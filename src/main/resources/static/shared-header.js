// Create a new file: shared-header.js
class SharedHeader {
    static render(showNav = false) {
        return `
            <header>
                <div class="header-content">
                    <a href="landing-page.html" class="logo">
                        <i class="ri-cloud-line"></i>
                        EventDrop
                    </a>
                    ${showNav ? `
                    <nav>
                        <ul>
                            <li><a href="landing-page.html">Home</a></li>
                            <li><a href="create.html">Create Room</a></li>
                        </ul>
                    </nav>` : ''}
                </div>
            </header>
        `;
    }
}

// Auto-inject header on page load
document.addEventListener('DOMContentLoaded', function() {
    const headerContainer = document.getElementById('header-placeholder');
    if (headerContainer) {
        const showNav = headerContainer.dataset.showNav === 'true';
        headerContainer.innerHTML = SharedHeader.render(showNav);
    }
});


/* Then in each HTML file, replace the header with: */
//<!-- Landing page: -->
//<div id="header-placeholder" data-show-nav="true"></div>
//
//<!-- Create/Join page: -->
//<div id="header-placeholder" data-show-nav="false"></div>
//
//<!-- Rooms page: -->
//<div id="header-placeholder" data-show-nav="false"></div>
//
///* And include these in each HTML head: */
//<link rel="stylesheet" href="shared-styles.css">
//<script src="shared-header.js"></script>