function redir_lang() {
    var userLang = navigator.language || navigator.userLanguage; 

    var no_redir_cookie = getCookie("redir");
    //if (userLang == "de" && no_redir_cookie == null) {
    //    window.location.replace('/de/index.html')
    //}
    if (no_redir_cookie == null) {
        alert("No cookie")
    } else if (getCookie("redir") =! null) {
        alert("Cookie is there")
    }


}


window.addEventListener("load", function() {
    redir_lang()
})

function set_cookie() {
    var myDate = new Date();
    myDate.setMonth(myDate.getMonth() + 12);
    document.cookie = "redir=no_redir" + ";expires=" + myDate + ";domain=loclahost;path=/"
}


function getCookie(name) {
    var dc = document.cookie;
    var prefix = name + "=";
    var begin = dc.indexOf("; " + prefix);
    if (begin == -1) {
        begin = dc.indexOf(prefix);
        if (begin != 0) return null;
    }
    else
    {
        begin += 2;
        var end = document.cookie.indexOf(";", begin);
        if (end == -1) {
        end = dc.length;
        }
    }
    // because unescape has been deprecated, replaced with decodeURI
    //return unescape(dc.substring(begin + prefix.length, end));
    return decodeURI(dc.substring(begin + prefix.length, end));
} 

function doSomething() {
    var myCookie = getCookie("no_redir");

    if (myCookie == null) {
        // do cookie doesn't exist stuff;
    }
    else {
        // do cookie exists stuff
    }
}