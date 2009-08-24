/*
 *  Copyright (c) 2009 Julien Ponge. All rights reserved.
 *
 *  <julien.ponge@gmail.com>
 *  http://julien.ponge.info/
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
 *
 *  This work is based on the LZMA SDK by Igor Pavlov.
 *  The LZMA SDK is placed under the public domain, and can be obtained from
 *
 *      http://www.7-zip.org/sdk.html
 *
 *  The LzmaInputStream and LzmaOutputStream classes were inspired by the
 *  work of Christopher League, although they are not derivative works.
 *
 *      http://contrapunctus.net/league/haques/lzmajio/
 */

package lzma.streams;

import org.junit.Test;
import org.apache.commons.io.FileUtils;
import static org.apache.commons.io.FileUtils.*;
import static org.apache.commons.io.FileUtils.contentEquals;

import java.io.*;

import lzma.sdk.lzma.Decoder;
import junit.framework.TestCase;

public class RountTripTest extends TestCase
{
    private void copy(InputStream in, OutputStream out) throws IOException
    {
        int nread = 0;
        final byte[] buffer = new byte[1024];
        while ((nread = in.read(buffer)) != -1)
        {
            out.write(buffer, 0, nread);
        }
    }

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
}
