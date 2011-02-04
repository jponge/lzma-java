/*
 *  Copyright (c) 2010-2011 Julien Ponge. All rights reserved.
 *
 *  Portions Copyright (c) 2011 Tamas Cservenak.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package lzma.streams;

import junit.framework.TestCase;
import lzma.sdk.lzma.Decoder;
import lzma.sdk.lzma.Encoder;
import org.apache.commons.io.IOUtils;

import java.io.*;

import static org.apache.commons.io.FileUtils.contentEquals;

public class RoundtripTest
        extends TestCase

{
    public void testEncoderDecoder()
            throws IOException
    {
        System.out.println("Encoder/Decoder roundtrip (low-level API)");
        final File srcDir = new File("target/test-classes");

        performRoundtrip(new File(srcDir, "plaintext.txt"));
        performRoundtrip(new File(srcDir, "ajar.jar"));
        performRoundtrip(new File(srcDir, "doc.pdf"));
    }

    public void testStreamingEncoderDecoder()
            throws Exception
    {
        System.out.println("Stream roundtrip");
        final File srcDir = new File("target/test-classes");

        performStreamRoundtrip(new File(srcDir, "plaintext.txt"));
        performStreamRoundtrip(new File(srcDir, "ajar.jar"));
        performStreamRoundtrip(new File(srcDir, "doc.pdf"));
    }

    public void testAltStreamingEncoderDecoder()
            throws Exception
    {
        System.out.println("alt-Stream roundtrip");
        final File srcDir = new File("target/test-classes");

        performAltStreamRoundtrip(new File(srcDir, "plaintext.txt"));
        performAltStreamRoundtrip(new File(srcDir, "ajar.jar"));
        performAltStreamRoundtrip(new File(srcDir, "doc.pdf"));
    }

    public void performRoundtrip(final File sourceFile)
            throws IOException
    {
        final File compressedFile = new File(sourceFile.getParentFile(), sourceFile.getName() + ".coder.lzma");
        final File decompressedFile = new File(sourceFile.getParentFile(), sourceFile.getName() + ".coder.unlzma");

        InputStream in = null;
        OutputStream out = null;

        // compressing
        in = new BufferedInputStream(new FileInputStream(sourceFile));
        out = new BufferedOutputStream(new FileOutputStream(compressedFile));

        final Encoder encoder = new Encoder();

        encoder.setDictionarySize(1 << 23);
        encoder.setEndMarkerMode(true);
        encoder.setMatchFinder(Encoder.EMatchFinderTypeBT4);
        encoder.setNumFastBytes(0x20);

        encoder.writeCoderProperties(out);
        long fileSize = sourceFile.length();
        for (int i = 0; i < 8; i++)
        {
            out.write((int) (fileSize >>> (8 * i)) & 0xFF);
        }
        encoder.code(in, out, -1, -1, null);
        out.flush();
        out.close();
        in.close();

        // decompressing
        in = new BufferedInputStream(new FileInputStream(compressedFile));
        out = new BufferedOutputStream(new FileOutputStream(decompressedFile));

        int propertiesSize = 5;
        byte[] properties = new byte[propertiesSize];
        if (in.read(properties, 0, propertiesSize) != propertiesSize)
        {
            throw new IOException("input .lzma file is too short");
        }
        Decoder decoder = new Decoder();
        if (!decoder.setDecoderProperties(properties))
        {
            throw new IOException("Incorrect stream properties");
        }
        long outSize = 0;
        for (int i = 0; i < 8; i++)
        {
            int v = in.read();
            if (v < 0)
            {
                throw new IOException("Can't read stream size");
            }
            outSize |= ((long) v) << (8 * i);
        }
        if (!decoder.code(in, out, outSize))
        {
            throw new IOException("Error in data stream");
        }
        out.flush();
        out.close();
        in.close();

        assertTrue("Source and uncompressed content does not equals!", contentEquals(sourceFile, decompressedFile));
        assertFalse("Source and compressed content equals!", contentEquals(sourceFile, compressedFile));
    }

    public void performStreamRoundtrip(final File sourceFile)
            throws IOException
    {
        final File compressedFile = new File(sourceFile.getParentFile(), sourceFile.getName() + ".stream.lzma");
        final File decompressedFile = new File(sourceFile.getParentFile(), sourceFile.getName() + ".stream.unlzma");

        InputStream in = null;
        OutputStream out = null;

        // testing with defaults
        in = new BufferedInputStream(new FileInputStream(sourceFile));
        out = new LzmaOutputStream.Builder(new BufferedOutputStream(new FileOutputStream(compressedFile))).build();

        IOUtils.copy(in, out);
        in.close();
        out.close();

        in = new LzmaInputStream(new BufferedInputStream(new FileInputStream(compressedFile)), new Decoder());
        out = new BufferedOutputStream(new FileOutputStream(decompressedFile));

        IOUtils.copy(in, out);
        in.close();
        out.close();

        assertTrue("Source and uncompressed content does not equals!", contentEquals(sourceFile, decompressedFile));
        assertFalse("Source and compressed content equals!", contentEquals(sourceFile, compressedFile));
    }

    public void performAltStreamRoundtrip(final File sourceFile)
            throws Exception
    {
        final File compressedFile = new File(sourceFile.getParentFile(), sourceFile.getName() + ".stream.lzma");
        final File decompressedFile = new File(sourceFile.getParentFile(), sourceFile.getName() + ".stream.unlzma");

        InputStream in = null;
        OutputStream out = null;

        // testing with defaults
        System.out.println(" o Stream compression");
        in = new BufferedInputStream(new FileInputStream(sourceFile));
        out =
                new LzmaOutputStream(new BufferedOutputStream(new FileOutputStream(compressedFile)),
                        new LzmaEncoderWrapper.Builder().build());

        IOUtils.copy(in, out);
        in.close();
        out.flush();
        out.close();

        Thread.sleep(1000);

        System.out.println(" o Stream decompression");
        in =
                new LzmaInputStream(new BufferedInputStream(new FileInputStream(compressedFile)),
                        new Decoder());
        out = new BufferedOutputStream(new FileOutputStream(decompressedFile));

        IOUtils.copy(in, out);
        in.close();
        out.close();

        assertTrue("Source and uncompressed content does not equals!", contentEquals(sourceFile, decompressedFile));
        assertFalse("Source and compressed content equals!", contentEquals(sourceFile, compressedFile));
    }

}
