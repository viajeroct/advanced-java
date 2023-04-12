SET TASK=mapper
SET MOD=advanced
SET SOLUTION=concurrent.IterativeParallelism

cd ../out/production/java-solutions/
java -cp . -p "../../../otest/;../../../shared/lib/" -m info.kgeorgiy.java.advanced.%TASK% %MOD% info.kgeorgiy.ja.trofimov.%SOLUTION%
