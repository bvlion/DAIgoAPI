# Unauthenticated API

## get /health

Monitoring

### request

Content | Parameter | Description
:--|:--|:--

```
http://127.0.0.1:8080/health
```

### response

Content | Parameter | Description
:--|:--|:--
status | ok | Result


```
{
    "status": "ok"
}
```

## get /terms_of_use

Return terms of use in html format

### request

Content | Parameter | Description
:--|:--|:--

```
http://127.0.0.1:8080/terms_of_use
```

### response

Content | Parameter | Description
:--|:--|:--
text | `<h1>test</h1>` | terms of use


```
{
    "text": "<h1 id=\"利用規約\">利用規約</h1>"
}
```

## get /privacy_policy

Return privacy policy in html format

### request

Content | Parameter | Description
:--|:--|:--

```
http://127.0.0.1:8080/privacy_policy
```

### response

Content | Parameter | Description
:--|:--|:--
text | `<h1>test</h1>` | privacy policy

```
{
    "text": "<h1 id=\"プライバシーポリシー\">プライバシーポリシー</h1>"
}
```
