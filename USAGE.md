# Usage

## Adding to your project
> [!NOTE]
> It is recommended to have the Minecraft version as a gradle.properties
> So you can reference it later on.
 ```properties
minecraft_version=your_mc_version_here
```
1. Add the following to your build.gradle(.kts) plugin block and import necessary classes
```gradle
import org.gradle.api.internal.file.FileOperations
import org.quiltmc.launchermeta.version.v1.Library
import org.quiltmc.launchermeta.version.v1.Version
import mappings.plugin.constants.Classifiers
import mappings.plugin.constants.Constants
import mappings.plugin.task.setup.DownloadWantedVersionManifestTask
import mappings.plugin.util.serializable.VersionParser

import java.util.stream.Stream
import java.util.stream.Collectors

plugins {
    id("com.bookkeepersmc.mappings-creator") version("<PLUGIN_VERSION_HERE>")
}
```

2. Configuration of buildscript.
Configure the extension as shown below. All items are configurable!
```gradle
mappingsCreator {
    minecraftVersion = "${minecraft_version}" // the minecraft version. defined in gradle.properties
    
    mappingsVersion = "MAPPINGS_VERSION_HERE" // preferably include the MC version here
    
    mappingsDir = file("mappings/") // where mappings files are stored.
    
    enigmaProfileConfig = file("enigma/enigma_profile.json") // enigma profile, shown below.
    
    unpickMeta = file('unpick/unpick.json') // shown below
}
```

3. Configure tasks.
All tasks make mappings work better! (Sources and javadoc are optional but recommended)
```gradle
tasks.combineUnpickDefinitions {
	// add project's static unpick definitions
	unpickDefinitions.from 'unpick/definitions/'
}

tasks.mappingLint {
	dictionaryFile = configurations.dictionary.singleFile
}

tasks.sourcesJar {
	sources.from sourceSets.constants.allSource
}

tasks.constantsJar {
	constants.from sourceSets.constants.output
}

tasks.build.dependsOn constantsJar, generatePackageInfoMappings,
	compressTiny, tinyJar, v2UnmergedMappingsJar, v2MergedMappingsJar
```

4. Dependencies. Add the following to your dependencies, sourceSets, and configurations block.<br>
> [!IMPORTANT]
> THIS IS REQUIRED!!!!!
```gradle
sourceSets {
	// for constantsJar and sourcesJar input
	constants
	// package info files, for javadoc input
	packageDocs
	// for javadoc input
	doclet
}

configurations {
	dictionary
}

dependencies {
	dictionary "ix0rai:qm-base-allowed-wordlist:f9c2abb8ad2df8bf64df06ae2f6ede86704b82c7"

	javadocClasspath("org.quiltmc:quilt-loader:0.26.1")
	javadocClasspath("org.jetbrains:annotations:25.0.0")
	// for some other jsr annotations
	javadocClasspath("com.google.code.findbugs:jsr305:3.0.2")
	hashed "org.quiltmc:hashed:${minecraft_version}"
	intermediary "net.fabricmc:intermediary:${minecraft_version}:v2"
}
```

5. Javadoc configuration. OPTIONAL BUT RECOMMENDED!
```gradle
tasks.named("javadoc", Javadoc) {
	// Needed for javadoc to find the doclet classes.
	dependsOn docletClasses

	final versionParser = tasks
		.named("downloadWantedVersionManifest", DownloadWantedVersionManifestTask)
		.flatMap(DownloadWantedVersionManifestTask::provideVersionParser)

	final MapProperty<String, String> minecraftLibVersionsByModule = project.objects.mapProperty(String, String)
	minecraftLibVersionsByModule.set(versionParser
		.map(VersionParser::get)
		.map(Version::getLibraries)
		.map {
			it.stream()
				.map(Library::getName)
				.flatMap { moduleName ->
					final delim = ':'
					final moduleParts = moduleName.split(delim)

					if (moduleParts.length == 3 || moduleParts.length == 4) {
						return Stream.of(Map.entry(moduleParts[0] + delim + moduleParts[1], moduleParts[2]))
					} else {
						logger.error("Unrecognized module format: ${moduleName}")

						return Stream.empty()
					}
				}
				.distinct()
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
		}
	)

	final Closure<Provider<String>> provideModuleVersion = { String module ->
		final versionProperty = project.objects.property(String)
			.convention(project.providers.provider { throw new GradleException("Missing version for module: ${module}") })

		versionProperty.set(minecraftLibVersionsByModule.getting(module))

		return versionProperty
	}

	final Closure<Provider<String>> formatVersionedUrl = { String baseUrl, Provider<String> version ->
		version.map {"${baseUrl}/${it}/" as String }
	}

	final gsonUrl = formatVersionedUrl(
		'https://javadoc.io/doc/com.google.code.gson/gson',
		provideModuleVersion('com.google.code.gson:gson')
	)

	// use hardcoded version because minecraft depends on a version that doesn't have javadocs
	final nettyUrl = formatVersionedUrl(
		'https://javadoc.io/doc/io.netty/netty-all',
		"4.1.68.Final"
	)

	final commonsCompressUrl = formatVersionedUrl(
		'https://javadoc.io/doc/org.apache.commons/commons-compress',
		provideModuleVersion('org.apache.commons:commons-compress')
	)

	final commonsCodecUrl = formatVersionedUrl(
		'https://javadoc.io/doc/commons-codec/commons-codec',
		provideModuleVersion('commons-codec:commons-codec')
	)

	[
		gsonUrl: gsonUrl,
		nettyUrl: nettyUrl,
		commonsCompressUrl: commonsCompressUrl,
		commonsCodecUrl: commonsCodecUrl
	].each(inputs::property)

	destinationDir = layout.buildDirectory.get().dir('docs').dir(project.version.toString()).getAsFile()
	final docletResources = sourceSets.doclet.resources.asFileTree

	// Failing on error is important to ensure that a Javadoc jar is available.
	failOnError = true
	maxMemory = '2G'

	final debug = providers.gradleProperty('mappings.plugin.debug.javadoc')
		.map(Boolean::parseBoolean)
		.getOrElse(false)

	verbose = debug

	options {
		if (debug) {
			verbose()
		}

		source = '21'
		encoding = 'UTF-8'
		charSet = 'UTF-8'
		memberLevel = JavadocMemberLevel.PRIVATE
		splitIndex true
		tags(
			'apiNote:a:API Note:',
			'implSpec:a:Implementation Requirements:',
			'implNote:a:Implementation Note:'
		)
		// taglet path, header, extra stylesheet settings deferred
		it.use()

		addBooleanOption '-allow-script-in-comments', true
		// https://docs.oracle.com/en/java/javase/21/docs/specs/man/javadoc.html#additional-options-provided-by-the-standard-doclet
		addBooleanOption 'Xdoclint:html', true
		addBooleanOption 'Xdoclint:syntax', true
		addBooleanOption 'Xdoclint:reference', true
		addBooleanOption 'Xdoclint:accessibility', true
		addStringOption '-notimestamp'
	}

	source sourceSets.constants.allJava + sourceSets.packageDocs.allJava

	doFirst {
		options {
			links(
				"https://javadoc.io/doc/com.google.guava/guava/33.2.1-jre/",
				gsonUrl.get(),
				"https://javadoc.io/doc/org.jetbrains/annotations/25.0.0/",
				"https://javadoc.io/doc/com.google.code.findbugs/jsr305/3.0.2/",
				commonsCompressUrl.get(),
				nettyUrl.get(),
				'https://javadoc.lwjgl.org/',
				'https://logging.apache.org/log4j/2.x/javadoc/log4j-api/',
				'https://fastutil.di.unimi.it/docs/',
				'https://commons.apache.org/proper/commons-logging/javadocs/api-release/',
				'https://commons.apache.org/proper/commons-lang/javadocs/api-release/',
				'https://commons.apache.org/proper/commons-io/apidocs/',
				commonsCodecUrl.get(),
				"https://docs.oracle.com/en/java/javase/${project.java}/docs/api/"
			)

			tagletPath sourceSets.doclet.output.classesDirs.files.toList()
			header docletResources.filter { it.name == 'javadoc_header.txt' }.singleFile.text.trim()
			addFileOption '-add-stylesheet', docletResources.filter { it.name == 'style.css' }.singleFile
		}
	}

	doLast {
		FILE_OPERATIONS.copy {
			it.from docletResources
			it.include 'copy_on_click.js'
			it.into destinationDir
		}
	}
}
```

5. Java config. The usual
```gradle
java {
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType(JavaCompile).configureEach {
	it.options.encoding = 'UTF-8'
	it.options.release = 21
}
```

6. Publishing. OPTIONAL!
```gradle
publishing {
	publications {
		maven(MavenPublication) {
			groupId 'your_id'
			artifactId Constants.MAPPINGS_NAME
			version project.version

			compressTiny.artifact maven
			artifact tinyJar
			artifact v2UnmergedMappingsJar
			artifact v2MergedMappingsJar
			artifact constantsJar

			artifact sourcesJar
			artifact javadocJar

			// See below the publishing block for intermediary artifact publishing
		}
	}

	repositories {
	    // add repositories here
	}
}

tasks.withType(AbstractPublishToMaven).configureEach {
	// Won't run if mapIntermediary.intermediaryFile is absent
	dependsOn buildIntermediary

	doFirst {
		if (mapIntermediary.intermediaryFile.isPresent()) {
			final addArtifact = { Task task, String artifactClassifier ->
				if (publication.artifacts.findAll { it.classifier == artifactClassifier }.isEmpty()) {
					publication.artifact(task) {
						classifier artifactClassifier
					}
				}
			}

			addArtifact(intermediaryV2MappingsJar, Classifiers.INTERMEDIARY_V2)
			addArtifact(intermediaryV2MergedMappingsJar, Classifiers.INTERMEDIARY_V2_MERGED)
		}
	}
}
```
