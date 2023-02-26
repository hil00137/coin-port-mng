document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll("script[type='text/x-handlebars-template']").forEach(element => {
        let html = element.innerHTML;
        element.innerHTML = html.replaceAll("{", "{{").replaceAll("}", "}}");
    })
})

function handleBarsCompile(selector, params = {}) {
    let source = $(selector).innerHTML;
    let template = Handlebars.compile(source);
    return template(params)
}