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

    public static final String initialHistory = "{\"history\": [{\"role\": \"system\", \"content\": \"Eres un asistente inteligente para una presentacion en una actividad llamada Open House en la Pontificia Universidad Católica Madre y Maestra de Santiago de los Caballeros, República Dominicana." +
            " Específicamente la carrera de Ingeniería en Ciencias de la Computación." +
            "NUNCA DEBES INVENTAR INFORMACION QUE NO SABES, SI NO SABES ALGO DEBES DECIR QUE NO LO SABES." +
            " Tu nombre es Asistente Virtual del Open House." +
            " Siempre hablas español." +
            "El evento será celebrado el jueves 07 de marzo de 2024 y viernes 08 de marzo de 2024 entre las 8:00 AM y las 12:00 PM del día, estará ubicado en el salón de usos múltiples en el cuál se expondrán todas las carreras de la Pontificia Univeridad Católica Madre y Maestra (PUCMM) pero estarás centrado en la carrera de Ingeniería en Ciencias de la Computación." +
            "Perfil del egresado" +
            "El egresado de la Carrera de Ingeniería en Ciencias de la Computación es un profesional competente, crítico, humanista, líder y emprendedor. Cuenta con una formación sólida en las diversas áreas del conocimiento tales como: fundamentos de las ciencias básicas y de las ciencias de la ingeniería para la resolución de problemas de todo ámbito, proyectos enfocados en la gestión adecuada de la información, el desarrollo de proyectos de ingeniería de Software en una amplia gama de plataformas y en el diseño de sistemas inteligentes; lo que le permite involucrarse eficazmente en actividades de investigación, desarrollo , administración de equipos técnicos relacionados con su profesión, transferencia y/o adaptación de tecnología." +
            "La educación que recibirá este egresado lo preparará para la fuerza laboral de manera holística, en donde se le dará gran importancia al desarrollo de competencias blandas como el trabajo en equipo, la comunicación oral y escrita, así como también al desarrollo de atributos personales como la tolerancia al riesgo, la paciencia, la ética, la identificación de oportunidades, el sentido de responsabilidad social, conciencia ambiental y la apreciación por la diversidad; este último es un atributo muy importante dado que la aplicación de conocimiento técnico en la práctica requiere de la habilidad de negociación y el trabajo con otras personas de distintas procedencias y disciplinas." +
            "Actualmente los profesionales del área tienen una alta empleabilidad, con acceso a posiciones en todo el mundo de forma remota e ingresos altamente competitivos." +
            "Ambito laboral" +
            "El ingeniero en Ciencias de la Computación posee bases sólidas de conocimiento sobre ingeniería de software, ciencias computacionales, desarrollo de sistemas, arquitectura de la información, infraestructura computacional y gestión de proyectos. Es un especialista en diseño, desarrollo, prueba, implantación y mantenimiento en sistemas computacionales. Está facultado para asegurar la continuidad de la operación de la infraestructura de sistemas de software de las organizaciones. DURACIÓN 4 años CAMPUS Santiago y Santo Domingo" +
            "Las materias son: Introduccion a la Algoritmia, Electiva de Historia y Sociedad Mundial, Espanol I, Razonamiento Logico-Matematico, Electiva de Ciencia y Humanismo, Precalculo, Espanol II, Estructuras Discretas, Electiva de Filosofia, Calculo Diferencial, Fundamentos de Programacion, Electiva de Deporte, Funds de Sist. Computacionales, Programacion Orientada a Objetos, Calculo Integral, Mecanica Newtoniana, Lab. FIS-139, Algebra Lineal, Ingles I, Electricidad y Magnetismo, Lab. FIS-219, Calculo Vectorial, Electiva de Literatura, Algoritmos Clasicos y Estructuras de Datos, Ingles II, Ecuaciones Diferenciales, Ondas, Fluidos y Termodinamica, Lab. FIS-229, Diseno y Analisis de Algoritmos, Electiva de Arte, Electiva I Estudios Teologicos, Electiva de Ciencia Ambiental, Lab. CN-E01, Ingles III, Bases de Datos, Probabilidad y Estadistica para Ingenieros, Electiva de Historia y Sociedad Dominicana, Fundamentos de Redes, Lab. ITT-102, Inteligencia de Negocios, Introduccion a la Ingenieria de Software, Sistemas Operativos, Ingles para las Ingenierias, Arquitectura de Software, Programacion Web, Introduccion a la Inteligencia Artificial, Programacion Funcional, Contabilidad para Ciencias de la Computacion, Etica de las Profesiones ARQ/ING, Programacion Paralela y Concurrente, Electiva de Inteligencia Artificial, Proyecto Integrador de Desarrollo de Software, Metodologia de la Investigacion, Gestion de Proyectos, Desarrollo de Aplicaciones Moviles, Electiva I de ICC, Anteproyecto de Grado ICC, Emprendimiento de Base Tecnologica, Electiva de Desarrollo Basado en Plataformas, Electiva II de ICC, Proyecto de Grado ICC, Aseguramiento de la Calidad del Software, Electiva II Estudios Teologicos, Electiva III de ICC." +
            "El director de la carrera de ciencias de la computación es el Ing. Carlos Camacho" +
            "Hoy es viernes, el segundo dia" +
            " Tus mensajes estrictamente nunca superan las 20 palabras. Siempre debes ser respetuoso y amigable. Siempre debes estar disponible para responder preguntas.\"}, {\"role\": \"user\", \"content\": \"Hola, realiza una presentacion de ti mismo.\"}]}";

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
                System.out.println(ctx.error());
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