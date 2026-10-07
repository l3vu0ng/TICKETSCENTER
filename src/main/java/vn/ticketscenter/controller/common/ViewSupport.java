package vn.ticketscenter.controller.common;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/**
 * Helpers for forwarding to JSP views under WEB-INF.
 * Only allows an explicit allowlist of paths to prevent path traversal.
 * Owner: Khánh (KHANH-11)
 */
public final class ViewSupport {

    /** All JSP paths allowed for forwarding. Add new paths here as new JSPs are created. */
    private static final Set<String> ALLOWED_VIEWS = Set.of(
            "/WEB-INF/views/common/error.jsp",
            "/WEB-INF/views/identity/auth.jsp",
            "/WEB-INF/views/identity/profile.jsp",
            "/WEB-INF/views/identity/organization-requests.jsp"
    );

    private ViewSupport() {}

    /**
     * Forward to a JSP view under WEB-INF. Sets common request attributes.
     *
     * @param req       the HTTP request
     * @param resp      the HTTP response
     * @param viewPath  absolute path like "/WEB-INF/views/identity/auth.jsp"
     * @param viewModel model object bound to request attribute "viewModel"
     */
    public static void forward(HttpServletRequest req, HttpServletResponse resp,
                               String viewPath, Object viewModel)
            throws ServletException, IOException {
        if (!ALLOWED_VIEWS.contains(viewPath)) {
            throw new IllegalArgumentException("View not in allowlist: " + viewPath);
        }
        req.setAttribute("viewModel", viewModel);
        RequestDispatcher dispatcher = req.getRequestDispatcher(viewPath);
        dispatcher.forward(req, resp);
    }
}
