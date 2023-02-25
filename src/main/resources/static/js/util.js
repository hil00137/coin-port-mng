function $(selector) {
    let nodeListOf = document.querySelectorAll(selector);
    if (nodeListOf.length === 0) {
        return null;
    } else if (nodeListOf.length === 1) {
        return nodeListOf[0];
    }
    return nodeListOf
}

function clear(selectors) {
    if (typeof selectors === 'string') {
      $(selectors).value = null
    } else if (Array.isArray(selectors)) {
        selectors.forEach((selector) => {
            $(selector).value = null
        })
    }
}