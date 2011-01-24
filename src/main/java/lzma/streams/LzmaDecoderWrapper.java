package lzma.streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lzma.sdk.lzma.Decoder;

import org.cservenak.streams.Coder;

public class LzmaDecoderWrapper
    implements Coder
{
    private final Decoder decoder;

    public LzmaDecoderWrapper( final Decoder decoder )
    {
        this.decoder = decoder;
    }

    @Override
    public void code( final InputStream in, final OutputStream out )
        throws IOException
    {
        byte[] properties = new byte[5];
        if ( in.read( properties ) != 5 )
        {
            throw new IOException( "LZMA file has no header!" );
        }

        if ( !decoder.setDecoderProperties( properties ) )
        {
            throw new IOException( "Decoder properties cannot be set!" );
        }

        // skip 8 bytes for file size
        if ( in.skip( 8 ) != 8 )
        {
            throw new IOException( "LZMA file has no file size encoded!" );
        }

        if ( !decoder.code( in, out, -1 ) )
        {
            throw new IOException( "Decoding unsuccessful!" );
        }
    }
}
