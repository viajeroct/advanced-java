SET task=concurrent
:: SET repo=..\java-advanced-2023
SET repo=..\..\shared

SET solutions=..\java-solutions
SET lib=%repo%\lib\*
SET test=%repo%\artifacts\info.kgeorgiy.java.advanced.%task%.jar
SET link=https://docs.oracle.com/en/java/javase/11/docs/api/
SET package=info.kgeorgiy.ja.trofimov.%task%

cd %solutions%
javadoc -d %solutions%\..\javadoc -link %link% -cp %lib%;%test%; -private -author -version %package%
