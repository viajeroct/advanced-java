SET TASK=mapper
SET MOD=list
SET SOLUTION=concurrent.IterativeParallelism

cd ../out/production/java-solutions/
java -cp . -p "../../../shared/artifacts/;../../../shared/lib/" -m info.kgeorgiy.java.advanced.%TASK% %MOD% info.kgeorgiy.ja.trofimov.%SOLUTION%
