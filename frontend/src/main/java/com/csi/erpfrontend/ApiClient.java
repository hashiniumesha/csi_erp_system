package com.csi.erpfrontend;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Central HTTP helper for talking to the Spring Boot backend.
 *
 * Every screen used to build its own HttpClient/HttpRequest by hand and
 * duplicate the same try/catch. Centralising it here means:
 *  - the base URL and the X-User-Role header (used by the backend's RBAC
 *    guard) only need to be set in one place, and
 *  - failures are translated into ApiException with a message that is
 *    already safe to show directly in the UI, instead of a raw stack trace.
 */
public class ApiClient {

    private static final String BASE_URL = "http://localhost:8080";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static class ApiException extends RuntimeException {
        public ApiException(String message) { super(message); }
    }

    public static JSONObject getObject(String path) {
        return new JSONObject(get(path));
    }

    public static JSONArray getArray(String path) {
        return new JSONArray(get(path));
    }

    public static JSONObject post(String path, JSONObject body) {
        String responseBody = send("POST", path, body.toString());
        return responseBody.isBlank() ? new JSONObject() : new JSONObject(responseBody);
    }

    public static String postForText(String path, JSONObject body) {
        return send("POST", path, body.toString());
    }

    public static JSONObject put(String path, JSONObject body) {
        String responseBody = send("PUT", path, body.toString());
        return responseBody.isBlank() ? new JSONObject() : new JSONObject(responseBody);
    }

    public static void delete(String path) {
        send("DELETE", path, null);
    }

    private static String get(String path) {
        return send("GET", path, null);
    }

    private static String send(String method, String path, String jsonBody) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path))
                    .header("Content-Type", "application/json");

            if (Session.getRoleName() != null) {
                builder.header("X-User-Role", Session.getRoleName());
            }

            HttpRequest request = switch (method) {
                case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
                case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
                case "DELETE" -> builder.DELETE().build();
                default -> builder.GET().build();
            };

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }

            throw new ApiException(friendlyMessage(response.statusCode(), response.body()));
        } catch (ApiException e) {
            throw e;
        } catch (java.net.ConnectException e) {
            throw new ApiException("Can't reach the server. Make sure the backend is running.");
        } catch (Exception e) {
            throw new ApiException("Something went wrong: " + e.getMessage());
        }
    }

    // Backend error bodies are currently raw exception text (e.g. "Supplier not
    // found") or a stack trace for unexpected failures. Until the backend
    // returns structured error JSON, this keeps the UI from ever showing a
    // stack trace to the user.
    private static String friendlyMessage(int statusCode, String rawBody) {
        if (statusCode == 403) {
            return "You don't have permission to do that.";
        }
        if (rawBody != null && !rawBody.isBlank() && !rawBody.trim().startsWith("{") && rawBody.length() < 200) {
            return rawBody;
        }
        return "The server couldn't complete that request. Please check your input and try again.";
    }
}
