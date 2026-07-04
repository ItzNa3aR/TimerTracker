@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo ============================================
echo   Time Tracker (Java) - сборка .exe
echo ============================================
echo.

where java >nul 2>nul
if errorlevel 1 (
    echo [ОШИБКА] Java не найдена в PATH.
    echo Установите JDK 17 или новее: https://adoptium.net
    echo При установке отметьте "Add to PATH" и "Set JAVA_HOME".
    echo.
    pause
    exit /b 1
)

echo [0/4] Проверяю Java...
java -version
echo.

where mvn >nul 2>nul
if errorlevel 1 (
    echo [ОШИБКА] Maven не найден в PATH.
    echo Скачайте: https://maven.apache.org/download.cgi
    echo Распакуйте архив и добавьте его папку bin в переменную PATH.
    echo.
    pause
    exit /b 1
)

where jpackage >nul 2>nul
if errorlevel 1 (
    echo [ОШИБКА] jpackage не найден.
    echo Он идёт в комплекте с JDK 17+ - проверьте, что у вас установлен
    echo именно JDK ^(а не JRE^) и что его папка bin в переменной PATH.
    echo.
    pause
    exit /b 1
)

echo [1/4] Собираю jar со всеми зависимостями внутри (может занять пару минут)...
call mvn -q clean package > build_log.txt 2>&1
if errorlevel 1 (
    echo [ОШИБКА] Сборка Maven не удалась.
    echo Подробности смотрите в build_log.txt
    echo.
    pause
    exit /b 1
)
echo Готово.
echo.

echo [2/4] Ищу собранный jar...
set JARNAME=
for %%f in (target\time-tracker-*.jar) do set JARNAME=%%~nxf
if "%JARNAME%"=="" (
    echo [ОШИБКА] jar-файл не найден в папке target\
    echo Подробности смотрите в build_log.txt
    echo.
    pause
    exit /b 1
)
echo Найден: %JARNAME%
echo.

echo [3/4] Собираю TimeTracker.exe через jpackage (со встроенной Java внутри)...
if exist "dist" rmdir /s /q dist
jpackage --input target --main-jar "%JARNAME%" --main-class com.timetracker.Main --name TimeTracker --type app-image --dest dist --icon assets\icon.ico --win-console >> build_log.txt 2>&1
if errorlevel 1 (
    echo [ОШИБКА] jpackage не смог собрать exe.
    echo Подробности смотрите в build_log.txt
    echo.
    pause
    exit /b 1
)

if not exist "dist\TimeTracker\TimeTracker.exe" (
    echo [ОШИБКА] Сборка завершилась, но exe не найден.
    echo Подробности смотрите в build_log.txt
    echo.
    pause
    exit /b 1
)

echo [4/4] Готово!
echo.
echo ============================================
echo   Файл: dist\TimeTracker\TimeTracker.exe
echo ============================================
echo Всю папку dist\TimeTracker можно скопировать
echo на любой Windows-компьютер - Java там не нужна,
echo она уже встроена внутрь папки.
echo.
echo Сейчас показывается консоль с логами - это специально,
echo чтобы вы сразу видели ошибки, если что-то пойдёт не так.
echo Как всё проверите - откройте этот .bat и уберите флаг
echo --win-console из команды jpackage, чтобы пересобрать
echo финальную версию без чёрного окна консоли.
echo.
pause
