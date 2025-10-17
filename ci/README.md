# CI Notes

`build-apk.yml` under `.github/workflows/` builds and signs the release APK using repository secrets. Configure the following in the repository settings before running the workflow:

- `KEYSTORE_BASE64`: Base64 encoding of `release-keystore.jks`
- `STORE_PASSWORD`: Keystore store password
- `KEY_ALIAS`: Keystore alias
- `KEY_PASSWORD`: Key password

The workflow outputs `skidl-release-apk` as an artifact for direct download.
