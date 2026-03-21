package main;

import control.GameEngine;
import net.web.EmbeddedServer;
import utils.Logger;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class Main {
    public static void main(String[] args) {
        System.out.println();

        EmbeddedServer embeddedServer = new EmbeddedServer();
        embeddedServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(embeddedServer::stop));

        try {
            URL serverUrl = new URI("http://localhost:8080").toURL();
            if (args.length == 1) serverUrl = new URI(args[0]).toURL();
            new GameEngine(serverUrl);
        } catch (IllegalArgumentException | MalformedURLException | URISyntaxException ignored) {
            Logger.log("Main", "Invalid argument provided: " + (args.length > 0 ? args[0] : ""));
            Logger.log("Main", "Is it the correct server's URL?");
            System.exit(0);
        }
    }
}
