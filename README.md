**Important: this project is not maintained anymore.**

You should look at the [Apache Commons Compress project](https://commons.apache.org/proper/commons-compress/)
for a rich set of supported compression formats, including LZMA.


# LZMA library for Java #

[![Build Status](https://secure.travis-ci.org/jponge/lzma-java.png)](http://travis-ci.org/jponge/lzma-java)

[http://jponge.github.com/lzma-java](http://jponge.github.com/lzma-java)

This library provides LZMA compression for applications that run on the Java platform.

## Background ##

This library is based on the [Java LZMA SDK](http://www.7-zip.org/sdk.html) by Igor Pavlov.
It provides some deserved enhancements.

While the original code works just fine, it has some serious issues for Java developers:

* this is a straight port of non-object, procedural code written in C to Java, and
* the code does not follow Java conventions (e.g., methods names start by
  capital letters)
* configuration of the LZMA encoder and decoders require passing around
  arrays and numbers for which no proper documentation or constants exists
  other than source code, and
* ...there is no stream api to plug into `java.io` streams.

There is unfortunately no public description of the LZMA algorithms other than source code, 
so a rewrite was clearly a hard task. I decided to create this library using the following
methodology.

1. Import the Java LZMA SDK code.
2. Convert methods and package names to Java conventions.
3. Reformat the code and organize imports.
4. Remove the useless (at least in a library) command-line interface classes.
5. Run static code analysis to clean the code (unused variables, unusued parameters,
   unused methods, expressions simplifications and more).
6. Do some profiling.
7. Build a streaming api that would fit into `java.io` streams.
8. Provide some higher-level abstractions to the LZMA encoder / decoders configuration.

Although not a derivate work, the streaming api classes were inspired from 
[the work of Christopher League](http://contrapunctus.net/league/haques/lzmajio/). I reused
his technique of fake streams and working threads to pass the data around between
encoders/decoders and "normal" Java streams.

## Using from Maven ##

The releases are pushed to Maven Central. Add the dependency as follows:

    <dependency>
        <groupId>com.github.jponge</groupId>
        <artifactId>lzma-java</artifactId>
        <version>1.2</version>
    </dependency>

## Usage ##

There are two main Java package hierarchies:

* `lzma.sdk` is the (reworked) Java LZMA SDK code, and
* `lzma.streams` contains the `LzmaInputStream` and `LzmaOutputStream` classes.

You will probably only be interested in using the `lzma.streams` package. The two
stream classes use the good practices of constructor dependency injection, and you will
need to pass them the decorated streams and LZMA encoder / decoders from the SDK.

You can simply instanciate a `Decoder` and pass it to the constructor of `LzmaInputStream`
without specifying further configuration: it will read it from the input stream.

The `Encoder` class that `LzmaOutputStream` depends on needs some configuration. You can
either do it manually (checkout the `Encoder` class to guess what those integer values mean!), 
or you can use the `LzmaOutputStream.Builder` class which makes it much easier to configure.

The following code is from a unit test. It should make the basic usage of the library relatively
obvious:

    public void test_round_trip() throws IOException
    {
        final File sourceFile = new File("LICENSE");
        final File compressed = File.createTempFile("lzma-java", "compressed");
        final File unCompressed = File.createTempFile("lzma-java", "uncompressed");
        
        final LzmaOutputStream compressedOut = new LzmaOutputStream.Builder(
                new BufferedOutputStream(new FileOutputStream(compressed)))
                .useMaximalDictionarySize()
                .useEndMarkerMode(true)
                .useBT4MatchFinder()
                .build();
        
        final InputStream sourceIn = new BufferedInputStream(new FileInputStream(sourceFile));
        
        copy(sourceIn, compressedOut);
        sourceIn.close();
        compressedOut.close();
        
        final LzmaInputStream compressedIn = new LzmaInputStream(
                new BufferedInputStream(new FileInputStream(compressed)),
                new Decoder());
                
        final OutputStream uncompressedOut = new BufferedOutputStream(
                new FileOutputStream(unCompressed));
                
        copy(compressedIn, uncompressedOut);
        compressedIn.close();
        uncompressedOut.close();
        
        assertTrue(contentEquals(sourceFile, unCompressed));
        assertFalse(contentEquals(sourceFile, compressed));
    }

## License ##

The LZMA SDK is in the public domain. I relicensed the whole under the liberal
[Apache License Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Contact ##

* julien.ponge@gmail.com
* [http://julien.ponge.info/](http://julien.ponge.info/)

The code, downloads and issue trackers are made available from GitHub at
[http://github.com/jponge/lzma-java](http://github.com/jponge/lzma-java).
Do not hesitate to contribute by forking and asking for pulls!
