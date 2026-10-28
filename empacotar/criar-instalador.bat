@echo off
REM Gera o programa para os testes com usuarios (Windows).
REM Uso:   empacotar\criar-instalador.bat [modo] [tipo]
REM   modo: livre (padrao) | tecnico | tarefas
REM         tecnico/tarefas = abre direto naquela interface, SEM botao de troca e gravando o registro de eventos (CSV)
REM   tipo: app-image (padrao, sem instalacao, basta copiar a pasta) | exe (instalador; exige WiX Toolset 3.x)
REM Requisitos: JDK 17+ com jpackage (ex.: Temurin) e Maven no PATH.
setlocal
cd /d "%~dp0\.."

set MODO=%1
if "%MODO%"=="" set MODO=livre
set TIPO=%2
if "%TIPO%"=="" set TIPO=app-image

set NOME=SecureFileManager
set OPCOES=--java-options "-Xmx1g"
if /I "%MODO%"=="tecnico" (
  set NOME=SecureFileManager-Tecnica
  set OPCOES=%OPCOES% --java-options "-Dsfm.modo=tecnico" --java-options "-Dsfm.teste=true"
)
if /I "%MODO%"=="tarefas" (
  set NOME=SecureFileManager-Tarefas
  set OPCOES=%OPCOES% --java-options "-Dsfm.modo=tarefas" --java-options "-Dsfm.teste=true"
)

echo === Compilando com Maven ===
call mvn -q clean package -DskipTests
if errorlevel 1 goto erro

if exist target\entrada rmdir /s /q target\entrada
mkdir target\entrada
copy /y target\lib\*.jar target\entrada >nul
copy /y target\secure-file-manager-1.0.0.jar target\entrada >nul

set EXTRA=
if /I "%TIPO%"=="exe" set EXTRA=--win-menu --win-shortcut --win-dir-chooser

echo === Gerando %NOME% (%TIPO%) ===
jpackage --type %TIPO% --dest target\instalador --name "%NOME%" --app-version 1.0.0 ^
  --vendor "IFRS Veranopolis" --input target\entrada --main-jar secure-file-manager-1.0.0.jar ^
  --main-class br.edu.ifrs.sfm.Main ^
  --add-modules java.base,java.desktop,java.logging,java.xml,java.prefs,java.management,java.naming,jdk.unsupported ^
  %OPCOES% %EXTRA%
if errorlevel 1 goto erro

echo.
echo Pronto! Veja a pasta target\instalador
exit /b 0

:erro
echo.
echo Falhou. Confira se o JDK (com jpackage) e o Maven estao no PATH. Para o tipo exe, instale tambem o WiX Toolset 3.x.
exit /b 1
