SET solutions=..\java-solutions
SET repo=..\java-advanced-2023
SET lib=%repo%\lib\*
SET test=%repo%\artifacts\info.kgeorgiy.java.advanced.implementor.jar
SET dst=%solutions%\..\scripts
SET man=%dst%\MANIFEST.MF
SET dep=info\kgeorgiy\java\advanced\implementor\
SET modules=%repo%\modules\

cd %solutions%

:: version 1
javac -d %dst% -cp %modules%;%lib%;%test%; info\kgeorgiy\ja\trofimov\implementor\Implementor.java

:: version 2
:: javac --module-path %repo%\artifacts\;%repo%\lib\ %solutions%\module-info.java @files.txt -d %dst%

cd %dst%

jar xf %test% %dep%Impler.class %dep%JarImpler.class %dep%ImplerException.class
jar cfm %dst%\Implementor.jar %man% info\kgeorgiy\ja\trofimov\implementor\*.class %dep%*.class
