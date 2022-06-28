package org.texttechnologylab.DockerUnifiedUIMAInterface.connection;

import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

public class DUUIWebsocketHandler implements IDUUIConnectionHandler {
    /***
     * @author
     * Dawit Terefer
     */
    private boolean success;
    private String uri;
    private WebsocketClient client;
    private SocketIO socketIO;
    public DUUIWebsocketHandler() {
    }


    @Override
    public void initiate(String uri) throws URISyntaxException {
        // this.socketIO= new SocketIO("http://127.0.0.1:9716");

        this.uri = uri.replaceFirst("http", "ws") + DUUIComposer.V1_COMPONENT_ENDPOINT_PROCESS_WEBSOCKET;
        this.client = new WebsocketClient(new URI(this.uri));
        /***
         * @edited
         * Givara Ebo
         */

        try {
            this.client.connectBlocking();
        } catch (InterruptedException e) {
            System.out.println("[WebsocketHandler] Connection to websocket failed!");
            System.exit(1);
        }
        //this.socketIO.close();


    }

    @Override
    public boolean success() {
        return success;
    }

    @Override
    public byte[] sendAwaitResponse(byte[] serializedObject) throws IOException {
        try {
            client.send(serializedObject);

        } catch (WebsocketNotConnectedException e) {
            System.out.println("WebsocketNotConnectedException Error not working");
        }
        while (client.messageStack.isEmpty()) {
            int c = 0;
        }
        byte[] result = client.messageStack.get(0);
        success = true;
        client.close();

        return result;
    }



}
