if [%1] == [] (
set /p model= "Enter model:"
)

java -Xmx1024M -classpath target/microbe-1.0-SNAPSHOT.jar;lib/ru.yandex-bolts-jar-20121016122946.jar;target/dependency/* ru.lj.alamar.microbe.Model %model% %*
