function set_cookie() {
    if (window.confirm("We will set a cookie if you click ok.")) {
        var myDate = new Date();
        myDate.setMonth(myDate.getMonth() + 12);
        document.cookie = "redir=no_redir" + ";expires=" + myDate + ";domain=localhost;path=/"
    }
}
