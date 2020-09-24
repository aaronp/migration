# Migration

![Build Status](https://github.com/aaronp/migration/workflows/Scala%20CI/badge.svg)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.aaronp/migration_2.13/badge.png)](https://maven-badges.herokuapp.com/maven-central/com.github.aaronp/migration_2.13)
[![Coverage Status](https://coveralls.io/repos/github/aaronp/migration/badge.svg?branch=master)](https://coveralls.io/github/aaronp/migration?branch=master)
[![Scaladoc](https://javadoc-badge.appspot.com/com.github.aaronp/migration_2.13.svg?label=scaladoc)](https://javadoc-badge.appspot.com/com.github.aaronp/migration_2.13)

Collection of utilities to support:
 * querying an index file from a URL
 * downloading each listed zip entry in that index to a target directory using a filename regex
 * unzipping said entry using a regex for the file entries and ensuring it is valid xml