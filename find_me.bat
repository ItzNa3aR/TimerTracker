@echo off
echo STARTING...
cd /d "C:\Users\Game On dp\Downloads\TimeTrackerJava"
echo Current folder: %cd%
echo List of files in current folder:
dir
echo.
echo Searching for jar files recursively:
dir /s *.jar
echo.
echo FINISHED.
pause