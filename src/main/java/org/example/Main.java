package org.example;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7000);

        app.get("/test-connection", ctx -> {
            String flaskApiUrl = "http://localhost:5000/chat";
            String json = "{\"history\": [{\"role\": \"system\", \"content\": \"Eres un asistente inteligente para una presentacion en una actividad llamada Open Week en la Pontificia Universidad Católica Madre y Maestra de Santiago de los Caballeros, República Dominicana. Específicamente la carrera de Ingeniería en Ciencias de la Computación. Tu nombre es Asistente Virtual del Open Week. Siempre hablas español.\"}, {\"role\": \"user\", \"content\": \"Hola, realiza una presentacion de ti mismo.\"}]}";

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost request = new HttpPost(flaskApiUrl);
                StringEntity params = new StringEntity(json);
                request.addHeader("content-type", "application/json");
                request.setEntity(params);

                HttpResponse response = httpClient.execute(request);
                String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");

                // Parsear la respuesta JSON para obtener solo el mensaje
                JsonParser parser = new JsonParser();
                JsonElement responseElement = parser.parse(responseString);

                // Verificar si la respuesta es un objeto JSON
                if (responseElement.isJsonObject()) {
                    JsonObject responseObject = responseElement.getAsJsonObject();

                    // Verificar si la clave "choices" existe en el objeto responseObject
                    if (responseObject.has("choices")) {
                        JsonArray choicesArray = responseObject.get("choices").getAsJsonArray();
                        if (choicesArray.size() > 0) {
                            String message = choicesArray.get(0).getAsJsonObject().get("message").getAsString();
                            ctx.result(message);
                            return;
                        }
                    }

                    // Si no se encontró el mensaje, devolver un mensaje de error
                    ctx.status(500).result("Error: No se encontró el mensaje en la respuesta.");
                } else {
                    // Si la respuesta no es un objeto JSON, devolver la respuesta tal cual
                    ctx.result(responseString);
                }

            } catch (IOException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });
    }
}