@echo off
setlocal

where gradle >NUL 2>&1
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)

echo Error: Gradle is not installed and this repository cannot include binary wrapper artifacts in PRs. 1>&2
echo Install Gradle 8.2+ and re-run this command. 1>&2
exit /b 1
