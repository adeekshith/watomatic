function redir_lang() {
    var userLang = navigator.language || navigator.userLanguage; 

    
    if (userLang == "de" && localStorage.getItem("redir") == null) {
        window.location.replace('/de/index.html')
    } else if (userLang == "ru" && no_redir_cookie == null) {
        window.location.replace('/ru/index.html')
    }

}


window.addEventListener("load", function() {
    redir_lang()
})

