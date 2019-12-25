package g4rb4g3.at.abrptransmittercompanion;

import fi.iki.elonen.NanoHTTPD;

public class AbrpTransmitterServer extends NanoHTTPD {

  public AbrpTransmitterServer(int port) {
    super(port);
  }

  @Override
  public Response serve(IHTTPSession session) {
    String msg = "<html><body><h1>Hello server</h1>\n";
    msg += "<p>Hello world!</p>";
    return newFixedLengthResponse( msg + "</body></html>\n" );
  }
}
