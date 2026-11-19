@echo off
setlocal EnableExtensions DisableDelayedExpansion
chcp 65001 >nul

rem HamHaeVoca reusable Windows APK builder.
rem This does not require opening Android Studio, but needs Gradle/JDK/Android SDK installed.
rem Keep this BAT wherever you want the completed APK to appear.
set "BUILDER_DIR=%~dp0"
set "CONFIG_FILE=%BUILDER_DIR%HamHaeVoca_Project_Path.txt"
set "APK_TARGET=%BUILDER_DIR%HamHaeVoca-debug.apk"
set "APK_OLD=%BUILDER_DIR%HamHaeVoca-debug.previous.apk"
set "LOG_FILE=%BUILDER_DIR%HamHaeVoca-build.log"
set "META_FILE=%BUILDER_DIR%HamHaeVoca-build-info.txt"
set "PROJECT_DIR="

if exist "%CONFIG_FILE%" (
  set /p PROJECT_DIR=<"%CONFIG_FILE%"
)
if not defined PROJECT_DIR if exist "%BUILDER_DIR%gradlew.bat" set "PROJECT_DIR=%BUILDER_DIR%"
if not defined PROJECT_DIR if exist "D:\Dev_Project\HamHaeVoca\gradlew.bat" set "PROJECT_DIR=D:\Dev_Project\HamHaeVoca"

echo ========================================================
echo HamHaeVoca - Reusable APK Builder
echo ========================================================
echo Builder location: "%BUILDER_DIR%"
echo.

:CHECK_PROJECT
if defined PROJECT_DIR if exist "%PROJECT_DIR%\gradlew.bat" if exist "%PROJECT_DIR%\app\src\main\AndroidManifest.xml" goto PROJECT_FOUND
echo [SETUP] Select your Android project root (contains gradlew.bat).
echo Example: D:\Dev_Project\HamHaeVoca
echo.
set "PROJECT_DIR="
set /p "PROJECT_DIR=Android project directory (or Q to quit): "
if /i "%PROJECT_DIR%"=="Q" goto FAILED
if not defined PROJECT_DIR goto CHECK_PROJECT
if "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"
if not exist "%PROJECT_DIR%\gradlew.bat" (
  echo [ERROR] Missing gradlew.bat.
  goto CHECK_PROJECT
)
if not exist "%PROJECT_DIR%\app\src\main\AndroidManifest.xml" (
  echo [ERROR] Missing app\src\main\AndroidManifest.xml.
  goto CHECK_PROJECT
)

:PROJECT_FOUND
if not exist "%PROJECT_DIR%\app\build.gradle" if not exist "%PROJECT_DIR%\app\build.gradle.kts" (
  echo [ERROR] Missing app Gradle build file.
  goto FAILED
)
>"%CONFIG_FILE%" echo %PROJECT_DIR%
echo Project: "%PROJECT_DIR%"
echo APK output: "%APK_TARGET%"
echo.
echo [INFO] The builder compiles current files in app\src.
echo [INFO] You must apply source-code changes to the project first.
echo [INFO] To change the project later, delete/edit "%CONFIG_FILE%".
echo.

rem Existing Android Studio installs include a JetBrains Runtime (JBR).
rem Prefer an explicitly configured JAVA_HOME; otherwise try common Studio locations.
if not defined JAVA_HOME (
  if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
)
if not defined JAVA_HOME (
  if exist "%ProgramFiles%\Android\Android Studio\jre\bin\java.exe" set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jre"
)
if defined JAVA_HOME (
  if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] JAVA_HOME is set but java.exe is missing: "%JAVA_HOME%"
    goto FAILED
  )
  echo Java: "%JAVA_HOME%"
) else (
  where java >nul 2>&1
  if errorlevel 1 (
    echo [ERROR] Java not found. Install JDK or configure JAVA_HOME.
    goto FAILED
  )
  echo Java: system PATH
)

set "APK_SOURCE=%PROJECT_DIR%\app\build\outputs\apk\debug\app-debug.apk"
if exist "%LOG_FILE%" del /q "%LOG_FILE%" >nul 2>&1

echo [1/3] Clean and build Debug APK from current sources...
echo [INFO] Log: "%LOG_FILE%"
pushd "%PROJECT_DIR%"
if errorlevel 1 goto FAILED
call "gradlew.bat" :app:clean :app:assembleDebug --no-daemon --rerun-tasks --console=plain >"%LOG_FILE%" 2>&1
set "GRADLE_EXIT=%ERRORLEVEL%"
popd
if not "%GRADLE_EXIT%"=="0" (
  echo [ERROR] Gradle build failed with exit code %GRADLE_EXIT%.
  echo.
  powershell -NoProfile -Command "Get-Content -LiteralPath '%LOG_FILE%' -Tail 35" 2>nul
  echo.
  echo [INFO] Full log: "%LOG_FILE%"
  goto FAILED
)
if not exist "%APK_SOURCE%" (
  echo [ERROR] Gradle succeeded but no Debug APK was found:
  echo "%APK_SOURCE%"
  goto FAILED
)

rem Copy only on successful build. Preserve the previous APK if there is one.
echo [2/3] Copy APK into builder directory...
if exist "%APK_OLD%" del /q "%APK_OLD%" >nul 2>&1
if exist "%APK_TARGET%" (
  copy /y "%APK_TARGET%" "%APK_OLD%" >nul
  if errorlevel 1 goto FAILED
)
copy /y "%APK_SOURCE%" "%APK_TARGET%" >nul
if errorlevel 1 (
  echo [ERROR] Cannot write APK to builder directory.
  goto FAILED
)

echo [3/3] Create build information...
>"%META_FILE%" echo HamHaeVoca Debug APK build
>>"%META_FILE%" echo Project: %PROJECT_DIR%
>>"%META_FILE%" echo Timestamp: %DATE% %TIME%
>>"%META_FILE%" echo APK: %APK_TARGET%
for %%F in ("%APK_TARGET%") do >>"%META_FILE%" echo Size: %%~zF bytes
where certutil >nul 2>&1
if not errorlevel 1 certutil -hashfile "%APK_TARGET%" SHA256 >>"%META_FILE%" 2>nul

echo.
echo [SUCCESS] Built from the current project sources.
echo APK: "%APK_TARGET%"
echo Log: "%LOG_FILE%"
echo Build info: "%META_FILE%"
echo [NOTE] The Gradle app versionName is not changed automatically.
echo [NOTE] Install the new APK to test updated functionality.
echo.
pause
exit /b 0

:FAILED
echo.
echo [FAILED] No new APK has been published by this run.
echo [INFO] Check "%LOG_FILE%" if Gradle was started.
echo.
pause
exit /b 1
