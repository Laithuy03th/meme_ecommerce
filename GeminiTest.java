import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GeminiTest {
    public static void main(String[] args) throws Exception {
        String apiKey = "AIzaSyDdm4WNuXjWKXnR_zbIZhwP4DjuftryemA";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=" + apiKey;
        String json = "{\"model\": \"models/gemini-embedding-001\", \"content\": {\"parts\": [{\"text\": \"Hello\"}]}}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status: " + response.statusCode());
        
        String body = response.body();
        // Just count commas in the values array
        int count = body.split(",").length;
        System.out.println("Values count: " + count);
    }
}
