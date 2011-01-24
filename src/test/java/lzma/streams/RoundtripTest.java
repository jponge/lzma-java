package lzma.streams;

import static org.apache.commons.io.FileUtils.contentEquals;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.TestCase;
import lzma.sdk.lzma.Decoder;
import lzma.sdk.lzma.Encoder;

import org.apache.commons.io.IOUtils;

public class RoundtripTest
    extends TestCase

{
    public void testEncoderDecoder()
        throws IOException
    {
        System.out.println( "Encoder/Decoder roundtrip (low-level API)" );
        final File srcDir = new File( "target/test-classes" );

        performRoundtrip( new File( srcDir, "plaintext.txt" ) );
        performRoundtrip( new File( srcDir, "ajar.jar" ) );
        performRoundtrip( new File( srcDir, "doc.pdf" ) );
    }

    public void testStreamingEncoderDecoder()
        throws Exception
    {
        System.out.println( "Stream roundtrip" );
        final File srcDir = new File( "target/test-classes" );

        performStreamRoundtrip( new File( srcDir, "plaintext.txt" ) );
        performStreamRoundtrip( new File( srcDir, "ajar.jar" ) );
        performStreamRoundtrip( new File( srcDir, "doc.pdf" ) );
    }

    public void testAltStreamingEncoderDecoder()
        throws Exception
    {
        System.out.println( "alt-Stream roundtrip" );
        final File srcDir = new File( "target/test-classes" );

        performAltStreamRoundtrip( new File( srcDir, "plaintext.txt" ) );
        performAltStreamRoundtrip( new File( srcDir, "ajar.jar" ) );
        performAltStreamRoundtrip( new File( srcDir, "doc.pdf" ) );
    }

    public void performRoundtrip( final File sourceFile )
        throws IOException
    {
        final File compressedFile = new File( sourceFile.getParentFile(), sourceFile.getName() + ".coder.lzma" );
        final File decompressedFile = new File( sourceFile.getParentFile(), sourceFile.getName() + ".coder.unlzma" );

        InputStream in = null;
        OutputStream out = null;

        // compressing
        in = new BufferedInputStream( new FileInputStream( sourceFile ) );
        out = new BufferedOutputStream( new FileOutputStream( compressedFile ) );

        final Encoder encoder = new Encoder();

        encoder.setDictionarySize( 1 << 23 );
        encoder.setEndMarkerMode( true );
        encoder.setMatchFinder( Encoder.EMatchFinderTypeBT4 );
        encoder.setNumFastBytes( 0x20 );

        encoder.writeCoderProperties( out );
        long fileSize = sourceFile.length();
        for ( int i = 0; i < 8; i++ )
            out.write( (int) ( fileSize >>> ( 8 * i ) ) & 0xFF );
        encoder.code( in, out, -1, -1, null );
        out.flush();
        out.close();
        in.close();

        // decompressing
        in = new BufferedInputStream( new FileInputStream( compressedFile ) );
        out = new BufferedOutputStream( new FileOutputStream( decompressedFile ) );

        int propertiesSize = 5;
        byte[] properties = new byte[propertiesSize];
        if ( in.read( properties, 0, propertiesSize ) != propertiesSize )
            throw new IOException( "input .lzma file is too short" );
        Decoder decoder = new Decoder();
        if ( !decoder.setDecoderProperties( properties ) )
            throw new IOException( "Incorrect stream properties" );
        long outSize = 0;
        for ( int i = 0; i < 8; i++ )
        {
            int v = in.read();
            if ( v < 0 )
                throw new IOException( "Can't read stream size" );
            outSize |= ( (long) v ) << ( 8 * i );
        }
        if ( !decoder.code( in, out, outSize ) )
            throw new IOException( "Error in data stream" );
        out.flush();
        out.close();
        in.close();

        assertTrue( "Source and uncompressed content does not equals!", contentEquals( sourceFile, decompressedFile ) );
        assertFalse( "Source and compressed content equals!", contentEquals( sourceFile, compressedFile ) );
    }

    public void performStreamRoundtrip( final File sourceFile )
        throws IOException
    {
        final File compressedFile = new File( sourceFile.getParentFile(), sourceFile.getName() + ".stream.lzma" );
        final File decompressedFile = new File( sourceFile.getParentFile(), sourceFile.getName() + ".stream.unlzma" );

        InputStream in = null;
        OutputStream out = null;

        // testing with defaults
        in = new BufferedInputStream( new FileInputStream( sourceFile ) );
        out =
            new LzmaOutputStream.Builder( new BufferedOutputStream( new FileOutputStream( compressedFile ) ) ).build();

        IOUtils.copy( in, out );
        in.close();
        out.close();

        in = new LzmaInputStream( new BufferedInputStream( new FileInputStream( compressedFile ) ), new Decoder() );
        out = new BufferedOutputStream( new FileOutputStream( decompressedFile ) );

        IOUtils.copy( in, out );
        in.close();
        out.close();

        assertTrue( "Source and uncompressed content does not equals!", contentEquals( sourceFile, decompressedFile ) );
        assertFalse( "Source and compressed content equals!", contentEquals( sourceFile, compressedFile ) );
    }

    public void performAltStreamRoundtrip( final File sourceFile )
        throws Exception
    {
        final File compressedFile = new File( sourceFile.getParentFile(), sourceFile.getName() + ".stream.lzma" );
        final File decompressedFile = new File( sourceFile.getParentFile(), sourceFile.getName() + ".stream.unlzma" );

        InputStream in = null;
        OutputStream out = null;

        // testing with defaults
        System.out.println( " o Stream compression" );
        in = new BufferedInputStream( new FileInputStream( sourceFile ) );
        out =
            new LzmaCompressingOutputStream( new BufferedOutputStream( new FileOutputStream( compressedFile ) ),
                new LzmaEncoderWrapper.Builder().build() );

        IOUtils.copy( in, out );
        in.close();
        out.flush();
        out.close();

        Thread.sleep( 1000 );

        System.out.println( " o Stream decompression" );
        in =
            new LzmaDecompressingInputStream( new BufferedInputStream( new FileInputStream( compressedFile ) ),
                new Decoder() );
        out = new BufferedOutputStream( new FileOutputStream( decompressedFile ) );

        IOUtils.copy( in, out );
        in.close();
        out.close();

        assertTrue( "Source and uncompressed content does not equals!", contentEquals( sourceFile, decompressedFile ) );
        assertFalse( "Source and compressed content equals!", contentEquals( sourceFile, compressedFile ) );
    }

}
