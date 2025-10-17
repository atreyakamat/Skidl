@echo off
setlocal
set SCRIPT_DIR=%~dp0
set GRADLE_VERSION=8.5
set DIST_NAME=gradle-%GRADLE_VERSION%
set DIST_DIR=%SCRIPT_DIR%gradle\%DIST_NAME%
set GRADLE_BIN=%DIST_DIR%\bin\gradle.bat

if not exist "%GRADLE_BIN%" (
  echo Downloading Gradle %GRADLE_VERSION%...
  powershell -Command "& { $ErrorActionPreference='Stop'; if (!(Test-Path '%SCRIPT_DIR%gradle')) { New-Item '%SCRIPT_DIR%gradle' -ItemType Directory | Out-Null }; $zip='%SCRIPT_DIR%gradle\%DIST_NAME%.zip'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/%DIST_NAME%-bin.zip' -OutFile $zip; Expand-Archive -Path $zip -DestinationPath '%SCRIPT_DIR%gradle' -Force; Remove-Item $zip }"
)

call "%GRADLE_BIN%" %*
