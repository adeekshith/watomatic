function set_cookie() {
    var no_redir_cookie = getCookie("redir");
    //if (window.confirm("We will set a cookie if you click ok.") && no_redir_cookie == null) {
        if (localStorage.getItem("redir") == null) {
            localStorage.setItem("redir","no_redir")
    }
}



