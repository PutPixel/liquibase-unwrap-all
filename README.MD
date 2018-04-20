# Goal
This plugin workaround problem of liquibase with `<includeAll/>` tag un-wrapping this annotation in master file.

See http://www.liquibase.org/documentation/includeall.html for more info.

# Before-After
Changeslog file in project: https://github.com/PutPixel/liquibase-unwrap-all/blob/master/src/test/resources/unit/project-to-test-with-unwrap-with-nested-folders/src/main/resources/db/changelog/db.changelog-master.xml

Changeslog file after processing: https://github.com/PutPixel/liquibase-unwrap-all/blob/master/src/test/resources/unit/project-to-test-with-unwrap-with-nested-folders/src/main/resources/db/changelog/db.changelog-master-should-be.xml

# How to use
Just include following code to your POM
```xml
<plugin>
    <groupId>com.github.putpixel</groupId>
    <artifactId>liquibase-unwrap-all-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>unwrap</goal>
            </goals>
            <phase>process-classes</phase>
        </execution>
    </executions>
    <configuration>
        <changesLogMasterLocation>path/to/changelog/master.xml</changesLogMasterLocation>
    </configuration>
</plugin>
```
