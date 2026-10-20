@echo off
cd /d "%~dp0"
java -Xms256m -Xmx1g -jar "cards.jar"
pause
