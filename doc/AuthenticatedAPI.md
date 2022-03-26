# Authenticated API

## get /get-dai-go

Create D◯I 語 

### request

Content | Parameter | Description
:--|:--|:--
target | 努力大事 | target text

```
http://127.0.0.1:8080/get-dai-go?target=%E5%8A%AA%E5%8A%9B%E5%A4%A7%E4%BA%8B
```

### response

Content | Parameter | Description
:--|:--|:--
text | DD | Result

```
{
    "text": "DD"
}
```

## post /upsert-dai-go

Register the D◯I 語 that couldn't be handled by automatic generation

### request

Content | Parameter | Description
:--|:--|:--
word | 大好物 | source word
dai_go | DKB | save word

```
http://127.0.0.1:8080/upsert-dai-go -d '
    {"word": "大好物", "dai_go": "DKB"}
'
```

### response

Content | Parameter | Description
:--|:--|:--
save | success | Save Result

```
{
  "save" : "success"
}
```

## get /get-samples

Get sample D◯I 語 words

### request

Content | Parameter | Description
:--|:--|:--

```
http://127.0.0.1:8080/get-samples
```

### response

Content | Parameter | Description
:--|:--|:--
samples | 努力大事 | List

```
{
    "samples": [
        "努力大事",
        "大好物"
    ]
}
```

## post /update-samples

Update the sample D◯I 語 words

### request

Content | Parameter | Description
:--|:--|:--

```
http://127.0.0.1:8080/update-samples
```

### response

Content | Parameter | Description
:--|:--|:--
samples | 努力大事 | List

```
{
    "samples": [
        "努力大事",
        "大好物"
    ]
}
```