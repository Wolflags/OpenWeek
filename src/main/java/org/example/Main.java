package org.example;


import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import okhttp3.*;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    private static final Set<WsContext> connectedClients = ConcurrentHashMap.newKeySet();
    private static final OkHttpClient httpClient = new OkHttpClient.Builder().build();

    // Historial inicial
    private static final String initialHistory = "{\"history\": [{\"role\": \"system\", \"content\": \"Eres un asistente inteligente para una presentacion en una actividad llamada Open Week en la Pontificia Universidad Católica Madre y Maestra de Santiago de los Caballeros, República Dominicana. Específicamente la carrera de Ingeniería en Ciencias de la Computación. Tu nombre es Asistente Virtual del Open Week. Siempre hablas español. Tus mensajes estrictamente nunca superan las 20 palabras.\"}, {\"role\": \"user\", \"content\": \"Hola, realiza una presentacion de ti mismo.\"}]}";

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
            });
        }).start(7000);

        app.get("/", ctx -> ctx.render("/public/chat.html"));

        app.ws("/chat", ws -> {
            ws.onConnect(ctx -> {
                System.out.println("Nuevo usuario conectado");
                connectedClients.add(ctx);

                // Envía el historial inicial al conectarse
                try {
                    sendToChatbot(initialHistory);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            ws.onMessage(ctx -> {
                String message = ctx.message();
                System.out.println("Mensaje recibido: " + message);

                try {
                    // Envía el mensaje al servidor Flask
                    sendToChatbot(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            ws.onClose(ctx -> {
                System.out.println("Usuario desconectado");
                connectedClients.remove(ctx);
            });

            ws.onError(ctx -> {
                System.out.println("Ocurrió un error en el WebSocket");
            });
        });
    }

    private static void sendToChatbot(String userMessage) {
        try {
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(JSON, userMessage);
            Request request = new Request.Builder()
                    .url("http://localhost:5000/chat")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    for (WsContext client : connectedClients) {
                        client.send(line);
                        System.out.println("Mensaje enviado al cliente: " + line);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            for (WsContext client : connectedClients) {
                client.send("Error al comunicarse con el chatbot: " + e.toString());
            }
        }
    }
}