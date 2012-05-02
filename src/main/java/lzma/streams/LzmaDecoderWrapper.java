/*
 *  Copyright (c) 2011 Tamas Cservenak. All rights reserved.
 *
 *  <tamas@cservenak.com>
 *  http://www.cservenak.com/
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

import lzma.sdk.lzma.Decoder;
import org.cservenak.streams.Coder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LzmaDecoderWrapper
        implements Coder
{
    private final Decoder decoder;

    public LzmaDecoderWrapper(final Decoder decoder)
    {
        this.decoder = decoder;
    }

    @Override
    public void code(final InputStream in, final OutputStream out)
            throws IOException
    {
        byte[] properties = new byte[5];
        if (in.read(properties) != 5)
        {
            throw new IOException("LZMA file has no header!");
        }

        if (!decoder.setDecoderProperties(properties))
        {
            throw new IOException("Decoder properties cannot be set!");
        }

		long outSize = 0;
		for (int i = 0; i < 8; i++)
		{
			int v = in.read();
			if (v < 0)
				throw new IOException("Can't read stream size");
			outSize |= ((long)v) << (8 * i);
		}

        if (!decoder.code(in, out, outSize))
        {
            throw new IOException("Decoding unsuccessful!");
        }
    }
}
