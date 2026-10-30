@echo off
setlocal EnableExtensions DisableDelayedExpansion

rem Place this file in the Android Studio project root, next to gradlew.bat.
rem The output APK is copied to the same folder as this builder.
set "BUILDER_DIR=%~dp0"
set "PROJECT_DIR=%BUILDER_DIR%"
set "GRADLEW=%PROJECT_DIR%gradlew.bat"
set "APK_SOURCE=%PROJECT_DIR%app\build\outputs\apk\debug\app-debug.apk"
set "APK_TARGET=%BUILDER_DIR%HamHaeVoca-debug.apk"

echo =====================================================
echo  HamHaeVoca APK Builder - Debug
echo =====================================================

if not exist "%GRADLEW%" (
    echo [ERROR] gradlew.bat not found in:
    echo         "%PROJECT_DIR%"
    echo Place Build_HamHaeVoca.bat next to gradlew.bat in the project root.
    goto :fail
)

if not exist "%PROJECT_DIR%app\build.gradle" if not exist "%PROJECT_DIR%app\build.gradle.kts" (
    echo [ERROR] Android app module not found in "%PROJECT_DIR%app".
    goto :fail
)

pushd "%PROJECT_DIR%" || goto :fail
echo [1/2] Building :app:assembleDebug...
call "%GRADLEW%" :app:assembleDebug
set "BUILD_EXIT=%ERRORLEVEL%"
popd

if not "%BUILD_EXIT%"=="0" (
    echo [ERROR] Gradle build failed with exit code %BUILD_EXIT%.
    goto :fail
)

if not exist "%APK_SOURCE%" (
    echo [ERROR] Build finished, but APK was not found:
    echo         "%APK_SOURCE%"
    goto :fail
)

echo [2/2] Copying APK to builder folder...
copy /Y "%APK_SOURCE%" "%APK_TARGET%" >nul
if errorlevel 1 (
    echo [ERROR] Could not copy the APK to the builder folder.
    goto :fail
)

echo.
echo [SUCCESS] APK created:
echo "%APK_TARGET%"
echo.
pause
exit /b 0

:fail
echo.
echo [FAILED] No new APK was published.
echo Check the messages above and your Android Studio build setup.
echo.
pause
exit /b 1
