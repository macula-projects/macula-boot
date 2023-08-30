## Macula Tools Archetype

脚手架工程

```shell
mvn archetype:generate \
    -DgroupId=com.infintius.xxp
    -DartifactId=xxp
    -Dversion=1.0.0-SNAPSHOT 
    -DarchetypeArtifactId=macula-boot-archetype 
    -DarchetypeGroupId=dev.macula.boot 
    -DarchetypeVersion=5.0.0-SNAPSHOT 
    -DarchetypeRepository=~/.m2/repository 
    -Dgitignore=.gitignore -DinteractiveMode=false
```