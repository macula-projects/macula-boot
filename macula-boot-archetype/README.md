## Macula Tools Archetype

脚手架工程

```shell
mvn archetype:generate \
    -DgroupId=dev.macula.samples	\										# 你的应用的GroupID
    -DartifactId=macula-samples	\											# 你的应用的ArtifactId
    -Dversion=1.0.0-SNAPSHOT \												# 你的应用的版本号
    -DarchetypeArtifactId=macula-boot-archetype \ 
    -DarchetypeGroupId=dev.macula.boot \
    -DarchetypeVersion=5.0.0 \
    -Dgitignore=.gitignore -DinteractiveMode=false
```