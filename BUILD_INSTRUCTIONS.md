# Build Instructions / Инструкции по сборке 🛠️

[English](#english) | [Русский](#русский)

---

## English

This guide provides detailed instructions on how to build and run the **Proton VPN-Next** project for both Android and Linux Desktop.

### 1. Prerequisites

Before you begin, ensure you have the following installed on your system:

- **JDK 17 or 18:** Required for Gradle and Kotlin compilation.
- **Android SDK:** Required for the Android application.
- **Go 1.22+:** Required to build the native VPN bridge and helper.
- **Gradle:** (Optional, use the included `./gradlew` wrapper).
- **pkexec:** Usually pre-installed on most Linux distributions (required for Desktop VPN).

### 2. Native Bridge & Helper (Mandatory for Desktop)

The Desktop version uses a native Go bridge for SRP authentication and a root helper for the TUN interface. You must build these before running the Desktop app.

1. Open your terminal in the project root.
2. Run the build script:
   ```bash
   chmod +x scripts/build-go-bridge.sh
   ./scripts/build-go-bridge.sh
   ```
3. This will generate:
   - `desktop/libs/libgovpn.so` (Shared library for crypto)
   - `desktop/libs/vpn-helper` (Binary for root execution)

### 3. Building for Android

#### Using Android Studio
1. Open Android Studio (Ladybug 2024.2.1+ recommended).
2. Open the project root.
3. Wait for Gradle Sync.
4. Click **Run** or use the **Build > Build APK** menu.

#### Using Terminal
```bash
./gradlew :app:assembleDebug
```
The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### 4. Building for Linux Desktop

The Desktop version is built using Compose Multiplatform.

1. Ensure you have built the **Native Bridge** (Step 2).
2. Run the application:
   ```bash
   ./gradlew :desktop:run
   ```

> [!IMPORTANT]
> **Root Privileges:** When you click "Connect" in the Desktop app, it will invoke `pkexec` to run the `vpn-helper`. You will be prompted for your system password to allow the creation of the `wg0` TUN interface.

---

## Русский

Это руководство содержит подробные инструкции по сборке и запуску проекта **Proton VPN-Next** для Android и Linux Desktop.

### 1. Предварительные требования

Убедитесь, что в вашей системе установлены следующие компоненты:

- **JDK 17 или 18:** Необходим для Gradle и компиляции Kotlin.
- **Android SDK:** Необходим для Android-приложения.
- **Go 1.22+:** Необходим для сборки нативного моста и VPN-помощника.
- **Gradle:** (Необязательно, используйте встроенный скрипт `./gradlew`).
- **pkexec:** Обычно предустановлен в большинстве дистрибутивов Linux (нужен для Desktop VPN).

### 2. Нативный мост и Помощник (Обязательно для Desktop)

Десктопная версия использует нативный мост на Go для SRP-аутентификации и помощник с правами root для управления TUN-интерфейсом. Вы должны собрать их перед запуском приложения.

1. Откройте терминал в корневой папке проекта.
2. Запустите скрипт сборки:
   ```bash
   chmod +x scripts/build-go-bridge.sh
   ./scripts/build-go-bridge.sh
   ```
3. После выполнения появятся файлы:
   - `desktop/libs/libgovpn.so` (Общая библиотека для криптографии)
   - `desktop/libs/vpn-helper` (Бинарный файл для выполнения от root)

### 3. Сборка для Android

#### Через Android Studio
1. Откройте Android Studio (рекомендуется Ladybug 2024.2.1+).
2. Откройте корневую папку проекта.
3. Дождитесь синхронизации Gradle.
4. Нажмите **Run** или используйте меню **Build > Build APK**.

#### Через терминал
```bash
./gradlew :app:assembleDebug
```
APK файл будет находиться здесь: `app/build/outputs/apk/debug/app-debug.apk`.

### 4. Сборка для Linux Desktop

Десктопная версия построена на базе Compose Multiplatform.

1. Убедитесь, что вы собрали **Нативный мост** (Шаг 2).
2. Запустите приложение:
   ```bash
   ./gradlew :desktop:run
   ```

> [!IMPORTANT]
> **Права Root:** Когда вы нажимаете «Подключиться» в десктопном приложении, оно вызывает `pkexec` для запуска `vpn-helper`. Система запросит ваш пароль для создания TUN-интерфейса `wg0`.
