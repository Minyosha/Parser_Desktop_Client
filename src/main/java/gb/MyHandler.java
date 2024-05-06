package gb;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.HttpStatusException;

import java.io.*;

public class MyHandler implements HttpHandler {
    private static String currentProject;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            handlePostRequest(exchange);
        } else {
            sendDefaultResponse(exchange);
        }
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
        StringBuilder requestBody = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            requestBody.append(line);
        }
        reader.close();

        String query = exchange.getRequestURI().getQuery();
        if (query != null && query.contains("update")) {
            sendResponse(exchange, "Project updated successfully!");
            processUpdateRequest(requestBody.toString());
        }

        if (query != null && query.contains("pause")) {
            if (!DownloadFiles.isRunning) {
                sendResponse(exchange, "Project is not running!");
            } else {
                DownloadFiles.shouldStop = true;
                sendResponse(exchange, "Project paused successfully!");
            }
        }

        if (query != null && query.contains("run")) {
            if (DownloadFiles.isRunning) {
                sendResponse(exchange, "Project is already running!");
            } else {
                sendResponse(exchange, "Project started successfully!");
                new Thread(() -> {
                    processRunRequest(requestBody.toString());
                }).start();
            }
        }

        if (query != null && query.contains("report")) {
            String response = processReportRequest(requestBody.toString());
            sendResponse(exchange, response);

        }

    }

    private String processReportRequest(String requestData) {
        try {
            JSONObject jsonObject = new JSONObject(requestData);
            String title = jsonObject.getString("title");
            String currentDirectory = System.getProperty("user.dir");
            currentDirectory = (currentDirectory + "\\" + title);
            File dir = new File(currentDirectory);
            if (dir.exists() && dir.isDirectory()) {
                String os = System.getProperty("os.name").toLowerCase();
                try {
                    if (os.contains("win")) {
                        Runtime.getRuntime().exec("explorer.exe " + currentDirectory);
                        return ("Report created successfully!");
                    } else if (os.contains("mac")) {
                        Runtime.getRuntime().exec("open " + currentDirectory);
                        return ("Report created successfully!");
                    } else if (os.contains("nix") || os.contains("nux")) {
                        Runtime.getRuntime().exec("xdg-open " + currentDirectory);
                        return ("Report created successfully!");
                    } else {
                        return ("Failed to create report. Unsupported operating system");
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return ("Unknown error occurred");
                }
            } else {
                return ("No such directory");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ("Директория не существует");
    }


    private void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private void processRunRequest(String requestData) {
        try {
            JSONObject jsonObject = new JSONObject(requestData);
            String userAgent = jsonObject.getString("userAgent");
            String title = jsonObject.getString("title");
            DownloadFiles.run(title, userAgent);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (HttpStatusException e) {
            e.printStackTrace();
        }
    }

    private void sendDefaultResponse(HttpExchange exchange) throws IOException {
        String response = "Hello, this is the server response!";
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private void processRequest(String requestBody) {
        System.out.println("Processing POST request with data: " + requestBody);
    }

    public void processUpdateRequest(String requestData) {
        JSONObject jsonObject = new JSONObject(requestData);

        JSONArray articles = jsonObject.getJSONArray("articles");
        JSONArray variants = jsonObject.getJSONArray("variants");
        String fileExtension = jsonObject.getString("fileExtension");
        String title = jsonObject.getString("title");
        String description = jsonObject.getString("description");
        String getHtmlStartSearch = jsonObject.getString("getHtmlStartSearch");
        String getHtmlStartSearchOffset = jsonObject.getString("getHtmlStartSearchOffset");
        String getHtmlEndSearch = jsonObject.getString("getHtmlEndSearch");
        String getHtmlEndSearchOffset = jsonObject.getString("getHtmlEndSearchOffset");

        currentProject = jsonObject.getString("title");


        System.out.println("Articles:");
        for (int i = 0; i < articles.length(); i++) {
            System.out.println("Article: " + articles.getString(i));
        }

        System.out.println("\nVariants:");
        for (int i = 0; i < variants.length(); i++) {
            System.out.println("Variant: " + variants.getString(i));
        }

        System.out.println("\nFile Extension: " + fileExtension);
        System.out.println("Title: " + title);
        System.out.println("Description: " + description);

        File directory = new File(title);
        if (!directory.exists()) {
            directory.mkdir();
        }

        File downloadedDirectory = new File(directory + "/Downloaded");
        if (!downloadedDirectory.exists()) {
            downloadedDirectory.mkdir();
        }

        File fileForTitle = new File(directory + "/" + title + ".txt");
        try (PrintWriter out = new PrintWriter(new FileWriter(fileForTitle))) {
            out.println(title);
            out.println(description);
        } catch (IOException e) {
            e.printStackTrace();
        }

        File fileForExtension = new File(directory + "/Extension.txt");
        try (PrintWriter out = new PrintWriter(new FileWriter(fileForExtension))) {
            out.println(fileExtension);
            out.println("\r");
        } catch (IOException e) {
            e.printStackTrace();
        }

        File fileParams = new File(directory + "/Parameters.txt");
        try (PrintWriter out = new PrintWriter(new FileWriter(fileParams))) {
            out.println(getHtmlStartSearch);
            out.println(getHtmlStartSearchOffset);
            out.println(getHtmlEndSearch);
            out.println(getHtmlEndSearchOffset);
        } catch (IOException e) {
            e.printStackTrace();
        }

        File downloadedFiles = new File(directory + "/Downloaded.txt");
        try (PrintWriter out = new PrintWriter(new FileWriter(downloadedFiles))) {
            out.println("\r");
        } catch (IOException e) {
            e.printStackTrace();
        }

        File articlesFile = new File(directory + "/Articles.txt");
        try (PrintWriter out = new PrintWriter(new FileWriter(articlesFile))) {
            for (Object article : articles) {
                out.println(article.toString());
            }
            out.println("\r");
        } catch (IOException e) {
            e.printStackTrace();
        }

        File needToDownload = new File(directory + "/Need to download.txt");
        try (PrintWriter out = new PrintWriter(new FileWriter(needToDownload))) {
            for (Object article : articles) {
                out.println(article.toString());
            }
            out.println("\r");
        } catch (IOException e) {
            e.printStackTrace();
        }

        File variantsFile = new File(directory + "/Variants.txt");
        try (PrintWriter out = new PrintWriter(new FileWriter(variantsFile))) {
            for (Object variant : variants) {
                out.println(variant.toString());
            }
            out.println("\r");
        } catch (IOException e) {
            e.printStackTrace();
        }

        File missedArticlesFile = new File(directory + "/MissedArticles.txt");
        try {
            missedArticlesFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}