package gb;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;


public class DownloadFiles {

    private static String title;
    private static String getHtmlStartSearch;
    private static String getHtmlStartSearchOffset;
    private static String getHtmlEndSearch;
    private static String getHtmlEndSearchOffset;
    private static String fileExtension;
    private static String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    public static volatile boolean shouldStop = false;
    public static volatile boolean isRunning = false;


    public static void run(String titlePassed, String userAgentPassed) throws HttpStatusException {
        isRunning = true;
        title = titlePassed;
        userAgent = userAgentPassed;
        Map<Integer, String> articlesMap = readArticlesFromFile(title);
        List<String> variants = readVariantsFromFile(title);
        List<Integer> keysToRemove = new ArrayList<>();

        readParametersFromFile(title);
        readFileExtensionFromFile(title);

        try {
            if (articlesMap != null) {

                for (Map.Entry<Integer, String> article : articlesMap.entrySet()) {
                    if (shouldStop) {
                        break;
                    }
                    String articleValue = article.getValue();
                    Integer articleKey = article.getKey();

                    for (String variant : variants) {
                        String url = variant + articleValue;
                        if (url != null) {
                            String imageLink = getImageLink(url);
                            if (imageLink != null) {
                                if (!shouldStop) {
                                    downloadImage(imageLink, articleValue);
                                    keysToRemove.add(articleKey);
                                }
                                break;
                            } else {
                                System.out.println("Image link not found for article: " + articleValue);
                            }
                        } else {
                            System.out.println("URL is null");
                        }
                    }
                }


            }
        } finally {
            for (Integer key : keysToRemove) {
                articlesMap.remove(key);
            }
            shouldStop = false;
            isRunning = false;
        }


        writeArticlesToFile(title, articlesMap);
    }


    public static Map<Integer, String> readArticlesFromFile(String title) {
        Map<Integer, String> articlesMap = new HashMap<>();
        int lineNumber = 0;

        try {
            FileReader fileReader = new FileReader(title + "/Need to download.txt");
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!line.trim().isEmpty()) { // Проверка, что строка не пустая
                    articlesMap.put(++lineNumber, line);
                }
            }

            bufferedReader.close();
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return articlesMap;
    }

    public static void writeArticlesToFile(String title, Map<Integer, String> articlesMap) {
        try {
            FileWriter fileWriter = new FileWriter(title + "/Need to download.txt");
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            for (Map.Entry<Integer, String> entry : articlesMap.entrySet()) {
                bufferedWriter.write(entry.getValue());
                bufferedWriter.newLine(); // для создания новой строки после каждого артикула
            }

            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> readVariantsFromFile(String title) {
        List<String> variants = new ArrayList<>();

        try {
            FileReader fileReader = new FileReader(title + "/Variants.txt");
            BufferedReader bufferedReader = new BufferedReader(fileReader);


            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!line.trim().isEmpty()) { // Проверка, что строка не пустая
                    variants.add(line);
                }
            }

            bufferedReader.close();
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return variants;
    }


    public static void readParametersFromFile(String title) {
        try {
            FileReader fileReader = new FileReader(title + "/Parameters.txt");
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            getHtmlStartSearch = bufferedReader.readLine();
            getHtmlStartSearchOffset = bufferedReader.readLine();
            getHtmlEndSearch = bufferedReader.readLine();
            getHtmlEndSearchOffset = bufferedReader.readLine();


            bufferedReader.close();
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readFileExtensionFromFile(String title) {
        try {
            FileReader fileReader = new FileReader(title + "/Extension.txt");
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            fileExtension = bufferedReader.readLine();
            bufferedReader.close();
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static String getImageLink(String productUrl) {
        String html = getHtml(productUrl, userAgent);
        if (html == null) {
            return null;
        }

        int startIndex = html.indexOf(getHtmlStartSearch) + Integer.parseInt(getHtmlStartSearchOffset);
        int endIndex = html.indexOf(getHtmlEndSearch, startIndex) + Integer.parseInt(getHtmlEndSearchOffset);
        if ((startIndex == -1) || (startIndex >= endIndex)) {
            return null;
        }
        if (startIndex + 200 > endIndex) {
            String imageLink = html.substring(startIndex, endIndex);
            return imageLink;
        }
        return null;

    }

    private static String getHtml(String url, String userAgent) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                Document doc = Jsoup.connect(url)
                        .timeout(5000)
                        .userAgent(userAgent)
                        .get();
                String html = doc.html();
                return html;
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    private static void downloadImage(String imageLink, String articleNumber) {
        if (shouldStop) {
            return;
        }
        try {
            URL url = new URL(imageLink);
            Path directoryPath = Paths.get(title + "/Downloaded");
            if (!Files.exists(directoryPath)) {
                Files.createDirectory(directoryPath);
            }
            String fileName = articleNumber + "." + fileExtension;
            Path filePath = directoryPath.resolve(fileName);
            Files.copy(url.openStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            System.out.println("Article number: " + articleNumber);
            e.printStackTrace();
        }
    }


}

