/**
 * Trigger Chain View - Client-side interaction for expanding/collapsing tree nodes.
 */
var TriggerChain = {

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
        var container = document.querySelector('.trigger-chain-tree');
        if (!container) return;

        var buttons = container.querySelectorAll('.trigger-chain-expand-btn');
        var subtrees = container.querySelectorAll('.trigger-chain-list');
        var toggleBtn = document.querySelector('.trigger-chain-toggle-all');

        // Check if any are collapsed
        var anyCollapsed = false;
        for (var i = 0; i < subtrees.length; i++) {
            if (subtrees[i].classList.contains('collapsed')) {
                anyCollapsed = true;
                break;
            }
        }

        if (anyCollapsed) {
            // Expand all
            for (var i = 0; i < subtrees.length; i++) {
                subtrees[i].classList.remove('collapsed');
            }
            for (var i = 0; i < buttons.length; i++) {
                buttons[i].classList.add('expanded');
            }
            if (toggleBtn) toggleBtn.textContent = 'Collapse All';
        } else {
            // Collapse all
            for (var i = 0; i < subtrees.length; i++) {
                subtrees[i].classList.add('collapsed');
            }
            for (var i = 0; i < buttons.length; i++) {
                buttons[i].classList.remove('expanded');
            }
            if (toggleBtn) toggleBtn.textContent = 'Expand All';
        }
    }
};

// Auto-expand all nodes on page load
document.addEventListener('DOMContentLoaded', function() {
    var buttons = document.querySelectorAll('.trigger-chain-expand-btn');
    for (var i = 0; i < buttons.length; i++) {
        buttons[i].classList.add('expanded');
    }
    var toggleBtn = document.querySelector('.trigger-chain-toggle-all');
    if (toggleBtn) toggleBtn.textContent = 'Collapse All';
});
