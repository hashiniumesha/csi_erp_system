package erp_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Role-based access control for the API.
 *
 * This is a pragmatic, documented simplification rather than full token-based
 * security: the frontend sends the logged-in user's role in an X-User-Role
 * header (see ApiClient.java on the frontend), and this filter checks that
 * header against which roles are allowed to call which URL prefix. It is not
 * cryptographically verified — a header can be spoofed by a direct API call
 * (e.g. from Postman) — the same honest limitation already documented for
 * the missing JWT layer. What it does provide is real enforcement against
 * the actual JavaFX client: a QC Officer's UI never even shows the Inventory
 * or Sales screens, and now the backend independently refuses those calls
 * too if they were ever made, rather than trusting the frontend alone.
 *
 * Admin is implicitly allowed everywhere and is never listed explicitly.
 */
@Component
public class RoleAccessFilter extends OncePerRequestFilter {

    private static final List<String> PUBLIC_PATHS = List.of("/api/auth/login");

    private static final Map<String, List<String>> RESTRICTED_PREFIXES = Map.of(
            "/api/qc", List.of("QC Officer"),
            "/api/grn", List.of("QC Officer"),
            "/api/inventory", List.of("Inventory Manager"),
            "/api/sales", List.of("Sales Officer")
    );

    // Unlike RESTRICTED_PREFIXES, these are exact method+path matches, not
    // prefixes — GET /api/users stays open to every role (it backs the
    // officer-picker dropdowns on every dashboard), but creating an account
    // is Admin-only, on the backend as well as being hidden from the UI for
    // every other role.
    private static final List<String> ADMIN_ONLY_EXACT = List.of("POST /api/users");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String role = request.getHeader("X-User-Role");

        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            chain.doFilter(request, response);
            return;
        }

        if (ADMIN_ONLY_EXACT.contains(request.getMethod() + " " + path) && !"Admin".equals(role)) {
            forbidden(response);
            return;
        }

        String matchedPrefix = RESTRICTED_PREFIXES.keySet().stream()
                .filter(path::startsWith)
                .findFirst()
                .orElse(null);

        if (matchedPrefix != null) {
            List<String> allowedRoles = RESTRICTED_PREFIXES.get(matchedPrefix);
            boolean allowed = "Admin".equals(role) || (role != null && allowedRoles.contains(role));
            if (!allowed) {
                forbidden(response);
                return;
            }
        }

        // Unlisted prefixes (login, and shared list endpoints like
        // /api/suppliers, /api/raw-materials, /api/users used to populate
        // dropdowns across every dashboard) are open to any logged-in role.
        chain.doFilter(request, response);
    }

    private void forbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"You don't have permission to do that.\"}");
    }
}
