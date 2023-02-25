function $(selector) {
    let nodeListOf = document.querySelectorAll(selector);
    if (nodeListOf.length === 0) {
        return null;
    } else if (nodeListOf.length === 1) {
        return nodeListOf[0];
    }
    return nodeListOf
}