import requests



with open("docs/assets/vue.css", "w") as f:
    r = requests.get("https://cdn.jsdelivr.net/npm/docsify/lib/themes/vue.css")
    result = r.text.replace('@import url("https://fonts.googleapis.com/css?family=Roboto+Mono|Source+Sans+Pro:300,400,600");', "")
    f.write(result)

with open("docs/assets/emoji.min.js", "w") as f:
    r = requests.get("https://cdn.jsdelivr.net/npm/docsify/lib/plugins/emoji.min.js")
    f.write(r.text)


with open("docs/assets/docsify.min.js", "w") as f:
    r = requests.get("https://cdn.jsdelivr.net/npm/docsify/lib/docsify.min.js")
    f.write(r.text)


