package lzma.streams;

import java.io.IOException;
import java.io.OutputStream;

import lzma.sdk.lzma.Encoder;

import org.cservenak.streams.CoderOutputStream;

public class LzmaCompressingOutputStream
    extends CoderOutputStream
{
    public LzmaCompressingOutputStream( final OutputStream out, final LzmaEncoderWrapper wrapper )
        throws IOException
    {
        super( out, wrapper );
    }

    public LzmaCompressingOutputStream( final OutputStream out, final Encoder lzmaEncoder )
        throws IOException
    {
        this( out, new LzmaEncoderWrapper( lzmaEncoder ) );
    }
}
