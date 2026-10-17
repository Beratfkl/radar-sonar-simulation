@echo off
if not exist bin mkdir bin
javac -encoding UTF-8 -d bin src\com\radarsonar\*.java src\com\radarsonar\ui\*.java src\com\radarsonar\model\*.java src\com\radarsonar\engine\*.java
if %ERRORLEVEL% equ 0 (
    java -cp bin com.radarsonar.Main
) else (
    echo Derleme hatasi olustu.
    pause
)
