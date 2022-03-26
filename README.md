# D◯I 語 API

API 4 D◯I 語 app

- Android
- iOS

## FW

- [Ktor](https://ktor.io/)

## Environment

### Install

- IntelliJ
- firebase ( npm install -g firebase-tools )

### Execution

```
firebase emulators:exec "./gradlew run" --only firestore
```

### Test

```
firebase emulators:exec "./gradlew clean test" --only firestore
```

## EndPoint

- [Authenticated API](/doc/AuthenticatedAPI.md)
- [Unauthenticated API](/doc/UnauthenticatedAPI.md)

## Test result

Push to main and GitHub Actions will upload the test results to [GitHub Pages](https://bvlion.github.io/DAIgoAPI/index.html)
