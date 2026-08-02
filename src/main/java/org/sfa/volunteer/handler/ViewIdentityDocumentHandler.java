package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sfa.volunteer.VolunteerApplication;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.service.IdentityDocumentStorageService;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.Cors;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Returns a volunteer's stored identity document for preview or download.
 *
 * Expired documents are still returned. The page shows them read-only rather
 * than hiding them - the volunteer needs to see what they are replacing, and
 * staff need to see what expired.
 *
 * There is deliberately no DeleteIdentityDocumentHandler counterpart to this
 * one, unlike the profile image handlers. Volunteers cannot remove an identity
 * document; a slot changes only by being overwritten through upload.
 */
public class ViewIdentityDocumentHandler
        implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final String METHODS = "POST,OPTIONS";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true)
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

    private static final ConfigurableApplicationContext CTX =
            new SpringApplicationBuilder(VolunteerApplication.class)
                    .web(WebApplicationType.NONE)
                    .properties(
                            "spring.main.web-application-type=none",
                            "spring.autoconfigure.exclude=org.springframework.cloud.function.serverless.web.ServerlessAutoConfiguration"
                    )
                    .run();

    private final IdentityDocumentStorageService storage =
            CTX.getBean(IdentityDocumentStorageService.class);
    private final UserService userService = CTX.getBean(UserService.class);

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        try {
            String region = header(event, "X-Dev-Region", "us-east-1");

            Map<String, Object> body = readJsonBody(event);
            String userId = asString(body.get("userId"));
            if (isBlank(userId)) {
                Map<String, Object> resp = apiError(400, "userId is required");
                debugResponse(context, "IDENTITY_VIEW_400", resp);
                return resp;
            }

            int documentSlot = asSlot(body.get("documentSlot"));

            // AUTH
            AuthInfo auth = authInfoFromEvent(event);
            authorizeUser(auth, userId);

            var docOpt = storage.download(userId, documentSlot, region);
            if (docOpt.isEmpty()) {
                // Nothing uploaded in this slot yet - the first-time state, not
                // an error. The page renders its empty upload control on 204.
                Map<String, Object> resp = apiNoContent();
                debugResponse(context, "IDENTITY_VIEW_204", resp);
                return resp;
            }

            var doc = docOpt.get();

            Map<String, Object> resp = new HashMap<>();
            resp.put("statusCode", 200);

            Map<String, String> h = Cors.headers(METHODS);
            h.put("Content-Type", doc.contentType()); // image/jpeg, image/png or application/pdf
            resp.put("headers", h);

            resp.put("isBase64Encoded", true);
            resp.put("body", Base64.getEncoder().encodeToString(doc.bytes()));

            debugResponse(context, "IDENTITY_VIEW_200", resp);
            return resp;

        } catch (Forbidden ex) {
            return apiErrorWithMethods(403, ex.getMessage(), METHODS);
        } catch (Exception e) {
            return handleKnownExceptions(e, METHODS);
        }
    }

    // ---------------- AUTH ----------------

    private static class Forbidden extends RuntimeException {
        Forbidden(String msg) { super(msg); }
    }

    private record AuthInfo(String email, String groups, boolean present) {}

    private AuthInfo authInfoFromEvent(Map<String, Object> event) {
        Map<String, Object> claims = claims(event);

        String email = asString(claims.get("email"));
        if (isBlank(email)) email = asString(claims.get("cognito:username"));
        if (isBlank(email)) email = asString(claims.get("username"));

        String groups = null;
        Object g = claims.get("cognito:groups");
        if (g instanceof String s) groups = s;
        else if (g instanceof Iterable<?> it) {
            StringBuilder sb = new StringBuilder();
            for (Object o : it) {
                if (sb.length() > 0) sb.append(",");
                sb.append(String.valueOf(o));
            }
            groups = sb.toString();
        }

        boolean present = !claims.isEmpty();
        return new AuthInfo(email, groups, present);
    }

    /**
     * Admins pass through; everyone else may only view their own document.
     * If the team adds a steward group for audit access, this is the method it
     * would be added to - read access only, never the upload handler.
     */
    private void authorizeUser(AuthInfo auth, String targetUserId) {
        if (!auth.present) throw new Forbidden("Missing Cognito authorizer/JWT claims");

        boolean isAdmin = auth.groups != null && auth.groups.toLowerCase().contains("admin");
        if (isAdmin) return;

        if (isBlank(auth.email)) throw new Forbidden("JWT email missing");

        String callerSid;
        try {
            callerSid = userService.getUserIdByEmailForAuth(auth.email);
        } catch (Exception e) {
            throw new Forbidden("JWT user not mapped to DB user");
        }
        if (isBlank(callerSid)) throw new Forbidden("JWT user not mapped to DB user");
        if (!callerSid.equals(targetUserId)) throw new Forbidden("Not allowed");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> claims(Map<String, Object> event) {
        Object rc = event.get("requestContext");
        if (rc instanceof Map<?, ?> rcMap) {
            Object auth = ((Map<String, Object>) rcMap).get("authorizer");
            if (auth instanceof Map<?, ?> authMap) {
                Object c = ((Map<String, Object>) authMap).get("claims");
                if (c instanceof Map<?, ?> m) return (Map<String, Object>) m;
            }
        }

        Map<String, Object> out = new HashMap<>();
        Object email = event.get("cognitoEmail");
        Object groups = event.get("cognitoGroups");

        if (email != null) out.put("email", String.valueOf(email));
        if (groups != null) out.put("cognito:groups", String.valueOf(groups));

        return out;
    }

    // ---------------- helpers ----------------

    private static Map<String, Object> apiNoContent() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("statusCode", 204);
        Map<String, String> h = Cors.headers(METHODS);
        h.remove("Content-Type");
        resp.put("headers", h);
        resp.put("isBase64Encoded", false);
        resp.put("body", "");
        return resp;
    }

    private static Map<String, Object> readJsonBody(Map<String, Object> event) throws Exception {
        Object raw = event.get("body");
        if (raw == null) return Map.of();

        if (raw instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mm = (Map<String, Object>) m;
            return mm;
        }

        String body = String.valueOf(raw).trim();
        if (body.isBlank()) return Map.of();

        boolean isB64 = Boolean.TRUE.equals(event.get("isBase64Encoded"));
        if (isB64) {
            body = new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8).trim();
        }

        if (body.startsWith("\"") && body.endsWith("\"")) {
            body = MAPPER.readValue(body, String.class).trim();
        }

        if (body.isBlank()) return Map.of();
        return MAPPER.readValue(body, new TypeReference<>() {});
    }

    /** Defaults to the primary document when the client omits the slot. */
    private static int asSlot(Object raw) {
        if (raw == null) return 1;
        try {
            return Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            // Let the service reject it rather than silently reading slot 1.
            return -1;
        }
    }

    private static String header(Map<String, Object> event, String name, String def) {
        Object headersObj = event.get("headers");
        if (!(headersObj instanceof Map)) return def;

        @SuppressWarnings("unchecked")
        Map<String, Object> headers = (Map<String, Object>) headersObj;

        for (Map.Entry<String, Object> en : headers.entrySet()) {
            if (en.getKey() != null && en.getKey().equalsIgnoreCase(name)) {
                String v = asString(en.getValue());
                return isBlank(v) ? def : v;
            }
        }
        return def;
    }

    private static Map<String, Object> apiError(int status, String msg) {
        return apiErrorWithMethods(status, msg, METHODS);
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String safeMsg(Exception e) {
        String m = e.getMessage();
        return (m == null || m.isBlank()) ? e.getClass().getSimpleName() : m;
    }

    private static void debugResponse(Context context, String label, Map<String, Object> resp) {
        if (context == null) return;
        try {
            Object h = resp.get("headers");
            context.getLogger().log(label + " statusCode=" + resp.get("statusCode") + "\n");
            context.getLogger().log(label + " headers=" + String.valueOf(h) + "\n");
            context.getLogger().log(label + " isBase64Encoded=" + resp.get("isBase64Encoded") + "\n");
        } catch (Exception e) {
            context.getLogger().log(label + " debug failed: " + e.getMessage() + "\n");
        }
    }

    private static Map<String, Object> handleKnownExceptions(Exception e, String methods) {
        if (e instanceof UserNotFoundException unfe) {
            return apiErrorWithMethods(404, "User not found: " + unfe.getMessage(), methods);
        }

        if (e instanceof ResponseStatusException rse) {
            int code = rse.getStatusCode().value();
            String msg = (rse.getReason() == null || rse.getReason().isBlank())
                    ? "Request failed" : rse.getReason();
            return apiErrorWithMethods(code, msg, methods);
        }

        return apiErrorWithMethods(500, safeMsg(e), methods);
    }

    private static Map<String, Object> apiErrorWithMethods(int status, String msg, String methods) {
        try {
            Map<String, Object> resp = new HashMap<>();
            resp.put("statusCode", status);
            resp.put("headers", Cors.headers(methods));
            resp.put("isBase64Encoded", false);
            resp.put("body", MAPPER.writeValueAsString(Map.of("message", msg)));
            return resp;
        } catch (Exception ex) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("statusCode", status);
            resp.put("headers", Cors.headers(methods));
            resp.put("isBase64Encoded", false);
            resp.put("body", "{\"message\":\"" + msg.replace("\"", "'") + "\"}");
            return resp;
        }
    }
}