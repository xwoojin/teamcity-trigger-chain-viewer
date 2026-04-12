/**
 * Trigger Chain View - Client-side interaction for expanding/collapsing tree nodes
 * and seamless auto-refresh via AJAX.
 */
var TriggerChain = {

    _refreshTimer: null,
    _refreshInterval: 3000,

    /**
     * Toggle a single node's children visibility.
     */
    toggle: function(button) {
        var item = button.closest('.trigger-chain-item');
        if (!item) return;

        var subtree = item.querySelector(':scope > .trigger-chain-list');
        if (!subtree) return;

        var isExpanded = button.classList.contains('expanded');
        if (isExpanded) {
            subtree.classList.add('collapsed');
            button.classList.remove('expanded');
        } else {
            subtree.classList.remove('collapsed');
            button.classList.add('expanded');
        }
    },

    /**
     * Toggle all nodes: expand all if any are collapsed, collapse all if all are expanded.
     */
    toggleAll: function() {
        var containers = document.querySelectorAll('.trigger-chain-tree');
        if (!containers.length) return;

        var buttons = document.querySelectorAll('.trigger-chain-expand-btn');
        var subtrees = document.querySelectorAll('.trigger-chain-list');
        var toggleBtn = document.querySelector('.trigger-chain-toggle-all');

        var anyCollapsed = false;
        for (var i = 0; i < subtrees.length; i++) {
            if (subtrees[i].classList.contains('collapsed')) {
                anyCollapsed = true;
                break;
            }
        }

        if (anyCollapsed) {
            for (var i = 0; i < subtrees.length; i++) {
                subtrees[i].classList.remove('collapsed');
            }
            for (var i = 0; i < buttons.length; i++) {
                buttons[i].classList.add('expanded');
            }
            if (toggleBtn) toggleBtn.textContent = 'Collapse All';
        } else {
            for (var i = 0; i < subtrees.length; i++) {
                subtrees[i].classList.add('collapsed');
            }
            for (var i = 0; i < buttons.length; i++) {
                buttons[i].classList.remove('expanded');
            }
            if (toggleBtn) toggleBtn.textContent = 'Expand All';
        }
    },

    /**
     * Save the current expand/collapse state of all nodes.
     */
    _saveExpandState: function() {
        var state = {};
        var nodes = document.querySelectorAll('.trigger-chain-node[data-buildtype-id]');
        for (var i = 0; i < nodes.length; i++) {
            var id = nodes[i].getAttribute('data-buildtype-id');
            var btn = nodes[i].querySelector('.trigger-chain-expand-btn');
            if (btn) {
                state[id] = btn.classList.contains('expanded');
            }
        }
        return state;
    },

    /**
     * Restore the expand/collapse state after DOM update.
     */
    _restoreExpandState: function(state) {
        var nodes = document.querySelectorAll('.trigger-chain-node[data-buildtype-id]');
        for (var i = 0; i < nodes.length; i++) {
            var id = nodes[i].getAttribute('data-buildtype-id');
            var btn = nodes[i].querySelector('.trigger-chain-expand-btn');
            if (!btn) continue;

            var item = nodes[i].closest('.trigger-chain-item');
            if (!item) continue;
            var subtree = item.querySelector(':scope > .trigger-chain-list');

            if (id in state) {
                if (state[id]) {
                    btn.classList.add('expanded');
                    if (subtree) subtree.classList.remove('collapsed');
                } else {
                    btn.classList.remove('expanded');
                    if (subtree) subtree.classList.add('collapsed');
                }
            } else {
                // New node — default expanded
                btn.classList.add('expanded');
                if (subtree) subtree.classList.remove('collapsed');
            }
        }
    },

    /**
     * Check if there are active builds that need refreshing.
     */
    _hasActiveBuilds: function() {
        return document.querySelectorAll(
            '.trigger-chain-status-running, .trigger-chain-status-queued, .trigger-chain-status-pending'
        ).length > 0;
    },

    /**
     * Seamlessly refresh only the trigger chain content via AJAX.
     */
    _doRefresh: function() {
        var container = document.querySelector('.trigger-chain-container');
        if (!container) return;

        var xhr = new XMLHttpRequest();
        xhr.open('GET', window.location.href, true);
        xhr.onreadystatechange = function() {
            if (xhr.readyState !== 4) return;
            if (xhr.status !== 200) return;

            // Parse the response and extract the new trigger chain content
            var parser = new DOMParser();
            var doc = parser.parseFromString(xhr.responseText, 'text/html');
            var newContainer = doc.querySelector('.trigger-chain-container');
            if (!newContainer) return;

            // Save current expand/collapse state
            var expandState = TriggerChain._saveExpandState();

            // Replace content
            container.innerHTML = newContainer.innerHTML;

            // Restore expand/collapse state
            TriggerChain._restoreExpandState(expandState);

            // Update toggle button text
            var toggleBtn = document.querySelector('.trigger-chain-toggle-all');
            if (toggleBtn) {
                var anyCollapsed = document.querySelectorAll('.trigger-chain-list.collapsed').length > 0;
                toggleBtn.textContent = anyCollapsed ? 'Expand All' : 'Collapse All';
            }

            // Check if we should continue refreshing
            if (!TriggerChain._hasActiveBuilds()) {
                TriggerChain.stopAutoRefresh();
            }
        };
        xhr.send();
    },

    /**
     * Start auto-refresh if there are active builds.
     */
    startAutoRefresh: function() {
        if (TriggerChain._hasActiveBuilds() && !TriggerChain._refreshTimer) {
            TriggerChain._refreshTimer = setInterval(function() {
                TriggerChain._doRefresh();
            }, TriggerChain._refreshInterval);
        }
    },

    /**
     * Stop auto-refresh.
     */
    stopAutoRefresh: function() {
        if (TriggerChain._refreshTimer) {
            clearInterval(TriggerChain._refreshTimer);
            TriggerChain._refreshTimer = null;
        }
    }
};

// Auto-expand all nodes on page load and start auto-refresh
document.addEventListener('DOMContentLoaded', function() {
    var buttons = document.querySelectorAll('.trigger-chain-expand-btn');
    for (var i = 0; i < buttons.length; i++) {
        buttons[i].classList.add('expanded');
    }
    var toggleBtn = document.querySelector('.trigger-chain-toggle-all');
    if (toggleBtn) toggleBtn.textContent = 'Collapse All';

    TriggerChain.startAutoRefresh();
});
