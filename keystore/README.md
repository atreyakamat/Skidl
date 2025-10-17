# Release Keystore Setup

1. Generate a keystore (example):
   ```bash
   keytool -genkeypair -v \
     -keystore release-keystore.jks \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias hotspot_skribble_key
   ```

2. Copy the generated `release-keystore.jks` file into this directory. **Do not commit it.**

3. Create `keystore.properties` at the project root:
   ```properties
   storeFile=keystore/release-keystore.jks
   storePassword=your-store-password
   keyAlias=hotspot_skribble_key
   keyPassword=your-key-password
   ```

4. For GitHub Actions, base64-encode the JKS:
   ```bash
   base64 keystore/release-keystore.jks > release-keystore.b64
   ```
   Upload the contents to the `KEYSTORE_BASE64` secret.
