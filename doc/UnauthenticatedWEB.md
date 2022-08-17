# Unauthenticated WEB

## get /view/terms_of_use

Return terms of use html

### request

Content | Parameter | Description
:--|:--|:--

```
http://127.0.0.1:8080/view/terms_of_use
```

### response

Content | Parameter | Description
:--|:--|:--
html | `<!DOCTYPE HTML>` | terms of use


```
<!DOCTYPE HTML>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>利用規約</title>
  <link href="https://cdnjs.cloudflare.com/ajax/libs/github-markdown-css/4.0.0/github-markdown.min.css" rel="stylesheet" type="text/css" media="all"/>
  <link href="/default.css" rel="stylesheet" type="text/css" media="all"/>
  <link rel="icon" href="/favicon-192x192.png" sizes="192x192" type="image/png">
  <link rel="icon" href="/favicon.ico">
</head>
<body>
<div class="container main">
  <div class="markdown-body"><h1 id="土台だけ用意">土台だけ用意</h1>
<p>利用規約</p>
</div>
</div>
</body>
</html>
```

## get /view/privacy_policy

Return privacy policy html

### request

Content | Parameter | Description
:--|:--|:--

```
http://127.0.0.1:8080/view/privacy_policy
```

### response

Content | Parameter | Description
:--|:--|:--
html | `<!DOCTYPE HTML>` | privacy policy

```
<!DOCTYPE HTML>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>プライバシーポリシー</title>
  <link href="https://cdnjs.cloudflare.com/ajax/libs/github-markdown-css/4.0.0/github-markdown.min.css" rel="stylesheet" type="text/css" media="all"/>
  <link href="/default.css" rel="stylesheet" type="text/css" media="all"/>
  <link rel="icon" href="/favicon-192x192.png" sizes="192x192" type="image/png">
  <link rel="icon" href="/favicon.ico">
</head>
<body>
<div class="container main">
  <div class="markdown-body"><h1 id="土台だけ用意">土台だけ用意</h1>
<p>プライバシーポリシー</p>
</div>
</div>
</body>
</html>
```

## get /app/rules

Return rules html for app

### request

Content | Parameter | Description
:--|:--|:--
backColor | #FFFFFF | html's background color
textColor | #000000 | html's text color
isPrivacyPolicy | true | if true show PrivacyPolicy else TermsOfUse

```
http://127.0.0.1:8080/app/rules?backColor=%23FFFFFF&textColor=%23000000&isPrivacyPolicy=true
```

### response

Content | Parameter | Description
:--|:--|:--
html | `<!DOCTYPE HTML>` | rules

```
<!DOCTYPE HTML>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link href="https://cdnjs.cloudflare.com/ajax/libs/github-markdown-css/4.0.0/github-markdown.min.css" rel="stylesheet" type="text/css" media="all" />
  <style>.small { font-size: 70%% !important;  color: #000000; }</style>
</head>
<body style="background-color: #FFFFFF;">
<div class="container main">
  <div class="markdown-body"><h1 id="土台だけ用意">土台だけ用意</h1>
<p>プライバシーポリシー</p>
</div>
</div>
</body>
</html>
```
