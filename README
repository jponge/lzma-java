# LZMA library for Java #

## Background ##

This library is based on the Java LZMA SDK by Igor Pavlov at http://www.7-zip.org/sdk.html.

While the code works just fine, any potential user will quickly realize that:

* this is a straight port of non-object procedural code written in C to Java, and
* the code does not follow Java conventions (e.g., methods names start by
  capital letters)
* configuration of the LZMA encoder and decoders require passing around
  arrays and numbers for which no documentation or constants is available, and
* ...there is no stream api to plug into java.io streams.

There is unfortunately no public description of the LZMA algorithms, so a
rewrite was clearly a hard task. I decided to create this library as follows.

1. Import the Java LZMA SDK code.
2. Convert methods and package names to Java conventions.
3. Reformat the code and organize imports.
4. Remove the useless (at least in a library) command-line interface classes.
5. Run static code analysis to clean the code (unused variables, unusued parameters,
   unused methods, expressions simplifications and more).
6. Do some profiling.
7. Build a streaming api that would fit into java.io streams.

Although not a derivate work, the streaming api classes were inspired from the work
of Christopher League at http://contrapunctus.net/league/haques/lzmajio/. I reused
his technique of fake streams and working threads to pass the data around between
encoders/decoders and "normal" Java streams.

## License ##

The LZMA SDK is in the public domain. I relicensed the whole under the liberal
Apache License Version 2.0: http://www.apache.org/licenses/LICENSE-2.0

## Contact ##

* <julien.ponge@gmail.com>
* http://julien.ponge.info/

The code, downloads and issue trackers are made available from GitHub at
http://github.com/jponge/lzma-java. Do not hesitate to contribute by forking
and asking for pulls!
