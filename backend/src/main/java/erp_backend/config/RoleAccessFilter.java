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
            "/api/sales", List.of("Sales Officer"),
            // Deliberately QC Officer here, not Inventory Manager - Admin
            // and QC review damage/wastage, even though Inventory Manager
            // is who records it via /api/inventory/damaged.
            "/api/damaged-products", List.of("QC Officer"),
            // Per proposal section 5.7's access matrix: P&L is Admin-only
            // (the "Accountant" role it also lists was never implemented
            // in this system - see the earlier scope-reduction decision).
            "/api/reports/profit-loss", List.of()
    );

    // Unlike RESTRICTED_PREFIXES, these are keyed by "METHOD /path/prefix"
    // and matched with startsWith on method+path together — this is what
    // lets a specific method on a path be locked down while other methods
    // on that same path (usually GET, backing a dropdown used across
    // several dashboards) stay open to every role. Values follow the same
    // convention as RESTRICTED_PREFIXES: Admin is implicitly allowed and
    // never needs to be listed; an empty list means Admin-only.
    //
    // Raw material create/edit/delete used to be Inventory-Manager-allowed
    // directly; now that those changes go through an approval request
    // instead (see ApprovalRequestController), only Admin can call the raw
    // endpoints directly - the same operations the approval flow itself
    // performs once a request is approved.
    private static final Map<String, List<String>> METHOD_PATH_RESTRICTIONS = Map.of(
            "POST /api/users", List.of(),
            "POST /api/raw-materials", List.of(),
            "PUT /api/raw-materials/", List.of(),
            "DELETE /api/raw-materials/", List.of(),
            "POST /api/approval-requests", List.of("Inventory Manager")
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String role = request.getHeader("X-User-Role");
        String methodPath = request.getMethod() + " " + path;

        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            chain.doFilter(request, response);
            return;
        }

        // Approving/rejecting a request is Admin-only. Checked ahead of the
        // generic map below since these paths share the "POST
        // /api/approval-requests" prefix with plain submission (which is
        // Inventory-Manager-allowed) — the /{id}/approve and /{id}/reject
        // suffixes can't be told apart from that shared prefix alone.
        boolean isApprovalDecision = "POST".equals(request.getMethod())
                && path.startsWith("/api/approval-requests/")
                && (path.endsWith("/approve") || path.endsWith("/reject"));
        if (isApprovalDecision && !"Admin".equals(role)) {
            forbidden(response);
            return;
        }

        String matchedMethodPath = isApprovalDecision ? null : METHOD_PATH_RESTRICTIONS.keySet().stream()
                .filter(methodPath::startsWith)
                .findFirst()
                .orElse(null);

        if (matchedMethodPath != null) {
            List<String> allowedRoles = METHOD_PATH_RESTRICTIONS.get(matchedMethodPath);
            boolean allowed = "Admin".equals(role) || (role != null && allowedRoles.contains(role));
            if (!allowed) {
                forbidden(response);
                return;
            }
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
